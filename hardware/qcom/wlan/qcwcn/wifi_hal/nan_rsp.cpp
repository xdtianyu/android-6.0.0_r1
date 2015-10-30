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


int NanCommand::isNanResponse()
{
    if (mNanVendorEvent == NULL) {
        ALOGE("NULL check failed");
        return WIFI_ERROR_INVALID_ARGS;
    }

    NanMsgHeader *pHeader = (NanMsgHeader *)mNanVendorEvent;

    switch (pHeader->msgId) {
    case NAN_MSG_ID_ERROR_RSP:
    case NAN_MSG_ID_CONFIGURATION_RSP:
    case NAN_MSG_ID_PUBLISH_SERVICE_CANCEL_RSP:
    case NAN_MSG_ID_PUBLISH_SERVICE_RSP:
    case NAN_MSG_ID_SUBSCRIBE_SERVICE_RSP:
    case NAN_MSG_ID_SUBSCRIBE_SERVICE_CANCEL_RSP:
    case NAN_MSG_ID_TRANSMIT_FOLLOWUP_RSP:
    case NAN_MSG_ID_STATS_RSP:
    case NAN_MSG_ID_ENABLE_RSP:
    case NAN_MSG_ID_DISABLE_RSP:
    case NAN_MSG_ID_TCA_RSP:
#ifdef NAN_2_0
    case NAN_MSG_ID_BEACON_SDF_RSP:
#endif /* NAN_2_0 */
        return 1;
    default:
        return 0;
    }
}


