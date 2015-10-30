/* Copyright (c) 2014, The Linux Foundation. All rights reserved.
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

#define LOG_TAG  "WifiHAL"
#include <dlfcn.h>
#include <cutils/sched_policy.h>
#include <unistd.h>

#include <utils/Log.h>
#include <time.h>

#include "common.h"
#include "cpp_bindings.h"
#include "rtt.h"
#include "wifi_hal.h"
#include "wifihal_internal.h"

/* Used to keep track of RTT enabled/disabled last state change. */
u8 RttLowiIfaceEnabled = 0;
/* Pointer to the table of LOWI callback funcs. */
lowi_cb_table_t *LowiWifiHalApi = NULL;

static wifi_error getLowiCbTable(lowi_cb_table_t **lowi_wifihal_api)
{
    getCbTable_t* lowiCbTable = NULL;
#if __WORDSIZE == 64
    void* lowi_handle = dlopen("/vendor/lib64/liblowi_wifihal.so", RTLD_NOW);
#else
    void* lowi_handle = dlopen("/vendor/lib/liblowi_wifihal.so", RTLD_NOW);
#endif
    if (!lowi_handle)
        ALOGE("NULL lowi_handle, err: %s", dlerror());
    if (lowi_handle != (void*)NULL)
    {
        lowiCbTable = (getCbTable_t*)dlsym(lowi_handle,
                                          "lowi_wifihal_get_cb_table");
        if (lowiCbTable != NULL) {
            ALOGE("before calling lowi_wifihal_api = (*lowiCbTable)()");
            *lowi_wifihal_api = lowiCbTable();
        }  else {
            /* LOWI is not there. */
            ALOGE("getLowiCbTable:dlsym failed. Exit.");
            return WIFI_ERROR_NOT_SUPPORTED;
        }
    } else {
        /* LOWI is not there. */
        ALOGE("getLowiCbTable: LOWI is not supported. Exit.");
        return WIFI_ERROR_UNINITIALIZED;
    }
    return WIFI_SUCCESS;
}

