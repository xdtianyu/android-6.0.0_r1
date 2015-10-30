/* Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */



#include <netlink/genl/genl.h>
#include <netlink/genl/family.h>
#include <netlink/genl/ctrl.h>
#include <linux/rtnetlink.h>
#include <netinet/in.h>
#include "wifiloggercmd.h"
#include "wifilogger_event_defs.h"
#include "wifilogger_diag.h"
#include "wifilogger_vendor_tag_defs.h"
#include "pkt_stats.h"

#define RING_BUF_ENTRY_SIZE 512
#define MAX_CONNECTIVITY_EVENTS 15 // should match the value in wifi_logger.h
static event_remap_t events[MAX_CONNECTIVITY_EVENTS] = {
    {WLAN_PE_DIAG_ASSOC_REQ_EVENT, WIFI_EVENT_ASSOCIATION_REQUESTED},
    {WLAN_PE_DIAG_AUTH_COMP_EVENT, WIFI_EVENT_AUTH_COMPLETE},
    {WLAN_PE_DIAG_ASSOC_COMP_EVENT, WIFI_EVENT_ASSOC_COMPLETE},
    {WLAN_PE_DIAG_AUTH_START_EVENT, WIFI_EVENT_FW_AUTH_STARTED},
    {WLAN_PE_DIAG_ASSOC_START_EVENT, WIFI_EVENT_FW_ASSOC_STARTED},
    {WLAN_PE_DIAG_REASSOC_START_EVENT, WIFI_EVENT_FW_RE_ASSOC_STARTED},
    {WLAN_PE_DIAG_SCAN_REQ_EVENT, WIFI_EVENT_DRIVER_SCAN_REQUESTED},
    {WLAN_PE_DIAG_SCAN_RES_FOUND_EVENT, WIFI_EVENT_DRIVER_SCAN_RESULT_FOUND},
    {WLAN_PE_DIAG_SCAN_COMP_EVENT, WIFI_EVENT_DRIVER_SCAN_COMPLETE},
    {WLAN_PE_DIAG_DISASSOC_REQ_EVENT, WIFI_EVENT_DISASSOCIATION_REQUESTED},
    {WLAN_PE_DIAG_ASSOC_REQ_EVENT, WIFI_EVENT_RE_ASSOCIATION_REQUESTED},
    {WLAN_PE_DIAG_ROAM_AUTH_START_EVENT, WIFI_EVENT_ROAM_AUTH_STARTED},
    {WLAN_PE_DIAG_ROAM_AUTH_COMP_EVENT, WIFI_EVENT_ROAM_AUTH_COMPLETE},
    {WLAN_PE_DIAG_ROAM_ASSOC_START_EVENT, WIFI_EVENT_ROAM_ASSOC_STARTED},
    {WLAN_PE_DIAG_ROAM_ASSOC_COMP_EVENT, WIFI_EVENT_ROAM_ASSOC_COMPLETE},
};

tlv_log* addLoggerTlv(u16 type, u16 length, u8* value, tlv_log *pOutTlv)
{

   pOutTlv->tag = type;
   pOutTlv->length = length;
   memcpy(&pOutTlv->value[0], value, length);

   return((tlv_log *)((u8 *)pOutTlv + sizeof(tlv_log) + length));
}

int add_reason_code_tag(tlv_log **tlvs, u16 reason_code)
{
    *tlvs = addLoggerTlv(WIFI_TAG_REASON_CODE, sizeof(u16),
                        (u8 *)&reason_code, *tlvs);
    return (sizeof(tlv_log) + sizeof(u16));
}

int add_status_tag(tlv_log **tlvs, int status)
{
    *tlvs = addLoggerTlv(WIFI_TAG_STATUS, sizeof(int),
                        (u8 *)&status, *tlvs);
    return (sizeof(tlv_log) + sizeof(int));
}

static wifi_error update_connectivity_ring_buf(hal_info *info,
                                               wifi_ring_buffer_entry *rbe,
                                               u32 size)
{
    struct timeval time;
    u32 total_length = size + sizeof(wifi_ring_buffer_entry);

    rbe->entry_size = size;
    rbe->flags = RING_BUFFER_ENTRY_FLAGS_HAS_BINARY |
                              RING_BUFFER_ENTRY_FLAGS_HAS_TIMESTAMP;
    rbe->type = ENTRY_TYPE_CONNECT_EVENT;
    gettimeofday(&time,NULL);
    rbe->timestamp = time.tv_usec + time.tv_sec * 1000 * 1000;

    /* Write if verbose level and handler are set */
    if (info->rb_infos[CONNECTIVITY_EVENTS_RB_ID].verbose_level >= 1 &&
        info->on_ring_buffer_data) {
        return ring_buffer_write(&info->rb_infos[CONNECTIVITY_EVENTS_RB_ID],
                      (u8*)rbe, total_length, 1);
    } else {
        return WIFI_ERROR_NOT_AVAILABLE;
    }

    return WIFI_SUCCESS;
}

static wifi_error process_bt_coex_scan_event(hal_info *info,
                                             u32 id, u8* buf, int length)
{
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    tlv_log *pTlv;
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    wifi_error status;

    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);
    pTlv = &pConnectEvent->tlvs[0];

    if (id == EVENT_WLAN_BT_COEX_BT_SCAN_START) {
        wlan_bt_coex_bt_scan_start_payload_type *pBtScanStart;
        bt_coex_bt_scan_start_vendor_data_t btScanStartVenData;

        pConnectEvent->event = WIFI_EVENT_BT_COEX_BT_SCAN_START;

        pBtScanStart = (wlan_bt_coex_bt_scan_start_payload_type *)buf;
        btScanStartVenData.scan_type = pBtScanStart->scan_type;
        btScanStartVenData.scan_bitmap = pBtScanStart->scan_bitmap;

        pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                            sizeof(bt_coex_bt_scan_start_vendor_data_t),
                            (u8 *)&btScanStartVenData, pTlv);
        tot_len += sizeof(tlv_log) +
                   sizeof(bt_coex_bt_scan_start_vendor_data_t);
    } else if(id == EVENT_WLAN_BT_COEX_BT_SCAN_STOP) {
        wlan_bt_coex_bt_scan_stop_payload_type *pBtScanStop;
        bt_coex_bt_scan_stop_vendor_data_t btScanStopVenData;

        pConnectEvent->event = WIFI_EVENT_BT_COEX_BT_SCAN_STOP;

        pBtScanStop = (wlan_bt_coex_bt_scan_stop_payload_type *)buf;
        btScanStopVenData.scan_type = pBtScanStop->scan_type;
        btScanStopVenData.scan_bitmap = pBtScanStop->scan_bitmap;

        pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                            sizeof(bt_coex_bt_scan_stop_vendor_data_t),
                            (u8 *)&btScanStopVenData, pTlv);
        tot_len += sizeof(tlv_log) + sizeof(bt_coex_bt_scan_stop_vendor_data_t);
    }
    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write bt_coex_scan event into ring buffer");
    }

    return status;
}

