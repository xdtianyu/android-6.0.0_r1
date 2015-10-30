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

#ifndef __WIFI_HAL_NAN_COMMAND_H__
#define __WIFH_HAL_NAN_COMMAND_H__

#include "common.h"
#include "cpp_bindings.h"
#include "nan.h"

class NanCommand : public WifiVendorCommand
{
private:
    NanCallbackHandler mHandler;
    char *mNanVendorEvent;
    u32 mNanDataLen;
    NanStaParameter *mStaParam;
    void *mUserData;

    //Function to check the initial few bytes of data to
    //determine whether NanResponse or NanEvent
    int isNanResponse();
    //Function which unparses the data and calls the NotifyResponse
    int handleNanResponse();
    //Function which will parse the mVendorData and gets
    // the rsp_data appropriately.
    int getNanResponse(NanResponseMsg *pRsp);

    //Function which will return the Nan Indication type based on
    //the initial few bytes of mVendorData
    NanIndicationType getIndicationType();
    //Function which calls the necessaryIndication callback
    //based on the indication type
    int handleNanIndication();
    //Various Functions to get the appropriate indications
    int getNanPublishReplied(NanPublishRepliedInd *event);
    int getNanPublishTerminated(NanPublishTerminatedInd *event);
    int getNanMatch(NanMatchInd *event);
    int getNanUnMatch(NanUnmatchInd *event);
    int getNanSubscribeTerminated(NanSubscribeTerminatedInd *event);
    int getNanFollowup(NanFollowupInd *event);
    int getNanDiscEngEvent(NanDiscEngEventInd *event);
    int getNanDisabled(NanDisabledInd *event);
    int getNanTca(NanTCAInd *event);
    int getNanBeaconSdfPayload(NanBeaconSdfPayloadInd *event);

    //Making the constructor private since this class is a singleton
    NanCommand(wifi_handle handle, int id, u32 vendor_id, u32 subcmd);

    static NanCommand *mNanCommandInstance;

    // Other private helper functions
    int calcNanTransmitPostDiscoverySize(
        const NanTransmitPostDiscovery *pPostDiscovery);
    void fillNanSocialChannelParamVal(
        const NanSocialChannelScanParams *pScanParams,
        u32* pChannelParamArr);
    u32 getNanTransmitPostConnectivityCapabilityVal(
        const NanTransmitPostConnectivityCapability *pCapab);
    void fillNanTransmitPostDiscoveryVal(
        const NanTransmitPostDiscovery *pTxDisc,
        u8 *pOutValue);
    int calcNanFurtherAvailabilityMapSize(
        const NanFurtherAvailabilityMap *pFam);
    void fillNanFurtherAvailabilityMapVal(
        const NanFurtherAvailabilityMap *pFam,
        u8 *pOutValue);

    void getNanReceivePostConnectivityCapabilityVal(
        const u8* pInValue,
        NanReceivePostConnectivityCapability *pRxCapab);
    int getNanReceivePostDiscoveryVal(const u8 *pInValue,
                                      u32 length,
                                      NanReceivePostDiscovery *pRxDisc);
    int getNanFurtherAvailabilityMap(const u8 *pInValue,
                                     u32 length,
                                     NanFurtherAvailabilityMap *pFam);

public:
    static NanCommand* instance(wifi_handle handle);
    virtual ~NanCommand();

    // This function implements creation of NAN specific Request
    // based on  the request type
    virtual int create();
    virtual int requestEvent();
    virtual int handleResponse(WifiEvent reply);
    virtual int handleEvent(WifiEvent &event);
    int setCallbackHandler(NanCallbackHandler nHandler,
                           void *pUserData);


    //Functions to fill the vendor data appropriately
    int putNanEnable(const NanEnableRequest *pReq);
    int putNanDisable(const NanDisableRequest *pReq);
    int putNanPublish(const NanPublishRequest *pReq);
    int putNanPublishCancel(const NanPublishCancelRequest *pReq);
    int putNanSubscribe(const NanSubscribeRequest *pReq);
    int putNanSubscribeCancel(const NanSubscribeCancelRequest *pReq);
    int putNanTransmitFollowup(const NanTransmitFollowupRequest *pReq);
    int putNanStats(const NanStatsRequest *pReq);
    int putNanConfig(const NanConfigRequest *pReq);
    int putNanTCA(const NanTCARequest *pReq);
    int putNanBeaconSdfPayload(const NanBeaconSdfPayloadRequest *pReq);
    int getNanStaParameter(NanStaParameter *pRsp);

    //Set the Id of the request
    void setId(int nId);
};
#endif /* __WIFH_HAL_NAN_COMMAND_H__ */

