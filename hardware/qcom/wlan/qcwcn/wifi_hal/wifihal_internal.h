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

#ifndef __WIFI_HAL_LOWI_INTERNAL_H__
#define __WIFI_HAL_LOWI_INTERNAL_H__

/*
  * The file defines the interface by which wifihal can call LOWI for the purposes
  * of initialization, rtt and gscan.
  */

#include "wifi_hal.h"

/*
  * This structure is a table of function pointers to the functions
  * used by the wifihal to interface with LOWI
  */
typedef struct
{
  int (*init)();
  int (*destroy)();
  int (*get_rtt_capabilities)(wifi_interface_handle iface,
                              wifi_rtt_capabilities *capabilities);
  int (*rtt_range_request)(u32 request_id,
                           wifi_interface_handle iface,
                           u32 num_rtt_config,
                           wifi_rtt_config rtt_config[],
                           wifi_rtt_event_handler handler);
  int (*rtt_range_cancel)(u32 request_id,
                          u32 num_devices,
                          mac_addr addr[]);
} lowi_cb_table_t;

/*
  * This is a function pointer to a function that gets the table
  * of callback functions populated by LOWI and to be used by wifihal
  */
typedef lowi_cb_table_t* (getCbTable_t)();

#endif