static wifi_error process_bt_coex_event(hal_info *info, u32 id,
                                        u8* buf, int length)
{
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    tlv_log *pTlv;
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    u8 link_id, link_state, link_role, link_type = 0, Rsco = 0;
    u16 Tsco = 0;
    wifi_error status;
    bt_coex_hid_vendor_data_t btCoexHidVenData;

    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);

    switch (id) {
        case EVENT_WLAN_BT_COEX_BT_SCO_START:
        {
            wlan_bt_coex_bt_sco_start_payload_type *pBtCoexStartPL;
            pBtCoexStartPL = (wlan_bt_coex_bt_sco_start_payload_type *)buf;

            link_id = pBtCoexStartPL->link_id;
            link_state = pBtCoexStartPL->link_state;
            link_role = pBtCoexStartPL->link_role;
            link_type = pBtCoexStartPL->link_type;
            Tsco = pBtCoexStartPL->Tsco;
            Rsco = pBtCoexStartPL->Rsco;

            pConnectEvent->event = WIFI_EVENT_BT_COEX_BT_SCO_START;
        }
        break;
        case EVENT_WLAN_BT_COEX_BT_SCO_STOP:
        {
            wlan_bt_coex_bt_sco_stop_payload_type *pBtCoexStopPL;
            pBtCoexStopPL = (wlan_bt_coex_bt_sco_stop_payload_type *)buf;

            link_id = pBtCoexStopPL->link_id;
            link_state = pBtCoexStopPL->link_state;
            link_role = pBtCoexStopPL->link_role;
            link_type = pBtCoexStopPL->link_type;
            Tsco = pBtCoexStopPL->Tsco;
            Rsco = pBtCoexStopPL->Rsco;

            pConnectEvent->event = WIFI_EVENT_BT_COEX_BT_SCO_STOP;
        }
        break;
        case EVENT_WLAN_BT_COEX_BT_HID_START:
        {
            wlan_bt_coex_bt_hid_start_payload_type *pBtCoexHidStartPL;
            pBtCoexHidStartPL = (wlan_bt_coex_bt_hid_start_payload_type *)buf;

            link_id = pBtCoexHidStartPL->link_id;
            link_state = pBtCoexHidStartPL->link_state;
            link_role = pBtCoexHidStartPL->link_role;
            btCoexHidVenData.Tsniff = pBtCoexHidStartPL->Tsniff;
            btCoexHidVenData.attempts = pBtCoexHidStartPL->attempts;

            pConnectEvent->event = WIFI_EVENT_BT_COEX_BT_HID_START;
        }
        break;
        case EVENT_WLAN_BT_COEX_BT_HID_STOP:
        {
            wlan_bt_coex_bt_hid_stop_payload_type *pBtCoexHidStopPL;
            pBtCoexHidStopPL = (wlan_bt_coex_bt_hid_stop_payload_type *)buf;

            link_id = pBtCoexHidStopPL->link_id;
            link_state = pBtCoexHidStopPL->link_state;
            link_role = pBtCoexHidStopPL->link_role;
            btCoexHidVenData.Tsniff = pBtCoexHidStopPL->Tsniff;
            btCoexHidVenData.attempts = pBtCoexHidStopPL->attempts;

            pConnectEvent->event = WIFI_EVENT_BT_COEX_BT_HID_STOP;
        }
        break;
        default:
            return WIFI_SUCCESS;
    }

    pTlv = &pConnectEvent->tlvs[0];
    pTlv = addLoggerTlv(WIFI_TAG_LINK_ID, sizeof(link_id), &link_id, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(link_id);

    pTlv = addLoggerTlv(WIFI_TAG_LINK_ROLE, sizeof(link_role),
                        &link_role, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(link_role);

    pTlv = addLoggerTlv(WIFI_TAG_LINK_STATE, sizeof(link_state),
                        &link_state, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(link_state);

    if ((pConnectEvent->event == EVENT_WLAN_BT_COEX_BT_SCO_START) ||
        (pConnectEvent->event == EVENT_WLAN_BT_COEX_BT_SCO_STOP)) {
        pTlv = addLoggerTlv(WIFI_TAG_LINK_TYPE, sizeof(link_type),
                            &link_type, pTlv);
        tot_len += sizeof(tlv_log) + sizeof(link_type);

        pTlv = addLoggerTlv(WIFI_TAG_TSCO, sizeof(Tsco), (u8 *)&Tsco, pTlv);
        tot_len += sizeof(tlv_log) + sizeof(Tsco);

        pTlv = addLoggerTlv(WIFI_TAG_RSCO, sizeof(Rsco), &Rsco, pTlv);
        tot_len += sizeof(tlv_log) + sizeof(Rsco);
    } else if ((pConnectEvent->event == EVENT_WLAN_BT_COEX_BT_HID_START) ||
               (pConnectEvent->event == EVENT_WLAN_BT_COEX_BT_HID_STOP)) {
        pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                            sizeof(bt_coex_hid_vendor_data_t),
                            (u8 *)&btCoexHidVenData, pTlv);
        tot_len += sizeof(tlv_log) + sizeof(bt_coex_hid_vendor_data_t);
    }

    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write bt_coex_event into ring buffer");
    }

    return status;
}

static wifi_error process_extscan_event(hal_info *info, u32 id,
                                        u8* buf, int length)
{
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    tlv_log *pTlv;
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    wifi_error status;

    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);
    pTlv = &pConnectEvent->tlvs[0];

    switch (id) {
    case EVENT_WLAN_EXTSCAN_CYCLE_STARTED:
        {
            ext_scan_cycle_vendor_data_t extScanCycleVenData;
            wlan_ext_scan_cycle_started_payload_type *pExtScanCycleStarted;
            pConnectEvent->event = WIFI_EVENT_G_SCAN_CYCLE_STARTED;
            pExtScanCycleStarted =
                           (wlan_ext_scan_cycle_started_payload_type *)buf;
            pTlv = addLoggerTlv(WIFI_TAG_SCAN_ID, sizeof(u32),
                            (u8 *)&pExtScanCycleStarted->scan_id, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(u32);

            extScanCycleVenData.timer_tick = pExtScanCycleStarted->timer_tick;
            extScanCycleVenData.scheduled_bucket_mask =
                                    pExtScanCycleStarted->scheduled_bucket_mask;
            extScanCycleVenData.scan_cycle_count =
                                         pExtScanCycleStarted->scan_cycle_count;

            pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                                sizeof(ext_scan_cycle_vendor_data_t),
                                (u8 *)&extScanCycleVenData, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(ext_scan_cycle_vendor_data_t);
        }
        break;
    case EVENT_WLAN_EXTSCAN_CYCLE_COMPLETED:
        {
            ext_scan_cycle_vendor_data_t extScanCycleVenData;
            wlan_ext_scan_cycle_completed_payload_type *pExtScanCycleCompleted;
            pConnectEvent->event = WIFI_EVENT_G_SCAN_CYCLE_COMPLETED;
            pExtScanCycleCompleted =
            (wlan_ext_scan_cycle_completed_payload_type *)buf;
            pTlv = addLoggerTlv(WIFI_TAG_SCAN_ID, sizeof(u32),
                            (u8 *)&pExtScanCycleCompleted->scan_id, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(u32);

            extScanCycleVenData.timer_tick = pExtScanCycleCompleted->timer_tick;
            extScanCycleVenData.scheduled_bucket_mask =
                                  pExtScanCycleCompleted->scheduled_bucket_mask;
            extScanCycleVenData.scan_cycle_count =
                                       pExtScanCycleCompleted->scan_cycle_count;

            pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                                sizeof(ext_scan_cycle_vendor_data_t),
                                (u8 *)&extScanCycleVenData, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(ext_scan_cycle_vendor_data_t);
        }
        break;
    case EVENT_WLAN_EXTSCAN_BUCKET_STARTED:
        {
            wlan_ext_scan_bucket_started_payload_type *pExtScanBucketStarted;
            u32 bucket_id;
            pConnectEvent->event = WIFI_EVENT_G_SCAN_BUCKET_STARTED;
            pExtScanBucketStarted =
                            (wlan_ext_scan_bucket_started_payload_type *)buf;
            bucket_id = (u32)pExtScanBucketStarted->bucket_id;
            pTlv = addLoggerTlv(WIFI_TAG_BUCKET_ID, sizeof(u32),
                                (u8 *)&bucket_id, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(u32);
        }
        break;
    case EVENT_WLAN_EXTSCAN_BUCKET_COMPLETED:
        {
            wlan_ext_scan_bucket_completed_payload_type *pExtScanBucketCmpleted;
            u32 bucket_id;
            pConnectEvent->event = WIFI_EVENT_G_SCAN_BUCKET_COMPLETED;
            pExtScanBucketCmpleted =
                            (wlan_ext_scan_bucket_completed_payload_type *)buf;
            bucket_id = (u32)pExtScanBucketCmpleted->bucket_id;
            pTlv = addLoggerTlv(WIFI_TAG_BUCKET_ID, sizeof(u32),
                                (u8 *)&bucket_id, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(u32);
        }
        break;
    case EVENT_WLAN_EXTSCAN_FEATURE_STOP:
        {
            wlan_ext_scan_feature_stop_payload_type *pExtScanStop;
            pConnectEvent->event = WIFI_EVENT_G_SCAN_STOP;
            pExtScanStop = (wlan_ext_scan_feature_stop_payload_type *)buf;
            pTlv = addLoggerTlv(WIFI_TAG_REQUEST_ID,
                                sizeof(wlan_ext_scan_feature_stop_payload_type),
                                (u8 *)&pExtScanStop, pTlv);
            tot_len += sizeof(tlv_log) +
                       sizeof(wlan_ext_scan_feature_stop_payload_type);
        }
        break;
    case EVENT_WLAN_EXTSCAN_RESULTS_AVAILABLE:
        {
            wlan_ext_scan_results_available_payload_type *pExtScanResultsAvail;
            ext_scan_results_available_vendor_data_t extScanResultsAvailVenData;
            u32 request_id;
            pConnectEvent->event = WIFI_EVENT_G_SCAN_RESULTS_AVAILABLE;
            pExtScanResultsAvail =
                          (wlan_ext_scan_results_available_payload_type *)buf;
            request_id = pExtScanResultsAvail->request_id;
            pTlv = addLoggerTlv(WIFI_TAG_REQUEST_ID, sizeof(u32),
                          (u8 *)&request_id, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(u32);

            extScanResultsAvailVenData.table_type =
                                               pExtScanResultsAvail->table_type;
            extScanResultsAvailVenData.entries_in_use =
                                           pExtScanResultsAvail->entries_in_use;
            extScanResultsAvailVenData.maximum_entries =
                                          pExtScanResultsAvail->maximum_entries;
            extScanResultsAvailVenData.scan_count_after_getResults =
                              pExtScanResultsAvail->scan_count_after_getResults;
            extScanResultsAvailVenData.threshold_num_scans =
                                      pExtScanResultsAvail->threshold_num_scans;

            pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                              sizeof(ext_scan_results_available_vendor_data_t),
                                (u8 *)&extScanResultsAvailVenData, pTlv);
            tot_len += sizeof(tlv_log) +
                       sizeof(ext_scan_results_available_vendor_data_t);
        }
        break;
    }

    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write ext_scan event into ring buffer");
    }

    return status;
}

static wifi_error process_addba_success_event(hal_info *info,
                                      u8* buf, int length)
{
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    tlv_log *pTlv;
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    wlan_add_block_ack_success_payload_type *pAddBASuccess;
    addba_success_vendor_data_t addBASuccessVenData;
    wifi_error status;

    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);
    pAddBASuccess = (wlan_add_block_ack_success_payload_type *)buf;

    addBASuccessVenData.ucBaTid = pAddBASuccess->ucBaTid;
    addBASuccessVenData.ucBaBufferSize = pAddBASuccess->ucBaBufferSize;
    addBASuccessVenData.ucBaSSN = pAddBASuccess->ucBaSSN;
    addBASuccessVenData.fInitiator = pAddBASuccess->fInitiator;

    pConnectEvent->event = WIFI_EVENT_BLOCK_ACK_NEGOTIATION_COMPLETE;
    pTlv = &pConnectEvent->tlvs[0];
    pTlv = addLoggerTlv(WIFI_TAG_ADDR, sizeof(pAddBASuccess->ucBaPeerMac),
                        (u8 *)pAddBASuccess->ucBaPeerMac, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(pAddBASuccess->ucBaPeerMac);

    tot_len += add_status_tag(&pTlv, (int)ADDBA_SUCCESS);

    pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                        sizeof(addba_success_vendor_data_t),
                        (u8 *)&addBASuccessVenData, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(addba_success_vendor_data_t);

    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write addba event into ring buffer");
    }

    return status;
}

static wifi_error process_addba_failed_event(hal_info *info,
                                      u8* buf, int length)
{
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    tlv_log *pTlv;
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    wlan_add_block_ack_failed_payload_type *pAddBAFailed;
    addba_failed_vendor_data_t addBAFailedVenData;
    wifi_error status;

    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);

    pAddBAFailed = (wlan_add_block_ack_failed_payload_type *)buf;
    addBAFailedVenData.ucBaTid = pAddBAFailed->ucBaTid;
    addBAFailedVenData.fInitiator = pAddBAFailed->fInitiator;

    pConnectEvent->event = WIFI_EVENT_BLOCK_ACK_NEGOTIATION_COMPLETE;
    pTlv = &pConnectEvent->tlvs[0];
    pTlv = addLoggerTlv(WIFI_TAG_ADDR, sizeof(pAddBAFailed->ucBaPeerMac),
                        (u8 *)pAddBAFailed->ucBaPeerMac, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(pAddBAFailed->ucBaPeerMac);

    tot_len += add_status_tag(&pTlv, (int)ADDBA_FAILURE);

    tot_len += add_reason_code_tag(&pTlv, (u16)pAddBAFailed->ucReasonCode);

    pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                        sizeof(addba_failed_vendor_data_t),
                        (u8 *)&pAddBAFailed, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(addba_failed_vendor_data_t);

    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write addba event into ring buffer");
    }

    return status;
}

