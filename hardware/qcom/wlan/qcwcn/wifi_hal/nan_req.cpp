/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "sync.h"
#include <utils/Log.h>
#include "nan.h"
#include "wifi_hal.h"
#include "nan_i.h"
#include "nancommand.h"

int NanCommand::putNanEnable(const NanEnableRequest *pReq)
{
    ALOGI("NAN_ENABLE");
    size_t message_len = NAN_MAX_ENABLE_REQ_SIZE;

    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

#ifdef NAN_2_0
    /* Removing the unsupported ones */
    message_len -= \
        (SIZEOF_TLV_HDR + sizeof(u8)  /* Random Time   */ + \
         SIZEOF_TLV_HDR + sizeof(u8)  /* Full Scan Int */);

    message_len += \
        (
          pReq->config_2dot4g_support ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->support_2dot4g_val)) : 0 \
        ) + \
        (
          pReq->config_2dot4g_beacons ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->beacon_2dot4g_val)) : 0 \
        ) + \
        (
          pReq->config_2dot4g_discovery ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->discovery_2dot4g_val)) : 0 \
        ) + \
        (
          pReq->config_5g_beacons ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->beacon_5g_val)) : 0 \
        ) + \
        (
          pReq->config_5g_discovery ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->discovery_5g_val)) : 0 \
        ) + \
        (
          pReq->config_5g_rssi_close ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->rssi_close_5g_val)) : 0 \
        ) + \
        (
          pReq->config_5g_rssi_middle ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->rssi_middle_5g_val)) : 0 \
        ) + \
        (
          pReq->config_5g_rssi_close_proximity ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->rssi_close_proximity_5g_val)) : 0 \
        ) + \
        (
          pReq->config_rssi_window_size ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->rssi_window_size_val)) : 0 \
        ) + \
        (
          pReq->config_oui ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->oui_val)) : 0 \
        ) + \
        (
          pReq->config_intf_addr ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->intf_addr_val)) : 0 \
        ) + \
        (
          pReq->config_cluster_attribute_val ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->config_cluster_attribute_val)) : 0 \
        ) + \
        (
          pReq->config_scan_params ? (SIZEOF_TLV_HDR + \
          NAN_MAX_SOCIAL_CHANNEL * sizeof(u32)) : 0 \
        ) + \
        (
          pReq->config_debug_flags ? (SIZEOF_TLV_HDR + \
          sizeof(u64)) : 0 \
        ) + \
        (
          pReq->config_random_factor_force ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->random_factor_force_val)) : 0 \
         ) + \
        (
          pReq->config_hop_count_force ? (SIZEOF_TLV_HDR + \
          sizeof(pReq->hop_count_force_val)) : 0 \
        );