/* Implementation of the API functions exposed in rtt.h */
wifi_error wifi_get_rtt_capabilities(wifi_interface_handle iface,
                                     wifi_rtt_capabilities *capabilities)
{
    int ret = WIFI_SUCCESS;

    ALOGD("wifi_get_rtt_capabilities: Entry");

    if (iface == NULL) {
        ALOGE("wifi_get_rtt_capabilities: NULL iface pointer provided."
            " Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    interface_info *ifaceInfo = getIfaceInfo(iface);
    wifi_handle wifiHandle = getWifiHandle(iface);

    if (capabilities == NULL) {
        ALOGE("wifi_get_rtt_capabilities: NULL capabilities pointer provided."
            " Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    if (LowiWifiHalApi == NULL) {
        ret = getLowiCbTable(&LowiWifiHalApi);
        if (ret != WIFI_SUCCESS || LowiWifiHalApi == NULL) {
            ALOGE("wifi_rtt_range_request: LOWI is not supported. Exit.");
            goto cleanup;
        }
    }

    /* Initialize LOWI if it isn't up already. */
    if (RttLowiIfaceEnabled == 0) {
        ALOGE("before calling lowi init()");
        ret = LowiWifiHalApi->init();
        if (ret) {
            ALOGE("wifi_get_rtt_capabilities(): failed lowi initialization. "
                "Returned error:%d. Exit.", ret);
            goto cleanup;
        }
    }
    RttLowiIfaceEnabled = 1;

    ALOGE("before calling get_rtt_capabilities");
    ret = LowiWifiHalApi->get_rtt_capabilities(iface, capabilities);
    if (ret != WIFI_SUCCESS) {
        ALOGE("wifi_get_rtt_capabilities(): lowi_wifihal_get_rtt_capabilities "
            "returned error:%d. Exit.", ret);
        goto cleanup;
    }

cleanup:
    /* Check if RTT cmd failed because Wi-Fi is Off */
    if (ret == WIFI_ERROR_NOT_AVAILABLE && LowiWifiHalApi) {
        ret = LowiWifiHalApi->destroy();
        RttLowiIfaceEnabled = 0;
        LowiWifiHalApi = NULL;
    }
    return (wifi_error)ret;
}

/* API to request RTT measurement */
wifi_error wifi_rtt_range_request(wifi_request_id id,
                                    wifi_interface_handle iface,
                                    unsigned num_rtt_config,
                                    wifi_rtt_config rtt_config[],
                                    wifi_rtt_event_handler handler)
{
    int ret = WIFI_SUCCESS;

    ALOGD("wifi_rtt_range_request: Entry");

    if (iface == NULL) {
        ALOGE("wifi_rtt_range_request: NULL iface pointer provided."
            " Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    interface_info *ifaceInfo = getIfaceInfo(iface);
    wifi_handle wifiHandle = getWifiHandle(iface);

    if (rtt_config == NULL) {
        ALOGE("wifi_rtt_range_request: NULL rtt_config pointer provided."
            " Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    if (num_rtt_config <= 0) {
        ALOGE("wifi_rtt_range_request: number of destination BSSIDs to "
            "measure RTT on = 0. Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    if (handler.on_rtt_results == NULL) {
        ALOGE("wifi_rtt_range_request: NULL capabilities pointer provided."
            " Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    if (LowiWifiHalApi == NULL) {
        ret = getLowiCbTable(&LowiWifiHalApi);
        if (ret != WIFI_SUCCESS || LowiWifiHalApi == NULL) {
            ALOGE("wifi_rtt_range_request: LOWI is not supported. Exit.");
            goto cleanup;
        }
    }

    /* Initialize LOWI if it isn't up already. */
    if (RttLowiIfaceEnabled == 0) {
        ret = LowiWifiHalApi->init();
        if (ret) {
            ALOGE("wifi_rtt_range_request(): failed lowi initialization. "
                "Returned error:%d. Exit.", ret);
            goto cleanup;
        }
    }
    RttLowiIfaceEnabled = 1;

    ret = LowiWifiHalApi->rtt_range_request(id,
                                            iface,
                                            num_rtt_config,
                                            rtt_config,
                                            handler);
    if (ret != WIFI_SUCCESS) {
        ALOGE("wifi_rtt_range_request: lowi_wifihal_rtt_range_request "
            "returned error:%d. Exit.", ret);
        goto cleanup;
    }

cleanup:
    /* Check if RTT cmd failed because Wi-Fi is Off. */
    if (ret == WIFI_ERROR_NOT_AVAILABLE && LowiWifiHalApi) {
        ret = LowiWifiHalApi->destroy();
        RttLowiIfaceEnabled = 0;
        LowiWifiHalApi = NULL;
    }
    return (wifi_error)ret;
}

/* API to cancel RTT measurements */
wifi_error wifi_rtt_range_cancel(wifi_request_id id,
                                   wifi_interface_handle iface,
                                   unsigned num_devices,
                                   mac_addr addr[])
{
    int ret = WIFI_SUCCESS;

    ALOGD("wifi_rtt_range_cancel: Entry");

    if (iface == NULL) {
        ALOGE("wifi_rtt_range_cancel: NULL iface pointer provided."
            " Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    interface_info *ifaceInfo = getIfaceInfo(iface);
    wifi_handle wifiHandle = getWifiHandle(iface);

    if (addr == NULL) {
        ALOGE("wifi_rtt_range_cancel: NULL addr pointer provided."
            " Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    if (num_devices <= 0) {
        ALOGE("wifi_rtt_range_cancel: number of destination BSSIDs to "
            "measure RTT on = 0. Exit.");
        return WIFI_ERROR_INVALID_ARGS;
    }

    if (LowiWifiHalApi == NULL) {
        ret = getLowiCbTable(&LowiWifiHalApi);
        if (ret != WIFI_SUCCESS || LowiWifiHalApi == NULL) {
            ALOGE("wifi_rtt_range_cancel: LOWI is not supported. Exit.");
            goto cleanup;
        }
    }

    /* Initialize LOWI if it isn't up already. */
    if (RttLowiIfaceEnabled == 0) {
        ret = LowiWifiHalApi->init();
        if (ret) {
            ALOGE("wifi_rtt_range_cancel(): failed lowi initialization. "
                "Returned error:%d. Exit.", ret);
            goto cleanup;
        }
    }
    RttLowiIfaceEnabled = 1;

    ret = LowiWifiHalApi->rtt_range_cancel(id, num_devices, addr);
    if (ret != WIFI_SUCCESS) {
        ALOGE("wifi_rtt_range_cancel: lowi_wifihal_rtt_range_cancel "
            "returned error:%d. Exit.", ret);
        goto cleanup;
    }

cleanup:
    /* Check if RTT cmd failed because Wi-Fi is Off. */
    if (ret == WIFI_ERROR_NOT_AVAILABLE && LowiWifiHalApi != NULL) {
        ALOGE("wifi_rtt_range_cancel: before calling destroy, ret=%d", ret);
        ret = LowiWifiHalApi->destroy();
        RttLowiIfaceEnabled = 0;
        LowiWifiHalApi = NULL;
    }
    return (wifi_error)ret;
}