static wifi_error process_roam_event(hal_info *info, u32 id,
                                     u8* buf, int length)
{
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    tlv_log *pTlv;
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    wifi_error status;

    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);

    switch (id)
    {
    case EVENT_WLAN_ROAM_SCAN_STARTED:
        {
            wlan_roam_scan_started_payload_type *pRoamScanStarted;
            roam_scan_started_vendor_data_t roamScanStartedVenData;
            pConnectEvent->event = WIFI_EVENT_ROAM_SCAN_STARTED;
            pRoamScanStarted = (wlan_roam_scan_started_payload_type *)buf;
            pTlv = &pConnectEvent->tlvs[0];
            pTlv = addLoggerTlv(WIFI_TAG_SCAN_ID,
                                sizeof(pRoamScanStarted->scan_id),
                                (u8 *)&pRoamScanStarted->scan_id, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(pRoamScanStarted->scan_id);
            roamScanStartedVenData.roam_scan_flags =
                                              pRoamScanStarted->roam_scan_flags;
            roamScanStartedVenData.cur_rssi = pRoamScanStarted->cur_rssi;
            memcpy(roamScanStartedVenData.scan_params,
                   pRoamScanStarted->scan_params,
                   sizeof(roamScanStartedVenData.scan_params));
            memcpy(roamScanStartedVenData.scan_channels,
                   pRoamScanStarted->scan_channels,
                   sizeof(roamScanStartedVenData.scan_channels));
            pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                                sizeof(roam_scan_started_vendor_data_t),
                                (u8 *)&roamScanStartedVenData, pTlv);
            tot_len += sizeof(tlv_log) +
                       sizeof(roam_scan_started_vendor_data_t);
        }
        break;
    case EVENT_WLAN_ROAM_SCAN_COMPLETE:
        {
            wlan_roam_scan_complete_payload_type *pRoamScanComplete;
            roam_scan_complete_vendor_data_t roamScanCompleteVenData;
            pConnectEvent->event = WIFI_EVENT_ROAM_SCAN_COMPLETE;
            pRoamScanComplete = (wlan_roam_scan_complete_payload_type *)buf;
            pTlv = &pConnectEvent->tlvs[0];

            pTlv = addLoggerTlv(WIFI_TAG_SCAN_ID,
                                sizeof(pRoamScanComplete->scan_id),
                                (u8 *)&pRoamScanComplete->scan_id, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(pRoamScanComplete->scan_id);

            roamScanCompleteVenData.reason = pRoamScanComplete->reason;
            roamScanCompleteVenData.completion_flags =
                                            pRoamScanComplete->completion_flags;
            roamScanCompleteVenData.num_candidate =
                                               pRoamScanComplete->num_candidate;
            roamScanCompleteVenData.flags = pRoamScanComplete->flags;

            pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                                sizeof(roam_scan_complete_vendor_data_t),
                                (u8 *)&roamScanCompleteVenData, pTlv);
            tot_len += sizeof(tlv_log) +
                       sizeof(roam_scan_complete_vendor_data_t);
        }
        break;
    case EVENT_WLAN_ROAM_CANDIDATE_FOUND:
        {
            wlan_roam_candidate_found_payload_type *pRoamCandidateFound;
            roam_candidate_found_vendor_data_t roamCandidateFoundVendata;
            pConnectEvent->event = WIFI_EVENT_ROAM_CANDIDATE_FOUND;
            pRoamCandidateFound = (wlan_roam_candidate_found_payload_type *)buf;
            pTlv = &pConnectEvent->tlvs[0];
            pTlv = addLoggerTlv(WIFI_TAG_CHANNEL,
                                sizeof(pRoamCandidateFound->channel),
                                (u8 *)&pRoamCandidateFound->channel, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(pRoamCandidateFound->channel);

            pTlv = addLoggerTlv(WIFI_TAG_RSSI,
                                sizeof(pRoamCandidateFound->rssi),
                                (u8 *)&pRoamCandidateFound->rssi, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(pRoamCandidateFound->rssi);

            pTlv = addLoggerTlv(WIFI_TAG_BSSID,
                                sizeof(pRoamCandidateFound->bssid),
                                (u8 *)pRoamCandidateFound->bssid, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(pRoamCandidateFound->bssid);

            pTlv = addLoggerTlv(WIFI_TAG_SSID,
                                sizeof(pRoamCandidateFound->ssid),
                                (u8 *)pRoamCandidateFound->ssid, pTlv);
            tot_len += sizeof(tlv_log) + sizeof(pRoamCandidateFound->ssid);

            roamCandidateFoundVendata.auth_mode =
                                   pRoamCandidateFound->auth_mode;
            roamCandidateFoundVendata.ucast_cipher =
                                         pRoamCandidateFound->ucast_cipher;
            roamCandidateFoundVendata.mcast_cipher =
                                         pRoamCandidateFound->mcast_cipher;
            pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                                sizeof(roam_candidate_found_vendor_data_t),
                                (u8 *)&roamCandidateFoundVendata, pTlv);
            tot_len += sizeof(tlv_log) +
                       sizeof(roam_candidate_found_vendor_data_t);
        }
        break;
        case EVENT_WLAN_ROAM_SCAN_CONFIG:
        {
            wlan_roam_scan_config_payload_type *pRoamScanConfig;
            roam_scan_config_vendor_data_t roamScanConfigVenData;

            pConnectEvent->event = WIFI_EVENT_ROAM_SCAN_CONFIG;
            pRoamScanConfig = (wlan_roam_scan_config_payload_type *)buf;

            pTlv = &pConnectEvent->tlvs[0];

            roamScanConfigVenData.flags = pRoamScanConfig->flags;
            memcpy(roamScanConfigVenData.roam_scan_config,
                   pRoamScanConfig->roam_scan_config,
                   sizeof(roamScanConfigVenData.roam_scan_config));

            pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                                sizeof(roam_scan_config_vendor_data_t),
                                (u8 *)&roamScanConfigVenData, pTlv);
            tot_len += sizeof(tlv_log) +
                       sizeof(roam_scan_config_vendor_data_t);
        }
        break;
    }

    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write roam event into ring buffer");
    }

    return status;
}