#endif /* NAN_2_0 */

    pNanEnableReqMsg pFwReq = (pNanEnableReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_OUT_OF_MEMORY;
    }

    ALOGI("Message Len %d", message_len);
    memset (pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_ENABLE_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    u8* tlvs = pFwReq->ptlv;

    /* Write the TLVs to the message. */
    tlvs = addTlv(NAN_TLV_TYPE_5G_SUPPORT, sizeof(pReq->support_5g),
                  (const u8*)&pReq->support_5g, tlvs);
    tlvs = addTlv(NAN_TLV_TYPE_CLUSTER_ID_LOW, sizeof(pReq->cluster_low),
                  (const u8*)&pReq->cluster_low, tlvs);
    tlvs = addTlv(NAN_TLV_TYPE_CLUSTER_ID_HIGH, sizeof(pReq->cluster_high),
                  (const u8*)&pReq->cluster_high, tlvs);
    tlvs = addTlv(NAN_TLV_TYPE_SID_BEACON, sizeof(pReq->sid_beacon),
                  (const u8*)&pReq->sid_beacon, tlvs);
    tlvs = addTlv(NAN_TLV_TYPE_RSSI_CLOSE, sizeof(pReq->rssi_close),
                  (const u8*)&pReq->rssi_close, tlvs);
    tlvs = addTlv(NAN_TLV_TYPE_RSSI_MEDIUM, sizeof(pReq->rssi_middle),
                  (const u8*)&pReq->rssi_middle, tlvs);
    tlvs = addTlv(NAN_TLV_TYPE_HOP_COUNT_LIMIT, sizeof(pReq->hop_count_limit),
                  (const u8*)&pReq->hop_count_limit, tlvs);
#ifndef NAN_2_0
    tlvs = addTlv(NAN_TLV_TYPE_RANDOM_UPDATE_TIME, sizeof(pReq->random_time),
                  (const u8*)&pReq->random_time, tlvs);
#endif /* NAN_2_0 */
    tlvs = addTlv(NAN_TLV_TYPE_MASTER_PREFERENCE, sizeof(pReq->master_pref),
                  (const u8*)&pReq->master_pref, tlvs);
#ifndef NAN_2_0
    tlvs = addTlv(NAN_TLV_TYPE_PERIODIC_SCAN_INTERVAL, sizeof(pReq->periodic_scan_interval),
                  (const u8*)&pReq->periodic_scan_interval, tlvs);
#endif /* NAN_2_0 */

#ifdef NAN_2_0
    if (pReq->config_2dot4g_support) {
        tlvs = addTlv(NAN_TLV_TYPE_2DOT4G_SUPPORT, sizeof(pReq->support_2dot4g_val),
                      (const u8*)&pReq->support_2dot4g_val, tlvs);
    }
    if (pReq->config_2dot4g_beacons) {
        tlvs = addTlv(NAN_TLV_TYPE_2DOT4G_BEACONS, sizeof(pReq->beacon_2dot4g_val),
                      (const u8*)&pReq->beacon_2dot4g_val, tlvs);
    }
    if (pReq->config_2dot4g_discovery) {
        tlvs = addTlv(NAN_TLV_TYPE_2DOT4G_SDF, sizeof(pReq->discovery_2dot4g_val),
                      (const u8*)&pReq->discovery_2dot4g_val, tlvs);
    }
    if (pReq->config_5g_beacons) {
        tlvs = addTlv(NAN_TLV_TYPE_5G_BEACON, sizeof(pReq->beacon_5g_val),
                      (const u8*)&pReq->beacon_5g_val, tlvs);
    }
    if (pReq->config_5g_discovery) {
        tlvs = addTlv(NAN_TLV_TYPE_5G_SDF, sizeof(pReq->discovery_5g_val),
                      (const u8*)&pReq->discovery_5g_val, tlvs);
    }
    /* Add the support of sending 5G RSSI values */
    if (pReq->config_5g_rssi_close) {
        tlvs = addTlv(NAN_TLV_TYPE_5G_RSSI_CLOSE, sizeof(pReq->rssi_close_5g_val),
                      (const u8*)&pReq->rssi_close_5g_val, tlvs);
    }
    if (pReq->config_5g_rssi_middle) {
        tlvs = addTlv(NAN_TLV_TYPE_5G_RSSI_MEDIUM, sizeof(pReq->rssi_middle_5g_val),
                      (const u8*)&pReq->rssi_middle_5g_val, tlvs);
    }
    if (pReq->config_5g_rssi_close_proximity) {
        tlvs = addTlv(NAN_TLV_TYPE_5G_RSSI_CLOSE_PROXIMITY,
                      sizeof(pReq->rssi_close_proximity_5g_val),
                      (const u8*)&pReq->rssi_close_proximity_5g_val, tlvs);
    }
    if (pReq->config_rssi_window_size) {
        tlvs = addTlv(NAN_TLV_TYPE_RSSI_AVERAGING_WINDOW_SIZE, sizeof(pReq->rssi_window_size_val),
                      (const u8*)&pReq->rssi_window_size_val, tlvs);
    }
    if (pReq->config_oui) {
        tlvs = addTlv(NAN_TLV_TYPE_CLUSTER_OUI_NETWORK_ID, sizeof(pReq->oui_val),
                      (const u8*)&pReq->oui_val, tlvs);
    }
    if (pReq->config_intf_addr) {
        tlvs = addTlv(NAN_TLV_TYPE_SOURCE_MAC_ADDRESS, sizeof(pReq->intf_addr_val),
                      (const u8*)&pReq->intf_addr_val[0], tlvs);
    }
    if (pReq->config_cluster_attribute_val) {
        tlvs = addTlv(NAN_TLV_TYPE_CLUSTER_ATTRIBUTE_IN_SDF, sizeof(pReq->config_cluster_attribute_val),
                      (const u8*)&pReq->config_cluster_attribute_val, tlvs);
    }
    if (pReq->config_scan_params) {
        u32 socialChannelParamVal[NAN_MAX_SOCIAL_CHANNEL];
        /* Fill the social channel param */
        fillNanSocialChannelParamVal(&pReq->scan_params_val,
                                     socialChannelParamVal);
        int i;
        for (i = 0; i < NAN_MAX_SOCIAL_CHANNEL; i++) {
            tlvs = addTlv(NAN_TLV_TYPE_SOCIAL_CHANNEL_SCAN_PARAMETERS,
                          sizeof(socialChannelParamVal[i]),
                          (const u8*)&socialChannelParamVal[i], tlvs);
        }
    }
    if (pReq->config_debug_flags) {
        tlvs = addTlv(NAN_TLV_TYPE_DEBUGGING_FLAGS,
                      sizeof(pReq->debug_flags_val),
                      (const u8*)&pReq->debug_flags_val, tlvs);
    }
    if (pReq->config_random_factor_force) {
        tlvs = addTlv(NAN_TLV_TYPE_RANDOM_FACTOR_FORCE,
                      sizeof(pReq->random_factor_force_val),
                      (const u8*)&pReq->random_factor_force_val, tlvs);
    }
    if (pReq->config_hop_count_force) {
        tlvs = addTlv(NAN_TLV_TYPE_HOP_COUNT_FORCE,
                      sizeof(pReq->hop_count_force_val),
                      (const u8*)&pReq->hop_count_force_val, tlvs);
    }
#endif /* NAN_2_0 */

    mVendorData = (char*)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanDisable(const NanDisableRequest *pReq)
{
    ALOGI("NAN_DISABLE");
    size_t message_len = sizeof(NanDisableReqMsg);

    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    pNanDisableReqMsg pFwReq = (pNanDisableReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_OUT_OF_MEMORY;
    }

    ALOGI("Message Len %d", message_len);
    memset (pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_DISABLE_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    mVendorData = (char*)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanConfig(const NanConfigRequest *pReq)
{
    ALOGI("NAN_CONFIG");
    size_t message_len = NAN_MAX_CONFIGURATION_REQ_SIZE;

    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

#ifndef NAN_2_0
    // Add additional message size for transmitting
    // further availability attribute if
    // additional_disc_window_slots is Non-zero value.
    if (pReq->additional_disc_window_slots != 0) {
        message_len += (SIZEOF_TLV_HDR + \
                        sizeof(pReq->additional_disc_window_slots));
    }
#endif /* NAN_2_0 */

#ifdef NAN_2_0
    message_len = sizeof(NanMsgHeader);

    message_len += \
         (
           pReq->config_sid_beacon ? (SIZEOF_TLV_HDR + \
           sizeof(pReq->sid_beacon)) : 0 \
         ) + \
         (
           pReq->config_master_pref ? (SIZEOF_TLV_HDR + \
           sizeof(pReq->master_pref)) : 0 \
         ) + \
         (
           pReq->config_5g_rssi_close_proximity ? (SIZEOF_TLV_HDR + \
           sizeof(pReq->rssi_close_proximity_5g_val)) : 0 \
         ) + \
         (
           pReq->config_rssi_window_size ? (SIZEOF_TLV_HDR + \
           sizeof(pReq->rssi_window_size_val)) : 0 \
         ) + \
         (
           pReq->config_cluster_attribute_val ? (SIZEOF_TLV_HDR + \
           sizeof(pReq->config_cluster_attribute_val)) : 0 \
         ) + \
         (
           pReq->config_scan_params ? (SIZEOF_TLV_HDR + \
           NAN_MAX_SOCIAL_CHANNEL * sizeof(u32)) : 0 \
         ) + \
         (
           pReq->config_debug_flags ? (SIZEOF_TLV_HDR + \
           sizeof(u64)) : 0 \
          ) + \
         (
           pReq->config_random_factor_force ? (SIZEOF_TLV_HDR + \
           sizeof(pReq->random_factor_force_val)) : 0 \
          ) + \
         (
           pReq->config_hop_count_force ? (SIZEOF_TLV_HDR + \
           sizeof(pReq->hop_count_force_val)) : 0 \
         ) + \
         (
           pReq->config_conn_capability ? (SIZEOF_TLV_HDR + \
           sizeof(u32)) : 0 \
         ) + \
         (
           pReq->config_discovery_attr ? (SIZEOF_TLV_HDR + \
           calcNanTransmitPostDiscoverySize(&pReq->discovery_attr_val)) : 0 \
         );

    if (pReq->config_fam && \
        calcNanFurtherAvailabilityMapSize(&pReq->fam_val)) {
        message_len += (SIZEOF_TLV_HDR + \
           calcNanFurtherAvailabilityMapSize(&pReq->fam_val));
    }
#endif /* NAN_2_0 */

    pNanConfigurationReqMsg pFwReq = (pNanConfigurationReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_OUT_OF_MEMORY;
    }

    ALOGI("Message Len %d", message_len);
    memset (pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_CONFIGURATION_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    u8* tlvs = pFwReq->ptlv;
    if (pReq->config_sid_beacon) {
        tlvs = addTlv(NAN_TLV_TYPE_SID_BEACON, sizeof(pReq->sid_beacon),
                      (const u8*)&pReq->sid_beacon, tlvs);
    }
#ifndef NAN_2_0
    tlvs = addTlv(NAN_TLV_TYPE_RANDOM_UPDATE_TIME, sizeof(pReq->random_time),
                  (const u8*)&pReq->random_time, tlvs);
#endif /* NAN_2_0 */
    if (pReq->config_master_pref) {
        tlvs = addTlv(NAN_TLV_TYPE_MASTER_PREFERENCE, sizeof(pReq->master_pref),
                      (const u8*)&pReq->master_pref, tlvs);
    }
#ifndef NAN_2_0
    tlvs = addTlv(NAN_TLV_TYPE_PERIODIC_SCAN_INTERVAL, sizeof(pReq->periodic_scan_interval),
                  (const u8*)&pReq->periodic_scan_interval, tlvs);
#endif /* NAN_2_0 */

/* In 2.0 Version of NAN this parameter does not have any significance */
#ifndef NAN_2_0
    if (pReq->additional_disc_window_slots != 0) {
        /*
        Construct the value in this manner
        Bit0 ==> 1/0 Enable/Disable FAW
        Bit1-2 ==>  reserved
        Bit3-7 ==>  FAW Slot Value.
        */
        u8 faw_value = 0x01;  /* Enable the first bit */
        /* Shifting the disc_window_slots by 3 and masking it with 0xf8
           so that the Bit 3 to 7 are updated
        */
        faw_value |= ((pReq->additional_disc_window_slots << 3) & (0xf8));
        tlvs = addTlv(NAN_TLV_TYPE_FURTHER_AVAILABILITY,
                      sizeof(faw_value),
                      (const u8*)&faw_value, tlvs);
    }
#endif /* NAN_2_0 */

#ifdef NAN_2_0
    if (pReq->config_rssi_window_size) {
        tlvs = addTlv(NAN_TLV_TYPE_RSSI_AVERAGING_WINDOW_SIZE, sizeof(pReq->rssi_window_size_val),
                      (const u8*)&pReq->rssi_window_size_val, tlvs);
    }
    if (pReq->config_scan_params) {
        u32 socialChannelParamVal[NAN_MAX_SOCIAL_CHANNEL];
        /* Fill the social channel param */
        fillNanSocialChannelParamVal(&pReq->scan_params_val,
                                 socialChannelParamVal);
        int i;
        for (i = 0; i < NAN_MAX_SOCIAL_CHANNEL; i++) {
            tlvs = addTlv(NAN_TLV_TYPE_SOCIAL_CHANNEL_SCAN_PARAMETERS,
                          sizeof(socialChannelParamVal[i]),
                          (const u8*)&socialChannelParamVal[i], tlvs);
        }
    }
    if (pReq->config_debug_flags) {
        tlvs = addTlv(NAN_TLV_TYPE_DEBUGGING_FLAGS,
                      sizeof(pReq->debug_flags_val),
                      (const u8*)&pReq->debug_flags_val, tlvs);
    }
    if (pReq->config_random_factor_force) {
        tlvs = addTlv(NAN_TLV_TYPE_RANDOM_FACTOR_FORCE,
                      sizeof(pReq->random_factor_force_val),
                      (const u8*)&pReq->random_factor_force_val, tlvs);
    }
    if (pReq->config_hop_count_force) {
        tlvs = addTlv(NAN_TLV_TYPE_HOP_COUNT_FORCE,
                      sizeof(pReq->hop_count_force_val),
                      (const u8*)&pReq->hop_count_force_val, tlvs);
    }
    if (pReq->config_conn_capability) {
        u32 val = \
        getNanTransmitPostConnectivityCapabilityVal(&pReq->conn_capability_val);
        tlvs = addTlv(NAN_TLV_TYPE_POST_NAN_CONNECTIVITY_CAPABILITIES_TRANSMIT,
                      sizeof(val), (const u8*)&val, tlvs);
    }
    if (pReq->config_discovery_attr) {
        fillNanTransmitPostDiscoveryVal(&pReq->discovery_attr_val,
                                        (u8*)(tlvs + SIZEOF_TLV_HDR));
        tlvs = addTlv(NAN_TLV_TYPE_POST_NAN_DISCOVERY_ATTRIBUTE_TRANSMIT,
                      calcNanTransmitPostDiscoverySize(&pReq->discovery_attr_val),
                      (const u8*)(tlvs + SIZEOF_TLV_HDR), tlvs);
    }
    if (pReq->config_fam && \
        calcNanFurtherAvailabilityMapSize(&pReq->fam_val)) {
        fillNanFurtherAvailabilityMapVal(&pReq->fam_val,
                                        (u8*)(tlvs + SIZEOF_TLV_HDR));
        tlvs = addTlv(NAN_TLV_TYPE_FURTHER_AVAILABILITY_MAP,
                      calcNanFurtherAvailabilityMapSize(&pReq->fam_val),
                      (const u8*)(tlvs + SIZEOF_TLV_HDR), tlvs);
    }
#endif /* NAN_2_0 */

    mVendorData = (char*)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}


int NanCommand::putNanPublish(const NanPublishRequest *pReq)
{
    ALOGI("NAN_PUBLISH");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    size_t message_len =
        sizeof(NanMsgHeader) + sizeof(NanPublishServiceReqParams) +
        (pReq->service_name_len ? SIZEOF_TLV_HDR + pReq->service_name_len : 0) +
        (pReq->service_specific_info_len ? SIZEOF_TLV_HDR + pReq->service_specific_info_len : 0) +
        (pReq->rx_match_filter_len ? SIZEOF_TLV_HDR + pReq->rx_match_filter_len : 0) +
        (pReq->tx_match_filter_len ? SIZEOF_TLV_HDR + pReq->tx_match_filter_len : 0);

    pNanPublishServiceReqMsg pFwReq = (pNanPublishServiceReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_OUT_OF_MEMORY;
    }

    ALOGI("Message Len %d", message_len);
    memset(pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_PUBLISH_SERVICE_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    pFwReq->publishServiceReqParams.ttl = pReq->ttl;
    pFwReq->publishServiceReqParams.period = pReq->period;
    pFwReq->publishServiceReqParams.replyIndFlag = pReq->replied_event_flag;
    pFwReq->publishServiceReqParams.publishType = pReq->publish_type;
    pFwReq->publishServiceReqParams.txType = pReq->tx_type;
#ifdef NAN_2_0
    /* Overwriting replyIndFlag to 0 based on v17 Nan Spec */
    pFwReq->publishServiceReqParams.replyIndFlag = 0;
    pFwReq->publishServiceReqParams.rssiThresholdFlag = pReq->rssi_threshold_flag;
    pFwReq->publishServiceReqParams.ota_flag = pReq->ota_flag;
    pFwReq->publishServiceReqParams.matchAlg = pReq->publish_match;
#endif /* NAN_2_0 */
    pFwReq->publishServiceReqParams.count = pReq->publish_count;
#ifdef NAN_2_0
    pFwReq->publishServiceReqParams.connmap = pReq->connmap;
#endif /* NAN_2_0 */
    pFwReq->publishServiceReqParams.reserved2 = 0;

    u8* tlvs = pFwReq->ptlv;
    if (pReq->service_name_len) {
        tlvs = addTlv(NAN_TLV_TYPE_SERVICE_NAME, pReq->service_name_len,
                      (const u8*)&pReq->service_name[0], tlvs);
    }
    if (pReq->service_specific_info_len) {
        tlvs = addTlv(NAN_TLV_TYPE_SERVICE_SPECIFIC_INFO, pReq->service_specific_info_len,
                      (const u8*)&pReq->service_specific_info[0], tlvs);
    }
    if (pReq->rx_match_filter_len) {
        tlvs = addTlv(NAN_TLV_TYPE_RX_MATCH_FILTER, pReq->rx_match_filter_len,
                      (const u8*)&pReq->rx_match_filter[0], tlvs);
    }
    if (pReq->tx_match_filter_len) {
        tlvs = addTlv(NAN_TLV_TYPE_TX_MATCH_FILTER, pReq->tx_match_filter_len,
                      (const u8*)&pReq->tx_match_filter[0], tlvs);
    }

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanPublishCancel(const NanPublishCancelRequest *pReq)
{
    ALOGI("NAN_PUBLISH_CANCEL");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }
    size_t message_len = sizeof(NanPublishServiceCancelReqMsg);

    pNanPublishServiceCancelReqMsg pFwReq =
        (pNanPublishServiceCancelReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    ALOGI("Message Len %d", message_len);
    memset(pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_PUBLISH_SERVICE_CANCEL_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanSubscribe(const NanSubscribeRequest *pReq)
{

    ALOGI("NAN_SUBSCRIBE");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    size_t message_len =
        sizeof(NanMsgHeader) + sizeof(NanSubscribeServiceReqParams) +
        (pReq->service_name_len ? SIZEOF_TLV_HDR + pReq->service_name_len : 0) +
        (pReq->service_specific_info_len ? SIZEOF_TLV_HDR + pReq->service_specific_info_len : 0) +
        (pReq->rx_match_filter_len ? SIZEOF_TLV_HDR + pReq->rx_match_filter_len : 0) +
        (pReq->tx_match_filter_len ? SIZEOF_TLV_HDR + pReq->tx_match_filter_len : 0);

#ifdef NAN_2_0
    message_len += \
        (pReq->num_intf_addr_present * (SIZEOF_TLV_HDR + NAN_MAC_ADDR_LEN));
#endif /* NAN_2_0 */

    pNanSubscribeServiceReqMsg pFwReq = (pNanSubscribeServiceReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    ALOGI("Message Len %d", message_len);
    memset(pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_SUBSCRIBE_SERVICE_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;


    pFwReq->subscribeServiceReqParams.ttl = pReq->ttl;
    pFwReq->subscribeServiceReqParams.period = pReq->period;
    pFwReq->subscribeServiceReqParams.subscribeType = pReq->subscribe_type;
    pFwReq->subscribeServiceReqParams.srfAttr = pReq->serviceResponseFilter;
    pFwReq->subscribeServiceReqParams.srfInclude = pReq->serviceResponseInclude;
    pFwReq->subscribeServiceReqParams.srfSend = pReq->useServiceResponseFilter;
    pFwReq->subscribeServiceReqParams.ssiRequired = pReq->ssiRequiredForMatchIndication;
    pFwReq->subscribeServiceReqParams.matchAlg = pReq->subscribe_match;
    pFwReq->subscribeServiceReqParams.count = pReq->subscribe_count;
#ifdef NAN_2_0
    pFwReq->subscribeServiceReqParams.rssiThresholdFlag = pReq->rssi_threshold_flag;
    pFwReq->subscribeServiceReqParams.ota_flag = pReq->ota_flag;
    pFwReq->subscribeServiceReqParams.connmap = pReq->connmap;
#endif /* NAN_2_0 */
    pFwReq->subscribeServiceReqParams.reserved = 0;

    u8* tlvs = pFwReq->ptlv;
    if (pReq->service_name_len) {
        tlvs = addTlv(NAN_TLV_TYPE_SERVICE_NAME, pReq->service_name_len,
                      (const u8*)&pReq->service_name[0], tlvs);
    }
    if (pReq->service_specific_info_len) {
        tlvs = addTlv(NAN_TLV_TYPE_SERVICE_SPECIFIC_INFO, pReq->service_specific_info_len,
                      (const u8*)&pReq->service_specific_info[0], tlvs);
    }
    if (pReq->rx_match_filter_len) {
        tlvs = addTlv(NAN_TLV_TYPE_RX_MATCH_FILTER, pReq->rx_match_filter_len,
                      (const u8*)&pReq->rx_match_filter[0], tlvs);
    }
    if (pReq->tx_match_filter_len) {
        tlvs = addTlv(NAN_TLV_TYPE_TX_MATCH_FILTER, pReq->tx_match_filter_len,
                      (const u8*)&pReq->tx_match_filter[0], tlvs);
    }

#ifdef NAN_2_0
    int i = 0;
    for (i = 0; i < pReq->num_intf_addr_present; i++)
    {
        tlvs = addTlv(NAN_TLV_TYPE_MAC_ADDRESS,
                      NAN_MAC_ADDR_LEN,
                      (const u8*)&pReq->intf_addr[i][0], tlvs);
    }
#endif /* NAN_2_0 */

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanSubscribeCancel(const NanSubscribeCancelRequest *pReq)
{
    ALOGI("NAN_SUBSCRIBE_CANCEL");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }
    size_t message_len = sizeof(NanSubscribeServiceCancelReqMsg);

    pNanSubscribeServiceCancelReqMsg pFwReq =
        (pNanSubscribeServiceCancelReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    ALOGI("Message Len %d", message_len);
    memset(pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_SUBSCRIBE_SERVICE_CANCEL_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}


int NanCommand::putNanTransmitFollowup(const NanTransmitFollowupRequest *pReq)
{
    ALOGI("TRANSMIT_FOLLOWUP");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    size_t message_len =
        sizeof(NanMsgHeader) + sizeof(NanTransmitFollowupReqParams) +
        (pReq->service_specific_info_len ? SIZEOF_TLV_HDR +
         pReq->service_specific_info_len : 0);

#ifdef NAN_2_0
    /* Mac address needs to be added in TLV */
    message_len += (SIZEOF_TLV_HDR + sizeof(pReq->addr));
#endif

    pNanTransmitFollowupReqMsg pFwReq = (pNanTransmitFollowupReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    ALOGI("Message Len %d", message_len);
    memset (pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_TRANSMIT_FOLLOWUP_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;


#ifndef NAN_2_0
    memcpy(pFwReq->transmitFollowupReqParams.macAddr, pReq->addr,
           sizeof(pFwReq->transmitFollowupReqParams.macAddr));
#else /* NAN_2_0 */
    pFwReq->transmitFollowupReqParams.matchHandle = pReq->match_handle;
#endif /* NAN_2_0 */
    pFwReq->transmitFollowupReqParams.priority = pReq->priority;
    pFwReq->transmitFollowupReqParams.window = pReq->dw_or_faw;
    pFwReq->transmitFollowupReqParams.reserved = 0;

    u8* tlvs = pFwReq->ptlv;

#ifdef NAN_2_0
    /* Mac address needs to be added in TLV */
    tlvs = addTlv(NAN_TLV_TYPE_MAC_ADDRESS, sizeof(pReq->addr),
                  (const u8*)&pReq->addr[0], tlvs);
    u16 tlv_type = NAN_TLV_TYPE_SERVICE_SPECIFIC_INFO;
#else /* NAN_2_0 */
    u16 tlv_type = (pReq->dw_or_faw == 0)? NAN_TLV_TYPE_SERVICE_SPECIFIC_INFO :
        NAN_TLV_TYPE_EXT_SERVICE_SPECIFIC_INFO;
#endif /* NAN_2_0 */

    if (pReq->service_specific_info_len) {
        tlvs = addTlv(tlv_type, pReq->service_specific_info_len,
                      (const u8*)&pReq->service_specific_info[0], tlvs);
    }

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanStats(const NanStatsRequest *pReq)
{
    ALOGI("NAN_STATS");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }
    size_t message_len = sizeof(NanStatsReqMsg);

    pNanStatsReqMsg pFwReq =
        (pNanStatsReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    ALOGI("Message Len %d", message_len);
    memset(pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_STATS_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    pFwReq->statsReqParams.statsId = pReq->stats_id;
    pFwReq->statsReqParams.clear = pReq->clear;
    pFwReq->statsReqParams.reserved = 0;

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanTCA(const NanTCARequest *pReq)
{
    ALOGI("NAN_TCA");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }
    size_t message_len = sizeof(NanTcaReqMsg);

#ifdef NAN_2_0
    message_len += (SIZEOF_TLV_HDR + 2 * sizeof(u32));
#endif

    pNanTcaReqMsg pFwReq =
        (pNanTcaReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    ALOGI("Message Len %d", message_len);
    memset(pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_TCA_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

#ifndef NAN_2_0
    pFwReq->tcaReqParams.tcaId = pReq->tca_id;
    pFwReq->tcaReqParams.rising = pReq->rising_direction_evt_flag;
    pFwReq->tcaReqParams.falling = pReq->falling_direction_evt_flag;
    pFwReq->tcaReqParams.clear = pReq->clear;
    pFwReq->tcaReqParams.reserved = 0;
    pFwReq->tcaReqParams.threshold = pReq->threshold;
#else /* NAN_2_0 */
    u32 tcaReqParams[2];
    memset (tcaReqParams, 0, sizeof(tcaReqParams));
    tcaReqParams[0] = (pReq->rising_direction_evt_flag & 0x01);
    tcaReqParams[0] |= (pReq->falling_direction_evt_flag & 0x01) << 1;
    tcaReqParams[0] |= (pReq->clear & 0x01) << 2;
    tcaReqParams[1] = pReq->threshold;

    u8* tlvs = pFwReq->ptlv;

    tlvs = addTlv(NAN_TLV_TYPE_TCA_CLUSTER_SIZE_REQ, sizeof(tcaReqParams),
                  (const u8*)&tcaReqParams[0], tlvs);
#endif

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;

    return WIFI_SUCCESS;
}

int NanCommand::putNanBeaconSdfPayload(const NanBeaconSdfPayloadRequest *pReq)
{
    int ret = WIFI_ERROR_NOT_SUPPORTED;
#ifdef NAN_2_0
    ALOGI("NAN_BEACON_SDF_PAYLAOD");
    if (pReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }
    size_t message_len = sizeof(NanMsgHeader) + \
        SIZEOF_TLV_HDR + sizeof(u32) + \
        pReq->vsa.vsa_len;

    pNanBeaconSdfPayloadReqMsg pFwReq =
        (pNanBeaconSdfPayloadReqMsg)malloc(message_len);
    if (pFwReq == NULL) {
        return WIFI_ERROR_INVALID_ARGS;
    }

    ALOGI("Message Len %d", message_len);
    memset(pFwReq, 0, message_len);
    pFwReq->fwHeader.msgVersion = (u16)NAN_MSG_VERSION1;
    pFwReq->fwHeader.msgId = NAN_MSG_ID_BEACON_SDF_REQ;
    pFwReq->fwHeader.msgLen = message_len;
    pFwReq->fwHeader.handle = pReq->header.handle;
    pFwReq->fwHeader.transactionId = pReq->header.transaction_id;

    /* Construct First 4 bytes of NanBeaconSdfPayloadReqMsg */
    u32 temp = 0;
    temp = pReq->vsa.payload_transmit_flag & 0x01;
    temp |= (pReq->vsa.tx_in_discovery_beacon & 0x01) << 1;
    temp |= (pReq->vsa.tx_in_sync_beacon & 0x01) << 2;
    temp |= (pReq->vsa.tx_in_service_discovery & 0x01) << 3;
    temp |= (pReq->vsa.vendor_oui & 0x00FFFFFF) << 8;

    int tlv_len = sizeof(u32) + pReq->vsa.vsa_len;
    u8* tempBuf = (u8*)malloc(tlv_len);
    if (tempBuf == NULL) {
        ALOGE("%s: Malloc failed", __func__);
        free(pFwReq);
        return WIFI_ERROR_INVALID_ARGS;
    }
    memset(tempBuf, 0, tlv_len);
    memcpy(tempBuf, &temp, sizeof(u32));
    memcpy((tempBuf + sizeof(u32)), pReq->vsa.vsa, pReq->vsa.vsa_len);

    u8* tlvs = pFwReq->ptlv;

    /* Write the TLVs to the message. */
    tlvs = addTlv(NAN_TLV_TYPE_VENDOR_SPECIFIC_ATTRIBUTE_TRANSMIT, tlv_len,
                  (const u8*)tempBuf, tlvs);
    free(tempBuf);

    mVendorData = (char *)pFwReq;
    mDataLen = message_len;
    ret = WIFI_SUCCESS;
#endif /* NAN_2_0 */
    return ret;
}
//callback handlers registered for nl message send
static int error_handler_nan(struct sockaddr_nl *nla, struct nlmsgerr *err,
                         void *arg)
{
    struct sockaddr_nl * tmp;
    int *ret = (int *)arg;
    tmp = nla;
    *ret = err->error;
    ALOGE("%s: Error code:%d (%s)", __func__, *ret, strerror(-(*ret)));
    return NL_STOP;
}

//callback handlers registered for nl message send
static int ack_handler_nan(struct nl_msg *msg, void *arg)
{
    int *ret = (int *)arg;
    struct nl_msg * a;

    ALOGE("%s: called", __func__);
    a = msg;
    *ret = 0;
    return NL_STOP;
}

//callback handlers registered for nl message send
static int finish_handler_nan(struct nl_msg *msg, void *arg)
{
  int *ret = (int *)arg;
  struct nl_msg * a;

  ALOGE("%s: called", __func__);
  a = msg;
  *ret = 0;
  return NL_SKIP;
}


//Override base class requestEvent and implement little differently here
//This will send the request message
//We dont wait for any response back in case of Nan as it is asynchronous
//thus no wait for condition.
int NanCommand::requestEvent()
{
    int res;
    struct nl_cb * cb;

    cb = nl_cb_alloc(NL_CB_DEFAULT);
    if (!cb) {
        ALOGE("%s: Callback allocation failed",__func__);
        res = -1;
        goto out;
    }

    /* create the message */
    res = create();
    if (res < 0)
        goto out;

    /* send message */
    ALOGE("%s:Handle:%p Socket Value:%p", __func__, mInfo, mInfo->cmd_sock);
    res = nl_send_auto_complete(mInfo->cmd_sock, mMsg.getMessage());
    if (res < 0)
        goto out;
    res = 1;

    nl_cb_err(cb, NL_CB_CUSTOM, error_handler_nan, &res);
    nl_cb_set(cb, NL_CB_FINISH, NL_CB_CUSTOM, finish_handler_nan, &res);
    nl_cb_set(cb, NL_CB_ACK, NL_CB_CUSTOM, ack_handler_nan, &res);

    // err is populated as part of finish_handler
    while (res > 0)
        nl_recvmsgs(mInfo->cmd_sock, cb);

    ALOGD("%s: Command invoked return value:%d",__func__, res);

out:
    //free the VendorData
    if (mVendorData) {
        free(mVendorData);
    }
    mVendorData = NULL;
    //cleanup the mMsg
    mMsg.destroy();
    return res;
}

int NanCommand::calcNanTransmitPostDiscoverySize(
    const NanTransmitPostDiscovery *pPostDiscovery)
{
    /* Fixed size of u32 for Conn Type, Device Role and R flag + Dur + Rsvd*/
    int ret = sizeof(u32);
    /* size of availability interval bit map is 4 bytes */
    ret += sizeof(u32);
    /* size of mac address is 6 bytes*/
    ret += (SIZEOF_TLV_HDR + NAN_MAC_ADDR_LEN);
    if (pPostDiscovery &&
        pPostDiscovery->type == NAN_CONN_WLAN_MESH) {
        /* size of WLAN_MESH_ID  */
        ret += (SIZEOF_TLV_HDR + \
                pPostDiscovery->mesh_id_len);
    }
    if (pPostDiscovery &&
        pPostDiscovery->type == NAN_CONN_WLAN_INFRA) {
        /* size of Infrastructure ssid  */
        ret += (SIZEOF_TLV_HDR + \
                pPostDiscovery->infrastructure_ssid_len);
    }
    ALOGI("%s:size:%d", __func__, ret);
    return ret;
}

void NanCommand::fillNanSocialChannelParamVal(
    const NanSocialChannelScanParams *pScanParams,
    u32* pChannelParamArr)
{
    int i;
    if (pChannelParamArr) {
        memset(pChannelParamArr, 0,
               NAN_MAX_SOCIAL_CHANNEL * sizeof(u32));
        for (i= 0; i < NAN_MAX_SOCIAL_CHANNEL; i++) {
            pChannelParamArr[i] = pScanParams->scan_period[i] << 16;
            pChannelParamArr[i] |= pScanParams->dwell_time[i] << 8;
        }
        pChannelParamArr[NAN_CHANNEL_6] |= 6;
        pChannelParamArr[NAN_CHANNEL_44]|= 44;
        pChannelParamArr[NAN_CHANNEL_149]|= 149;
        ALOGI("%s: Filled SocialChannelParamVal", __func__);
        hexdump((char*)pChannelParamArr, NAN_MAX_SOCIAL_CHANNEL * sizeof(u32));
    }
    return;
}

u32 NanCommand::getNanTransmitPostConnectivityCapabilityVal(
    const NanTransmitPostConnectivityCapability *pCapab)
{
    u32 ret = 0;
    ret |= (pCapab->payload_transmit_flag? 1:0) << 16;
    ret |= (pCapab->is_mesh_supported? 1:0) << 5;
    ret |= (pCapab->is_ibss_supported? 1:0) << 4;
    ret |= (pCapab->wlan_infra_field? 1:0) << 3;
    ret |= (pCapab->is_tdls_supported? 1:0) << 2;
    ret |= (pCapab->is_wfds_supported? 1:0) << 1;
    ret |= (pCapab->is_wfd_supported? 1:0);
    ALOGI("%s: val:%d", __func__, ret);
    return ret;
}

void NanCommand::fillNanTransmitPostDiscoveryVal(
    const NanTransmitPostDiscovery *pTxDisc,
    u8 *pOutValue)
{
#ifdef NAN_2_0
    if (pTxDisc && pOutValue) {
        u8 *tlvs = &pOutValue[8];
        pOutValue[0] = pTxDisc->type;
        pOutValue[1] = pTxDisc->role;
        pOutValue[2] = (pTxDisc->transmit_freq? 1:0);
        pOutValue[2] |= ((pTxDisc->duration & 0x03) << 1);
        memcpy(&pOutValue[4], &pTxDisc->avail_interval_bitmap,
               sizeof(pTxDisc->avail_interval_bitmap));
        tlvs = addTlv(NAN_TLV_TYPE_MAC_ADDRESS,
                    NAN_MAC_ADDR_LEN,
                    (const u8*)&pTxDisc->addr[0],
                    tlvs);
        if (pTxDisc->type == NAN_CONN_WLAN_MESH) {
            tlvs = addTlv(NAN_TLV_TYPE_WLAN_MESH_ID,
                        pTxDisc->mesh_id_len,
                        (const u8*)&pTxDisc->mesh_id[0],
                        tlvs);
        }
        if (pTxDisc->type == NAN_CONN_WLAN_INFRA) {
            tlvs = addTlv(NAN_TLV_TYPE_FW_WLAN_INFRASTRUCTURE_SSID,
                        pTxDisc->infrastructure_ssid_len,
                        (const u8*)&pTxDisc->infrastructure_ssid_val[0],
                        tlvs);
        }
        ALOGI("%s: Filled TransmitPostDiscoveryVal", __func__);
        hexdump((char*)pOutValue, calcNanTransmitPostDiscoverySize(pTxDisc));
    }
#endif /* NAN_2_0 */
    return;
}

void NanCommand::fillNanFurtherAvailabilityMapVal(
    const NanFurtherAvailabilityMap *pFam,
    u8 *pOutValue)
{
//ToDo: Fixme - build issue
#if 0
    int idx = 0;
    if (pFam && pOutValue) {
        u32 famsize = calcNanFurtherAvailabilityMapSize(pFam);
        pNanFurtherAvailabilityMapAttrTlv pFwReq = \
            (pNanFurtherAvailabilityMapAttrTlv)pOutValue;

        memset(pOutValue, 0, famsize);
        pFwReq->numChan = pFam->numchans;
        for (idx = 0; idx < pFam->numchans; idx++) {
            const NanFurtherAvailabilityChannel *pFamChan =  \
                &pFam->famchan[idx];
            pNanFurtherAvailabilityChan pFwFamChan = \
                (pNanFurtherAvailabilityChan)((u8*)&pFwReq->pFaChan[0] + \
                (idx * sizeof(NanFurtherAvailabilityChan)));

            pFwFamChan->entryCtrl.availIntDuration = \
                pFamChan->entry_control;
            pFwFamChan->entryCtrl.mapId = \
                pFamChan->mapid;
            pFwFamChan->opClass =  pFamChan->class_val;
            pFwFamChan->channel = pFamChan->channel;
            memcpy(&pFwFamChan->availIntBitmap,
                   &pFamChan->avail_interval_bitmap,
                   sizeof(pFwFamChan->availIntBitmap));
        }
        ALOGI("%s: Filled FurtherAvailabilityMapVal", __func__);
        hexdump((char*)pOutValue, famsize);
    }
#endif
    return;
}

int NanCommand::calcNanFurtherAvailabilityMapSize(
    const NanFurtherAvailabilityMap *pFam)
{
    int ret = 0;
    if (pFam && pFam->numchans &&
        pFam->numchans <= NAN_MAX_FAM_CHANNELS) {
        /* Fixed size of u8 for numchans*/
        ret = sizeof(u8);
        /* numchans * sizeof(FamChannels) */
        //ToDo: Fix build
        //ret += (pFam->numchans * sizeof(NanFurtherAvailabilityChan));
    }
    ALOGI("%s:size:%d", __func__, ret);
    return ret;
}