int NanCommand::getNanResponse(NanResponseMsg *pRsp)
{
    if (mNanVendorEvent == NULL || pRsp == NULL) {
        ALOGE("NULL check failed");
        return WIFI_ERROR_INVALID_ARGS;
    }

    NanMsgHeader *pHeader = (NanMsgHeader *)mNanVendorEvent;

    switch (pHeader->msgId) {
        case NAN_MSG_ID_ERROR_RSP:
        {
            pNanErrorRspMsg pFwRsp = \
                (pNanErrorRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_ERROR;
            break;
        }
        case NAN_MSG_ID_CONFIGURATION_RSP:
        {
            pNanConfigurationRspMsg pFwRsp = \
                (pNanConfigurationRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_CONFIG;
        }
        break;
        case NAN_MSG_ID_PUBLISH_SERVICE_CANCEL_RSP:
        {
            pNanPublishServiceCancelRspMsg pFwRsp = \
                (pNanPublishServiceCancelRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_PUBLISH_CANCEL;
            break;
        }
        case NAN_MSG_ID_PUBLISH_SERVICE_RSP:
        {
            pNanPublishServiceRspMsg pFwRsp = \
                (pNanPublishServiceRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_PUBLISH;
            break;
        }
        case NAN_MSG_ID_SUBSCRIBE_SERVICE_RSP:
        {
            pNanSubscribeServiceRspMsg pFwRsp = \
                (pNanSubscribeServiceRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_SUBSCRIBE;
        }
        break;
        case NAN_MSG_ID_SUBSCRIBE_SERVICE_CANCEL_RSP:
        {
            pNanSubscribeServiceCancelRspMsg pFwRsp = \
                (pNanSubscribeServiceCancelRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_SUBSCRIBE_CANCEL;
            break;
        }
        case NAN_MSG_ID_TRANSMIT_FOLLOWUP_RSP:
        {
            pNanTransmitFollowupRspMsg pFwRsp = \
                (pNanTransmitFollowupRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_TRANSMIT_FOLLOWUP;
            break;
        }
        case NAN_MSG_ID_STATS_RSP:
        {
            pNanStatsRspMsg pFwRsp = \
                (pNanStatsRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->statsRspParams.status;
            pRsp->value = pFwRsp->statsRspParams.value;
            pRsp->response_type = NAN_RESPONSE_STATS;
            pRsp->body.stats_response.stats_id = \
                (NanStatsId)pFwRsp->statsRspParams.statsId;
            ALOGI("%s: stats_id:%d",__func__,
                  pRsp->body.stats_response.stats_id);
            u8 *pInputTlv = pFwRsp->ptlv;
            NanTlv outputTlv;
            memset(&outputTlv, 0, sizeof(outputTlv));
            u16 readLen = 0;
            int remainingLen = (mNanDataLen -  \
                (sizeof(NanMsgHeader) + sizeof(NanStatsRspParams)));

            if (remainingLen > 0) {
                readLen = NANTLV_ReadTlv(pInputTlv, &outputTlv);
                ALOGI("%s: Remaining Len:%d readLen:%d type:%d length:%d",
                      __func__, remainingLen, readLen, outputTlv.type,
                      outputTlv.length);
                if (outputTlv.length <= \
                    sizeof(pRsp->body.stats_response.data)) {
                    memcpy(&pRsp->body.stats_response.data, outputTlv.value,
                           outputTlv.length);
                    hexdump((char*)&pRsp->body.stats_response.data, outputTlv.length);
                }
                else {
                    ALOGE("%s:copying only sizeof(pRsp->body.stats_response.data):%d",
                          __func__, sizeof(pRsp->body.stats_response.data));
                    memcpy(&pRsp->body.stats_response.data, outputTlv.value,
                           sizeof(pRsp->body.stats_response.data));
                    hexdump((char*)&pRsp->body.stats_response.data,
                            sizeof(pRsp->body.stats_response.data));
                }
            }
            else
                ALOGI("%s: No TLV's present",__func__);
            break;
        }
        case NAN_MSG_ID_ENABLE_RSP:
        {
            pNanEnableRspMsg pFwRsp = \
                (pNanEnableRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_ENABLED;
            break;
        }
        case NAN_MSG_ID_DISABLE_RSP:
        {
            pNanDisableRspMsg pFwRsp = \
                (pNanDisableRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = 0;
            pRsp->response_type = NAN_RESPONSE_DISABLED;
            break;
        }
        case NAN_MSG_ID_TCA_RSP:
        {
            pNanTcaRspMsg pFwRsp = \
                (pNanTcaRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = pFwRsp->value;
            pRsp->response_type = NAN_RESPONSE_TCA;
            break;
        }
#ifdef NAN_2_0
        case NAN_MSG_ID_BEACON_SDF_RSP:
        {
            pNanBeaconSdfPayloadRspMsg pFwRsp = \
                (pNanBeaconSdfPayloadRspMsg)mNanVendorEvent;
            pRsp->header.handle = pFwRsp->fwHeader.handle;
            pRsp->header.transaction_id = pFwRsp->fwHeader.transactionId;
            pRsp->status = pFwRsp->status;
            pRsp->value = 0;
            pRsp->response_type = NAN_RESPONSE_BEACON_SDF_PAYLOAD;
            break;
        }
#endif /* NAN_2_0 */
        default:
            return  -1;
    }
    return  0;
}

int NanCommand::handleNanResponse()
{
    //parse the data and call
    //the response callback handler with the populated
    //NanResponseMsg
    NanResponseMsg  rsp_data;
    int ret;

    ALOGV("handleNanResponse called %p", this);
    memset(&rsp_data, 0, sizeof(rsp_data));
    //get the rsp_data
    ret = getNanResponse(&rsp_data);

    ALOGI("handleNanResponse ret:%d status:%u value:%u response_type:%u",
          ret, rsp_data.status, rsp_data.value, rsp_data.response_type);
    if (ret == 0 && (rsp_data.response_type == NAN_RESPONSE_STATS) &&
        (mStaParam != NULL) &&
        (rsp_data.body.stats_response.stats_id == NAN_STATS_ID_DE_TIMING_SYNC)) {
        /*
           Fill the staParam with appropriate values and return from here.
           No need to call NotifyResponse as the request is for getting the
           STA response
        */
        NanSyncStats *pSyncStats = &rsp_data.body.stats_response.data.sync_stats;
        mStaParam->master_rank = pSyncStats->myRank;
        mStaParam->master_pref = (pSyncStats->myRank & 0xFF00000000000000) >> 56;
        mStaParam->random_factor = (pSyncStats->myRank & 0x00FF000000000000) >> 48;
        mStaParam->hop_count = pSyncStats->currAmHopCount;
        mStaParam->beacon_transmit_time = pSyncStats->currAmBTT;

        return ret;
    }
    //Call the NotifyResponse Handler
    if (ret == 0 && mHandler.NotifyResponse) {
        (*mHandler.NotifyResponse)(&rsp_data, mUserData);
    }
    return ret;
}