wifi_error process_firmware_prints(hal_info *info, u8 *buf, u16 length)
{
    wifi_ring_buffer_entry rb_entry_hdr;
    struct timeval time;
    wifi_error status;

    rb_entry_hdr.entry_size = length;
    rb_entry_hdr.flags = RING_BUFFER_ENTRY_FLAGS_HAS_TIMESTAMP;
    rb_entry_hdr.type = ENTRY_TYPE_DATA;
    gettimeofday(&time, NULL);
    rb_entry_hdr.timestamp = time.tv_usec + time.tv_sec * 1000 * 1000;

    /* Write if verbose and handler is set */
    if (info->rb_infos[FIRMWARE_PRINTS_RB_ID].verbose_level >= 1 &&
        info->on_ring_buffer_data) {
        /* Write header and payload separately to avoid
         * complete payload memcpy */
        status = ring_buffer_write(&info->rb_infos[FIRMWARE_PRINTS_RB_ID],
                                   (u8*)&rb_entry_hdr,
                                   sizeof(wifi_ring_buffer_entry), 0);
        if (status != WIFI_SUCCESS) {
            ALOGE("Failed to write firmware prints rb header %d", status);
            return status;
        }
        status = ring_buffer_write(&info->rb_infos[FIRMWARE_PRINTS_RB_ID],
                                   buf, length, 1);
        if (status != WIFI_SUCCESS) {
            ALOGE("Failed to write firmware prints rb payload %d", status);
            return status;
        }
    } else {
        return WIFI_ERROR_NOT_AVAILABLE;
    }

    return WIFI_SUCCESS;
}

static wifi_error process_fw_diag_msg(hal_info *info, u8* buf, u16 length)
{
    u16 count = 0, id, payloadlen;
    wifi_error status;
    fw_diag_msg_hdr_t *diag_msg_hdr;

    buf += 4;
    length -= 4;

    while (length > (count + sizeof(fw_diag_msg_hdr_t))) {
        diag_msg_hdr = (fw_diag_msg_hdr_t *)(buf + count);

        id = diag_msg_hdr->diag_id;
        payloadlen = diag_msg_hdr->u.payload_len;

        switch (diag_msg_hdr->diag_event_type) {
            case WLAN_DIAG_TYPE_EVENT:
            {
                switch (id) {
                    case EVENT_WLAN_BT_COEX_BT_SCO_START:
                    case EVENT_WLAN_BT_COEX_BT_SCO_STOP:
                    case EVENT_WLAN_BT_COEX_BT_HID_START:
                    case EVENT_WLAN_BT_COEX_BT_HID_STOP:
                        status = process_bt_coex_event(info, id,
                                                       diag_msg_hdr->payload,
                                                       payloadlen);
                        if (status != WIFI_SUCCESS) {
                            ALOGE("Failed to process bt_coex event");
                            return status;
                        }
                        break;
                    case EVENT_WLAN_BT_COEX_BT_SCAN_START:
                    case EVENT_WLAN_BT_COEX_BT_SCAN_STOP:
                        status = process_bt_coex_scan_event(info, id,
                                                       diag_msg_hdr->payload,
                                                       payloadlen);
                        if (status != WIFI_SUCCESS) {
                            ALOGE("Failed to process bt_coex_scan event");
                            return status;
                        }
                        break;
                   case EVENT_WLAN_EXTSCAN_CYCLE_STARTED:
                   case EVENT_WLAN_EXTSCAN_CYCLE_COMPLETED:
                   case EVENT_WLAN_EXTSCAN_BUCKET_STARTED:
                   case EVENT_WLAN_EXTSCAN_BUCKET_COMPLETED:
                   case EVENT_WLAN_EXTSCAN_FEATURE_STOP:
                   case EVENT_WLAN_EXTSCAN_RESULTS_AVAILABLE:
                        status = process_extscan_event(info, id,
                                                       diag_msg_hdr->payload,
                                                       payloadlen);
                        if (status != WIFI_SUCCESS) {
                            ALOGE("Failed to process extscan event");
                            return status;
                        }
                        break;
                   case EVENT_WLAN_ROAM_SCAN_STARTED:
                   case EVENT_WLAN_ROAM_SCAN_COMPLETE:
                   case EVENT_WLAN_ROAM_CANDIDATE_FOUND:
                   case EVENT_WLAN_ROAM_SCAN_CONFIG:
                        status = process_roam_event(info, id,
                                                    diag_msg_hdr->payload,
                                                    payloadlen);
                        if (status != WIFI_SUCCESS) {
                            ALOGE("Failed to process roam event");
                            return status;
                        }
                        break;
                   case EVENT_WLAN_ADD_BLOCK_ACK_SUCCESS:
                        status = process_addba_success_event(info,
                                                       diag_msg_hdr->payload,
                                                       payloadlen);
                        if (status != WIFI_SUCCESS) {
                            ALOGE("Failed to process addba success event");
                            return status;
                        }
                        break;
                   case EVENT_WLAN_ADD_BLOCK_ACK_FAILED:
                        status = process_addba_failed_event(info,
                                                      diag_msg_hdr->payload,
                                                      payloadlen);
                        if (status != WIFI_SUCCESS) {
                            ALOGE("Failed to process addba failed event");
                            return status;
                        }
                        break;
                   default:
                        return WIFI_SUCCESS;
                }
            }
            break;
            case WLAN_DIAG_TYPE_LOG:
            {
            }
            break;
            case WLAN_DIAG_TYPE_MSG:
            {
                /* Length field is only one byte for WLAN_DIAG_TYPE_MSG */
                payloadlen = diag_msg_hdr->u.msg_hdr.payload_len;
                process_firmware_prints(info, diag_msg_hdr->payload,
                                        payloadlen);
            }
            break;
            default:
                return WIFI_SUCCESS;
        }
        count += payloadlen + sizeof(fw_diag_msg_hdr_t);
    }
    return WIFI_SUCCESS;
}

static wifi_error remap_event(int in_event, int *out_event)
{
    int i = 0;
    while (i < MAX_CONNECTIVITY_EVENTS) {
        if (events[i].q_event == in_event) {
            *out_event = events[i].g_event;
            return WIFI_SUCCESS;
        }
        i++;
    }
    return WIFI_ERROR_UNKNOWN;
}

