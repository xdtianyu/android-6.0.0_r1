/******************************************************************************
*
* Copyright (C) 2012 Ittiam Systems Pvt Ltd, Bangalore
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
******************************************************************************/
/**
*******************************************************************************
* @file
*  ihevcd_version.c
*
* @brief
*  Contains version info for HEVC decoder
*
* @author
*  Harish
*
* @par List of Functions:
* - ihevcd_get_version()
*
* @remarks
*  None
*
*******************************************************************************
*/
/*****************************************************************************/
/* File Includes                                                             */
/*****************************************************************************/
#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#include "ihevc_typedefs.h"
#include "iv.h"
#include "ivd.h"
#include "ihevcd_cxa.h"

#include "ihevc_defs.h"
#include "ihevc_debug.h"
#include "ihevc_structs.h"
/**
 * Name of the codec
 */
#define CODEC_NAME              "HEVCDEC"
/**
 * Codec release type, production or evaluation
 */
#define CODEC_RELEASE_TYPE      "production"
/**
 * Version string. First two digits signify major version and last two minor
 * Increment major version for API change or major feature update
 */
#define CODEC_RELEASE_VER       "04.04"
/**
 * Vendor name
 */
#define CODEC_VENDOR            "ITTIAM"

/**
*******************************************************************************
* Concatenates various strings to form a version string
*******************************************************************************
*/
#define VERSION(version_string, codec_name, codec_release_type, codec_release_ver, codec_vendor)    \
    strcpy(version_string,"@(#)Id:");                                                               \
    strcat(version_string,codec_name);                                                              \
    strcat(version_string,"_");                                                                     \
    strcat(version_string,codec_release_type);                                                      \
    strcat(version_string," Ver:");                                                                 \
    strcat(version_string,codec_release_ver);                                                       \
    strcat(version_string," Released by ");                                                         \
    strcat(version_string,codec_vendor);                                                            \
    strcat(version_string," Build: ");                                                              \
    strcat(version_string,__DATE__);                                                                \
    strcat(version_string," @ ");                                                                   \
    strcat(version_string,__TIME__);


/**
*******************************************************************************
*
* @brief
*  Fills the version info in the given string
*
* @par Description:
*
*
* @param[in] pc_version_string
*  Pointer to hold version info
*
* @param[in] u4_version_buffer_size
*  Size of the buffer passed
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
IV_API_CALL_STATUS_T ihevcd_get_version(CHAR *pc_version_string,
                                        UWORD32 u4_version_buffer_size)
{
    CHAR ac_version_tmp[512];
    VERSION(ac_version_tmp, CODEC_NAME, CODEC_RELEASE_TYPE, CODEC_RELEASE_VER, CODEC_VENDOR);

    if(u4_version_buffer_size >= (strlen(ac_version_tmp) + 1))
    {
        memcpy(pc_version_string, ac_version_tmp, (strlen(ac_version_tmp) + 1));
        return IV_SUCCESS;
    }
    else
    {
        return IV_FAIL;
    }

}


