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

#include "nan.h"
#include "nan_i.h"
#include "wifi_hal.h"
#include "common.h"
#include "cpp_bindings.h"
#include <utils/Log.h>
#include "nancommand.h"

#ifdef __GNUC__
#define PRINTF_FORMAT(a,b) __attribute__ ((format (printf, (a), (b))))
#define STRUCT_PACKED __attribute__ ((packed))
#else
#define PRINTF_FORMAT(a,b)
#define STRUCT_PACKED
#endif

#include "qca-vendor.h"

//Singleton Static Instance
NanCommand* NanCommand::mNanCommandInstance  = NULL;

//Implementation of the functions exposed in nan.h
wifi_error nan_register_handler(wifi_handle handle,
                                NanCallbackHandler handlers,
                                void* userdata)
{
    // Obtain the singleton instance
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }
    ret = nCommand->setCallbackHandler(handlers, userdata);
    return (wifi_error)ret;
}

wifi_error nan_get_version(wifi_handle handle,
                           NanVersion* version)
{
    *version = (NAN_MAJOR_VERSION <<16 | NAN_MINOR_VERSION << 8 | NAN_MICRO_VERSION);
    return WIFI_SUCCESS;
}

/*  Function to send enable request to the wifi driver.*/
wifi_error nan_enable_request(wifi_request_id id,
                              wifi_handle handle,
                              NanEnableRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanEnable(msg);
    if (ret != 0) {
        ALOGE("%s: putNanEnable Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send disable request to the wifi driver.*/
wifi_error nan_disable_request(wifi_request_id id,
                               wifi_handle handle,
                               NanDisableRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanDisable(msg);
    if (ret != 0) {
        ALOGE("%s: putNanDisable Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send publish request to the wifi driver.*/
wifi_error nan_publish_request(wifi_request_id id,
                               wifi_handle handle,
                               NanPublishRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanPublish(msg);
    if (ret != 0) {
        ALOGE("%s: putNanPublish Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send publish cancel to the wifi driver.*/
wifi_error nan_publish_cancel_request(wifi_request_id id,
                                      wifi_handle handle,
                                      NanPublishCancelRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanPublishCancel(msg);
    if (ret != 0) {
        ALOGE("%s: putNanPublishCancel Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send Subscribe request to the wifi driver.*/
wifi_error nan_subscribe_request(wifi_request_id id,
                                 wifi_handle handle,
                                 NanSubscribeRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanSubscribe(msg);
    if (ret != 0) {
        ALOGE("%s: putNanSubscribe Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to cancel subscribe to the wifi driver.*/
wifi_error nan_subscribe_cancel_request(wifi_request_id id,
                                        wifi_handle handle,
                                        NanSubscribeCancelRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanSubscribeCancel(msg);
    if (ret != 0) {
        ALOGE("%s: putNanSubscribeCancel Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send NAN follow up request to the wifi driver.*/
wifi_error nan_transmit_followup_request(wifi_request_id id,
                                         wifi_handle handle,
                                         NanTransmitFollowupRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanTransmitFollowup(msg);
    if (ret != 0) {
        ALOGE("%s: putNanTransmitFollowup Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send NAN statistics request to the wifi driver.*/
wifi_error nan_stats_request(wifi_request_id id,
                             wifi_handle handle,
                             NanStatsRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanStats(msg);
    if (ret != 0) {
        ALOGE("%s: putNanStats Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send NAN configuration request to the wifi driver.*/
wifi_error nan_config_request(wifi_request_id id,
                              wifi_handle handle,
                              NanConfigRequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanConfig(msg);
    if (ret != 0) {
        ALOGE("%s: putNanConfig Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_tca_request(wifi_request_id id,
                           wifi_handle handle,
                           NanTCARequest* msg)
{
    int ret = 0;
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanTCA(msg);
    if (ret != 0) {
        ALOGE("%s: putNanTCA Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
cleanup:
    return (wifi_error)ret;
}

/*  Function to send NAN Beacon sdf payload to the wifi driver.
    This instructs the Discovery Engine to begin publishing the
    received payload in any Beacon or Service Discovery Frame
    transmitted*/
wifi_error nan_beacon_sdf_payload_request(wifi_request_id id,
                                         wifi_handle handle,
                                         NanBeaconSdfPayloadRequest* msg)
{
    int ret = WIFI_ERROR_NOT_SUPPORTED;
#ifdef NAN_2_0
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    ret = nCommand->putNanBeaconSdfPayload(msg);
    if (ret != 0) {
        ALOGE("%s: putNanBeaconSdfPayload Error:%d",__func__, ret);
        goto cleanup;
    }
    nCommand->setId(id);
    ret = nCommand->requestEvent();
    if (ret != 0) {
        ALOGE("%s: requestEvent Error:%d",__func__, ret);
    }
#endif /* NAN_2_0 */
cleanup:
    return (wifi_error)ret;
}

wifi_error nan_get_sta_parameter(wifi_request_id id,
                                 wifi_handle handle,
                                 NanStaParameter* msg)
{
    int ret = WIFI_ERROR_NOT_SUPPORTED;
#ifdef NAN_2_0
    NanCommand *nCommand;

    nCommand = NanCommand::instance(handle);
    if (nCommand == NULL) {
        ALOGE("%s: Error NanCommand NULL", __func__);
        return WIFI_ERROR_UNKNOWN;
    }

    nCommand->setId(id);
    ret = nCommand->getNanStaParameter(msg);
    if (ret != 0) {
        ALOGE("%s: getNanStaParameter Error:%d",__func__, ret);
        goto cleanup;
    }
#endif /* NAN_2_0 */
cleanup:
    return (wifi_error)ret;
}

// Implementation related to nan class common functions
// Constructor
//Making the constructor private since this class is a singleton
NanCommand::NanCommand(wifi_handle handle, int id, u32 vendor_id, u32 subcmd)
        : WifiVendorCommand(handle, id, vendor_id, subcmd)
{
    ALOGV("NanCommand %p constructed", this);
    memset(&mHandler, 0,sizeof(mHandler));
    mNanVendorEvent = NULL;
    mNanDataLen = 0;
    mStaParam = NULL;
    mUserData = NULL;
}

NanCommand* NanCommand::instance(wifi_handle handle)
{
    if (handle == NULL) {
        ALOGE("Handle is invalid");
        return NULL;
    }
    if (mNanCommandInstance == NULL) {
        mNanCommandInstance = new NanCommand(handle, 0,
                                             OUI_QCA,
                                             QCA_NL80211_VENDOR_SUBCMD_NAN);
        ALOGV("NanCommand %p created", mNanCommandInstance);
        return mNanCommandInstance;
    }
    else
    {
        if (handle != getWifiHandle(mNanCommandInstance->mInfo)) {
            /* upper layer must have cleaned up the handle and reinitialized,
               so we need to update the same */
            ALOGI("Handle different, update the handle");
            mNanCommandInstance->mInfo = (hal_info *)handle;
        }
    }
    ALOGV("NanCommand %p created already", mNanCommandInstance);
    return mNanCommandInstance;
}

NanCommand::~NanCommand()
{
    ALOGV("NanCommand %p destroyed", this);
    unregisterVendorHandler(mVendor_id, mSubcmd);
}

// This function implements creation of Vendor command
// For NAN just call base Vendor command create
int NanCommand::create() {
    return (WifiVendorCommand::create());
}

int NanCommand::handleResponse(WifiEvent reply){
    ALOGI("skipping a response");
    return NL_SKIP;
}

int NanCommand::setCallbackHandler(NanCallbackHandler nHandler,
                                   void *pUserData)
{
    int res = 0;
    mHandler = nHandler;
    mUserData = pUserData;
    res = registerVendorHandler(mVendor_id, mSubcmd);
    if (res != 0) {
        //error case should not happen print log
        ALOGE("%s: Unable to register Vendor Handler Vendor Id=0x%x subcmd=%u",
              __func__, mVendor_id, mSubcmd);
    }
    return res;
}

// This function will be the main handler for incoming event
// QCA_NL80211_VENDOR_SUBCMD_NAN
//Call the appropriate callback handler after parsing the vendor data.
int NanCommand::handleEvent(WifiEvent &event)
{
    ALOGI("Got a NAN message from Driver");
    WifiVendorCommand::handleEvent(event);

    if (mSubcmd == QCA_NL80211_VENDOR_SUBCMD_NAN){
        // Parse the vendordata and get the NAN attribute
        struct nlattr *tb_vendor[QCA_WLAN_VENDOR_ATTR_MAX + 1];
        nla_parse(tb_vendor, QCA_WLAN_VENDOR_ATTR_MAX,
                  (struct nlattr *)mVendorData,
                  mDataLen, NULL);
        // Populating the mNanVendorEvent and mNanDataLen to point to NAN data.
        mNanVendorEvent = (char *)nla_data(tb_vendor[QCA_WLAN_VENDOR_ATTR_NAN]);
        mNanDataLen = nla_len(tb_vendor[QCA_WLAN_VENDOR_ATTR_NAN]);

        if (isNanResponse()) {
            //handleNanResponse will parse the data and call
            //the response callback handler with the populated
            //NanResponseMsg
            handleNanResponse();
        }
        else {
            //handleNanIndication will parse the data and call
            //the corresponding Indication callback handler
            //with the corresponding populated Indication event
            handleNanIndication();
        }
    }
    else {
        //error case should not happen print log
        ALOGE("%s: Wrong NAN subcmd received %d", __func__, mSubcmd);
    }
    return NL_SKIP;
}

/*Helper function to Write and Read TLV called in indication as well as request */
u16 NANTLV_WriteTlv(pNanTlv pInTlv, u8 *pOutTlv)
{
    u16 writeLen = 0;
    u16 i;

    if (!pInTlv)
    {
        ALOGE("NULL pInTlv");
        return writeLen;
    }

    if (!pOutTlv)
    {
        ALOGE("NULL pOutTlv");
        return writeLen;
    }

    *pOutTlv++ = pInTlv->type & 0xFF;
    *pOutTlv++ = (pInTlv->type & 0xFF00) >> 8;
    writeLen += 2;

    ALOGV("WRITE TLV type %u, writeLen %u", pInTlv->type, writeLen);

    *pOutTlv++ = pInTlv->length & 0xFF;
    *pOutTlv++ = (pInTlv->length & 0xFF00) >> 8;
    writeLen += 2;

    ALOGV("WRITE TLV length %u, writeLen %u", pInTlv->length, writeLen);

    for (i=0; i < pInTlv->length; ++i)
    {
        *pOutTlv++ = pInTlv->value[i];
    }

    writeLen += pInTlv->length;
    ALOGV("WRITE TLV value, writeLen %u", writeLen);
    return writeLen;
}

u16 NANTLV_ReadTlv(u8 *pInTlv, pNanTlv pOutTlv)
{
    u16 readLen = 0;
    u16 tmp = 0;

    if (!pInTlv)
    {
        ALOGE("NULL pInTlv");
        return readLen;
    }

    if (!pOutTlv)
    {
        ALOGE("NULL pOutTlv");
        return readLen;
    }

    pOutTlv->type = *pInTlv++;
    pOutTlv->type |= *pInTlv++ << 8;
    readLen += 2;

    ALOGV("READ TLV type %u, readLen %u", pOutTlv->type, readLen);

    pOutTlv->length = *pInTlv++;
    pOutTlv->length |= *pInTlv++ << 8;
    readLen += 2;

    ALOGV("READ TLV length %u, readLen %u", pOutTlv->length, readLen);

    if (pOutTlv->length)
    {
        pOutTlv->value = pInTlv;
        readLen += pOutTlv->length;
    }
    else
    {
        pOutTlv->value = NULL;
    }

    ALOGV("READ TLV value %u, readLen %u", pOutTlv->value, readLen);

    /* Map the right TLV value based on NAN version in Firmware
       which the framework can understand*/
    tmp = pOutTlv->type;
    pOutTlv->type = getNanTlvtypeFromFWTlvtype(pOutTlv->type);
    ALOGI("%s: FWTlvtype:%d NanTlvtype:%d", __func__,
          tmp, pOutTlv->type);
    return readLen;
}

u8* addTlv(u16 type, u16 length, const u8* value, u8* pOutTlv)
{
   NanTlv nanTlv;
   u16 len;
   u16 tmp =0;

   /* Set the right TLV based on NAN version in Firmware */
   tmp = type;
   type = getFWTlvtypeFromNanTlvtype(type);
   ALOGI("%s: NanTlvtype:%d FWTlvtype:%d", __func__,
         tmp, type);

   nanTlv.type = type;
   nanTlv.length = length;
   nanTlv.value = (u8*)value;

   len = NANTLV_WriteTlv(&nanTlv, pOutTlv);
   return (pOutTlv + len);
}

void NanCommand::setId(int nId)
{
    mId = nId;
}

u16 getNanTlvtypeFromFWTlvtype(u16 fwTlvtype)
{
#ifndef NAN_2_0
    /* In case of Pronto no mapping required */
    return fwTlvtype;
#else /* NAN_2_0 */
    if (fwTlvtype <= NAN_TLV_TYPE_FW_SERVICE_SPECIFIC_INFO) {
        /* return the TLV value as is */
        return fwTlvtype;
    }
    if (fwTlvtype >= NAN_TLV_TYPE_FW_TCA_LAST) {
        return fwTlvtype;
    }
    /* Other FW TLV values and Config types map it
       appropriately
    */
    switch (fwTlvtype) {
    case NAN_TLV_TYPE_FW_EXT_SERVICE_SPECIFIC_INFO:
        return NAN_TLV_TYPE_EXT_SERVICE_SPECIFIC_INFO;
    case NAN_TLV_TYPE_FW_VENDOR_SPECIFIC_ATTRIBUTE_TRANSMIT:
        return NAN_TLV_TYPE_VENDOR_SPECIFIC_ATTRIBUTE_TRANSMIT;
    case NAN_TLV_TYPE_FW_VENDOR_SPECIFIC_ATTRIBUTE_RECEIVE:
        return NAN_TLV_TYPE_VENDOR_SPECIFIC_ATTRIBUTE_RECEIVE;
    case NAN_TLV_TYPE_FW_POST_NAN_CONNECTIVITY_CAPABILITIES_RECEIVE:
        return NAN_TLV_TYPE_POST_NAN_CONNECTIVITY_CAPABILITIES_RECEIVE;
    case NAN_TLV_TYPE_FW_POST_NAN_DISCOVERY_ATTRIBUTE_RECEIVE:
        return NAN_TLV_TYPE_POST_NAN_DISCOVERY_ATTRIBUTE_RECEIVE;
    case NAN_TLV_TYPE_FW_BEACON_SDF_PAYLOAD_RECEIVE:
        return NAN_TLV_TYPE_BEACON_SDF_PAYLOAD_RECEIVE;

    case NAN_TLV_TYPE_FW_24G_SUPPORT:
        return NAN_TLV_TYPE_2DOT4G_SUPPORT;
    case NAN_TLV_TYPE_FW_24G_BEACON:
            return NAN_TLV_TYPE_2DOT4G_BEACONS;
    case NAN_TLV_TYPE_FW_24G_SDF:
        return NAN_TLV_TYPE_2DOT4G_SDF;
    case NAN_TLV_TYPE_FW_24G_RSSI_CLOSE:
        return NAN_TLV_TYPE_RSSI_CLOSE;
    case NAN_TLV_TYPE_FW_24G_RSSI_MIDDLE:
        return NAN_TLV_TYPE_RSSI_MEDIUM;
    case NAN_TLV_TYPE_FW_24G_RSSI_CLOSE_PROXIMITY:
        return NAN_TLV_TYPE_RSSI_CLOSE_PROXIMITY;
    case NAN_TLV_TYPE_FW_5G_SUPPORT:
        return NAN_TLV_TYPE_5G_SUPPORT;
    case NAN_TLV_TYPE_FW_5G_BEACON:
        return NAN_TLV_TYPE_5G_BEACON;
    case NAN_TLV_TYPE_FW_5G_SDF:
            return NAN_TLV_TYPE_5G_SDF;
    case NAN_TLV_TYPE_FW_5G_RSSI_CLOSE:
            return NAN_TLV_TYPE_5G_RSSI_CLOSE;
    case NAN_TLV_TYPE_FW_5G_RSSI_MIDDLE:
        return NAN_TLV_TYPE_5G_RSSI_MEDIUM;
    case NAN_TLV_TYPE_FW_5G_RSSI_CLOSE_PROXIMITY:
        return NAN_TLV_TYPE_5G_RSSI_CLOSE_PROXIMITY;
    case NAN_TLV_TYPE_FW_SID_BEACON:
        return NAN_TLV_TYPE_SID_BEACON;
    case NAN_TLV_TYPE_FW_HOP_COUNT_LIMIT:
        return NAN_TLV_TYPE_HOP_COUNT_LIMIT;
    case NAN_TLV_TYPE_FW_MASTER_PREFERENCE:
        return NAN_TLV_TYPE_MASTER_PREFERENCE;
    case NAN_TLV_TYPE_FW_CLUSTER_ID_LOW:
        return NAN_TLV_TYPE_CLUSTER_ID_LOW;
    case NAN_TLV_TYPE_FW_CLUSTER_ID_HIGH:
        return NAN_TLV_TYPE_CLUSTER_ID_HIGH;
    case NAN_TLV_TYPE_FW_RSSI_AVERAGING_WINDOW_SIZE:
            return NAN_TLV_TYPE_RSSI_AVERAGING_WINDOW_SIZE;
    case NAN_TLV_TYPE_FW_CLUSTER_OUI_NETWORK_ID:
        return NAN_TLV_TYPE_CLUSTER_OUI_NETWORK_ID;
    case NAN_TLV_TYPE_FW_SOURCE_MAC_ADDRESS:
            return NAN_TLV_TYPE_SOURCE_MAC_ADDRESS;
    case NAN_TLV_TYPE_FW_CLUSTER_ATTRIBUTE_IN_SDF:
        return NAN_TLV_TYPE_CLUSTER_ATTRIBUTE_IN_SDF;
    case NAN_TLV_TYPE_FW_SOCIAL_CHANNEL_SCAN_PARAMS:
        return NAN_TLV_TYPE_SOCIAL_CHANNEL_SCAN_PARAMETERS;
    case NAN_TLV_TYPE_FW_DEBUGGING_FLAGS:
        return NAN_TLV_TYPE_DEBUGGING_FLAGS;
    case NAN_TLV_TYPE_FW_POST_NAN_CONNECTIVITY_CAPABILITIES_TRANSMIT:
        return NAN_TLV_TYPE_POST_NAN_CONNECTIVITY_CAPABILITIES_TRANSMIT;
    case NAN_TLV_TYPE_FW_POST_NAN_DISCOVERY_ATTRIBUTE_TRANSMIT:
        return NAN_TLV_TYPE_POST_NAN_DISCOVERY_ATTRIBUTE_TRANSMIT;
    case NAN_TLV_TYPE_FW_FURTHER_AVAILABILITY_MAP:
        return NAN_TLV_TYPE_FURTHER_AVAILABILITY_MAP;
    case NAN_TLV_TYPE_FW_HOP_COUNT_FORCE:
        return NAN_TLV_TYPE_HOP_COUNT_FORCE;
    case NAN_TLV_TYPE_FW_RANDOM_FACTOR_FORCE:
        return NAN_TLV_TYPE_RANDOM_FACTOR_FORCE;

    /* Attrib types */
    /* Unmapped attrib types */
    case NAN_TLV_TYPE_FW_AVAILABILITY_INTERVALS_MAP:
        break;
    case NAN_TLV_TYPE_FW_WLAN_MESH_ID:
        return NAN_TLV_TYPE_WLAN_MESH_ID;
    case NAN_TLV_TYPE_FW_MAC_ADDRESS:
        return NAN_TLV_TYPE_MAC_ADDRESS;
    case NAN_TLV_TYPE_FW_RECEIVED_RSSI_VALUE:
        return NAN_TLV_TYPE_RECEIVED_RSSI_VALUE;
    case NAN_TLV_TYPE_FW_CLUSTER_ATTRIBUTE:
        return NAN_TLV_TYPE_CLUSTER_ATTIBUTE;
    case NAN_TLV_TYPE_FW_WLAN_INFRASTRUCTURE_SSID:
        return NAN_TLV_TYPE_WLAN_INFRASTRUCTURE_SSID;

    /* Events Type */
    case NAN_TLV_TYPE_FW_EVENT_SELF_STATION_MAC_ADDRESS:
        return NAN_EVENT_ID_STA_MAC_ADDR;
    case NAN_TLV_TYPE_FW_EVENT_STARTED_CLUSTER:
        return NAN_EVENT_ID_STARTED_CLUSTER;
    case NAN_TLV_TYPE_FW_EVENT_JOINED_CLUSTER:
        return NAN_EVENT_ID_JOINED_CLUSTER;
    /* unmapped Event Type */
    case NAN_TLV_TYPE_FW_EVENT_CLUSTER_SCAN_RESULTS:
        break;

    case NAN_TLV_TYPE_FW_TCA_CLUSTER_SIZE_REQ:
    case NAN_TLV_TYPE_FW_TCA_CLUSTER_SIZE_RSP:
        return NAN_TCA_ID_CLUSTER_SIZE;

    default:
        break;
    }
    ALOGE("%s: Unhandled FW TLV value:%d", __func__, fwTlvtype);
    return 0xFFFF;
#endif /*NAN_2_0*/
}

u16 getFWTlvtypeFromNanTlvtype(u16 nanTlvtype)
{
#ifndef NAN_2_0
    /* In case of Pronto no mapping required */
    return nanTlvtype;
#else /* NAN_2_0 */
    if (nanTlvtype <= NAN_TLV_TYPE_SERVICE_SPECIFIC_INFO) {
        /* return the TLV value as is */
        return nanTlvtype;
    }
    if (nanTlvtype >= NAN_TLV_TYPE_STATS_FIRST &&
        nanTlvtype <= NAN_TLV_TYPE_STATS_LAST) {
        return nanTlvtype;
    }
    /* Other NAN TLV values and Config types map it
       appropriately
    */
    switch (nanTlvtype) {
    case NAN_TLV_TYPE_EXT_SERVICE_SPECIFIC_INFO:
        return NAN_TLV_TYPE_FW_EXT_SERVICE_SPECIFIC_INFO;
    case NAN_TLV_TYPE_SDF_LAST:
        return NAN_TLV_TYPE_FW_SDF_LAST;

    /* Configuration types */
    case NAN_TLV_TYPE_5G_SUPPORT:
        return NAN_TLV_TYPE_FW_5G_SUPPORT;
    case NAN_TLV_TYPE_SID_BEACON:
        return NAN_TLV_TYPE_FW_SID_BEACON;
    case NAN_TLV_TYPE_5G_SYNC_DISC:
        break;
    case NAN_TLV_TYPE_RSSI_CLOSE:
        return NAN_TLV_TYPE_FW_24G_RSSI_CLOSE;
    case NAN_TLV_TYPE_RSSI_MEDIUM:
        return NAN_TLV_TYPE_FW_24G_RSSI_MIDDLE;
    case NAN_TLV_TYPE_HOP_COUNT_LIMIT:
        return NAN_TLV_TYPE_FW_HOP_COUNT_LIMIT;
    /* unmapped */
    case NAN_TLV_TYPE_RANDOM_UPDATE_TIME:
        break;
    case NAN_TLV_TYPE_MASTER_PREFERENCE:
        return NAN_TLV_TYPE_FW_MASTER_PREFERENCE;
    /* unmapped */
    case NAN_TLV_TYPE_EARLY_WAKEUP:
        break;
    case NAN_TLV_TYPE_PERIODIC_SCAN_INTERVAL:
        break;
    case NAN_TLV_TYPE_CLUSTER_ID_LOW:
        return NAN_TLV_TYPE_FW_CLUSTER_ID_LOW;
    case NAN_TLV_TYPE_CLUSTER_ID_HIGH:
        return NAN_TLV_TYPE_FW_CLUSTER_ID_HIGH;
    case NAN_TLV_TYPE_RSSI_CLOSE_PROXIMITY:
        return NAN_TLV_TYPE_FW_24G_RSSI_CLOSE_PROXIMITY;
    case NAN_TLV_TYPE_CONFIG_LAST:
        return NAN_TLV_TYPE_FW_CONFIG_LAST;
    case NAN_TLV_TYPE_FURTHER_AVAILABILITY:
        break;

    /* All Stats type are unmapped as of now */

    /* Attributes types */
    case NAN_TLV_TYPE_WLAN_MESH_ID:
        return NAN_TLV_TYPE_FW_WLAN_MESH_ID;
    case NAN_TLV_TYPE_MAC_ADDRESS:
        return NAN_TLV_TYPE_FW_MAC_ADDRESS;
    case NAN_TLV_TYPE_RECEIVED_RSSI_VALUE:
        return NAN_TLV_TYPE_FW_RECEIVED_RSSI_VALUE;
    case NAN_TLV_TYPE_TCA_CLUSTER_SIZE_REQ:
        return NAN_TLV_TYPE_FW_TCA_CLUSTER_SIZE_REQ;
    case NAN_TLV_TYPE_ATTRS_LAST:
        return NAN_TLV_TYPE_FW_ATTRS_LAST;

    case NAN_TLV_TYPE_VENDOR_SPECIFIC_ATTRIBUTE_TRANSMIT:
        return NAN_TLV_TYPE_FW_VENDOR_SPECIFIC_ATTRIBUTE_TRANSMIT;
    case NAN_TLV_TYPE_VENDOR_SPECIFIC_ATTRIBUTE_RECEIVE:
        return NAN_TLV_TYPE_FW_VENDOR_SPECIFIC_ATTRIBUTE_RECEIVE;
    case NAN_TLV_TYPE_POST_NAN_CONNECTIVITY_CAPABILITIES_TRANSMIT:
        return NAN_TLV_TYPE_FW_POST_NAN_CONNECTIVITY_CAPABILITIES_TRANSMIT;
    case NAN_TLV_TYPE_POST_NAN_CONNECTIVITY_CAPABILITIES_RECEIVE:
        return NAN_TLV_TYPE_FW_POST_NAN_CONNECTIVITY_CAPABILITIES_RECEIVE;
    case NAN_TLV_TYPE_POST_NAN_DISCOVERY_ATTRIBUTE_TRANSMIT:
        return NAN_TLV_TYPE_FW_POST_NAN_DISCOVERY_ATTRIBUTE_TRANSMIT;
    case NAN_TLV_TYPE_POST_NAN_DISCOVERY_ATTRIBUTE_RECEIVE:
        return NAN_TLV_TYPE_FW_POST_NAN_DISCOVERY_ATTRIBUTE_RECEIVE;
    case NAN_TLV_TYPE_FURTHER_AVAILABILITY_MAP:
        return NAN_TLV_TYPE_FW_FURTHER_AVAILABILITY_MAP;
    case NAN_TLV_TYPE_BEACON_SDF_PAYLOAD_RECEIVE:
        return NAN_TLV_TYPE_FW_BEACON_SDF_PAYLOAD_RECEIVE;

    case NAN_TLV_TYPE_2DOT4G_SUPPORT:
        return NAN_TLV_TYPE_FW_24G_SUPPORT;
    case NAN_TLV_TYPE_2DOT4G_BEACONS:
        return NAN_TLV_TYPE_FW_24G_BEACON;
    case NAN_TLV_TYPE_2DOT4G_SDF:
        return NAN_TLV_TYPE_FW_24G_SDF;
    case NAN_TLV_TYPE_5G_BEACON:
        return NAN_TLV_TYPE_FW_5G_BEACON;
    case NAN_TLV_TYPE_5G_SDF:
        return NAN_TLV_TYPE_FW_5G_SDF;
    case NAN_TLV_TYPE_5G_RSSI_CLOSE:
        return NAN_TLV_TYPE_FW_5G_RSSI_CLOSE;
    case NAN_TLV_TYPE_5G_RSSI_MEDIUM:
        return NAN_TLV_TYPE_FW_5G_RSSI_MIDDLE;
    case NAN_TLV_TYPE_5G_RSSI_CLOSE_PROXIMITY:
        return NAN_TLV_TYPE_FW_5G_RSSI_CLOSE_PROXIMITY;
    case NAN_TLV_TYPE_RSSI_AVERAGING_WINDOW_SIZE:
        return NAN_TLV_TYPE_FW_RSSI_AVERAGING_WINDOW_SIZE;
    case NAN_TLV_TYPE_CLUSTER_OUI_NETWORK_ID:
        return NAN_TLV_TYPE_FW_CLUSTER_OUI_NETWORK_ID;
    case NAN_TLV_TYPE_SOURCE_MAC_ADDRESS:
        return NAN_TLV_TYPE_FW_SOURCE_MAC_ADDRESS;
    case NAN_TLV_TYPE_CLUSTER_ATTRIBUTE_IN_SDF:
        return NAN_TLV_TYPE_FW_CLUSTER_ATTRIBUTE_IN_SDF;
    case NAN_TLV_TYPE_SOCIAL_CHANNEL_SCAN_PARAMETERS:
        return NAN_TLV_TYPE_FW_SOCIAL_CHANNEL_SCAN_PARAMS;
    case NAN_TLV_TYPE_DEBUGGING_FLAGS:
        return NAN_TLV_TYPE_FW_DEBUGGING_FLAGS;
    case NAN_TLV_TYPE_WLAN_INFRASTRUCTURE_SSID:
        return NAN_TLV_TYPE_FW_WLAN_INFRASTRUCTURE_SSID;
    case NAN_TLV_TYPE_RANDOM_FACTOR_FORCE:
        return NAN_TLV_TYPE_FW_RANDOM_FACTOR_FORCE;
    case NAN_TLV_TYPE_HOP_COUNT_FORCE:
        return NAN_TLV_TYPE_FW_HOP_COUNT_FORCE;


    default:
        break;
    }
    ALOGE("%s: Unhandled NAN TLV value:%d", __func__, nanTlvtype);
    return 0xFFFF;
#endif /* NAN_2_0 */
}