static wifi_error process_wlan_pe_event(hal_info *info, u8* buf, int length)
{
    wlan_pe_event_t *pWlanPeEvent;
    pe_event_vendor_data_t peEventVenData;
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    tlv_log *pTlv;
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    wifi_error status;

    pWlanPeEvent = (wlan_pe_event_t *)buf;

    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);

    status = remap_event(pWlanPeEvent->event_type,
                         (int *)&pConnectEvent->event);
    if (status != WIFI_SUCCESS)
        return status;

    pTlv = &pConnectEvent->tlvs[0];
    pTlv = addLoggerTlv(WIFI_TAG_BSSID, sizeof(pWlanPeEvent->bssid),
                        (u8 *)pWlanPeEvent->bssid, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(pWlanPeEvent->bssid);

    tot_len += add_status_tag(&pTlv, (int)pWlanPeEvent->status);

    pTlv = addLoggerTlv(WIFI_TAG_REASON_CODE, sizeof(pWlanPeEvent->reason_code),
                        (u8 *)&pWlanPeEvent->reason_code, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(pWlanPeEvent->reason_code);

    peEventVenData.sme_state = pWlanPeEvent->sme_state;
    peEventVenData.mlm_state = pWlanPeEvent->mlm_state;

    pTlv = addLoggerTlv(WIFI_TAG_VENDOR_SPECIFIC,
                        sizeof(pe_event_vendor_data_t),
                        (u8 *)&peEventVenData, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(pe_event_vendor_data_t);

    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write pe event into ring buffer");
    }

    return status;
}

static wifi_error process_wlan_eapol_event(hal_info *info, u8* buf, int length)
{
    wifi_ring_buffer_driver_connectivity_event *pConnectEvent;
    wlan_eapol_event_t *pWlanEapolEvent;
    wifi_ring_buffer_entry *pRingBufferEntry;
    u8 out_buf[RING_BUF_ENTRY_SIZE];
    int tot_len = sizeof(wifi_ring_buffer_driver_connectivity_event);
    tlv_log *pTlv;
    u32 eapol_msg_type = 0;
    wifi_error status;

    pWlanEapolEvent = (wlan_eapol_event_t *)buf;
    pRingBufferEntry = (wifi_ring_buffer_entry *)&out_buf[0];
    memset(pRingBufferEntry, 0, RING_BUF_ENTRY_SIZE);
    pConnectEvent = (wifi_ring_buffer_driver_connectivity_event *)
                     (pRingBufferEntry + 1);

    if (pWlanEapolEvent->event_sub_type ==
        WLAN_DRIVER_EAPOL_FRAME_TRANSMIT_REQUESTED)
        pConnectEvent->event = WIFI_EVENT_DRIVER_EAPOL_FRAME_TRANSMIT_REQUESTED;
    else
        pConnectEvent->event = WIFI_EVENT_DRIVER_EAPOL_FRAME_RECEIVED;

    pTlv = &pConnectEvent->tlvs[0];

    if ((pWlanEapolEvent->eapol_key_info & EAPOL_MASK) == EAPOL_M1_MASK)
        eapol_msg_type = 1;
    else if ((pWlanEapolEvent->eapol_key_info & EAPOL_MASK) == EAPOL_M2_MASK)
        eapol_msg_type = 2;
    else if ((pWlanEapolEvent->eapol_key_info & EAPOL_MASK) == EAPOL_M3_MASK)
        eapol_msg_type = 3;
    else if ((pWlanEapolEvent->eapol_key_info & EAPOL_MASK) == EAPOL_M4_MASK)
        eapol_msg_type = 4;
    else
        ALOGI("Unknow EAPOL message type \n");
    pTlv = addLoggerTlv(WIFI_TAG_EAPOL_MESSAGE_TYPE, sizeof(u32),
                        (u8 *)&eapol_msg_type, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(u32);
    pTlv = addLoggerTlv(WIFI_TAG_ADDR1, sizeof(pWlanEapolEvent->dest_addr),
                        (u8 *)pWlanEapolEvent->dest_addr, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(pWlanEapolEvent->dest_addr);
    pTlv = addLoggerTlv(WIFI_TAG_ADDR2, sizeof(pWlanEapolEvent->src_addr),
                        (u8 *)pWlanEapolEvent->src_addr, pTlv);
    tot_len += sizeof(tlv_log) + sizeof(pWlanEapolEvent->src_addr);

    status = update_connectivity_ring_buf(info, pRingBufferEntry, tot_len);
    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write eapol event into ring buffer");
    }

    return status;
}

static wifi_error process_wakelock_event(hal_info *info, u8* buf, int length)
{
    wlan_wake_lock_event_t *pWlanWakeLockEvent;
    wake_lock_event *pWakeLockEvent;
    wifi_power_event *pPowerEvent;
    tlv_log *pTlv;
    wifi_ring_buffer_entry *pRingBufferEntry;
    u16 len_ring_buffer_entry;
    struct timeval time;
    wifi_error status;
    u8 wl_ring_buffer[RING_BUF_ENTRY_SIZE];
    u16 entry_size;

    pWlanWakeLockEvent = (wlan_wake_lock_event_t *)(buf);
    entry_size = sizeof(wifi_power_event) +
                 sizeof(tlv_log) +
                 sizeof(wake_lock_event) +
                 pWlanWakeLockEvent->name_len + 1;
    len_ring_buffer_entry = sizeof(wifi_ring_buffer_entry) + entry_size;

    if (len_ring_buffer_entry > RING_BUF_ENTRY_SIZE) {
        pRingBufferEntry = (wifi_ring_buffer_entry *)malloc(
                len_ring_buffer_entry);
        if (pRingBufferEntry == NULL) {
            ALOGE("%s: Failed to allocate memory", __FUNCTION__);
            return WIFI_ERROR_OUT_OF_MEMORY;
        }
    } else {
        pRingBufferEntry = (wifi_ring_buffer_entry *)wl_ring_buffer;
    }

    pPowerEvent = (wifi_power_event *)(pRingBufferEntry + 1);
    pPowerEvent->event = WIFI_TAG_WAKE_LOCK_EVENT;

    pTlv = &pPowerEvent->tlvs[0];
    pTlv->tag = WIFI_TAG_WAKE_LOCK_EVENT;
    pTlv->length = sizeof(wake_lock_event) +
                   pWlanWakeLockEvent->name_len + 1;

    pWakeLockEvent = (wake_lock_event *)pTlv->value;
    pWakeLockEvent->status = pWlanWakeLockEvent->status;
    pWakeLockEvent->reason = pWlanWakeLockEvent->reason;
    memcpy(pWakeLockEvent->name, pWlanWakeLockEvent->name,
           pWlanWakeLockEvent->name_len);

    pRingBufferEntry->entry_size = entry_size;
    pRingBufferEntry->flags = RING_BUFFER_ENTRY_FLAGS_HAS_BINARY |
                              RING_BUFFER_ENTRY_FLAGS_HAS_TIMESTAMP;
    pRingBufferEntry->type = ENTRY_TYPE_POWER_EVENT;
    gettimeofday(&time, NULL);
    pRingBufferEntry->timestamp = time.tv_usec + time.tv_sec * 1000 * 1000;

    /* Write if verbose and handler is set */
    if (info->rb_infos[POWER_EVENTS_RB_ID].verbose_level >= 1 &&
        info->on_ring_buffer_data) {
        status = ring_buffer_write(&info->rb_infos[POWER_EVENTS_RB_ID],
                                   (u8*)pRingBufferEntry,
                                   len_ring_buffer_entry, 1);
    } else {
        status = WIFI_ERROR_NOT_AVAILABLE;
    }

    if ((u8 *)pRingBufferEntry != wl_ring_buffer) {
        ALOGI("Message with more than RING_BUF_ENTRY_SIZE");
        free(pRingBufferEntry);
    }

    return status;
}

static void process_wlan_log_complete_event(hal_info *info,
                                                  u8* buf,
                                                  int length)
{
    wlan_log_complete_event_t *lfd_event;

    ALOGV("Received log completion event from driver");
    lfd_event = (wlan_log_complete_event_t *)buf;

    push_out_all_ring_buffers(info);

    if (lfd_event->is_fatal == WLAN_LOG_TYPE_FATAL) {
        ALOGE("Received fatal event, sending alert");
        send_alert(info, lfd_event->reason_code);
    }
}

static wifi_error update_stats_to_ring_buf(hal_info *info,
                      u8 *rb_entry, u32 size)
{
    int num_records = 1;
    wifi_ring_buffer_entry *pRingBufferEntry =
        (wifi_ring_buffer_entry *)rb_entry;
    struct timeval time;

    pRingBufferEntry->entry_size = size - sizeof(wifi_ring_buffer_entry);
    pRingBufferEntry->flags = RING_BUFFER_ENTRY_FLAGS_HAS_BINARY |
                              RING_BUFFER_ENTRY_FLAGS_HAS_TIMESTAMP;
    pRingBufferEntry->type = ENTRY_TYPE_PKT;
    gettimeofday(&time,NULL);
    pRingBufferEntry->timestamp = time.tv_usec + time.tv_sec * 1000 * 1000;

    // Write if verbose and handler is set
    if ((info->rb_infos[PKT_STATS_RB_ID].verbose_level >= VERBOSE_DEBUG_PROBLEM)
        && info->on_ring_buffer_data) {
        ring_buffer_write(&info->rb_infos[PKT_STATS_RB_ID],
                          (u8*)pRingBufferEntry,
                          size,
                          num_records);
    } else {
        return WIFI_ERROR_NOT_AVAILABLE;
    }

    return WIFI_SUCCESS;
}

static u16 get_rate(u16 mcs_r, u8 short_gi)
{
    u16 tx_rate = 0;
    MCS mcs;
    static u16 rate_lookup[][8] = {{96, 48, 24, 12, 108, 72, 36, 18},
                            {22, 11,  4,  2,  22, 11,  4,  0}};
    static u16 MCS_rate_lookup_ht[][8] =
                                  {{ 13,  14,  27,  30,  59,  65,  117,  130},
                                   { 26,  29,  54,  60, 117, 130,  234,  260},
                                   { 39,  43,  81,  90, 176, 195,  351,  390},
                                   { 52,  58, 108, 120, 234, 260,  468,  520},
                                   { 78,  87, 162, 180, 351, 390,  702,  780},
                                   {104, 116, 216, 240, 468, 520,  936, 1040},
                                   {117, 130, 243, 270, 527, 585, 1053, 1170},
                                   {130, 144, 270, 300, 585, 650, 1170, 1300},
                                   {156, 173, 324, 360, 702, 780, 1404, 1560},
                                   {  0,   0, 360, 400, 780, 867, 1560, 1733},
                                   { 26,  29,  54,  60, 117, 130,  234,  260},
                                   { 52,  58, 108, 120, 234, 260,  468,  520},
                                   { 78,  87, 162, 180, 351, 390,  702,  780},
                                   {104, 116, 216, 240, 468, 520,  936, 1040},
                                   {156, 173, 324, 360, 702, 780, 1404, 1560},
                                   {208, 231, 432, 480, 936,1040, 1872, 2080},
                                   {234, 261, 486, 540,1053,1170, 2106, 2340},
                                   {260, 289, 540, 600,1170,1300, 2340, 2600},
                                   {312, 347, 648, 720,1404,1560, 2808, 3120},
                                   {  0,   0, 720, 800,1560,1733, 3120, 3467}};

    mcs.mcs = mcs_r;
    if ((mcs.mcs_s.preamble < 4) && (mcs.mcs_s.rate < 10)) {
        switch(mcs.mcs_s.preamble)
        {
            case 0:
            case 1:
                if(mcs.mcs_s.rate<8) {
                    tx_rate = rate_lookup [mcs.mcs_s.preamble][mcs.mcs_s.rate];
                    if (mcs.mcs_s.nss)
                        tx_rate *=2;
                } else {
                    ALOGE("Unexpected rate value");
                }
            break;
            case 2:
                if(mcs.mcs_s.rate<8) {
                    if (!mcs.mcs_s.nss)
                        tx_rate = MCS_rate_lookup_ht[mcs.mcs_s.rate]
                                                      [2*mcs.mcs_s.bw+short_gi];
                    else
                        tx_rate = MCS_rate_lookup_ht[10+mcs.mcs_s.rate]
                                                      [2*mcs.mcs_s.bw+short_gi];
                } else {
                    ALOGE("Unexpected HT mcs.mcs_s index");
                }
            break;
            case 3:
                if (!mcs.mcs_s.nss)
                    tx_rate = MCS_rate_lookup_ht[mcs.mcs_s.rate]
                                                      [2*mcs.mcs_s.bw+short_gi];
                else
                    tx_rate = MCS_rate_lookup_ht[10+mcs.mcs_s.rate]
                                                      [2*mcs.mcs_s.bw+short_gi];
            break;
            default:
                ALOGE("Unexpected preamble");
        }
    }
    return tx_rate;
}

static u16 get_rx_rate(u16 mcs)
{
    /* TODO: guard interval is not specified currently */
    return get_rate(mcs, 0);
}

static wifi_error parse_rx_stats(hal_info *info, u8 *buf, u16 size)
{
    wifi_error status;
    rb_pkt_stats_t *rx_stats_rcvd = (rb_pkt_stats_t *)buf;
    u8 rb_pkt_entry_buf[RING_BUF_ENTRY_SIZE];
    wifi_ring_buffer_entry *pRingBufferEntry;
    u32 len_ring_buffer_entry = 0;

    len_ring_buffer_entry = sizeof(wifi_ring_buffer_entry)
                            + sizeof(wifi_ring_per_packet_status_entry)
                            + RX_HTT_HDR_STATUS_LEN;

    if (len_ring_buffer_entry > RING_BUF_ENTRY_SIZE) {
        pRingBufferEntry = (wifi_ring_buffer_entry *)malloc(
                len_ring_buffer_entry);
        if (pRingBufferEntry == NULL) {
            ALOGE("%s: Failed to allocate memory", __FUNCTION__);
            return WIFI_ERROR_OUT_OF_MEMORY;
        }
    } else {
        pRingBufferEntry = (wifi_ring_buffer_entry *)rb_pkt_entry_buf;
    }

    wifi_ring_per_packet_status_entry *rb_pkt_stats =
        (wifi_ring_per_packet_status_entry *)(pRingBufferEntry + 1);

    if (size != sizeof(rb_pkt_stats_t)) {
        ALOGE("%s Unexpected rx stats event length: %d", __FUNCTION__, size);
        return WIFI_ERROR_UNKNOWN;
    }

    memset(rb_pkt_stats, 0, sizeof(wifi_ring_per_packet_status_entry));

    /* Peer tx packet and it is an Rx packet for us */
    rb_pkt_stats->flags |= PER_PACKET_ENTRY_FLAGS_DIRECTION_TX;

    if (!rx_stats_rcvd->mpdu_end.tkip_mic_err)
        rb_pkt_stats->flags |= PER_PACKET_ENTRY_FLAGS_TX_SUCCESS;

    rb_pkt_stats->flags |= PER_PACKET_ENTRY_FLAGS_80211_HEADER;

    if (rx_stats_rcvd->mpdu_start.encrypted)
        rb_pkt_stats->flags |= PER_PACKET_ENTRY_FLAGS_PROTECTED;

    rb_pkt_stats->tid = rx_stats_rcvd->mpdu_start.tid;

    if (rx_stats_rcvd->ppdu_start.preamble_type == PREAMBLE_L_SIG_RATE) {
        if (!rx_stats_rcvd->ppdu_start.l_sig_rate_select)
            rb_pkt_stats->MCS |= 1 << 6;
        rb_pkt_stats->MCS |= rx_stats_rcvd->ppdu_start.l_sig_rate % 8;
        /*BW is 0 for legacy cases*/
    } else if (rx_stats_rcvd->ppdu_start.preamble_type ==
               PREAMBLE_VHT_SIG_A_1) {
        rb_pkt_stats->MCS |= 2 << 6;
        rb_pkt_stats->MCS |=
            (rx_stats_rcvd->ppdu_start.ht_sig_vht_sig_a_1 & BITMASK(7)) %8;
        rb_pkt_stats->MCS |=
            ((rx_stats_rcvd->ppdu_start.ht_sig_vht_sig_a_1 >> 7) & 1) << 8;
    } else if (rx_stats_rcvd->ppdu_start.preamble_type ==
               PREAMBLE_VHT_SIG_A_2) {
        rb_pkt_stats->MCS |= 3 << 6;
        rb_pkt_stats->MCS |=
            (rx_stats_rcvd->ppdu_start.ht_sig_vht_sig_a_2 >> 4) & BITMASK(4);
        rb_pkt_stats->MCS |=
            (rx_stats_rcvd->ppdu_start.ht_sig_vht_sig_a_1 & 3) << 8;
    }
    rb_pkt_stats->last_transmit_rate = get_rx_rate(rb_pkt_stats->MCS);

    rb_pkt_stats->rssi = rx_stats_rcvd->ppdu_start.rssi_comb;
    rb_pkt_stats->link_layer_transmit_sequence
        = rx_stats_rcvd->mpdu_start.seq_num;

    rb_pkt_stats->firmware_entry_timestamp
        = rx_stats_rcvd->ppdu_end.wb_timestamp;

    memcpy(&rb_pkt_stats->data[0], &rx_stats_rcvd->rx_hdr_status[0],
        RX_HTT_HDR_STATUS_LEN);

    status = update_stats_to_ring_buf(info, (u8 *)pRingBufferEntry,
                                      len_ring_buffer_entry);

    if (status != WIFI_SUCCESS) {
        ALOGE("Failed to write Rx stats into the ring buffer");
    }

    if ((u8 *)pRingBufferEntry != rb_pkt_entry_buf) {
        ALOGI("Message with more than RING_BUF_ENTRY_SIZE");
        free (pRingBufferEntry);
    }

    return status;
}

static void parse_tx_rate_and_mcs(struct tx_ppdu_start *ppdu_start,
                                wifi_ring_per_packet_status_entry *rb_pkt_stats)
{
    u16 tx_rate = 0, short_gi = 0;
    MCS mcs;

    if (ppdu_start->valid_s0_bw20) {
        short_gi = ppdu_start->s0_bw20.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s0_bw20.rate;
        mcs.mcs_s.nss       = ppdu_start->s0_bw20.nss;
        mcs.mcs_s.preamble  = ppdu_start->s0_bw20.preamble_type;
        mcs.mcs_s.bw        = BW_20_MHZ;
    } else if (ppdu_start->valid_s0_bw40) {
        short_gi = ppdu_start->s0_bw40.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s0_bw40.rate;
        mcs.mcs_s.nss       = ppdu_start->s0_bw40.nss;
        mcs.mcs_s.preamble  = ppdu_start->s0_bw40.preamble_type;
        mcs.mcs_s.bw        = BW_40_MHZ;
    } else if (ppdu_start->valid_s0_bw80) {
        short_gi = ppdu_start->s0_bw80.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s0_bw80.rate;
        mcs.mcs_s.nss       = ppdu_start->s0_bw80.nss;
        mcs.mcs_s.preamble  = ppdu_start->s0_bw80.preamble_type;
        mcs.mcs_s.bw        = BW_80_MHZ;
    } else if (ppdu_start->valid_s0_bw160) {
        short_gi = ppdu_start->s0_bw160.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s0_bw160.rate;
        mcs.mcs_s.nss       = ppdu_start->s0_bw160.nss;
        mcs.mcs_s.preamble  = ppdu_start->s0_bw160.preamble_type;
        mcs.mcs_s.bw        = BW_160_MHZ;
    } else if (ppdu_start->valid_s1_bw20) {
        short_gi = ppdu_start->s1_bw20.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s1_bw20.rate;
        mcs.mcs_s.nss       = ppdu_start->s1_bw20.nss;
        mcs.mcs_s.preamble  = ppdu_start->s1_bw20.preamble_type;
        mcs.mcs_s.bw        = BW_20_MHZ;
    } else if (ppdu_start->valid_s1_bw40) {
        short_gi = ppdu_start->s1_bw40.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s1_bw40.rate;
        mcs.mcs_s.nss       = ppdu_start->s1_bw40.nss;
        mcs.mcs_s.preamble  = ppdu_start->s1_bw40.preamble_type;
        mcs.mcs_s.bw        = BW_40_MHZ;
    } else if (ppdu_start->valid_s1_bw80) {
        short_gi = ppdu_start->s1_bw80.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s1_bw80.rate;
        mcs.mcs_s.nss       = ppdu_start->s1_bw80.nss;
        mcs.mcs_s.preamble  = ppdu_start->s1_bw80.preamble_type;
        mcs.mcs_s.bw        = BW_80_MHZ;
    } else if (ppdu_start->valid_s1_bw160) {
        short_gi = ppdu_start->s1_bw160.short_gi;
        mcs.mcs_s.rate      = ppdu_start->s1_bw160.rate;
        mcs.mcs_s.nss       = ppdu_start->s1_bw160.nss;
        mcs.mcs_s.preamble  = ppdu_start->s1_bw160.preamble_type;
        mcs.mcs_s.bw        = BW_160_MHZ;
    }

    rb_pkt_stats->MCS = mcs.mcs;
    rb_pkt_stats->last_transmit_rate = get_rate(mcs.mcs, short_gi);
}

static wifi_error parse_tx_stats(hal_info *info, void *buf,
                                 u32 buflen, u8 logtype)
{
    wifi_error status;
    wifi_ring_buffer_entry *pRingBufferEntry =
        (wifi_ring_buffer_entry *)info->pkt_stats->tx_stats;

    wifi_ring_per_packet_status_entry *rb_pkt_stats =
        (wifi_ring_per_packet_status_entry *)(pRingBufferEntry + 1);

    ALOGV("Received Tx stats: log_type : %d", logtype);
    switch (logtype)
    {
        case PKTLOG_TYPE_TX_CTRL:
        {
            if (buflen != sizeof (wh_pktlog_txctl)) {
                ALOGE("Unexpected tx_ctrl event length: %d", buflen);
                return WIFI_ERROR_UNKNOWN;
            }

            wh_pktlog_txctl *stats = (wh_pktlog_txctl *)buf;
            struct tx_ppdu_start *ppdu_start =
                (struct tx_ppdu_start *)(&stats->u.ppdu_start);

            if (ppdu_start->frame_control & BIT(DATA_PROTECTED))
                rb_pkt_stats->flags |=
                    PER_PACKET_ENTRY_FLAGS_PROTECTED;
            rb_pkt_stats->link_layer_transmit_sequence
                = ppdu_start->start_seq_num;
            rb_pkt_stats->tid = ppdu_start->qos_ctl & 0xF;
            parse_tx_rate_and_mcs(ppdu_start, rb_pkt_stats);
            info->pkt_stats->tx_stats_events |=  BIT(PKTLOG_TYPE_TX_CTRL);
        }
        break;
        case PKTLOG_TYPE_TX_STAT:
        {
            if (buflen != sizeof(struct tx_ppdu_end)) {
                ALOGE("Unexpected tx_stat event length: %d", buflen);
                return WIFI_ERROR_UNKNOWN;
            }

            /* This should be the first event for tx-stats: So,
             * previous stats are invalid. Flush the old stats and treat
             * this as new packet
             */
            if (info->pkt_stats->tx_stats_events)
                memset(rb_pkt_stats, 0,
                        sizeof(wifi_ring_per_packet_status_entry));

            struct tx_ppdu_end *tx_ppdu_end = (struct tx_ppdu_end*)(buf);

            if (tx_ppdu_end->stat.tx_ok)
                rb_pkt_stats->flags |=
                    PER_PACKET_ENTRY_FLAGS_TX_SUCCESS;
            rb_pkt_stats->transmit_success_timestamp =
                tx_ppdu_end->try_list.try_00.timestamp;
            rb_pkt_stats->rssi = tx_ppdu_end->stat.ack_rssi_ave;
            rb_pkt_stats->num_retries =
                tx_ppdu_end->stat.total_tries;

            info->pkt_stats->tx_stats_events =  BIT(PKTLOG_TYPE_TX_STAT);
        }
        break;
        case PKTLOG_TYPE_RC_UPDATE:
        case PKTLOG_TYPE_TX_MSDU_ID:
        case PKTLOG_TYPE_TX_FRM_HDR:
        case PKTLOG_TYPE_RC_FIND:
        case PKTLOG_TYPE_TX_VIRT_ADDR:
            ALOGV("%s : Unsupported log_type received : %d",
                  __FUNCTION__, logtype);
        break;
        default:
        {
            ALOGV("%s : Unexpected log_type received : %d",
                  __FUNCTION__, logtype);
            return WIFI_ERROR_UNKNOWN;
        }
    }

    if ((info->pkt_stats->tx_stats_events &  BIT(PKTLOG_TYPE_TX_CTRL))&&
        (info->pkt_stats->tx_stats_events &  BIT(PKTLOG_TYPE_TX_STAT))) {
        /* No tx payload as of now, add the length to parameter size(3rd)
         * if there is any payload
         */
        status = update_stats_to_ring_buf(info,
                                          (u8 *)pRingBufferEntry,
                                     sizeof(wifi_ring_buffer_entry) +
                                     sizeof(wifi_ring_per_packet_status_entry));

        /* Flush the local copy after writing the stats to ring buffer
         * for tx-stats.
         */
        info->pkt_stats->tx_stats_events = 0;
        memset(rb_pkt_stats, 0,
                sizeof(wifi_ring_per_packet_status_entry));

        if (status != WIFI_SUCCESS) {
            ALOGE("Failed to write into the ring buffer: %d", logtype);
            return status;
        }
    }

    return WIFI_SUCCESS;
}

static wifi_error parse_stats_record(hal_info *info, u8 *buf, u16 record_type,
                              u16 record_len)
{
    wifi_error status;
    if (record_type == PKTLOG_TYPE_RX_STAT) {
        status = parse_rx_stats(info, buf, record_len);
    } else {
        status = parse_tx_stats(info, buf, record_len, record_type);
    }
    return status;
}

static wifi_error parse_stats(hal_info *info, u8 *data, u32 buflen)
{
    wh_pktlog_hdr_t *pkt_stats_header;
    wifi_error status = WIFI_SUCCESS;

    do {
        if (buflen < sizeof(wh_pktlog_hdr_t)) {
            status = WIFI_ERROR_INVALID_ARGS;
            break;
        }

        pkt_stats_header = (wh_pktlog_hdr_t *)data;

        if (buflen < (sizeof(wh_pktlog_hdr_t) + pkt_stats_header->size)) {
            status = WIFI_ERROR_INVALID_ARGS;
            break;
        }
        status = parse_stats_record(info,
                                    (u8 *)(pkt_stats_header + 1),
                                    pkt_stats_header->log_type,
                                    pkt_stats_header->size);
        if (status != WIFI_SUCCESS) {
            ALOGE("Failed to parse the stats type : %d",
                  pkt_stats_header->log_type);
            return status;
        }
        data += (sizeof(wh_pktlog_hdr_t) + pkt_stats_header->size);
        buflen -= (sizeof(wh_pktlog_hdr_t) + pkt_stats_header->size);
    } while (buflen > 0);

    return status;
}

wifi_error process_driver_prints(hal_info *info, u8 *buf, u16 length)
{
    wifi_ring_buffer_entry rb_entry_hdr;
    struct timeval time;
    wifi_error status;

    rb_entry_hdr.entry_size = length;
    rb_entry_hdr.flags = RING_BUFFER_ENTRY_FLAGS_HAS_TIMESTAMP;
    rb_entry_hdr.type = ENTRY_TYPE_DATA;
    gettimeofday(&time, NULL);
    rb_entry_hdr.timestamp = time.tv_usec + time.tv_sec * 1000 * 1000;

    /* Write if verbose and handler is set */
    if (info->rb_infos[DRIVER_PRINTS_RB_ID].verbose_level >= 1 &&
        info->on_ring_buffer_data) {
        /* Write header and payload separately to avoid
         * complete payload memcpy */
        status = ring_buffer_write(&info->rb_infos[DRIVER_PRINTS_RB_ID],
                                   (u8*)&rb_entry_hdr,
                                   sizeof(wifi_ring_buffer_entry), 0);
        if (status != WIFI_SUCCESS) {
            ALOGE("Failed to write kernel prints rb header %d", status);
            return status;
        }
        status = ring_buffer_write(&info->rb_infos[DRIVER_PRINTS_RB_ID],
                                   buf, length, 1);
        if (status != WIFI_SUCCESS) {
            ALOGE("Failed to write kernel prints rb payload %d", status);
            return status;
        }
    } else {
        return WIFI_ERROR_NOT_AVAILABLE;
    }

    return WIFI_SUCCESS;
}

wifi_error diag_message_handler(hal_info *info, nl_msg *msg)
{
    tAniNlHdr *wnl = (tAniNlHdr *)nlmsg_hdr(msg);
    u8 *buf;
    wifi_error status;

    /* Check nlmsg_type also to avoid processing unintended msgs */
    if (wnl->nlh.nlmsg_type == ANI_NL_MSG_PUMAC) {
        if (wnl->wmsg.type == ANI_NL_MSG_LOG_HOST_EVENT_LOG_TYPE) {
            uint32_t diag_host_type;

            buf = (uint8_t *)(wnl + 1);
            diag_host_type = *(uint32_t *)(buf);
            ALOGV("diag type = %d", diag_host_type);

            buf +=  sizeof(uint32_t); //diag_type
            if (diag_host_type == DIAG_TYPE_HOST_EVENTS) {
                host_event_hdr_t *event_hdr =
                              (host_event_hdr_t *)(buf);
                ALOGV("diag event_id = %x length %d",
                      event_hdr->event_id, event_hdr->length);
                buf += sizeof(host_event_hdr_t);
                switch (event_hdr->event_id) {
                    case EVENT_WLAN_WAKE_LOCK:
                        process_wakelock_event(info, buf, event_hdr->length);
                        break;
                    case EVENT_WLAN_PE:
                        process_wlan_pe_event(info, buf, event_hdr->length);
                        break;
                    case EVENT_WLAN_EAPOL:
                        process_wlan_eapol_event(info, buf, event_hdr->length);
                        break;
                    case EVENT_WLAN_LOG_COMPLETE:
                        process_wlan_log_complete_event(info, buf, event_hdr->length);
                        break;
                    default:
                        return WIFI_SUCCESS;
                }
            } else if (diag_host_type == DIAG_TYPE_HOST_LOG_MSGS) {
                drv_msg_t *drv_msg = (drv_msg_t *) (buf);
                ALOGV("diag event_type = %0x length = %d",
                      drv_msg->event_type, drv_msg->length);
                if (drv_msg->event_type == WLAN_PKT_LOG_STATS) {
                    if ((info->pkt_stats->prev_seq_no + 1) !=
                            drv_msg->u.pkt_stats_event.msg_seq_no) {
                        ALOGE("Few pkt stats messages missed: rcvd = %d, prev = %d",
                                drv_msg->u.pkt_stats_event.msg_seq_no,
                                info->pkt_stats->prev_seq_no);
                        if (info->pkt_stats->tx_stats_events) {
                            info->pkt_stats->tx_stats_events = 0;
                            memset(&info->pkt_stats->tx_stats, 0,
                                    sizeof(wifi_ring_per_packet_status_entry));
                        }
                    }

                    info->pkt_stats->prev_seq_no =
                        drv_msg->u.pkt_stats_event.msg_seq_no;
                    status = parse_stats(info,
                            drv_msg->u.pkt_stats_event.payload,
                            drv_msg->u.pkt_stats_event.payload_len);
                    if (status != WIFI_SUCCESS) {
                        ALOGE("%s: Failed to parse Tx-Rx stats", __FUNCTION__);
                        ALOGE("Received msg Seq_num : %d",
                                drv_msg->u.pkt_stats_event.msg_seq_no);
                        hexdump((char *)drv_msg->u.pkt_stats_event.payload,
                                drv_msg->u.pkt_stats_event.payload_len);
                        return status;
                    }
                }
            }
        }
    } else if (wnl->nlh.nlmsg_type == ANI_NL_MSG_LOG) {
        if (wnl->wmsg.type == ANI_NL_MSG_LOG_HOST_PRINT_TYPE) {
            process_driver_prints(info, (u8 *)(wnl + 1), wnl->wmsg.length);
        }
    } else if (wnl->nlh.nlmsg_type == ANI_NL_MSG_CNSS_DIAG) {
        uint16_t diag_fw_type;
        uint32_t event_id;
        buf = (uint8_t *)NLMSG_DATA(wnl);

        fw_event_hdr_t *event_hdr =
                          (fw_event_hdr_t *)(buf);
        diag_fw_type = event_hdr->diag_type;
        if (diag_fw_type == DIAG_TYPE_FW_MSG) {
            dbglog_slot *slot;
            u16 length = 0;
            u32 version = 0;

            slot = (dbglog_slot *)buf;
            length = get_le32((u8 *)&slot->length);
            process_fw_diag_msg(info, &slot->payload[0], length);
        }
    }
    return WIFI_SUCCESS;
}
