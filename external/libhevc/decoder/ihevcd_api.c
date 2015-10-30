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
*  ihevcd_api.c
*
* @brief
*  Contains api functions definitions for HEVC decoder
*
* @author
*  Harish
*
* @par List of Functions:
* - api_check_struct_sanity()
* - ihevcd_get_version()
* - ihevcd_set_default_params()
* - ihevcd_init()
* - ihevcd_get_num_rec()
* - ihevcd_fill_num_mem_rec()
* - ihevcd_init_mem_rec()
* - ihevcd_retrieve_memrec()
* - ihevcd_set_display_frame()
* - ihevcd_set_flush_mode()
* - ihevcd_get_status()
* - ihevcd_get_buf_info()
* - ihevcd_set_params()
* - ihevcd_reset()
* - ihevcd_rel_display_frame()
* - ihevcd_disable_deblk()
* - ihevcd_get_frame_dimensions()
* - ihevcd_set_num_cores()
* - ihevcd_ctl()
* - ihevcd_cxa_api_function()
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
#include "ithread.h"

#include "ihevc_defs.h"
#include "ihevc_debug.h"

#include "ihevc_structs.h"
#include "ihevc_macros.h"
#include "ihevc_platform_macros.h"

#include "ihevc_buf_mgr.h"
#include "ihevc_dpb_mgr.h"
#include "ihevc_disp_mgr.h"
#include "ihevc_common_tables.h"
#include "ihevc_cabac_tables.h"
#include "ihevc_error.h"

#include "ihevcd_defs.h"
#include "ihevcd_trace.h"

#include "ihevcd_function_selector.h"
#include "ihevcd_structs.h"
#include "ihevcd_error.h"
#include "ihevcd_utils.h"
#include "ihevcd_decode.h"
#include "ihevcd_job_queue.h"
#include "ihevcd_statistics.h"

/*****************************************************************************/
/* Function Prototypes                                                       */
/*****************************************************************************/
IV_API_CALL_STATUS_T ihevcd_get_version(CHAR *pc_version_string,
                                        UWORD32 u4_version_buffer_size);



/**
*******************************************************************************
*
* @brief
*  Used to test arguments for corresponding API call
*
* @par Description:
*  For each command the arguments are validated
*
* @param[in] ps_handle
*  Codec handle at API level
*
* @param[in] pv_api_ip
*  Pointer to input structure
*
* @param[out] pv_api_op
*  Pointer to output structure
*
* @returns  Status of error checking
*
* @remarks
*
*
*******************************************************************************
*/

static IV_API_CALL_STATUS_T api_check_struct_sanity(iv_obj_t *ps_handle,
                                                    void *pv_api_ip,
                                                    void *pv_api_op)
{
    IVD_API_COMMAND_TYPE_T e_cmd;
    UWORD32 *pu4_api_ip;
    UWORD32 *pu4_api_op;
    WORD32 i, j;

    if(NULL == pv_api_op)
        return (IV_FAIL);

    if(NULL == pv_api_ip)
        return (IV_FAIL);

    pu4_api_ip = (UWORD32 *)pv_api_ip;
    pu4_api_op = (UWORD32 *)pv_api_op;
    e_cmd = (IVD_API_COMMAND_TYPE_T)*(pu4_api_ip + 1);

    *(pu4_api_op + 1) = 0;
    /* error checks on handle */
    switch((WORD32)e_cmd)
    {
        case IV_CMD_GET_NUM_MEM_REC:
        case IV_CMD_FILL_NUM_MEM_REC:
            break;
        case IV_CMD_INIT:
            if(ps_handle == NULL)
            {
                *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
                *(pu4_api_op + 1) |= IVD_HANDLE_NULL;
                return IV_FAIL;
            }

            if(ps_handle->u4_size != sizeof(iv_obj_t))
            {
                *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
                *(pu4_api_op + 1) |= IVD_HANDLE_STRUCT_SIZE_INCORRECT;
                DEBUG("Sizes do not match. Expected: %d, Got: %d",
                                sizeof(iv_obj_t), ps_handle->u4_size);
                return IV_FAIL;
            }
            break;
        case IVD_CMD_REL_DISPLAY_FRAME:
        case IVD_CMD_SET_DISPLAY_FRAME:
        case IVD_CMD_GET_DISPLAY_FRAME:
        case IVD_CMD_VIDEO_DECODE:
        case IV_CMD_RETRIEVE_MEMREC:
        case IVD_CMD_VIDEO_CTL:
            if(ps_handle == NULL)
            {
                *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
                *(pu4_api_op + 1) |= IVD_HANDLE_NULL;
                return IV_FAIL;
            }

            if(ps_handle->u4_size != sizeof(iv_obj_t))
            {
                *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
                *(pu4_api_op + 1) |= IVD_HANDLE_STRUCT_SIZE_INCORRECT;
                return IV_FAIL;
            }


            if(ps_handle->pv_codec_handle == NULL)
            {
                *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
                *(pu4_api_op + 1) |= IVD_INVALID_HANDLE_NULL;
                return IV_FAIL;
            }
            break;
        default:
            *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
            *(pu4_api_op + 1) |= IVD_INVALID_API_CMD;
            return IV_FAIL;
    }

    switch((WORD32)e_cmd)
    {
        case IV_CMD_GET_NUM_MEM_REC:
        {
            ihevcd_cxa_num_mem_rec_ip_t *ps_ip =
                            (ihevcd_cxa_num_mem_rec_ip_t *)pv_api_ip;
            ihevcd_cxa_num_mem_rec_op_t *ps_op =
                            (ihevcd_cxa_num_mem_rec_op_t *)pv_api_op;
            ps_op->s_ivd_num_mem_rec_op_t.u4_error_code = 0;

            if(ps_ip->s_ivd_num_mem_rec_ip_t.u4_size
                            != sizeof(ihevcd_cxa_num_mem_rec_ip_t))
            {
                ps_op->s_ivd_num_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_num_mem_rec_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if(ps_op->s_ivd_num_mem_rec_op_t.u4_size
                            != sizeof(ihevcd_cxa_num_mem_rec_op_t))
            {
                ps_op->s_ivd_num_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_num_mem_rec_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }
        }
            break;
        case IV_CMD_FILL_NUM_MEM_REC:
        {
            ihevcd_cxa_fill_mem_rec_ip_t *ps_ip =
                            (ihevcd_cxa_fill_mem_rec_ip_t *)pv_api_ip;
            ihevcd_cxa_fill_mem_rec_op_t *ps_op =
                            (ihevcd_cxa_fill_mem_rec_op_t *)pv_api_op;
            iv_mem_rec_t *ps_mem_rec;
            WORD32 max_wd = ps_ip->s_ivd_fill_mem_rec_ip_t.u4_max_frm_wd;
            WORD32 max_ht = ps_ip->s_ivd_fill_mem_rec_ip_t.u4_max_frm_ht;

            max_wd = ALIGN64(max_wd);
            max_ht = ALIGN64(max_ht);

            ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code = 0;

            if((ps_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                            > sizeof(ihevcd_cxa_fill_mem_rec_ip_t))
                            || (ps_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                                            < sizeof(iv_fill_mem_rec_ip_t)))
            {
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if((ps_op->s_ivd_fill_mem_rec_op_t.u4_size
                            != sizeof(ihevcd_cxa_fill_mem_rec_op_t))
                            && (ps_op->s_ivd_fill_mem_rec_op_t.u4_size
                                            != sizeof(iv_fill_mem_rec_op_t)))
            {
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if(max_wd < MIN_WD)
            {
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                IVD_REQUESTED_WIDTH_NOT_SUPPPORTED;
                return (IV_FAIL);
            }

            if(max_wd > MAX_WD)
            {
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                IVD_REQUESTED_WIDTH_NOT_SUPPPORTED;
                return (IV_FAIL);
            }

            if(max_ht < MIN_HT)
            {
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                IVD_REQUESTED_HEIGHT_NOT_SUPPPORTED;
                return (IV_FAIL);
            }

            if((max_ht * max_wd) > (MAX_HT * MAX_WD))

            {
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                IVD_REQUESTED_HEIGHT_NOT_SUPPPORTED;
                return (IV_FAIL);
            }

            if(NULL == ps_ip->s_ivd_fill_mem_rec_ip_t.pv_mem_rec_location)
            {
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                IVD_NUM_REC_NOT_SUFFICIENT;
                return (IV_FAIL);
            }

            /* check memrecords sizes are correct */
            ps_mem_rec = ps_ip->s_ivd_fill_mem_rec_ip_t.pv_mem_rec_location;
            for(i = 0; i < MEM_REC_CNT; i++)
            {
                if(ps_mem_rec[i].u4_size != sizeof(iv_mem_rec_t))
                {
                    ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |= 1
                                    << IVD_UNSUPPORTEDPARAM;
                    ps_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                                    IVD_MEM_REC_STRUCT_SIZE_INCORRECT;
                    return IV_FAIL;
                }
            }
        }
            break;

        case IV_CMD_INIT:
        {
            ihevcd_cxa_init_ip_t *ps_ip = (ihevcd_cxa_init_ip_t *)pv_api_ip;
            ihevcd_cxa_init_op_t *ps_op = (ihevcd_cxa_init_op_t *)pv_api_op;
            iv_mem_rec_t *ps_mem_rec;
            WORD32 max_wd = ps_ip->s_ivd_init_ip_t.u4_frm_max_wd;
            WORD32 max_ht = ps_ip->s_ivd_init_ip_t.u4_frm_max_ht;

            max_wd = ALIGN64(max_wd);
            max_ht = ALIGN64(max_ht);

            ps_op->s_ivd_init_op_t.u4_error_code = 0;

            if((ps_ip->s_ivd_init_ip_t.u4_size > sizeof(ihevcd_cxa_init_ip_t))
                            || (ps_ip->s_ivd_init_ip_t.u4_size
                                            < sizeof(ivd_init_ip_t)))
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if((ps_op->s_ivd_init_op_t.u4_size != sizeof(ihevcd_cxa_init_op_t))
                            && (ps_op->s_ivd_init_op_t.u4_size
                                            != sizeof(ivd_init_op_t)))
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if(ps_ip->s_ivd_init_ip_t.u4_num_mem_rec != MEM_REC_CNT)
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_INIT_DEC_NOT_SUFFICIENT;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if(max_wd < MIN_WD)
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_INIT_DEC_WIDTH_NOT_SUPPPORTED;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if(max_wd > MAX_WD)
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_INIT_DEC_WIDTH_NOT_SUPPPORTED;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if(max_ht < MIN_HT)
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_INIT_DEC_HEIGHT_NOT_SUPPPORTED;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if((max_ht * max_wd) > (MAX_HT * MAX_WD))

            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_INIT_DEC_HEIGHT_NOT_SUPPPORTED;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if(NULL == ps_ip->s_ivd_init_ip_t.pv_mem_rec_location)
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_NUM_REC_NOT_SUFFICIENT;
                DEBUG("\n");
                return (IV_FAIL);
            }

            if((ps_ip->s_ivd_init_ip_t.e_output_format != IV_YUV_420P)
                            && (ps_ip->s_ivd_init_ip_t.e_output_format
                                            != IV_YUV_422ILE)
                            && (ps_ip->s_ivd_init_ip_t.e_output_format
                                            != IV_RGB_565)
                            && (ps_ip->s_ivd_init_ip_t.e_output_format
                                            != IV_RGBA_8888)
                            && (ps_ip->s_ivd_init_ip_t.e_output_format
                                            != IV_YUV_420SP_UV)
                            && (ps_ip->s_ivd_init_ip_t.e_output_format
                                            != IV_YUV_420SP_VU))
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_INIT_DEC_COL_FMT_NOT_SUPPORTED;
                DEBUG("\n");
                return (IV_FAIL);
            }

            /* verify number of mem records */
            if(ps_ip->s_ivd_init_ip_t.u4_num_mem_rec < MEM_REC_CNT)
            {
                ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_init_op_t.u4_error_code |=
                                IVD_INIT_DEC_MEM_REC_NOT_SUFFICIENT;
                DEBUG("\n");
                return IV_FAIL;
            }

            ps_mem_rec = ps_ip->s_ivd_init_ip_t.pv_mem_rec_location;
            /* check memrecords sizes are correct */
            for(i = 0; i < (WORD32)ps_ip->s_ivd_init_ip_t.u4_num_mem_rec; i++)
            {
                if(ps_mem_rec[i].u4_size != sizeof(iv_mem_rec_t))
                {
                    ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                    << IVD_UNSUPPORTEDPARAM;
                    ps_op->s_ivd_init_op_t.u4_error_code |=
                                    IVD_MEM_REC_STRUCT_SIZE_INCORRECT;
                    DEBUG("i: %d\n", i);
                    return IV_FAIL;
                }
                /* check memrecords pointers are not NULL */

                if(ps_mem_rec[i].pv_base == NULL)
                {

                    ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                    << IVD_UNSUPPORTEDPARAM;
                    ps_op->s_ivd_init_op_t.u4_error_code |=
                                    IVD_INIT_DEC_MEM_REC_BASE_NULL;
                    DEBUG("i: %d\n", i);
                    return IV_FAIL;

                }

            }

            /* verify memtabs for overlapping regions */
            {
                void *start[MEM_REC_CNT];
                void *end[MEM_REC_CNT];

                start[0] = (ps_mem_rec[0].pv_base);
                end[0] = (UWORD8 *)(ps_mem_rec[0].pv_base)
                                + ps_mem_rec[0].u4_mem_size - 1;
                for(i = 1; i < MEM_REC_CNT; i++)
                {
                    /* This array is populated to check memtab overlapp */
                    start[i] = (ps_mem_rec[i].pv_base);
                    end[i] = (UWORD8 *)(ps_mem_rec[i].pv_base)
                                    + ps_mem_rec[i].u4_mem_size - 1;

                    for(j = 0; j < i; j++)
                    {
                        if((start[i] >= start[j]) && (start[i] <= end[j]))
                        {
                            ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                            << IVD_UNSUPPORTEDPARAM;
                            ps_op->s_ivd_init_op_t.u4_error_code |=
                                            IVD_INIT_DEC_MEM_REC_OVERLAP_ERR;
                            DEBUG("i: %d, j: %d\n", i, j);
                            return IV_FAIL;
                        }

                        if((end[i] >= start[j]) && (end[i] <= end[j]))
                        {
                            ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                            << IVD_UNSUPPORTEDPARAM;
                            ps_op->s_ivd_init_op_t.u4_error_code |=
                                            IVD_INIT_DEC_MEM_REC_OVERLAP_ERR;
                            DEBUG("i: %d, j: %d\n", i, j);
                            return IV_FAIL;
                        }

                        if((start[i] < start[j]) && (end[i] > end[j]))
                        {
                            ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                            << IVD_UNSUPPORTEDPARAM;
                            ps_op->s_ivd_init_op_t.u4_error_code |=
                                            IVD_INIT_DEC_MEM_REC_OVERLAP_ERR;
                            DEBUG("i: %d, j: %d\n", i, j);
                            return IV_FAIL;
                        }
                    }

                }
            }

            {
                iv_mem_rec_t mem_rec_ittiam_api[MEM_REC_CNT];
                ihevcd_cxa_fill_mem_rec_ip_t s_fill_mem_rec_ip;
                ihevcd_cxa_fill_mem_rec_op_t s_fill_mem_rec_op;
                IV_API_CALL_STATUS_T e_status;

                WORD32 i;
                s_fill_mem_rec_ip.s_ivd_fill_mem_rec_ip_t.e_cmd =
                                IV_CMD_FILL_NUM_MEM_REC;
                s_fill_mem_rec_ip.s_ivd_fill_mem_rec_ip_t.pv_mem_rec_location =
                                mem_rec_ittiam_api;
                s_fill_mem_rec_ip.s_ivd_fill_mem_rec_ip_t.u4_max_frm_wd =
                                max_wd;
                s_fill_mem_rec_ip.s_ivd_fill_mem_rec_ip_t.u4_max_frm_ht =
                                max_ht;

                if(ps_ip->s_ivd_init_ip_t.u4_size
                                > offsetof(ihevcd_cxa_init_ip_t, i4_level))
                {
                    s_fill_mem_rec_ip.i4_level = ps_ip->i4_level;
                }
                else
                {
                    s_fill_mem_rec_ip.i4_level = IHEVC_LEVEL_31;
                }

                if(ps_ip->s_ivd_init_ip_t.u4_size
                                > offsetof(ihevcd_cxa_init_ip_t,
                                           u4_num_ref_frames))
                {
                    s_fill_mem_rec_ip.u4_num_ref_frames =
                                    ps_ip->u4_num_ref_frames;
                }
                else
                {
                    s_fill_mem_rec_ip.u4_num_ref_frames = (MAX_REF_CNT + 1);
                }

                if(ps_ip->s_ivd_init_ip_t.u4_size
                                > offsetof(ihevcd_cxa_init_ip_t,
                                           u4_num_reorder_frames))
                {
                    s_fill_mem_rec_ip.u4_num_reorder_frames =
                                    ps_ip->u4_num_reorder_frames;
                }
                else
                {
                    s_fill_mem_rec_ip.u4_num_reorder_frames = (MAX_REF_CNT + 1);
                }

                if(ps_ip->s_ivd_init_ip_t.u4_size
                                > offsetof(ihevcd_cxa_init_ip_t,
                                           u4_num_extra_disp_buf))
                {
                    s_fill_mem_rec_ip.u4_num_extra_disp_buf =
                                    ps_ip->u4_num_extra_disp_buf;
                }
                else
                {
                    s_fill_mem_rec_ip.u4_num_extra_disp_buf = 0;
                }

                if(ps_ip->s_ivd_init_ip_t.u4_size
                                > offsetof(ihevcd_cxa_init_ip_t,
                                           u4_share_disp_buf))
                {
#ifndef LOGO_EN
                    s_fill_mem_rec_ip.u4_share_disp_buf =
                                    ps_ip->u4_share_disp_buf;
#else
                    s_fill_mem_rec_ip.u4_share_disp_buf = 0;
#endif
                }
                else
                {
                    s_fill_mem_rec_ip.u4_share_disp_buf = 0;
                }

                s_fill_mem_rec_ip.e_output_format =
                                ps_ip->s_ivd_init_ip_t.e_output_format;

                if((s_fill_mem_rec_ip.e_output_format != IV_YUV_420P)
                                && (s_fill_mem_rec_ip.e_output_format
                                                != IV_YUV_420SP_UV)
                                && (s_fill_mem_rec_ip.e_output_format
                                                != IV_YUV_420SP_VU))
                {
                    s_fill_mem_rec_ip.u4_share_disp_buf = 0;
                }

                s_fill_mem_rec_ip.s_ivd_fill_mem_rec_ip_t.u4_size =
                                sizeof(ihevcd_cxa_fill_mem_rec_ip_t);
                s_fill_mem_rec_op.s_ivd_fill_mem_rec_op_t.u4_size =
                                sizeof(ihevcd_cxa_fill_mem_rec_op_t);

                for(i = 0; i < MEM_REC_CNT; i++)
                    mem_rec_ittiam_api[i].u4_size = sizeof(iv_mem_rec_t);

                e_status = ihevcd_cxa_api_function(NULL,
                                                   (void *)&s_fill_mem_rec_ip,
                                                   (void *)&s_fill_mem_rec_op);
                if(IV_FAIL == e_status)
                {
                    ps_op->s_ivd_init_op_t.u4_error_code =
                                    s_fill_mem_rec_op.s_ivd_fill_mem_rec_op_t.u4_error_code;
                    DEBUG("Fail\n");
                    return (IV_FAIL);
                }

                for(i = 0; i < MEM_REC_CNT; i++)
                {
#ifdef ARMRVDS
                    if((UWORD32)(ps_mem_rec[i].pv_base) & (mem_rec_ittiam_api[i].u4_mem_alignment - 1))
                    {
                        ps_op->s_ivd_init_op_t.u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_init_op_t.u4_error_code |= IVD_INIT_DEC_MEM_REC_ALIGNMENT_ERR;
                        DEBUG("Fail\n");
                        return IV_FAIL;
                    }
#endif

                    if(ps_mem_rec[i].u4_mem_size
                                    < mem_rec_ittiam_api[i].u4_mem_size)
                    {
                        ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_init_op_t.u4_error_code |=
                                        IVD_INIT_DEC_MEM_REC_INSUFFICIENT_SIZE;
                        DEBUG("i: %d \n", i);
                        return IV_FAIL;
                    }
                    if(ps_mem_rec[i].u4_mem_alignment
                                    != mem_rec_ittiam_api[i].u4_mem_alignment)
                    {
                        ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_init_op_t.u4_error_code |=
                                        IVD_INIT_DEC_MEM_REC_ALIGNMENT_ERR;
                        DEBUG("i: %d \n", i);
                        return IV_FAIL;
                    }
                    if(ps_mem_rec[i].e_mem_type
                                    != mem_rec_ittiam_api[i].e_mem_type)
                    {
                        UWORD32 check = IV_SUCCESS;
                        UWORD32 diff = mem_rec_ittiam_api[i].e_mem_type
                                        - ps_mem_rec[i].e_mem_type;

                        if((ps_mem_rec[i].e_mem_type
                                        <= IV_EXTERNAL_CACHEABLE_SCRATCH_MEM)
                                        && (mem_rec_ittiam_api[i].e_mem_type
                                                        >= IV_INTERNAL_NONCACHEABLE_PERSISTENT_MEM))
                        {
                            check = IV_FAIL;
                        }
                        if(3 != (mem_rec_ittiam_api[i].e_mem_type % 4))
                        {
                            /*
                             * It is not IV_EXTERNAL_NONCACHEABLE_PERSISTENT_MEM or IV_EXTERNAL_CACHEABLE_PERSISTENT_MEM
                             */
                            if((diff < 1) || (diff > 3))
                            {
                                // Difference between 1 and 3 is okay for all cases other than the two filtered
                                // with the MOD condition above
                                check = IV_FAIL;
                            }
                        }
                        else
                        {
                            if(diff == 1)
                            {
                                /*
                                 * This particular case is when codec asked for External Persistent, but got
                                 * Internal Scratch.
                                 */
                                check = IV_FAIL;
                            }
                            if((diff != 2) && (diff != 3))
                            {
                                check = IV_FAIL;
                            }
                        }
                        if(check == IV_FAIL)
                        {
                            ps_op->s_ivd_init_op_t.u4_error_code |= 1
                                            << IVD_UNSUPPORTEDPARAM;
                            ps_op->s_ivd_init_op_t.u4_error_code |=
                                            IVD_INIT_DEC_MEM_REC_INCORRECT_TYPE;
                            DEBUG("i: %d \n", i);
                            return IV_FAIL;
                        }
                    }
                }
            }

        }
            break;

        case IVD_CMD_GET_DISPLAY_FRAME:
        {
            ihevcd_cxa_get_display_frame_ip_t *ps_ip =
                            (ihevcd_cxa_get_display_frame_ip_t *)pv_api_ip;
            ihevcd_cxa_get_display_frame_op_t *ps_op =
                            (ihevcd_cxa_get_display_frame_op_t *)pv_api_op;

            ps_op->s_ivd_get_display_frame_op_t.u4_error_code = 0;

            if((ps_ip->s_ivd_get_display_frame_ip_t.u4_size
                            != sizeof(ihevcd_cxa_get_display_frame_ip_t))
                            && (ps_ip->s_ivd_get_display_frame_ip_t.u4_size
                                            != sizeof(ivd_get_display_frame_ip_t)))
            {
                ps_op->s_ivd_get_display_frame_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_get_display_frame_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if((ps_op->s_ivd_get_display_frame_op_t.u4_size
                            != sizeof(ihevcd_cxa_get_display_frame_op_t))
                            && (ps_op->s_ivd_get_display_frame_op_t.u4_size
                                            != sizeof(ivd_get_display_frame_op_t)))
            {
                ps_op->s_ivd_get_display_frame_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_get_display_frame_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

        }
            break;

        case IVD_CMD_REL_DISPLAY_FRAME:
        {
            ihevcd_cxa_rel_display_frame_ip_t *ps_ip =
                            (ihevcd_cxa_rel_display_frame_ip_t *)pv_api_ip;
            ihevcd_cxa_rel_display_frame_op_t *ps_op =
                            (ihevcd_cxa_rel_display_frame_op_t *)pv_api_op;

            ps_op->s_ivd_rel_display_frame_op_t.u4_error_code = 0;

            if((ps_ip->s_ivd_rel_display_frame_ip_t.u4_size
                            != sizeof(ihevcd_cxa_rel_display_frame_ip_t))
                            && (ps_ip->s_ivd_rel_display_frame_ip_t.u4_size
                                            != sizeof(ivd_rel_display_frame_ip_t)))
            {
                ps_op->s_ivd_rel_display_frame_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_rel_display_frame_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if((ps_op->s_ivd_rel_display_frame_op_t.u4_size
                            != sizeof(ihevcd_cxa_rel_display_frame_op_t))
                            && (ps_op->s_ivd_rel_display_frame_op_t.u4_size
                                            != sizeof(ivd_rel_display_frame_op_t)))
            {
                ps_op->s_ivd_rel_display_frame_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_rel_display_frame_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

        }
            break;

        case IVD_CMD_SET_DISPLAY_FRAME:
        {
            ihevcd_cxa_set_display_frame_ip_t *ps_ip =
                            (ihevcd_cxa_set_display_frame_ip_t *)pv_api_ip;
            ihevcd_cxa_set_display_frame_op_t *ps_op =
                            (ihevcd_cxa_set_display_frame_op_t *)pv_api_op;
            UWORD32 j;

            ps_op->s_ivd_set_display_frame_op_t.u4_error_code = 0;

            if((ps_ip->s_ivd_set_display_frame_ip_t.u4_size
                            != sizeof(ihevcd_cxa_set_display_frame_ip_t))
                            && (ps_ip->s_ivd_set_display_frame_ip_t.u4_size
                                            != sizeof(ivd_set_display_frame_ip_t)))
            {
                ps_op->s_ivd_set_display_frame_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_set_display_frame_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if((ps_op->s_ivd_set_display_frame_op_t.u4_size
                            != sizeof(ihevcd_cxa_set_display_frame_op_t))
                            && (ps_op->s_ivd_set_display_frame_op_t.u4_size
                                            != sizeof(ivd_set_display_frame_op_t)))
            {
                ps_op->s_ivd_set_display_frame_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_set_display_frame_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if(ps_ip->s_ivd_set_display_frame_ip_t.num_disp_bufs == 0)
            {
                ps_op->s_ivd_set_display_frame_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_set_display_frame_op_t.u4_error_code |=
                                IVD_DISP_FRM_ZERO_OP_BUFS;
                return IV_FAIL;
            }

            for(j = 0; j < ps_ip->s_ivd_set_display_frame_ip_t.num_disp_bufs;
                            j++)
            {
                if(ps_ip->s_ivd_set_display_frame_ip_t.s_disp_buffer[j].u4_num_bufs
                                == 0)
                {
                    ps_op->s_ivd_set_display_frame_op_t.u4_error_code |= 1
                                    << IVD_UNSUPPORTEDPARAM;
                    ps_op->s_ivd_set_display_frame_op_t.u4_error_code |=
                                    IVD_DISP_FRM_ZERO_OP_BUFS;
                    return IV_FAIL;
                }

                for(i = 0;
                                i
                                                < (WORD32)ps_ip->s_ivd_set_display_frame_ip_t.s_disp_buffer[j].u4_num_bufs;
                                i++)
                {
                    if(ps_ip->s_ivd_set_display_frame_ip_t.s_disp_buffer[j].pu1_bufs[i]
                                    == NULL)
                    {
                        ps_op->s_ivd_set_display_frame_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_set_display_frame_op_t.u4_error_code |=
                                        IVD_DISP_FRM_OP_BUF_NULL;
                        return IV_FAIL;
                    }

                    if(ps_ip->s_ivd_set_display_frame_ip_t.s_disp_buffer[j].u4_min_out_buf_size[i]
                                    == 0)
                    {
                        ps_op->s_ivd_set_display_frame_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_set_display_frame_op_t.u4_error_code |=
                                        IVD_DISP_FRM_ZERO_OP_BUF_SIZE;
                        return IV_FAIL;
                    }
                }
            }
        }
            break;

        case IVD_CMD_VIDEO_DECODE:
        {
            ihevcd_cxa_video_decode_ip_t *ps_ip =
                            (ihevcd_cxa_video_decode_ip_t *)pv_api_ip;
            ihevcd_cxa_video_decode_op_t *ps_op =
                            (ihevcd_cxa_video_decode_op_t *)pv_api_op;

            DEBUG("The input bytes is: %d",
                            ps_ip->s_ivd_video_decode_ip_t.u4_num_Bytes);
            ps_op->s_ivd_video_decode_op_t.u4_error_code = 0;

            if(ps_ip->s_ivd_video_decode_ip_t.u4_size
                            != sizeof(ihevcd_cxa_video_decode_ip_t)
                            && ps_ip->s_ivd_video_decode_ip_t.u4_size
                                            != offsetof(ivd_video_decode_ip_t,
                                                        s_out_buffer))
            {
                ps_op->s_ivd_video_decode_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_video_decode_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if(ps_op->s_ivd_video_decode_op_t.u4_size
                            != sizeof(ihevcd_cxa_video_decode_op_t)
                            && ps_op->s_ivd_video_decode_op_t.u4_size
                                            != offsetof(ivd_video_decode_op_t,
                                                        u4_output_present))
            {
                ps_op->s_ivd_video_decode_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_video_decode_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

        }
            break;

        case IV_CMD_RETRIEVE_MEMREC:
        {
            ihevcd_cxa_retrieve_mem_rec_ip_t *ps_ip =
                            (ihevcd_cxa_retrieve_mem_rec_ip_t *)pv_api_ip;
            ihevcd_cxa_retrieve_mem_rec_op_t *ps_op =
                            (ihevcd_cxa_retrieve_mem_rec_op_t *)pv_api_op;
            iv_mem_rec_t *ps_mem_rec;

            ps_op->s_ivd_retrieve_mem_rec_op_t.u4_error_code = 0;

            if(ps_ip->s_ivd_retrieve_mem_rec_ip_t.u4_size
                            != sizeof(ihevcd_cxa_retrieve_mem_rec_ip_t))
            {
                ps_op->s_ivd_retrieve_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_retrieve_mem_rec_op_t.u4_error_code |=
                                IVD_IP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            if(ps_op->s_ivd_retrieve_mem_rec_op_t.u4_size
                            != sizeof(ihevcd_cxa_retrieve_mem_rec_op_t))
            {
                ps_op->s_ivd_retrieve_mem_rec_op_t.u4_error_code |= 1
                                << IVD_UNSUPPORTEDPARAM;
                ps_op->s_ivd_retrieve_mem_rec_op_t.u4_error_code |=
                                IVD_OP_API_STRUCT_SIZE_INCORRECT;
                return (IV_FAIL);
            }

            ps_mem_rec = ps_ip->s_ivd_retrieve_mem_rec_ip_t.pv_mem_rec_location;
            /* check memrecords sizes are correct */
            for(i = 0; i < MEM_REC_CNT; i++)
            {
                if(ps_mem_rec[i].u4_size != sizeof(iv_mem_rec_t))
                {
                    ps_op->s_ivd_retrieve_mem_rec_op_t.u4_error_code |= 1
                                    << IVD_UNSUPPORTEDPARAM;
                    ps_op->s_ivd_retrieve_mem_rec_op_t.u4_error_code |=
                                    IVD_MEM_REC_STRUCT_SIZE_INCORRECT;
                    return IV_FAIL;
                }
            }
        }
            break;

        case IVD_CMD_VIDEO_CTL:
        {
            UWORD32 *pu4_ptr_cmd;
            UWORD32 sub_command;

            pu4_ptr_cmd = (UWORD32 *)pv_api_ip;
            pu4_ptr_cmd += 2;
            sub_command = *pu4_ptr_cmd;

            switch(sub_command)
            {
                case IVD_CMD_CTL_SETPARAMS:
                {
                    ihevcd_cxa_ctl_set_config_ip_t *ps_ip;
                    ihevcd_cxa_ctl_set_config_op_t *ps_op;
                    ps_ip = (ihevcd_cxa_ctl_set_config_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_set_config_op_t *)pv_api_op;

                    if(ps_ip->s_ivd_ctl_set_config_ip_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_set_config_ip_t))
                    {
                        ps_op->s_ivd_ctl_set_config_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_set_config_op_t.u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                }
                    //no break; is needed here
                case IVD_CMD_CTL_SETDEFAULT:
                {
                    ihevcd_cxa_ctl_set_config_op_t *ps_op;
                    ps_op = (ihevcd_cxa_ctl_set_config_op_t *)pv_api_op;
                    if(ps_op->s_ivd_ctl_set_config_op_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_set_config_op_t))
                    {
                        ps_op->s_ivd_ctl_set_config_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_set_config_op_t.u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                }
                    break;

                case IVD_CMD_CTL_GETPARAMS:
                {
                    ihevcd_cxa_ctl_getstatus_ip_t *ps_ip;
                    ihevcd_cxa_ctl_getstatus_op_t *ps_op;

                    ps_ip = (ihevcd_cxa_ctl_getstatus_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_getstatus_op_t *)pv_api_op;
                    if(ps_ip->s_ivd_ctl_getstatus_ip_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_getstatus_ip_t))
                    {
                        ps_op->s_ivd_ctl_getstatus_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_getstatus_op_t.u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                    if((ps_op->s_ivd_ctl_getstatus_op_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_getstatus_op_t)) &&
                       (ps_op->s_ivd_ctl_getstatus_op_t.u4_size
                                    != sizeof(ivd_ctl_getstatus_op_t)))
                    {
                        ps_op->s_ivd_ctl_getstatus_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_getstatus_op_t.u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                }
                    break;

                case IVD_CMD_CTL_GETBUFINFO:
                {
                    ihevcd_cxa_ctl_getbufinfo_ip_t *ps_ip;
                    ihevcd_cxa_ctl_getbufinfo_op_t *ps_op;
                    ps_ip = (ihevcd_cxa_ctl_getbufinfo_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_getbufinfo_op_t *)pv_api_op;

                    if(ps_ip->s_ivd_ctl_getbufinfo_ip_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_getbufinfo_ip_t))
                    {
                        ps_op->s_ivd_ctl_getbufinfo_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_getbufinfo_op_t.u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                    if(ps_op->s_ivd_ctl_getbufinfo_op_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_getbufinfo_op_t))
                    {
                        ps_op->s_ivd_ctl_getbufinfo_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_getbufinfo_op_t.u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                }
                    break;

                case IVD_CMD_CTL_GETVERSION:
                {
                    ihevcd_cxa_ctl_getversioninfo_ip_t *ps_ip;
                    ihevcd_cxa_ctl_getversioninfo_op_t *ps_op;
                    ps_ip = (ihevcd_cxa_ctl_getversioninfo_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_getversioninfo_op_t *)pv_api_op;
                    if(ps_ip->s_ivd_ctl_getversioninfo_ip_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_getversioninfo_ip_t))
                    {
                        ps_op->s_ivd_ctl_getversioninfo_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_getversioninfo_op_t.u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                    if(ps_op->s_ivd_ctl_getversioninfo_op_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_getversioninfo_op_t))
                    {
                        ps_op->s_ivd_ctl_getversioninfo_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_getversioninfo_op_t.u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                }
                    break;

                case IVD_CMD_CTL_FLUSH:
                {
                    ihevcd_cxa_ctl_flush_ip_t *ps_ip;
                    ihevcd_cxa_ctl_flush_op_t *ps_op;
                    ps_ip = (ihevcd_cxa_ctl_flush_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_flush_op_t *)pv_api_op;
                    if(ps_ip->s_ivd_ctl_flush_ip_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_flush_ip_t))
                    {
                        ps_op->s_ivd_ctl_flush_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_flush_op_t.u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                    if(ps_op->s_ivd_ctl_flush_op_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_flush_op_t))
                    {
                        ps_op->s_ivd_ctl_flush_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_flush_op_t.u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                }
                    break;

                case IVD_CMD_CTL_RESET:
                {
                    ihevcd_cxa_ctl_reset_ip_t *ps_ip;
                    ihevcd_cxa_ctl_reset_op_t *ps_op;
                    ps_ip = (ihevcd_cxa_ctl_reset_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_reset_op_t *)pv_api_op;
                    if(ps_ip->s_ivd_ctl_reset_ip_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_reset_ip_t))
                    {
                        ps_op->s_ivd_ctl_reset_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_reset_op_t.u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                    if(ps_op->s_ivd_ctl_reset_op_t.u4_size
                                    != sizeof(ihevcd_cxa_ctl_reset_op_t))
                    {
                        ps_op->s_ivd_ctl_reset_op_t.u4_error_code |= 1
                                        << IVD_UNSUPPORTEDPARAM;
                        ps_op->s_ivd_ctl_reset_op_t.u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }
                }
                    break;
                case IHEVCD_CXA_CMD_CTL_DEGRADE:
                {
                    ihevcd_cxa_ctl_degrade_ip_t *ps_ip;
                    ihevcd_cxa_ctl_degrade_op_t *ps_op;

                    ps_ip = (ihevcd_cxa_ctl_degrade_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_degrade_op_t *)pv_api_op;

                    if(ps_ip->u4_size
                                    != sizeof(ihevcd_cxa_ctl_degrade_ip_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    if(ps_op->u4_size
                                    != sizeof(ihevcd_cxa_ctl_degrade_op_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    if((ps_ip->i4_degrade_pics < 0) ||
                       (ps_ip->i4_degrade_pics > 4) ||
                       (ps_ip->i4_nondegrade_interval < 0) ||
                       (ps_ip->i4_degrade_type < 0) ||
                       (ps_ip->i4_degrade_type > 15))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        return IV_FAIL;
                    }

                    break;
                }

                case IHEVCD_CXA_CMD_CTL_GET_BUFFER_DIMENSIONS:
                {
                    ihevcd_cxa_ctl_get_frame_dimensions_ip_t *ps_ip;
                    ihevcd_cxa_ctl_get_frame_dimensions_op_t *ps_op;

                    ps_ip =
                                    (ihevcd_cxa_ctl_get_frame_dimensions_ip_t *)pv_api_ip;
                    ps_op =
                                    (ihevcd_cxa_ctl_get_frame_dimensions_op_t *)pv_api_op;

                    if(ps_ip->u4_size
                                    != sizeof(ihevcd_cxa_ctl_get_frame_dimensions_ip_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    if(ps_op->u4_size
                                    != sizeof(ihevcd_cxa_ctl_get_frame_dimensions_op_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    break;
                }

                case IHEVCD_CXA_CMD_CTL_GET_VUI_PARAMS:
                {
                    ihevcd_cxa_ctl_get_vui_params_ip_t *ps_ip;
                    ihevcd_cxa_ctl_get_vui_params_op_t *ps_op;

                    ps_ip =
                                    (ihevcd_cxa_ctl_get_vui_params_ip_t *)pv_api_ip;
                    ps_op =
                                    (ihevcd_cxa_ctl_get_vui_params_op_t *)pv_api_op;

                    if(ps_ip->u4_size
                                    != sizeof(ihevcd_cxa_ctl_get_vui_params_ip_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    if(ps_op->u4_size
                                    != sizeof(ihevcd_cxa_ctl_get_vui_params_op_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    break;
                }
                case IHEVCD_CXA_CMD_CTL_SET_NUM_CORES:
                {
                    ihevcd_cxa_ctl_set_num_cores_ip_t *ps_ip;
                    ihevcd_cxa_ctl_set_num_cores_op_t *ps_op;

                    ps_ip = (ihevcd_cxa_ctl_set_num_cores_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_set_num_cores_op_t *)pv_api_op;

                    if(ps_ip->u4_size
                                    != sizeof(ihevcd_cxa_ctl_set_num_cores_ip_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    if(ps_op->u4_size
                                    != sizeof(ihevcd_cxa_ctl_set_num_cores_op_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

#ifdef MULTICORE
                    if((ps_ip->u4_num_cores < 1) || (ps_ip->u4_num_cores > MAX_NUM_CORES))
#else
                    if(ps_ip->u4_num_cores != 1)
#endif
                        {
                            ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                            return IV_FAIL;
                        }
                    break;
                }
                case IHEVCD_CXA_CMD_CTL_SET_PROCESSOR:
                {
                    ihevcd_cxa_ctl_set_processor_ip_t *ps_ip;
                    ihevcd_cxa_ctl_set_processor_op_t *ps_op;

                    ps_ip = (ihevcd_cxa_ctl_set_processor_ip_t *)pv_api_ip;
                    ps_op = (ihevcd_cxa_ctl_set_processor_op_t *)pv_api_op;

                    if(ps_ip->u4_size
                                    != sizeof(ihevcd_cxa_ctl_set_processor_ip_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_IP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    if(ps_op->u4_size
                                    != sizeof(ihevcd_cxa_ctl_set_processor_op_t))
                    {
                        ps_op->u4_error_code |= 1 << IVD_UNSUPPORTEDPARAM;
                        ps_op->u4_error_code |=
                                        IVD_OP_API_STRUCT_SIZE_INCORRECT;
                        return IV_FAIL;
                    }

                    break;
                }
                default:
                    *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
                    *(pu4_api_op + 1) |= IVD_UNSUPPORTED_API_CMD;
                    return IV_FAIL;
            }
        }
            break;
        default:
            *(pu4_api_op + 1) |= 1 << IVD_UNSUPPORTEDPARAM;
            *(pu4_api_op + 1) |= IVD_UNSUPPORTED_API_CMD;
            return IV_FAIL;
    }

    return IV_SUCCESS;
}


/**
*******************************************************************************
*
* @brief
*  Sets default dynamic parameters
*
* @par Description:
*  Sets default dynamic parameters. Will be called in ihevcd_init() to ensure
* that even if set_params is not called, codec  continues to work
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_set_default_params(codec_t *ps_codec)
{

    WORD32 ret = IV_SUCCESS;

    ps_codec->e_pic_skip_mode = IVD_SKIP_NONE;
    ps_codec->i4_strd = 0;
    ps_codec->i4_disp_strd = 0;
    ps_codec->i4_header_mode = 0;
    ps_codec->e_pic_out_order = IVD_DISPLAY_FRAME_OUT;
    return ret;
}

void ihevcd_update_function_ptr(codec_t *ps_codec)
{

    /* Init inter pred function array */
    ps_codec->apf_inter_pred[0] = NULL;
    ps_codec->apf_inter_pred[1] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_copy_fptr;
    ps_codec->apf_inter_pred[2] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_vert_fptr;
    ps_codec->apf_inter_pred[3] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_horz_fptr;
    ps_codec->apf_inter_pred[4] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_horz_w16out_fptr;
    ps_codec->apf_inter_pred[5] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_copy_w16out_fptr;
    ps_codec->apf_inter_pred[6] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_vert_w16out_fptr;
    ps_codec->apf_inter_pred[7] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_horz_w16out_fptr;
    ps_codec->apf_inter_pred[8] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_horz_w16out_fptr;
    ps_codec->apf_inter_pred[9] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_vert_w16inp_fptr;
    ps_codec->apf_inter_pred[10] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_luma_vert_w16inp_w16out_fptr;
    ps_codec->apf_inter_pred[11] = NULL;
    ps_codec->apf_inter_pred[12] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_copy_fptr;
    ps_codec->apf_inter_pred[13] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_vert_fptr;
    ps_codec->apf_inter_pred[14] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_horz_fptr;
    ps_codec->apf_inter_pred[15] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_horz_w16out_fptr;
    ps_codec->apf_inter_pred[16] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_copy_w16out_fptr;
    ps_codec->apf_inter_pred[17] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_vert_w16out_fptr;
    ps_codec->apf_inter_pred[18] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_horz_w16out_fptr;
    ps_codec->apf_inter_pred[19] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_horz_w16out_fptr;
    ps_codec->apf_inter_pred[20] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_vert_w16inp_fptr;
    ps_codec->apf_inter_pred[21] = (pf_inter_pred)ps_codec->s_func_selector.ihevc_inter_pred_chroma_vert_w16inp_w16out_fptr;

    /* Init intra pred function array */
    ps_codec->apf_intra_pred_luma[0] = (pf_intra_pred)NULL;
    ps_codec->apf_intra_pred_luma[1] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_planar_fptr;
    ps_codec->apf_intra_pred_luma[2] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_dc_fptr;
    ps_codec->apf_intra_pred_luma[3] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_mode2_fptr;
    ps_codec->apf_intra_pred_luma[4] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_mode_3_to_9_fptr;
    ps_codec->apf_intra_pred_luma[5] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_horz_fptr;
    ps_codec->apf_intra_pred_luma[6] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_mode_11_to_17_fptr;
    ps_codec->apf_intra_pred_luma[7] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_mode_18_34_fptr;
    ps_codec->apf_intra_pred_luma[8] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_mode_19_to_25_fptr;
    ps_codec->apf_intra_pred_luma[9] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_ver_fptr;
    ps_codec->apf_intra_pred_luma[10] =  (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_luma_mode_27_to_33_fptr;

    ps_codec->apf_intra_pred_chroma[0] = (pf_intra_pred)NULL;
    ps_codec->apf_intra_pred_chroma[1] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_planar_fptr;
    ps_codec->apf_intra_pred_chroma[2] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_dc_fptr;
    ps_codec->apf_intra_pred_chroma[3] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_mode2_fptr;
    ps_codec->apf_intra_pred_chroma[4] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_mode_3_to_9_fptr;
    ps_codec->apf_intra_pred_chroma[5] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_horz_fptr;
    ps_codec->apf_intra_pred_chroma[6] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_mode_11_to_17_fptr;
    ps_codec->apf_intra_pred_chroma[7] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_mode_18_34_fptr;
    ps_codec->apf_intra_pred_chroma[8] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_mode_19_to_25_fptr;
    ps_codec->apf_intra_pred_chroma[9] =  (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_ver_fptr;
    ps_codec->apf_intra_pred_chroma[10] = (pf_intra_pred)ps_codec->s_func_selector.ihevc_intra_pred_chroma_mode_27_to_33_fptr;

    /* Init itrans_recon function array */
    ps_codec->apf_itrans_recon[0] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_itrans_recon_4x4_ttype1_fptr;
    ps_codec->apf_itrans_recon[1] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_itrans_recon_4x4_fptr;
    ps_codec->apf_itrans_recon[2] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_itrans_recon_8x8_fptr;
    ps_codec->apf_itrans_recon[3] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_itrans_recon_16x16_fptr;
    ps_codec->apf_itrans_recon[4] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_itrans_recon_32x32_fptr;
    ps_codec->apf_itrans_recon[5] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_chroma_itrans_recon_4x4_fptr;
    ps_codec->apf_itrans_recon[6] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_chroma_itrans_recon_8x8_fptr;
    ps_codec->apf_itrans_recon[7] = (pf_itrans_recon)ps_codec->s_func_selector.ihevc_chroma_itrans_recon_16x16_fptr;

    /* Init recon function array */
    ps_codec->apf_recon[0] = (pf_recon)ps_codec->s_func_selector.ihevc_recon_4x4_ttype1_fptr;
    ps_codec->apf_recon[1] = (pf_recon)ps_codec->s_func_selector.ihevc_recon_4x4_fptr;
    ps_codec->apf_recon[2] = (pf_recon)ps_codec->s_func_selector.ihevc_recon_8x8_fptr;
    ps_codec->apf_recon[3] = (pf_recon)ps_codec->s_func_selector.ihevc_recon_16x16_fptr;
    ps_codec->apf_recon[4] = (pf_recon)ps_codec->s_func_selector.ihevc_recon_32x32_fptr;
    ps_codec->apf_recon[5] = (pf_recon)ps_codec->s_func_selector.ihevc_chroma_recon_4x4_fptr;
    ps_codec->apf_recon[6] = (pf_recon)ps_codec->s_func_selector.ihevc_chroma_recon_8x8_fptr;
    ps_codec->apf_recon[7] = (pf_recon)ps_codec->s_func_selector.ihevc_chroma_recon_16x16_fptr;

    /* Init itrans_recon_dc function array */
    ps_codec->apf_itrans_recon_dc[0] = (pf_itrans_recon_dc)ps_codec->s_func_selector.ihevcd_itrans_recon_dc_luma_fptr;
    ps_codec->apf_itrans_recon_dc[1] = (pf_itrans_recon_dc)ps_codec->s_func_selector.ihevcd_itrans_recon_dc_chroma_fptr;

    /* Init sao function array */
    ps_codec->apf_sao_luma[0] = (pf_sao_luma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class0_fptr;
    ps_codec->apf_sao_luma[1] = (pf_sao_luma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class1_fptr;
    ps_codec->apf_sao_luma[2] = (pf_sao_luma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class2_fptr;
    ps_codec->apf_sao_luma[3] = (pf_sao_luma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class3_fptr;

    ps_codec->apf_sao_chroma[0] = (pf_sao_chroma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class0_chroma_fptr;
    ps_codec->apf_sao_chroma[1] = (pf_sao_chroma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class1_chroma_fptr;
    ps_codec->apf_sao_chroma[2] = (pf_sao_chroma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class2_chroma_fptr;
    ps_codec->apf_sao_chroma[3] = (pf_sao_chroma)ps_codec->s_func_selector.ihevc_sao_edge_offset_class3_chroma_fptr;
}
/**
*******************************************************************************
*
* @brief
*  Initialize the context. This will be called by  init_mem_rec and during
* reset
*
* @par Description:
*  Initializes the context
*
* @param[in] ps_codec
*  Codec context pointer
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_init(codec_t *ps_codec)
{
    WORD32 status = IV_SUCCESS;
    WORD32 i;


    ps_codec->i4_num_disp_bufs = 1;
    ps_codec->i4_flush_mode = 0;

    ps_codec->i4_ht = ps_codec->i4_disp_ht = ps_codec->i4_max_ht;
    ps_codec->i4_wd = ps_codec->i4_disp_wd = ps_codec->i4_max_wd;
    ps_codec->i4_strd = 0;
    ps_codec->i4_disp_strd = 0;
    ps_codec->i4_num_cores = 1;

    ps_codec->u4_pic_cnt = 0;
    ps_codec->u4_disp_cnt = 0;

    ps_codec->i4_header_mode = 0;
    ps_codec->i4_header_in_slice_mode = 0;
    ps_codec->i4_sps_done = 0;
    ps_codec->i4_pps_done = 0;
    ps_codec->i4_init_done   = 1;
    ps_codec->i4_first_pic_done = 0;
    ps_codec->s_parse.i4_first_pic_init = 0;
    ps_codec->i4_error_code = 0;
    ps_codec->i4_reset_flag = 0;
    ps_codec->i4_cra_as_first_pic = 1;
    ps_codec->i4_rasl_output_flag = 0;

    ps_codec->i4_prev_poc_msb = 0;
    ps_codec->i4_prev_poc_lsb = -1;
    ps_codec->i4_max_prev_poc_lsb = -1;
    ps_codec->s_parse.i4_abs_pic_order_cnt = -1;

    /* Set ref chroma format by default to 420SP UV interleaved */
    ps_codec->e_ref_chroma_fmt = IV_YUV_420SP_UV;

    /* If the codec is in shared mode and required format is 420 SP VU interleaved then change
     * reference buffers chroma format
     */
    if(IV_YUV_420SP_VU == ps_codec->e_chroma_fmt)
    {
        ps_codec->e_ref_chroma_fmt = IV_YUV_420SP_VU;
    }



    ps_codec->i4_disable_deblk_pic = 0;

    ps_codec->i4_degrade_pic_cnt    = 0;
    ps_codec->i4_degrade_pics       = 0;
    ps_codec->i4_degrade_type       = 0;
    ps_codec->i4_disable_sao_pic    = 0;
    ps_codec->i4_fullpel_inter_pred = 0;
    ps_codec->u4_enable_fmt_conv_ahead = 0;
    ps_codec->i4_share_disp_buf_cnt = 0;

    {
        sps_t *ps_sps = ps_codec->ps_sps_base;
        pps_t *ps_pps = ps_codec->ps_pps_base;

        for(i = 0; i < MAX_SPS_CNT; i++)
        {
            ps_sps->i1_sps_valid = 0;
            ps_sps++;
        }

        for(i = 0; i < MAX_PPS_CNT; i++)
        {
            ps_pps->i1_pps_valid = 0;
            ps_pps++;
        }
    }

    ihevcd_set_default_params(ps_codec);
    ps_codec->pv_proc_jobq = ihevcd_jobq_init(ps_codec->pv_proc_jobq_buf, ps_codec->i4_proc_jobq_buf_size);
    RETURN_IF((ps_codec->pv_proc_jobq == NULL), IV_FAIL);

    /* Update the jobq context to all the threads */
    ps_codec->s_parse.pv_proc_jobq = ps_codec->pv_proc_jobq;
    for(i = 0; i < MAX_PROCESS_THREADS; i++)
    {
        ps_codec->as_process[i].pv_proc_jobq = ps_codec->pv_proc_jobq;
        ps_codec->as_process[i].i4_id = i;
        ps_codec->as_process[i].ps_codec = ps_codec;

        /* Set the following to zero assuming it is a single core solution
         * When threads are launched these will be set appropriately
         */
        ps_codec->as_process[i].i4_check_parse_status = 0;
        ps_codec->as_process[i].i4_check_proc_status = 0;
    }
    /* Initialize MV Bank buffer manager */
    ihevc_buf_mgr_init((buf_mgr_t *)ps_codec->pv_mv_buf_mgr);

    /* Initialize Picture buffer manager */
    ihevc_buf_mgr_init((buf_mgr_t *)ps_codec->pv_pic_buf_mgr);

    ps_codec->ps_pic_buf = (pic_buf_t *)ps_codec->pv_pic_buf_base;

    memset(ps_codec->ps_pic_buf, 0, BUF_MGR_MAX_CNT  * sizeof(pic_buf_t));



    /* Initialize display buffer manager */
    ihevc_disp_mgr_init((disp_mgr_t *)ps_codec->pv_disp_buf_mgr);

    /* Initialize dpb manager */
    ihevc_dpb_mgr_init((dpb_mgr_t *)ps_codec->pv_dpb_mgr);

    ps_codec->e_processor_soc = SOC_GENERIC;
    /* The following can be over-ridden using soc parameter as a hack */
    ps_codec->u4_nctb = 0x7FFFFFFF;
    ihevcd_init_arch(ps_codec);

    ihevcd_init_function_ptr(ps_codec);

    ihevcd_update_function_ptr(ps_codec);

    return status;
}

/**
*******************************************************************************
*
* @brief
*  Gets number of memory records required by the codec
*
* @par Description:
*  Gets codec mem record requirements and adds concealment  modules
* requirements
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_get_num_rec(void *pv_api_ip, void *pv_api_op)
{

    iv_num_mem_rec_op_t *ps_mem_q_op;

    UNUSED(pv_api_ip);
    ps_mem_q_op = (iv_num_mem_rec_op_t *)pv_api_op;
    ps_mem_q_op->u4_num_mem_rec = MEM_REC_CNT;
    DEBUG("Get num mem records without concealment %d\n",
                    ps_mem_q_op->u4_num_mem_rec);
#ifdef APPLY_CONCEALMENT
    {
        IV_API_CALL_STATUS_T status;
        icncl_num_mem_rec_ip_t cncl_mem_ip;
        icncl_num_mem_rec_op_t cncl_mem_op;

        cncl_mem_ip.s_ivd_num_rec_ip_t.e_cmd = IV_CMD_GET_NUM_MEM_REC;
        cncl_mem_ip.s_ivd_num_rec_ip_t.u4_size = sizeof(icncl_num_mem_rec_ip_t);

        status = icncl_api_function(NULL, (void *)&cncl_mem_ip, (void *)&cncl_mem_op);

        if(status == IV_SUCCESS)
        {
            /* Add the concealment library's memory requirements */
            ps_mem_q_op->u4_num_mem_rec += cncl_mem_op.s_ivd_num_mem_rec_op_t.u4_num_mem_rec;
            DEBUG("Get num mem records %d\n", ps_mem_q_op->u4_num_mem_rec);
            return status; /* Nothing else to do, return */
        }
        else
        {
            /*
             * Something went wrong with the concealment library call.
             */
            DEBUG("ERROR: Get num mem records %d\n", ps_mem_q_op->u4_num_mem_rec);
            return status;
        }

    }
#endif //APPLY_CONCEALMENT


    return IV_SUCCESS;

}

/**
*******************************************************************************
*
* @brief
*  Fills memory requirements of the codec
*
* @par Description:
*  Gets codec mem record requirements and adds concealment  modules
* requirements
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_fill_num_mem_rec(void *pv_api_ip, void *pv_api_op)
{

    ihevcd_cxa_fill_mem_rec_ip_t *ps_mem_q_ip;
    ihevcd_cxa_fill_mem_rec_op_t *ps_mem_q_op;
    WORD32 level;
    WORD32 num_reorder_frames;
    WORD32 num_ref_frames;
    WORD32 num_extra_disp_bufs;
    WORD32 max_dpb_size;

    iv_mem_rec_t *ps_mem_rec;
    iv_mem_rec_t *ps_mem_rec_base;
    WORD32 no_of_mem_rec_filled;
    WORD32 chroma_format, share_disp_buf;
    WORD32 max_ctb_cnt;
    WORD32 max_wd_luma, max_wd_chroma;
    WORD32 max_ht_luma, max_ht_chroma;
    WORD32 max_tile_cols, max_tile_rows;
    WORD32 max_ctb_rows, max_ctb_cols;
    WORD32 max_num_cu_cols;
    WORD32 i;
    WORD32 max_num_4x4_cols;
    IV_API_CALL_STATUS_T status = IV_SUCCESS;
    no_of_mem_rec_filled = 0;

    //TODO: Remove as and when the following are used
    UNUSED(num_extra_disp_bufs);
    UNUSED(no_of_mem_rec_filled);
    UNUSED(max_wd_chroma);
    UNUSED(max_ht_chroma);

    ps_mem_q_ip = (ihevcd_cxa_fill_mem_rec_ip_t *)pv_api_ip;
    ps_mem_q_op = (ihevcd_cxa_fill_mem_rec_op_t *)pv_api_op;

    if(ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                    > offsetof(ihevcd_cxa_fill_mem_rec_ip_t, i4_level))
    {
        level = ps_mem_q_ip->i4_level;
        /* Spec requires level should be multiplied by 30
         * API has values where level is multiplied by 10. This keeps it consistent with H264
         * Because of the above differences, level is multiplied by 3 here.
         */
        level *= 3;
    }
    else
    {
        level = MAX_LEVEL;
    }

    if(ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                    > offsetof(ihevcd_cxa_fill_mem_rec_ip_t,
                               u4_num_reorder_frames))
    {
        num_reorder_frames = ps_mem_q_ip->u4_num_reorder_frames;
    }
    else
    {
        num_reorder_frames = MAX_REF_CNT;
    }

    if(ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                    > offsetof(ihevcd_cxa_fill_mem_rec_ip_t, u4_num_ref_frames))
    {
        num_ref_frames = ps_mem_q_ip->u4_num_ref_frames;
    }
    else
    {
        num_ref_frames = MAX_REF_CNT;
    }

    if(ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                    > offsetof(ihevcd_cxa_fill_mem_rec_ip_t,
                               u4_num_extra_disp_buf))
    {
        num_extra_disp_bufs = ps_mem_q_ip->u4_num_extra_disp_buf;
    }
    else
    {
        num_extra_disp_bufs = 0;
    }

    if(ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                    > offsetof(ihevcd_cxa_fill_mem_rec_ip_t, u4_share_disp_buf))
    {
#ifndef LOGO_EN
        share_disp_buf = ps_mem_q_ip->u4_share_disp_buf;
#else
        share_disp_buf = 0;
#endif
    }
    else
    {
        share_disp_buf = 0;
    }

    if(ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_size
                    > offsetof(ihevcd_cxa_fill_mem_rec_ip_t, e_output_format))
    {
        chroma_format = ps_mem_q_ip->e_output_format;
    }
    else
    {
        chroma_format = -1;
    }

    /* Shared disp buffer mode is supported only for 420SP formats */
    if((chroma_format != IV_YUV_420P) &&
       (chroma_format != IV_YUV_420SP_UV) &&
       (chroma_format != IV_YUV_420SP_VU))
    {
        share_disp_buf = 0;
    }

    {

        max_ht_luma = ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_max_frm_ht;
        max_wd_luma = ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_max_frm_wd;

        max_ht_luma = ALIGN64(max_ht_luma);
        max_wd_luma = ALIGN64(max_wd_luma);



        max_tile_cols = (max_wd_luma + MIN_TILE_WD - 1) / MIN_TILE_WD;
        max_tile_rows = (max_ht_luma + MIN_TILE_HT - 1) / MIN_TILE_HT;
        max_ctb_rows  = max_ht_luma / MIN_CTB_SIZE;
        max_ctb_cols  = max_wd_luma / MIN_CTB_SIZE;
        max_ctb_cnt   = max_ctb_rows * max_ctb_cols;
        max_num_cu_cols = max_wd_luma / MIN_CU_SIZE;
        max_num_4x4_cols = max_wd_luma / 4;
    }
    /*
     * If level is lesser than 31 and the resolution required is higher,
     * then make the level at least 31.
     */
    /*    if (num_mbs > MAX_NUM_MBS_3_0 && level < MAX_LEVEL)
     {
     level           = MAX_LEVEL;
     }
     */
    if((level < MIN_LEVEL) || (level > MAX_LEVEL))
    {
        ps_mem_q_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                        IHEVCD_LEVEL_UNSUPPORTED;
        level = MAX_LEVEL;
    }
    if(num_ref_frames > MAX_REF_CNT)
    {
        ps_mem_q_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                        IHEVCD_NUM_REF_UNSUPPORTED;
        num_ref_frames = MAX_REF_CNT;
    }

    if(num_reorder_frames > MAX_REF_CNT)
    {
        ps_mem_q_op->s_ivd_fill_mem_rec_op_t.u4_error_code |=
                        IHEVCD_NUM_REORDER_UNSUPPORTED;
        num_reorder_frames = MAX_REF_CNT;
    }

    max_dpb_size = ihevcd_get_dpb_size(level, max_wd_luma * max_ht_luma);
    ps_mem_rec_base = ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.pv_mem_rec_location;

    /* Set all memory reconds as persistent and alignment as 128
     * by default
     */
    ps_mem_rec = ps_mem_rec_base;
    for(i = 0; i < MEM_REC_CNT; i++)
    {
        ps_mem_rec->u4_mem_alignment = 128;
        ps_mem_rec->e_mem_type = IV_EXTERNAL_CACHEABLE_PERSISTENT_MEM;
        ps_mem_rec++;
    }

    /* Request memory for HEVCD object */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_IV_OBJ];
    ps_mem_rec->u4_mem_size = sizeof(iv_obj_t);

    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_IV_OBJ,
                    ps_mem_rec->u4_mem_size);

    /* Request memory for HEVC Codec context */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_CODEC];
    ps_mem_rec->u4_mem_size = sizeof(codec_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_CODEC,
                    ps_mem_rec->u4_mem_size);

    /* Request memory for buffer which holds bitstream after emulation prevention */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_BITSBUF];
    ps_mem_rec->u4_mem_size = MAX((max_wd_luma * max_ht_luma), MIN_BITSBUF_SIZE);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_BITSBUF,
                    ps_mem_rec->u4_mem_size);

    /* Request memory for buffer which holds TU structures and coeff data for
     * a set of CTBs in the current picture */
    /*TODO Currently the buffer is allocated at a frame level. Reduce this to
     * allocate for s set of CTBs and add appropriate synchronization logic to
     * ensure that this is data is not overwritten before consumption
     */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TU_DATA];
    ps_mem_rec->u4_mem_size = ihevcd_get_tu_data_size(max_wd_luma * max_ht_luma);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_TU_DATA,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_MVBANK];

    ps_mem_rec->u4_mem_size = sizeof(buf_mgr_t);

    /* Size for holding mv_buf_t for each MV Bank */
    /* Note this allocation is done for BUF_MGR_MAX_CNT instead of
     * max_dpb_size or MAX_DPB_SIZE for following reasons
     * max_dpb_size will be based on max_wd and max_ht
     * For higher max_wd and max_ht this number will be smaller than MAX_DPB_SIZE
     * But during actual initialization number of buffers allocated can be more
     *
     * One extra MV Bank is needed to hold current pics MV bank.
     * Since this is only a structure allocation and not actual buffer allocation,
     * it is allocated for (MAX_DPB_SIZE + 1) entries
     */
    ps_mem_rec->u4_mem_size += (MAX_DPB_SIZE + 1) * sizeof(mv_buf_t);

    {
        /* Allocate for pu_map, pu_t and pic_pu_idx for each MV bank */
        /* Note: Number of luma samples is not max_wd * max_ht here, instead it is
         * set to maximum number of luma samples allowed at the given level.
         * This is done to ensure that any stream with width and height lesser
         * than max_wd and max_ht is supported. Number of buffers required can be greater
         * for lower width and heights at a given level and this increased number of buffers
         * might require more memory than what max_wd and max_ht buffer would have required
         * Also note one extra buffer is allocted to store current pictures MV bank
         * In case of asynchronous parsing and processing, number of buffers should increase here
         * based on when parsing and processing threads are synchronized
         */
        WORD32 lvl_idx = ihevcd_get_lvl_idx(level);
        WORD32 max_luma_samples = gai4_ihevc_max_luma_pic_size[lvl_idx];
        ps_mem_rec->u4_mem_size += (max_dpb_size + 1) *
                        ihevcd_get_pic_mv_bank_size(max_luma_samples);
        DEBUG("\nMemory record Id %d = %d \n", MEM_REC_MVBANK,
                        ps_mem_rec->u4_mem_size);
    }
    // TODO GPU : Have to creat ping-pong view for VPS,SPS,PPS.
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_VPS];
    ps_mem_rec->u4_mem_size = MAX_VPS_CNT * sizeof(vps_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_VPS,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SPS];
    ps_mem_rec->u4_mem_size = MAX_SPS_CNT * sizeof(sps_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_SPS,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PPS];
    ps_mem_rec->u4_mem_size = MAX_PPS_CNT * sizeof(pps_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PPS,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SLICE_HDR];
    ps_mem_rec->u4_mem_size = MAX_SLICE_HDR_CNT * sizeof(slice_header_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_SLICE_HDR,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TILE];
    {
        WORD32 tile_size;

        tile_size  = max_tile_cols * max_tile_rows;
        tile_size  *= sizeof(tile_t);


        ps_mem_rec->u4_mem_size = MAX_PPS_CNT * tile_size;
    }


    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_TILE,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_ENTRY_OFST];
    {
        WORD32 num_entry_points;

        /* One entry point per tile */
        num_entry_points  = max_tile_cols * max_tile_rows;

        /* One entry point per row of CTBs */
        /*********************************************************************/
        /* Only tiles or entropy sync is enabled at a time in main           */
        /* profile, but since memory required does not increase too much,    */
        /* this allocation is done to handle both cases                      */
        /*********************************************************************/
        num_entry_points  += max_ctb_rows;


        ps_mem_rec->u4_mem_size = sizeof(WORD32) * num_entry_points;
    }


    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_ENTRY_OFST,
                    ps_mem_rec->u4_mem_size);


    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SCALING_MAT];
    {
        WORD32 scaling_mat_size;

        SCALING_MAT_SIZE(scaling_mat_size)
        ps_mem_rec->u4_mem_size = (MAX_SPS_CNT + MAX_PPS_CNT) * scaling_mat_size * sizeof(WORD16);
    }
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_SCALING_MAT,
                    ps_mem_rec->u4_mem_size);

    /* Holds one row skip_flag at 8x8 level used during parsing */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_SKIP_FLAG];

    /* 1 bit per 8x8 */
    ps_mem_rec->u4_mem_size = max_num_cu_cols / 8;
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PARSE_SKIP_FLAG,
                  ps_mem_rec->u4_mem_size);

    /* Holds one row skip_flag at 8x8 level used during parsing */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_CT_DEPTH];

    /* 2 bits per 8x8 */
    ps_mem_rec->u4_mem_size = max_num_cu_cols / 4;
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PARSE_CT_DEPTH,
                  ps_mem_rec->u4_mem_size);

    /* Holds one row skip_flag at 8x8 level used during parsing */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_INTRA_PRED_MODE];

    /* 8 bits per 4x4 */
    /* 16 bytes each for top and left 64 pixels and 16 bytes for default mode */
    ps_mem_rec->u4_mem_size = 3 * 16 * sizeof(UWORD8);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PARSE_INTRA_PRED_MODE,
                  ps_mem_rec->u4_mem_size);

    /* Holds one intra mode at 8x8 level for entire picture */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_INTRA_FLAG];

    /* 1 bit per 8x8 */
    ps_mem_rec->u4_mem_size = (max_wd_luma / MIN_CU_SIZE) * (max_ht_luma / MIN_CU_SIZE) / 8;
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_INTRA_FLAG,
                  ps_mem_rec->u4_mem_size);

    /* Holds one transquant bypass flag at 8x8 level for entire picture */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TRANSQUANT_BYPASS_FLAG];

    /* 1 bit per 8x8 */
    /* Extra row and column are allocated for easy processing of top and left blocks while loop filtering */
    ps_mem_rec->u4_mem_size = ((max_wd_luma + 64) / MIN_CU_SIZE) * ((max_ht_luma + 64) / MIN_CU_SIZE) / 8;
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_TRANSQUANT_BYPASS_FLAG,
                  ps_mem_rec->u4_mem_size);

    /* Request memory to hold thread handles for each processing thread */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_THREAD_HANDLE];
    ps_mem_rec->u4_mem_size = MAX_PROCESS_THREADS * ithread_get_handle_size();
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_THREAD_HANDLE,
                    ps_mem_rec->u4_mem_size);


    {
        WORD32 job_queue_size;
        WORD32 num_jobs;
        ps_mem_rec = &ps_mem_rec_base[MEM_REC_PROC_JOBQ];


        /* One job per row of CTBs */
        num_jobs  = max_ctb_rows;

        /* One each tile a row of CTBs, num_jobs has to incremented */
        num_jobs  *= max_tile_cols;

        /* One format convert/frame copy job per row of CTBs for non-shared mode*/
        num_jobs  += max_ctb_rows;


        job_queue_size = ihevcd_jobq_ctxt_size();
        job_queue_size += num_jobs * sizeof(proc_job_t);
        ps_mem_rec->u4_mem_size = job_queue_size;
        DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PROC_JOBQ,
                        ps_mem_rec->u4_mem_size);
    }


    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_MAP];
    ps_mem_rec->u4_mem_size = max_ctb_cnt;
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PARSE_MAP,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PROC_MAP];
    ps_mem_rec->u4_mem_size = max_ctb_cnt;
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PROC_MAP,
                    ps_mem_rec->u4_mem_size);


    ps_mem_rec = &ps_mem_rec_base[MEM_REC_DISP_MGR];

    /* size for holding display manager context */
    ps_mem_rec->u4_mem_size = sizeof(buf_mgr_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_DISP_MGR,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_DPB_MGR];

    /* size for holding dpb manager context */
    ps_mem_rec->u4_mem_size = sizeof(dpb_mgr_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_DPB_MGR,
                    ps_mem_rec->u4_mem_size);

    /** Holds top and left neighbor's pu idx into picture level pu array */
    /* Only one top row is enough but left has to be replicated for each process context */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PIC_PU_IDX_NEIGHBOR];

    ps_mem_rec->u4_mem_size = (max_num_4x4_cols  /* left */ + MAX_PROCESS_THREADS * (MAX_CTB_SIZE / 4)/* top */ + 1/* top right */) * sizeof(WORD32);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PIC_PU_IDX_NEIGHBOR,
                    ps_mem_rec->u4_mem_size);



    /* TO hold scratch buffers needed for each process context */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PROC_SCRATCH];
    {
        WORD32 size = 0;
        WORD32 inter_pred_tmp_buf_size;
        WORD32 ntaps_luma;
        WORD32 pu_map_size;
        WORD32 sao_size = 0;
        ntaps_luma = 8;

        /* Max inter pred size (number of bytes) */
        inter_pred_tmp_buf_size = sizeof(WORD16) * (MAX_CTB_SIZE + ntaps_luma) * MAX_CTB_SIZE;
        inter_pred_tmp_buf_size = ALIGN64(inter_pred_tmp_buf_size);


        /* To hold pu_index w.r.t. frame level pu_t array for a CTB at 4x4 level*/
        /* 16 x 16 4x4 in a CTB of size 64 x 64 and two extra needed for holding
         * neighbors
         */
        pu_map_size = sizeof(WORD32) * (18 * 18);

        pu_map_size = ALIGN64(pu_map_size);
        size += pu_map_size;

        /* To hold inter pred temporary buffers */
        size += 2 * inter_pred_tmp_buf_size;


        /* Allocate for each process context */
        size *= MAX_PROCESS_THREADS;


        /* To hold SAO left buffer for luma */
        sao_size += sizeof(UWORD8) * (MAX(max_ht_luma, max_wd_luma));

        /* To hold SAO left buffer for chroma */
        sao_size += sizeof(UWORD8) * (MAX(max_ht_luma, max_wd_luma));

        /* To hold SAO top buffer for luma */
        sao_size += sizeof(UWORD8) * max_wd_luma;

        /* To hold SAO top buffer for chroma */
        sao_size += sizeof(UWORD8) * max_wd_luma;

        /* To hold SAO top left luma pixel value for last output ctb in a row*/
        sao_size += sizeof(UWORD8) * max_ctb_rows;

        /* To hold SAO top left chroma pixel value last output ctb in a row*/
        sao_size += sizeof(UWORD8) * max_ctb_rows * 2;

        /* To hold SAO top left pixel luma for current ctb - column array*/
        sao_size += sizeof(UWORD8) * max_ctb_rows;

        /* To hold SAO top left pixel chroma for current ctb-column array*/
        sao_size += sizeof(UWORD8) * max_ctb_rows * 2;

        /* To hold SAO top right pixel luma pixel value last output ctb in a row*/
        sao_size += sizeof(UWORD8) * max_ctb_cols;

        /* To hold SAO top right pixel chroma pixel value last output ctb in a row*/
        sao_size += sizeof(UWORD8) * max_ctb_cols * 2;

        /*To hold SAO botton bottom left pixels for luma*/
        sao_size += sizeof(UWORD8) * max_ctb_rows;

        /*To hold SAO botton bottom left pixels for luma*/
        sao_size += sizeof(UWORD8) * max_ctb_rows * 2;
        sao_size = ALIGN64(sao_size);
        size += sao_size;
        ps_mem_rec->u4_mem_size = size;
    }
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_PROC_SCRATCH,
                    ps_mem_rec->u4_mem_size);

    /* TO hold scratch buffers needed for each SAO context */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SAO_SCRATCH];
    {
        WORD32 size = 0;

        size = 4 * MAX_CTB_SIZE * MAX_CTB_SIZE;

        /* 2 temporary buffers*/
        size *= 2;

        size *= MAX_PROCESS_THREADS;

        ps_mem_rec->u4_mem_size = size;
    }
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_SAO_SCRATCH,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_BS_QP];
    {
        WORD32 size = 0;
        WORD32 vert_bs_size, horz_bs_size;
        WORD32 qp_const_flag_size;
        WORD32 qp_size, num_8x8;

        /* Max Number of vertical edges */
        vert_bs_size = max_wd_luma / 8 + 2 * MAX_CTB_SIZE / 8;

        /* Max Number of horizontal edges - extra MAX_CTB_SIZE / 8 to handle the last 4 rows separately(shifted CTB processing) */
        vert_bs_size *= (max_ht_luma + MAX_CTB_SIZE) / MIN_TU_SIZE;

        /* Number of bytes */
        vert_bs_size /= 8;

        /* Two bits per edge */
        vert_bs_size *= 2;

        /* Max Number of horizontal edges */
        horz_bs_size = max_ht_luma / 8 + MAX_CTB_SIZE / 8;

        /* Max Number of vertical edges - extra MAX_CTB_SIZE / 8 to handle the last 4 columns separately(shifted CTB processing) */
        horz_bs_size *= (max_wd_luma + MAX_CTB_SIZE) / MIN_TU_SIZE;

        /* Number of bytes */
        horz_bs_size /= 8;

        /* Two bits per edge */
        horz_bs_size *= 2;

        /* Max CTBs in a row */
        qp_const_flag_size = max_wd_luma / MIN_CTB_SIZE + 1 /* The last ctb row deblk is done in last ctb + 1 row.*/;

        /* Max CTBs in a column */
        qp_const_flag_size *= max_ht_luma / MIN_CTB_SIZE;

        /* Number of bytes */
        qp_const_flag_size = (qp_const_flag_size + 7) >> 3;

        /* QP changes at CU level - So store at 8x8 level */
        num_8x8 = (max_ht_luma * max_wd_luma) / (MIN_CU_SIZE * MIN_CU_SIZE);
        qp_size = num_8x8;

        /* To hold vertical boundary strength */
        size += vert_bs_size;

        /* To hold horizontal boundary strength */
        size += horz_bs_size;

        /* To hold QP */
        size += qp_size;

        /* To hold QP const in CTB flags */
        size += qp_const_flag_size;

        ps_mem_rec->u4_mem_size = size;
    }

    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_BS_QP,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TILE_IDX];
    {
        WORD32 size = 0;
        /* Max CTBs in a row */
        size  = max_wd_luma / MIN_CTB_SIZE + 2 /* Top row and bottom row extra. This ensures accessing left,top in first row
                                                  and right in last row will not result in invalid access*/;
        /* Max CTBs in a column */
        size *= max_ht_luma / MIN_CTB_SIZE;

        size *= sizeof(UWORD16);
        ps_mem_rec->u4_mem_size = size;
    }
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_TILE_IDX,
                    ps_mem_rec->u4_mem_size);

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SAO];
    {
        UWORD32 size;

        /* 4 bytes per color component per CTB */
        size = 3 * 4;

        /* MAX number of CTBs in a row */
        size *= max_wd_luma / MIN_CTB_SIZE;

        /* MAX number of CTBs in a column */
        size *= max_ht_luma / MIN_CTB_SIZE;
        ps_mem_rec->u4_mem_size = size;
    }

    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_SAO,
                    ps_mem_rec->u4_mem_size);


    ps_mem_rec = &ps_mem_rec_base[MEM_REC_REF_PIC];

    /* size for holding buffer manager context */
    ps_mem_rec->u4_mem_size = sizeof(buf_mgr_t);

    /* Size for holding pic_buf_t for each reference picture */
    /* Note this allocation is done for BUF_MGR_MAX_CNT instead of
     * max_dpb_size or MAX_DPB_SIZE for following reasons
     * max_dpb_size will be based on max_wd and max_ht
     * For higher max_wd and max_ht this number will be smaller than MAX_DPB_SIZE
     * But during actual initialization number of buffers allocated can be more
     *
     * Also to handle display depth application can allocate more than what
     * codec asks for in case of non-shared mode
     * Since this is only a structure allocation and not actual buffer allocation,
     * it is allocated for BUF_MGR_MAX_CNT entries
     */
    ps_mem_rec->u4_mem_size += BUF_MGR_MAX_CNT * sizeof(pic_buf_t);

    /* In case of non-shared mode allocate for reference picture buffers */
    /* In case of shared and 420p output, allocate for chroma samples */
    if((0 == share_disp_buf) || (chroma_format == IV_YUV_420P))
    {
        UWORD32 init_num_bufs;
        UWORD32 init_extra_bufs;
        WORD32 chroma_only;

        chroma_only = 0;
        init_extra_bufs = 0;
        init_num_bufs = num_reorder_frames + num_ref_frames + 1;

        /* In case of shared display buffers and chroma format 420P
         * Allocate for chroma in reference buffers, luma buffer will be display buffer
         */

        if((1 == share_disp_buf) && (chroma_format == IV_YUV_420P))
        {
            chroma_only = 1;
            init_extra_bufs = num_extra_disp_bufs;
        }

        /* Note: Number of luma samples is not max_wd * max_ht here, instead it is
         * set to maximum number of luma samples allowed at the given level.
         * This is done to ensure that any stream with width and height lesser
         * than max_wd and max_ht is supported. Number of buffers required can be greater
         * for lower width and heights at a given level and this increased number of buffers
         * might require more memory than what max_wd and max_ht buffer would have required
         * Number of buffers is doubled in order to return one frame at a time instead of sending
         * multiple outputs during dpb full case.
         * Also note one extra buffer is allocted to store current picture
         * In case of asynchronous parsing and processing, number of buffers should increase here
         * based on when parsing and processing threads are synchronized
         */
        ps_mem_rec->u4_mem_size +=
                        ihevcd_get_total_pic_buf_size(max_wd_luma * max_ht_luma, level,  PAD_WD,  PAD_HT,
                                                      init_num_bufs, init_extra_bufs, chroma_only);
    }
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_REF_PIC,
                    ps_mem_rec->u4_mem_size);

    /* Request memory to hold mem records to be returned during retrieve call */
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_BACKUP];
    ps_mem_rec->u4_mem_size = MEM_REC_CNT * sizeof(iv_mem_rec_t);
    DEBUG("\nMemory record Id %d = %d \n", MEM_REC_BACKUP,
                    ps_mem_rec->u4_mem_size);

    /* Each memtab size is aligned to next multiple of 128 bytes */
    /* This is to ensure all the memtabs start at different cache lines */
    ps_mem_rec = ps_mem_rec_base;
    for(i = 0; i < MEM_REC_CNT; i++)
    {
        ps_mem_rec->u4_mem_size = ALIGN128(ps_mem_rec->u4_mem_size);
        ps_mem_rec++;
    }
    ps_mem_q_op->s_ivd_fill_mem_rec_op_t.u4_num_mem_rec_filled = MEM_REC_CNT;
#ifdef APPLY_CONCEALMENT
    {
        IV_API_CALL_STATUS_T status;
        icncl_fill_mem_rec_ip_t cncl_fill_ip;
        icncl_fill_mem_rec_op_t cncl_fill_op;
        UWORD8 mem_loc = MEM_REC_CNT;

        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.e_cmd = IV_CMD_FILL_NUM_MEM_REC;
        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.pv_mem_rec_location = &(memTab[mem_loc]);
        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.u4_size = ps_mem_q_ip->s_ivd_fill_mem_rec_ip_t.u4_size;
        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.u4_max_frm_wd = max_wd_luma;
        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.u4_max_frm_ht = max_ht_luma;

        status = icncl_api_function(NULL, (void *)&cncl_fill_ip, (void *)&cncl_fill_op);

        if(IV_SUCCESS == status)
        {
            icncl_num_mem_rec_ip_t cncl_mem_ip;
            icncl_num_mem_rec_op_t cncl_mem_op;

            cncl_mem_ip.s_ivd_num_rec_ip_t.e_cmd = IV_CMD_GET_NUM_MEM_REC;
            cncl_mem_ip.s_ivd_num_rec_ip_t.u4_size = sizeof(icncl_num_mem_rec_ip_t);

            status = icncl_api_function(NULL, (void *)&cncl_mem_ip, (void *)&cncl_mem_op);
            if(IV_SUCCESS == status)
            {
                ps_mem_q_op->s_ivd_fill_mem_rec_op_t.u4_num_mem_rec_filled += cncl_mem_op.s_ivd_num_mem_rec_op_t.u4_num_mem_rec;
            }
        }

        return status;

    }
#endif //APPLY_CONCEALMENT
    DEBUG("Num mem recs in fill call : %d\n",
                    ps_mem_q_op->s_ivd_fill_mem_rec_op_t.u4_num_mem_rec_filled);


    return (status);
}


/**
*******************************************************************************
*
* @brief
*  Initializes from mem records passed to the codec
*
* @par Description:
*  Initializes pointers based on mem records passed
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_init_mem_rec(iv_obj_t *ps_codec_obj,
                           void *pv_api_ip,
                           void *pv_api_op)
{

    ihevcd_cxa_init_ip_t *dec_init_ip;
    ihevcd_cxa_init_op_t *dec_init_op;
    WORD32 i;
    iv_mem_rec_t *ps_mem_rec, *ps_mem_rec_base;
    WORD32 status = IV_SUCCESS;
    codec_t *ps_codec;
    WORD32 max_tile_cols, max_tile_rows;

    dec_init_ip = (ihevcd_cxa_init_ip_t *)pv_api_ip;
    dec_init_op = (ihevcd_cxa_init_op_t *)pv_api_op;

    ps_mem_rec_base = dec_init_ip->s_ivd_init_ip_t.pv_mem_rec_location;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_CODEC];
    ps_codec_obj->pv_codec_handle = ps_mem_rec->pv_base;

    ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    /* Note this memset can not be done in init() call, since init will called
    during reset as well. And calling this during reset will mean all pointers
    need to reinitialized*/
    memset(ps_codec, 0, sizeof(codec_t));

    if(dec_init_ip->s_ivd_init_ip_t.u4_size
                    > offsetof(ihevcd_cxa_init_ip_t, i4_level))
    {
        ps_codec->i4_init_level = dec_init_ip->i4_level;

        ps_codec->i4_init_level *= 3;
    }
    else
    {
        ps_codec->i4_init_level = MAX_LEVEL;
    }

    if(dec_init_ip->s_ivd_init_ip_t.u4_size
                    > offsetof(ihevcd_cxa_init_ip_t, u4_num_ref_frames))
    {
        ps_codec->i4_init_num_ref = dec_init_ip->u4_num_ref_frames;
    }
    else
    {
        ps_codec->i4_init_num_ref = MAX_REF_CNT;
    }

    if(dec_init_ip->s_ivd_init_ip_t.u4_size
                    > offsetof(ihevcd_cxa_init_ip_t, u4_num_reorder_frames))
    {
        ps_codec->i4_init_num_reorder = dec_init_ip->u4_num_reorder_frames;
    }
    else
    {
        ps_codec->i4_init_num_reorder = MAX_REF_CNT;
    }

    if(dec_init_ip->s_ivd_init_ip_t.u4_size
                    > offsetof(ihevcd_cxa_init_ip_t, u4_num_extra_disp_buf))
    {
        ps_codec->i4_init_num_extra_disp_buf =
                        dec_init_ip->u4_num_extra_disp_buf;
    }
    else
    {
        ps_codec->i4_init_num_extra_disp_buf = 0;
    }

    if(dec_init_ip->s_ivd_init_ip_t.u4_size
                    > offsetof(ihevcd_cxa_init_ip_t, u4_share_disp_buf))
    {
#ifndef LOGO_EN
        ps_codec->i4_share_disp_buf = dec_init_ip->u4_share_disp_buf;
#else
        ps_codec->i4_share_disp_buf = 0;
#endif
    }
    else
    {
        ps_codec->i4_share_disp_buf = 0;
    }
    /* Shared display mode is supported only for 420SP and 420P formats */
    if((dec_init_ip->s_ivd_init_ip_t.e_output_format != IV_YUV_420P) &&
       (dec_init_ip->s_ivd_init_ip_t.e_output_format != IV_YUV_420SP_UV) &&
       (dec_init_ip->s_ivd_init_ip_t.e_output_format != IV_YUV_420SP_VU))
    {
        ps_codec->i4_share_disp_buf = 0;
    }

    if((ps_codec->i4_init_level < MIN_LEVEL)
                    || (ps_codec->i4_init_level > MAX_LEVEL))
    {
        dec_init_op->s_ivd_init_op_t.u4_error_code |= IHEVCD_LEVEL_UNSUPPORTED;
        return (IV_FAIL);
    }

    if(ps_codec->i4_init_num_ref > MAX_REF_CNT)
    {
        dec_init_op->s_ivd_init_op_t.u4_error_code |=
                        IHEVCD_NUM_REF_UNSUPPORTED;
        ps_codec->i4_init_num_ref = MAX_REF_CNT;
    }

    if(ps_codec->i4_init_num_reorder > MAX_REF_CNT)
    {
        dec_init_op->s_ivd_init_op_t.u4_error_code |=
                        IHEVCD_NUM_REORDER_UNSUPPORTED;
        ps_codec->i4_init_num_reorder = MAX_REF_CNT;
    }

    if(ps_codec->i4_init_num_extra_disp_buf > MAX_REF_CNT)
    {
        dec_init_op->s_ivd_init_op_t.u4_error_code |=
                        IHEVCD_NUM_EXTRA_DISP_UNSUPPORTED;
        ps_codec->i4_init_num_extra_disp_buf = 0;
    }

    ps_codec->e_chroma_fmt = dec_init_ip->s_ivd_init_ip_t.e_output_format;

    ps_codec->i4_max_wd = dec_init_ip->s_ivd_init_ip_t.u4_frm_max_wd;
    ps_codec->i4_max_ht = dec_init_ip->s_ivd_init_ip_t.u4_frm_max_ht;

    ps_codec->i4_max_wd = ALIGN64(ps_codec->i4_max_wd);
    ps_codec->i4_max_ht = ALIGN64(ps_codec->i4_max_ht);

    ps_codec->i4_new_max_wd = ps_codec->i4_max_wd;
    ps_codec->i4_new_max_ht = ps_codec->i4_max_ht;

    max_tile_cols = (ps_codec->i4_max_wd + MIN_TILE_WD - 1) / MIN_TILE_WD;
    max_tile_rows = (ps_codec->i4_max_ht + MIN_TILE_HT - 1) / MIN_TILE_HT;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_BACKUP];
    ps_codec->ps_mem_rec_backup = (iv_mem_rec_t *)ps_mem_rec->pv_base;

    memcpy(ps_codec->ps_mem_rec_backup, ps_mem_rec_base,
           MEM_REC_CNT * sizeof(iv_mem_rec_t));

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_BITSBUF];
    ps_codec->pu1_bitsbuf = (UWORD8 *)ps_mem_rec->pv_base;
    ps_codec->u4_bitsbuf_size = ps_mem_rec->u4_mem_size;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TU_DATA];
    ps_codec->pv_tu_data = ps_mem_rec->pv_base;
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_MVBANK];
    ps_codec->pv_mv_buf_mgr = ps_mem_rec->pv_base;
    ps_codec->pv_mv_bank_buf_base = (UWORD8 *)ps_codec->pv_mv_buf_mgr + sizeof(buf_mgr_t);

    ps_codec->i4_total_mv_bank_size = ps_mem_rec->u4_mem_size - sizeof(buf_mgr_t);


    ps_mem_rec = &ps_mem_rec_base[MEM_REC_VPS];
    ps_codec->ps_vps_base = (vps_t *)ps_mem_rec->pv_base;
    ps_codec->s_parse.ps_vps_base = ps_codec->ps_vps_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SPS];
    ps_codec->ps_sps_base = (sps_t *)ps_mem_rec->pv_base;
    ps_codec->s_parse.ps_sps_base = ps_codec->ps_sps_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PPS];
    ps_codec->ps_pps_base = (pps_t *)ps_mem_rec->pv_base;
    ps_codec->s_parse.ps_pps_base = ps_codec->ps_pps_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SLICE_HDR];
    ps_codec->ps_slice_hdr_base = (slice_header_t *)ps_mem_rec->pv_base;
    ps_codec->s_parse.ps_slice_hdr_base = ps_codec->ps_slice_hdr_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TILE];
    ps_codec->ps_tile = (tile_t *)ps_mem_rec->pv_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_ENTRY_OFST];
    ps_codec->pi4_entry_ofst = (WORD32 *)ps_mem_rec->pv_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SCALING_MAT];
    ps_codec->pi2_scaling_mat = (WORD16 *)ps_mem_rec->pv_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_SKIP_FLAG];
    ps_codec->s_parse.pu4_skip_cu_top = (UWORD32 *)ps_mem_rec->pv_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_CT_DEPTH];
    ps_codec->s_parse.pu4_ct_depth_top = (UWORD32 *)ps_mem_rec->pv_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_INTRA_PRED_MODE];
    ps_codec->s_parse.pu1_luma_intra_pred_mode_left =
                    (UWORD8 *)ps_mem_rec->pv_base;
    ps_codec->s_parse.pu1_luma_intra_pred_mode_top  =
                    (UWORD8 *)ps_mem_rec->pv_base + 16;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_INTRA_FLAG];

    memset(ps_mem_rec->pv_base, 0, (ps_codec->i4_max_wd / MIN_CU_SIZE) * (ps_codec->i4_max_ht / MIN_CU_SIZE) / 8);

    ps_codec->pu1_pic_intra_flag = (UWORD8 *)ps_mem_rec->pv_base;
    ps_codec->s_parse.pu1_pic_intra_flag = ps_codec->pu1_pic_intra_flag;
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TRANSQUANT_BYPASS_FLAG];

    {
        WORD32 loop_filter_size = ((ps_codec->i4_max_wd  + 64) / MIN_CU_SIZE) * ((ps_codec->i4_max_ht + 64) / MIN_CU_SIZE) / 8;
        WORD32 loop_filter_strd = (ps_codec->i4_max_wd + 63) >> 6;

        memset(ps_mem_rec->pv_base, 0, loop_filter_size);

        /* The offset is added for easy processing of top and left blocks while loop filtering */
        ps_codec->pu1_pic_no_loop_filter_flag = (UWORD8 *)ps_mem_rec->pv_base + loop_filter_strd + 1;
        ps_codec->s_parse.pu1_pic_no_loop_filter_flag = ps_codec->pu1_pic_no_loop_filter_flag;
        ps_codec->s_parse.s_deblk_ctxt.pu1_pic_no_loop_filter_flag = ps_codec->pu1_pic_no_loop_filter_flag;
        ps_codec->s_parse.s_sao_ctxt.pu1_pic_no_loop_filter_flag = ps_codec->pu1_pic_no_loop_filter_flag;
    }

    /* Initialize pointers in PPS structures */
    {
        sps_t *ps_sps = ps_codec->ps_sps_base;
        pps_t *ps_pps = ps_codec->ps_pps_base;
        tile_t *ps_tile =  ps_codec->ps_tile;
        WORD16 *pi2_scaling_mat =  ps_codec->pi2_scaling_mat;
        WORD32 scaling_mat_size;

        SCALING_MAT_SIZE(scaling_mat_size);

        for(i = 0; i < MAX_SPS_CNT; i++)
        {
            ps_sps->pi2_scaling_mat  = pi2_scaling_mat;
            pi2_scaling_mat += scaling_mat_size;
            ps_sps++;
        }

        for(i = 0; i < MAX_PPS_CNT; i++)
        {
            ps_pps->ps_tile = ps_tile;
            ps_tile += (max_tile_cols * max_tile_rows);

            ps_pps->pi2_scaling_mat  = pi2_scaling_mat;
            pi2_scaling_mat += scaling_mat_size;
            ps_pps++;
        }

    }

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_THREAD_HANDLE];
    for(i = 0; i < MAX_PROCESS_THREADS; i++)
    {
        WORD32 handle_size = ithread_get_handle_size();
        ps_codec->apv_process_thread_handle[i] =
                        (UWORD8 *)ps_mem_rec->pv_base + (i * handle_size);
    }

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PROC_JOBQ];
    ps_codec->pv_proc_jobq_buf = ps_mem_rec->pv_base;
    ps_codec->i4_proc_jobq_buf_size = ps_mem_rec->u4_mem_size;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PARSE_MAP];
    ps_codec->pu1_parse_map = (UWORD8 *)ps_mem_rec->pv_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PROC_MAP];
    ps_codec->pu1_proc_map = (UWORD8 *)ps_mem_rec->pv_base;
    ps_mem_rec = &ps_mem_rec_base[MEM_REC_DISP_MGR];
    ps_codec->pv_disp_buf_mgr = ps_mem_rec->pv_base;

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_DPB_MGR];
    ps_codec->pv_dpb_mgr = ps_mem_rec->pv_base;


    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PIC_PU_IDX_NEIGHBOR];

    for(i = 0; i < MAX_PROCESS_THREADS; i++)
    {
        UWORD32 *pu4_buf = (UWORD32 *)ps_mem_rec->pv_base;
        ps_codec->as_process[i].pu4_pic_pu_idx_left = pu4_buf + i * (MAX_CTB_SIZE / 4);
        memset(ps_codec->as_process[i].pu4_pic_pu_idx_left, 0, sizeof(UWORD32) * MAX_CTB_SIZE / 4);
        ps_codec->as_process[i].pu4_pic_pu_idx_top = pu4_buf + MAX_PROCESS_THREADS * (MAX_CTB_SIZE / 4);
    }
    memset(ps_codec->as_process[0].pu4_pic_pu_idx_top, 0, sizeof(UWORD32) * (ps_codec->i4_max_wd / 4 + 1));


    ps_mem_rec = &ps_mem_rec_base[MEM_REC_PROC_SCRATCH];
    {
        UWORD8 *pu1_buf = (UWORD8 *)ps_mem_rec->pv_base;
        WORD32 pic_pu_idx_map_size;

        WORD32 inter_pred_tmp_buf_size, ntaps_luma;

        /* Max inter pred size */
        ntaps_luma = 8;
        inter_pred_tmp_buf_size = sizeof(WORD16) * (MAX_CTB_SIZE + ntaps_luma) * MAX_CTB_SIZE;

        inter_pred_tmp_buf_size = ALIGN64(inter_pred_tmp_buf_size);

        /* To hold pu_index w.r.t. frame level pu_t array for a CTB */
        pic_pu_idx_map_size = sizeof(WORD32) * (18 * 18);
        pic_pu_idx_map_size = ALIGN64(pic_pu_idx_map_size);
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].pi2_inter_pred_tmp_buf1 = (WORD16 *)pu1_buf;
            pu1_buf += inter_pred_tmp_buf_size;

            ps_codec->as_process[i].pi2_inter_pred_tmp_buf2 = (WORD16 *)pu1_buf;
            pu1_buf += inter_pred_tmp_buf_size;

            /* Inverse transform intermediate and inverse scan output buffers reuse inter pred scratch buffers */
            ps_codec->as_process[i].pi2_itrans_intrmd_buf =
                            ps_codec->as_process[i].pi2_inter_pred_tmp_buf2;
            ps_codec->as_process[i].pi2_invscan_out =
                            ps_codec->as_process[i].pi2_inter_pred_tmp_buf1;

            ps_codec->as_process[i].pu4_pic_pu_idx_map = (UWORD32 *)pu1_buf;
            ps_codec->as_process[i].s_bs_ctxt.pu4_pic_pu_idx_map =
                            (UWORD32 *)pu1_buf;
            pu1_buf += pic_pu_idx_map_size;

            //   ps_codec->as_process[i].pi2_inter_pred_tmp_buf3 = (WORD16 *)pu1_buf;
            //   pu1_buf += inter_pred_tmp_buf_size;

            ps_codec->as_process[i].i4_inter_pred_tmp_buf_strd = MAX_CTB_SIZE;

        }
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_left_luma = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_left_luma = (UWORD8 *)pu1_buf;
        pu1_buf += MAX(ps_codec->i4_max_ht, ps_codec->i4_max_wd);

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_left_chroma = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_left_chroma = (UWORD8 *)pu1_buf;
        pu1_buf += MAX(ps_codec->i4_max_ht, ps_codec->i4_max_wd);
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_luma = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_luma = (UWORD8 *)pu1_buf;
        pu1_buf += ps_codec->i4_max_wd;

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_chroma = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_chroma = (UWORD8 *)pu1_buf;
        pu1_buf += ps_codec->i4_max_wd;
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_luma_top_left_ctb = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_luma_top_left_ctb = (UWORD8 *)pu1_buf;
        pu1_buf += ps_codec->i4_max_ht / MIN_CTB_SIZE;

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_chroma_top_left_ctb = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_chroma_top_left_ctb = (UWORD8 *)pu1_buf;
        pu1_buf += (ps_codec->i4_max_ht / MIN_CTB_SIZE) * 2;

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_left_luma_curr_ctb = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_left_luma_curr_ctb = (UWORD8 *)pu1_buf;
        pu1_buf += ps_codec->i4_max_ht / MIN_CTB_SIZE;

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_left_chroma_curr_ctb = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_left_chroma_curr_ctb = (UWORD8 *)pu1_buf;

        pu1_buf += (ps_codec->i4_max_ht / MIN_CTB_SIZE) * 2;
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_left_luma_top_right = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_left_luma_top_right = (UWORD8 *)pu1_buf;

        pu1_buf += ps_codec->i4_max_wd / MIN_CTB_SIZE;
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_left_chroma_top_right = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_left_chroma_top_right = (UWORD8 *)pu1_buf;

        pu1_buf += (ps_codec->i4_max_wd / MIN_CTB_SIZE) * 2;

        /*Per CTB, Store 1 value for luma , 2 values for chroma*/
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_left_luma_bot_left = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_left_luma_bot_left = (UWORD8 *)pu1_buf;

        pu1_buf += (ps_codec->i4_max_ht / MIN_CTB_SIZE);

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_sao_src_top_left_chroma_bot_left = (UWORD8 *)pu1_buf;
        }
        ps_codec->s_parse.s_sao_ctxt.pu1_sao_src_top_left_chroma_bot_left = (UWORD8 *)pu1_buf;

        pu1_buf += (ps_codec->i4_max_ht / MIN_CTB_SIZE) * 2;
    }

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SAO_SCRATCH];
    {
        UWORD8 *pu1_buf = (UWORD8 *)ps_mem_rec->pv_base;
        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_sao_ctxt.pu1_tmp_buf_luma = (UWORD8 *)pu1_buf;
            pu1_buf += 4 * MAX_CTB_SIZE * MAX_CTB_SIZE * sizeof(UWORD8);

            ps_codec->as_process[i].s_sao_ctxt.pu1_tmp_buf_chroma = (UWORD8 *)pu1_buf;
            pu1_buf += 4 * MAX_CTB_SIZE * MAX_CTB_SIZE * sizeof(UWORD8);
        }
    }

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_BS_QP];
    {
        UWORD8 *pu1_buf = (UWORD8 *)ps_mem_rec->pv_base;
        WORD32 vert_bs_size, horz_bs_size;
        WORD32 qp_const_flag_size;
        WORD32 qp_size;
        WORD32 num_8x8;

        /* Max Number of vertical edges */
        vert_bs_size = ps_codec->i4_max_wd / 8 + 2 * MAX_CTB_SIZE / 8;

        /* Max Number of horizontal edges - extra MAX_CTB_SIZE / 8 to handle the last 4 rows separately(shifted CTB processing) */
        vert_bs_size *= (ps_codec->i4_max_ht + MAX_CTB_SIZE) / MIN_TU_SIZE;

        /* Number of bytes */
        vert_bs_size /= 8;

        /* Two bits per edge */
        vert_bs_size *= 2;

        /* Max Number of horizontal edges */
        horz_bs_size = ps_codec->i4_max_ht / 8 + MAX_CTB_SIZE / 8;

        /* Max Number of vertical edges - extra MAX_CTB_SIZE / 8 to handle the last 4 columns separately(shifted CTB processing) */
        horz_bs_size *= (ps_codec->i4_max_wd + MAX_CTB_SIZE) / MIN_TU_SIZE;

        /* Number of bytes */
        horz_bs_size /= 8;

        /* Two bits per edge */
        horz_bs_size *= 2;

        /* Max CTBs in a row */
        qp_const_flag_size = ps_codec->i4_max_wd / MIN_CTB_SIZE + 1 /* The last ctb row deblk is done in last ctb + 1 row.*/;

        /* Max CTBs in a column */
        qp_const_flag_size *= ps_codec->i4_max_ht / MIN_CTB_SIZE;

        /* Number of bytes */
        qp_const_flag_size /= 8;

        /* QP changes at CU level - So store at 8x8 level */
        num_8x8 = (ps_codec->i4_max_ht * ps_codec->i4_max_wd) / (MIN_CU_SIZE * MIN_CU_SIZE);
        qp_size = num_8x8;
        memset(pu1_buf, 0, vert_bs_size + horz_bs_size + qp_size + qp_const_flag_size);

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].s_bs_ctxt.pu4_pic_vert_bs = (UWORD32 *)pu1_buf;
            ps_codec->as_process[i].s_deblk_ctxt.s_bs_ctxt.pu4_pic_vert_bs = (UWORD32 *)pu1_buf;
            ps_codec->s_parse.s_deblk_ctxt.s_bs_ctxt.pu4_pic_vert_bs = (UWORD32 *)pu1_buf;
            pu1_buf += vert_bs_size;

            ps_codec->as_process[i].s_bs_ctxt.pu4_pic_horz_bs = (UWORD32 *)pu1_buf;
            ps_codec->as_process[i].s_deblk_ctxt.s_bs_ctxt.pu4_pic_horz_bs = (UWORD32 *)pu1_buf;
            ps_codec->s_parse.s_deblk_ctxt.s_bs_ctxt.pu4_pic_horz_bs = (UWORD32 *)pu1_buf;
            pu1_buf += horz_bs_size;

            ps_codec->as_process[i].s_bs_ctxt.pu1_pic_qp = (UWORD8 *)pu1_buf;
            ps_codec->as_process[i].s_deblk_ctxt.s_bs_ctxt.pu1_pic_qp = (UWORD8 *)pu1_buf;
            ps_codec->s_parse.s_deblk_ctxt.s_bs_ctxt.pu1_pic_qp = (UWORD8 *)pu1_buf;
            pu1_buf += qp_size;

            ps_codec->as_process[i].s_bs_ctxt.pu1_pic_qp_const_in_ctb = (UWORD8 *)pu1_buf;
            ps_codec->as_process[i].s_deblk_ctxt.s_bs_ctxt.pu1_pic_qp_const_in_ctb = (UWORD8 *)pu1_buf;
            ps_codec->s_parse.s_deblk_ctxt.s_bs_ctxt.pu1_pic_qp_const_in_ctb = (UWORD8 *)pu1_buf;
            pu1_buf += qp_const_flag_size;

            pu1_buf -= (vert_bs_size + horz_bs_size + qp_size + qp_const_flag_size);
        }
        ps_codec->s_parse.s_bs_ctxt.pu4_pic_vert_bs = (UWORD32 *)pu1_buf;
        pu1_buf += vert_bs_size;

        ps_codec->s_parse.s_bs_ctxt.pu4_pic_horz_bs = (UWORD32 *)pu1_buf;
        pu1_buf += horz_bs_size;

        ps_codec->s_parse.s_bs_ctxt.pu1_pic_qp = (UWORD8 *)pu1_buf;
        pu1_buf += qp_size;

        ps_codec->s_parse.s_bs_ctxt.pu1_pic_qp_const_in_ctb = (UWORD8 *)pu1_buf;
        pu1_buf += qp_const_flag_size;

    }

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_TILE_IDX];
    {
        UWORD8 *pu1_buf = (UWORD8 *)ps_mem_rec->pv_base;

        for(i = 0; i < MAX_PROCESS_THREADS; i++)
        {
            ps_codec->as_process[i].pu1_tile_idx = (UWORD16 *)pu1_buf + ps_codec->i4_max_wd / MIN_CTB_SIZE /* Offset 1 row */;
        }
    }

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_SAO];
    ps_codec->s_parse.ps_pic_sao = (sao_t *)ps_mem_rec->pv_base;
    ps_codec->s_parse.s_sao_ctxt.ps_pic_sao = (sao_t *)ps_mem_rec->pv_base;
    for(i = 0; i < MAX_PROCESS_THREADS; i++)
    {
        ps_codec->as_process[i].s_sao_ctxt.ps_pic_sao = ps_codec->s_parse.ps_pic_sao;
    }

    ps_mem_rec = &ps_mem_rec_base[MEM_REC_REF_PIC];
    ps_codec->pv_pic_buf_mgr = ps_mem_rec->pv_base;
    ps_codec->pv_pic_buf_base = (UWORD8 *)ps_codec->pv_pic_buf_mgr + sizeof(buf_mgr_t);
    ps_codec->i4_total_pic_buf_size = ps_mem_rec->u4_mem_size - sizeof(buf_mgr_t);
    ps_codec->pu1_cur_chroma_ref_buf = (UWORD8 *)ps_codec->pv_pic_buf_base + BUF_MGR_MAX_CNT * sizeof(pic_buf_t);
    ps_codec->i4_remaining_pic_buf_size = ps_codec->i4_total_pic_buf_size - BUF_MGR_MAX_CNT * sizeof(pic_buf_t);




#ifdef APPLY_CONCEALMENT
    {

        UWORD32 mem_loc;

        icncl_init_ip_t cncl_init_ip;
        icncl_init_op_t cncl_init_op;
        iv_mem_rec_t *ps_mem_rec;
        DecStruct *ps_codec;

        ps_mem_rec = dec_init_ip->s_ivd_init_ip_t.pv_mem_rec_location;
        mem_loc = MEM_REC_CNT;

        ps_codec->ps_conceal = (iv_obj_t *)ps_mem_rec[mem_loc].pv_base;
        ps_codec->i4_first_frame_done = 0;

        cncl_init_ip.u4_size = sizeof(icncl_init_ip_t);
        cncl_init_ip.pv_mem_rec_location = &(ps_mem_rec[mem_loc]);
        cncl_init_ip.e_cmd = IV_CMD_INIT;

        status = icncl_api_function(ps_codec->ps_conceal, (void *)&cncl_init_ip, (void *)&cncl_init_op);

    }
#endif //APPLY_CONCEALMENT

    status = ihevcd_init(ps_codec);

    TRACE_INIT(NULL);
    STATS_INIT();
    return status;
}
/**
*******************************************************************************
*
* @brief
*  Retrieves mem records passed to the codec
*
* @par Description:
*  Retrieves memrecs passed earlier
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_retrieve_memrec(iv_obj_t *ps_codec_obj,
                              void *pv_api_ip,
                              void *pv_api_op)
{

    iv_retrieve_mem_rec_ip_t *dec_clr_ip;
    iv_retrieve_mem_rec_op_t *dec_clr_op;
    codec_t *ps_codec;
    dec_clr_ip = (iv_retrieve_mem_rec_ip_t *)pv_api_ip;
    dec_clr_op = (iv_retrieve_mem_rec_op_t *)pv_api_op;
    ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    if(ps_codec->i4_init_done != 1)
    {
        dec_clr_op->u4_error_code |= 1 << IVD_FATALERROR;
        dec_clr_op->u4_error_code |= IHEVCD_INIT_NOT_DONE;
        return IV_FAIL;
    }

    memcpy(dec_clr_ip->pv_mem_rec_location, ps_codec->ps_mem_rec_backup,
           MEM_REC_CNT * (sizeof(iv_mem_rec_t)));
    dec_clr_op->u4_num_mem_rec_filled = MEM_REC_CNT;

#ifdef APPLY_CONCEALMENT
    {
        IV_API_CALL_STATUS_T status;
        icncl_fill_mem_rec_ip_t cncl_fill_ip;
        icncl_fill_mem_rec_op_t cncl_fill_op;

        iv_mem_rec_t *ps_mem_rec;

        UWORD8 mem_loc = MEM_REC_CNT;
        UWORD8 num_cncl_mem = 0;

        ps_mem_rec = dec_clr_ip->pv_mem_rec_location;

        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.e_cmd = IV_CMD_FILL_NUM_MEM_REC;
        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.pv_mem_rec_location = &(ps_mem_rec[mem_loc]);
        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.u4_size = sizeof(icncl_fill_mem_rec_ip_t);

        status = icncl_api_function(NULL, (void *)&cncl_fill_ip, (void *)&cncl_fill_op);

        cncl_fill_ip.s_ivd_fill_mem_rec_ip_t.e_cmd = IV_CMD_RETRIEVE_MEMREC;
        cncl_fill_op.s_ivd_fill_mem_rec_op_t.u4_size = sizeof(icncl_fill_mem_rec_op_t);

        status = icncl_api_function(ps_codec->ps_conceal, (void *)&cncl_fill_ip, (void *)&cncl_fill_op);

        if(status == IV_SUCCESS)
        {
            /* Add the concealment library's memory requirements */
            dec_clr_op->u4_num_mem_rec_filled += cncl_fill_op.s_ivd_fill_mem_rec_op_t.u4_num_mem_rec_filled;
        }
    }
#endif //APPLY_CONCEALMENT
    DEBUG("Retrieve num mem recs: %d\n",
                    dec_clr_op->u4_num_mem_rec_filled);
    STATS_PRINT();
    ihevcd_jobq_free((jobq_t *)ps_codec->pv_proc_jobq);



    return IV_SUCCESS;

}
/**
*******************************************************************************
*
* @brief
*  Passes display buffer from application to codec
*
* @par Description:
*  Adds display buffer to the codec
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_set_display_frame(iv_obj_t *ps_codec_obj,
                                void *pv_api_ip,
                                void *pv_api_op)
{
    WORD32 ret = IV_SUCCESS;

    ivd_set_display_frame_ip_t *ps_dec_disp_ip;
    ivd_set_display_frame_op_t *ps_dec_disp_op;

    WORD32 i;

    codec_t *ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    ps_dec_disp_ip = (ivd_set_display_frame_ip_t *)pv_api_ip;
    ps_dec_disp_op = (ivd_set_display_frame_op_t *)pv_api_op;

    ps_codec->i4_num_disp_bufs = 0;
    if(ps_codec->i4_share_disp_buf)
    {
        UWORD32 num_bufs = ps_dec_disp_ip->num_disp_bufs;
        pic_buf_t *ps_pic_buf;
        UWORD8 *pu1_buf;
        WORD32 buf_ret;
        WORD32 strd;
        strd = ps_codec->i4_strd;
        if(0 == strd)
            strd = ps_codec->i4_max_wd + PAD_WD;
        num_bufs = MIN(num_bufs, BUF_MGR_MAX_CNT);
        ps_codec->i4_num_disp_bufs = num_bufs;

        ps_pic_buf = (pic_buf_t *)ps_codec->ps_pic_buf;
        for(i = 0; i < (WORD32)num_bufs; i++)
        {
            pu1_buf =  ps_dec_disp_ip->s_disp_buffer[i].pu1_bufs[0];
            ps_pic_buf->pu1_luma = pu1_buf + strd * PAD_TOP + PAD_LEFT;

            if(ps_codec->e_chroma_fmt == IV_YUV_420P)
            {
                pu1_buf =  ps_codec->pu1_cur_chroma_ref_buf;
                ps_codec->pu1_cur_chroma_ref_buf += strd * (ps_codec->i4_ht / 2 + PAD_HT / 2);
                ps_codec->i4_remaining_pic_buf_size -= strd * (ps_codec->i4_ht / 2 + PAD_HT / 2);

                if(0 > ps_codec->i4_remaining_pic_buf_size)
                {
                    ps_codec->i4_error_code = IHEVCD_BUF_MGR_ERROR;
                    return IHEVCD_BUF_MGR_ERROR;
                }

            }
            else
            {
                /* For YUV 420SP case use display buffer itself as chroma ref buffer */
                pu1_buf =  ps_dec_disp_ip->s_disp_buffer[i].pu1_bufs[1];
            }

            ps_pic_buf->pu1_chroma = pu1_buf + strd * (PAD_TOP / 2) + PAD_LEFT;

            buf_ret = ihevc_buf_mgr_add((buf_mgr_t *)ps_codec->pv_pic_buf_mgr, ps_pic_buf, i);

            if(0 != buf_ret)
            {
                ps_codec->i4_error_code = IHEVCD_BUF_MGR_ERROR;
                return IHEVCD_BUF_MGR_ERROR;
            }

            /* Mark pic buf as needed for display */
            /* This ensures that till the buffer is explicitly passed to the codec,
             * application owns the buffer. Decoder is allowed to use a buffer only
             * when application sends it through fill this buffer call in OMX
             */
            ihevc_buf_mgr_set_status((buf_mgr_t *)ps_codec->pv_pic_buf_mgr, i, BUF_MGR_DISP);

            ps_pic_buf++;

            /* Store display buffers in codec context. Needed for 420p output */
            memcpy(&ps_codec->s_disp_buffer[ps_codec->i4_share_disp_buf_cnt],
                   &ps_dec_disp_ip->s_disp_buffer[i],
                   sizeof(ps_dec_disp_ip->s_disp_buffer[i]));

            ps_codec->i4_share_disp_buf_cnt++;

        }
    }

    ps_dec_disp_op->u4_error_code = 0;
    return ret;

}

/**
*******************************************************************************
*
* @brief
*  Sets the decoder in flush mode. Decoder will come out of  flush only
* after returning all the buffers or at reset
*
* @par Description:
*  Sets the decoder in flush mode
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_set_flush_mode(iv_obj_t *ps_codec_obj,
                             void *pv_api_ip,
                             void *pv_api_op)
{

    codec_t *ps_codec;
    ivd_ctl_flush_op_t *ps_ctl_op = (ivd_ctl_flush_op_t *)pv_api_op;
    UNUSED(pv_api_ip);
    ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    /* Signal flush frame control call */
    ps_codec->i4_flush_mode = 1;

    ps_ctl_op->u4_error_code = 0;

    /* Set pic count to zero, so that decoder starts buffering again */
    /* once it comes out of flush mode */
    ps_codec->u4_pic_cnt = 0;
    ps_codec->u4_disp_cnt = 0;
    return IV_SUCCESS;


}

/**
*******************************************************************************
*
* @brief
*  Gets decoder status and buffer requirements
*
* @par Description:
*  Gets the decoder status
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/

WORD32 ihevcd_get_status(iv_obj_t *ps_codec_obj,
                         void *pv_api_ip,
                         void *pv_api_op)
{

    WORD32 i;
    codec_t *ps_codec;
    WORD32 wd, ht;
    ivd_ctl_getstatus_op_t *ps_ctl_op = (ivd_ctl_getstatus_op_t *)pv_api_op;

    UNUSED(pv_api_ip);

    ps_ctl_op->u4_error_code = 0;

    ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    ps_ctl_op->u4_min_num_in_bufs = MIN_IN_BUFS;
    if(ps_codec->e_chroma_fmt == IV_YUV_420P)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_420;
    else if(ps_codec->e_chroma_fmt == IV_YUV_422ILE)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_422ILE;
    else if(ps_codec->e_chroma_fmt == IV_RGB_565)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_RGB565;
    else if(ps_codec->e_chroma_fmt == IV_RGBA_8888)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_RGBA8888;
    else if((ps_codec->e_chroma_fmt == IV_YUV_420SP_UV)
                    || (ps_codec->e_chroma_fmt == IV_YUV_420SP_VU))
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_420SP;

    ps_ctl_op->u4_num_disp_bufs = 1;

    for(i = 0; i < (WORD32)ps_ctl_op->u4_min_num_in_bufs; i++)
    {
        ps_ctl_op->u4_min_in_buf_size[i] = MAX((ps_codec->i4_wd * ps_codec->i4_ht), MIN_BITSBUF_SIZE);
    }

    wd = ps_codec->i4_wd;
    ht = ps_codec->i4_ht;

    if(ps_codec->i4_sps_done)
    {
        if(0 == ps_codec->i4_share_disp_buf)
        {
            wd = ps_codec->i4_disp_wd;
            ht = ps_codec->i4_disp_ht;

        }
        else
        {
            wd = ps_codec->i4_disp_strd;
            ht = ps_codec->i4_ht + PAD_HT;
        }
    }
    else
    {
        if(0 == ps_codec->i4_share_disp_buf)
        {
            wd = ps_codec->i4_new_max_wd;
            ht = ps_codec->i4_new_max_ht;
        }
        else
        {
            wd = ALIGN32(wd + PAD_WD);
            ht += PAD_HT;
        }
    }

    if(ps_codec->i4_disp_strd > wd)
        wd = ps_codec->i4_disp_strd;

    if(0 == ps_codec->i4_share_disp_buf)
        ps_ctl_op->u4_num_disp_bufs = 1;
    else
    {
        WORD32 pic_size;
        WORD32 max_dpb_size;

        if(ps_codec->i4_sps_done)
        {
            sps_t *ps_sps = (ps_codec->s_parse.ps_sps_base + ps_codec->i4_sps_id);
            WORD32 reorder_pic_cnt;
            WORD32 ref_pic_cnt;
            WORD32 level;

            reorder_pic_cnt = MIN(ps_sps->ai1_sps_max_num_reorder_pics[0], ps_codec->i4_init_num_reorder);
            pic_size = ps_sps->i2_pic_width_in_luma_samples * ps_sps->i2_pic_height_in_luma_samples;

            level = ps_codec->i4_init_level;
            max_dpb_size = ihevcd_get_dpb_size(level, pic_size);
            ref_pic_cnt = max_dpb_size;
            ps_ctl_op->u4_num_disp_bufs = reorder_pic_cnt;

            ps_ctl_op->u4_num_disp_bufs += ref_pic_cnt + 1;

        }
        else
        {
            pic_size = ps_codec->i4_max_wd * ps_codec->i4_max_ht;
            max_dpb_size = ihevcd_get_dpb_size(ps_codec->i4_init_level, pic_size);
            ps_ctl_op->u4_num_disp_bufs = 2 * max_dpb_size;

            ps_ctl_op->u4_num_disp_bufs = MIN(ps_ctl_op->u4_num_disp_bufs,
                            (UWORD32)(ps_codec->i4_init_num_ref + ps_codec->i4_init_num_reorder + 1));

        }

        ps_ctl_op->u4_num_disp_bufs = MIN(
                        ps_ctl_op->u4_num_disp_bufs, 32);
    }

    /*!*/
    if(ps_codec->e_chroma_fmt == IV_YUV_420P)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht);
        ps_ctl_op->u4_min_out_buf_size[1] = (wd * ht) >> 2;
        ps_ctl_op->u4_min_out_buf_size[2] = (wd * ht) >> 2;
    }
    else if(ps_codec->e_chroma_fmt == IV_YUV_422ILE)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht) * 2;
        ps_ctl_op->u4_min_out_buf_size[1] =
                        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    else if(ps_codec->e_chroma_fmt == IV_RGB_565)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht) * 2;
        ps_ctl_op->u4_min_out_buf_size[1] =
                        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    else if(ps_codec->e_chroma_fmt == IV_RGBA_8888)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht) * 4;
        ps_ctl_op->u4_min_out_buf_size[1] =
                        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    else if((ps_codec->e_chroma_fmt == IV_YUV_420SP_UV)
                    || (ps_codec->e_chroma_fmt == IV_YUV_420SP_VU))
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht);
        ps_ctl_op->u4_min_out_buf_size[1] = (wd * ht) >> 1;
        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    ps_ctl_op->u4_pic_ht = ht;
    ps_ctl_op->u4_pic_wd = wd;
    ps_ctl_op->u4_frame_rate = 30000;
    ps_ctl_op->u4_bit_rate = 1000000;
    ps_ctl_op->e_content_type = IV_PROGRESSIVE;
    ps_ctl_op->e_output_chroma_format = ps_codec->e_chroma_fmt;
    ps_codec->i4_num_disp_bufs = ps_ctl_op->u4_num_disp_bufs;

    if(ps_ctl_op->u4_size == sizeof(ihevcd_cxa_ctl_getstatus_op_t))
    {
        ihevcd_cxa_ctl_getstatus_op_t *ps_ext_ctl_op = (ihevcd_cxa_ctl_getstatus_op_t *)ps_ctl_op;
        ps_ext_ctl_op->u4_coded_pic_wd = ps_codec->i4_wd;
        ps_ext_ctl_op->u4_coded_pic_wd = ps_codec->i4_ht;
    }
    return IV_SUCCESS;
}
/**
*******************************************************************************
*
* @brief
*  Gets decoder buffer requirements
*
* @par Description:
*  Gets the decoder buffer requirements. If called before  header decoder,
* buffer requirements are based on max_wd  and max_ht else actual width and
* height will be used
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_get_buf_info(iv_obj_t *ps_codec_obj,
                           void *pv_api_ip,
                           void *pv_api_op)
{

    codec_t *ps_codec;
    UWORD32 i = 0;
    WORD32 wd, ht;
    ivd_ctl_getbufinfo_op_t *ps_ctl_op =
                    (ivd_ctl_getbufinfo_op_t *)pv_api_op;

    UNUSED(pv_api_ip);
    ps_ctl_op->u4_error_code = 0;

    ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    ps_ctl_op->u4_min_num_in_bufs = MIN_IN_BUFS;
    if(ps_codec->e_chroma_fmt == IV_YUV_420P)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_420;
    else if(ps_codec->e_chroma_fmt == IV_YUV_422ILE)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_422ILE;
    else if(ps_codec->e_chroma_fmt == IV_RGB_565)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_RGB565;
    else if(ps_codec->e_chroma_fmt == IV_RGBA_8888)
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_RGBA8888;
    else if((ps_codec->e_chroma_fmt == IV_YUV_420SP_UV)
                    || (ps_codec->e_chroma_fmt == IV_YUV_420SP_VU))
        ps_ctl_op->u4_min_num_out_bufs = MIN_OUT_BUFS_420SP;

    ps_ctl_op->u4_num_disp_bufs = 1;

    for(i = 0; i < ps_ctl_op->u4_min_num_in_bufs; i++)
    {
        ps_ctl_op->u4_min_in_buf_size[i] = MAX((ps_codec->i4_wd * ps_codec->i4_ht), MIN_BITSBUF_SIZE);
    }

    wd = ps_codec->i4_max_wd;
    ht = ps_codec->i4_max_ht;

    if(ps_codec->i4_sps_done)
    {
        if(0 == ps_codec->i4_share_disp_buf)
        {
            wd = ps_codec->i4_disp_wd;
            ht = ps_codec->i4_disp_ht;

        }
        else
        {
            wd = ps_codec->i4_disp_strd;
            ht = ps_codec->i4_ht + PAD_HT;
        }
    }
    else
    {
        if(1 == ps_codec->i4_share_disp_buf)
        {
            wd = ALIGN32(wd + PAD_WD);
            ht += PAD_HT;
        }
    }

    if(ps_codec->i4_disp_strd > wd)
        wd = ps_codec->i4_disp_strd;

    if(0 == ps_codec->i4_share_disp_buf)
        ps_ctl_op->u4_num_disp_bufs = 1;
    else
    {
        WORD32 pic_size;
        WORD32 max_dpb_size;

        if(ps_codec->i4_sps_done)
        {
            sps_t *ps_sps = (ps_codec->s_parse.ps_sps_base + ps_codec->i4_sps_id);
            WORD32 reorder_pic_cnt;
            WORD32 ref_pic_cnt;
            WORD32 level;

            reorder_pic_cnt = MIN(ps_sps->ai1_sps_max_num_reorder_pics[0], ps_codec->i4_init_num_reorder);
            pic_size = ps_sps->i2_pic_width_in_luma_samples * ps_sps->i2_pic_height_in_luma_samples;

            level = ps_codec->i4_init_level;
            max_dpb_size = ihevcd_get_dpb_size(level, pic_size);
            ref_pic_cnt = max_dpb_size;
            ps_ctl_op->u4_num_disp_bufs = reorder_pic_cnt;

            ps_ctl_op->u4_num_disp_bufs += ref_pic_cnt + 1;

        }
        else
        {
            pic_size = ps_codec->i4_max_wd * ps_codec->i4_max_ht;
            max_dpb_size = ihevcd_get_dpb_size(ps_codec->i4_init_level, pic_size);
            ps_ctl_op->u4_num_disp_bufs = 2 * max_dpb_size;

            ps_ctl_op->u4_num_disp_bufs = MIN(ps_ctl_op->u4_num_disp_bufs,
                            (UWORD32)(ps_codec->i4_init_num_ref + ps_codec->i4_init_num_reorder + 1));

        }

        ps_ctl_op->u4_num_disp_bufs = MIN(
                        ps_ctl_op->u4_num_disp_bufs, 32);

    }

    /*!*/
    if(ps_codec->e_chroma_fmt == IV_YUV_420P)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht);
        ps_ctl_op->u4_min_out_buf_size[1] = (wd * ht) >> 2;
        ps_ctl_op->u4_min_out_buf_size[2] = (wd * ht) >> 2;
    }
    else if(ps_codec->e_chroma_fmt == IV_YUV_422ILE)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht) * 2;
        ps_ctl_op->u4_min_out_buf_size[1] =
                        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    else if(ps_codec->e_chroma_fmt == IV_RGB_565)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht) * 2;
        ps_ctl_op->u4_min_out_buf_size[1] =
                        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    else if(ps_codec->e_chroma_fmt == IV_RGBA_8888)
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht) * 4;
        ps_ctl_op->u4_min_out_buf_size[1] =
                        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    else if((ps_codec->e_chroma_fmt == IV_YUV_420SP_UV)
                    || (ps_codec->e_chroma_fmt == IV_YUV_420SP_VU))
    {
        ps_ctl_op->u4_min_out_buf_size[0] = (wd * ht);
        ps_ctl_op->u4_min_out_buf_size[1] = (wd * ht) >> 1;
        ps_ctl_op->u4_min_out_buf_size[2] = 0;
    }
    ps_codec->i4_num_disp_bufs = ps_ctl_op->u4_num_disp_bufs;

    return IV_SUCCESS;
}


/**
*******************************************************************************
*
* @brief
*  Sets dynamic parameters
*
* @par Description:
*  Sets dynamic parameters. Note Frame skip, decode header  mode are dynamic
*  Dynamic change in stride is not  supported
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_set_params(iv_obj_t *ps_codec_obj,
                         void *pv_api_ip,
                         void *pv_api_op)
{

    codec_t *ps_codec;
    WORD32 ret = IV_SUCCESS;
    WORD32 strd;
    ivd_ctl_set_config_ip_t *s_ctl_dynparams_ip =
                    (ivd_ctl_set_config_ip_t *)pv_api_ip;
    ivd_ctl_set_config_op_t *s_ctl_dynparams_op =
                    (ivd_ctl_set_config_op_t *)pv_api_op;

    ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    s_ctl_dynparams_op->u4_error_code = 0;

    ps_codec->e_pic_skip_mode = s_ctl_dynparams_ip->e_frm_skip_mode;

    if(s_ctl_dynparams_ip->e_frm_skip_mode != IVD_SKIP_NONE)
    {

        if((s_ctl_dynparams_ip->e_frm_skip_mode != IVD_SKIP_P) &&
           (s_ctl_dynparams_ip->e_frm_skip_mode != IVD_SKIP_B) &&
           (s_ctl_dynparams_ip->e_frm_skip_mode != IVD_SKIP_PB))
        {
            s_ctl_dynparams_op->u4_error_code = (1 << IVD_UNSUPPORTEDPARAM);
            ret = IV_FAIL;
        }
    }

    strd = ps_codec->i4_disp_strd;
    if(1 == ps_codec->i4_share_disp_buf)
    {
        strd = ps_codec->i4_strd;
    }


    if((-1 != (WORD32)s_ctl_dynparams_ip->u4_disp_wd) &&
                    (0  != s_ctl_dynparams_ip->u4_disp_wd) &&
                    (0  != strd) &&
                    ((WORD32)s_ctl_dynparams_ip->u4_disp_wd < strd))
    {
        s_ctl_dynparams_op->u4_error_code |= (1 << IVD_UNSUPPORTEDPARAM);
        s_ctl_dynparams_op->u4_error_code |= IHEVCD_INVALID_DISP_STRD;
        ret = IV_FAIL;
    }
    else
    {
        if((WORD32)s_ctl_dynparams_ip->u4_disp_wd >= ps_codec->i4_wd)
        {
            strd = s_ctl_dynparams_ip->u4_disp_wd;
        }
        else if(0 == ps_codec->i4_sps_done)
        {
            strd = s_ctl_dynparams_ip->u4_disp_wd;
        }
        else if(s_ctl_dynparams_ip->u4_disp_wd == 0)
        {
            strd = ps_codec->i4_disp_strd;
        }
        else
        {
            strd = 0;
            s_ctl_dynparams_op->u4_error_code |= (1 << IVD_UNSUPPORTEDPARAM);
            s_ctl_dynparams_op->u4_error_code |= IHEVCD_INVALID_DISP_STRD;
            ret = IV_FAIL;
        }
    }

    ps_codec->i4_disp_strd = strd;
    if(1 == ps_codec->i4_share_disp_buf)
    {
        ps_codec->i4_strd = strd;
    }

    if(s_ctl_dynparams_ip->e_vid_dec_mode == IVD_DECODE_FRAME)
        ps_codec->i4_header_mode = 0;
    else if(s_ctl_dynparams_ip->e_vid_dec_mode == IVD_DECODE_HEADER)
        ps_codec->i4_header_mode = 1;
    else
    {

        s_ctl_dynparams_op->u4_error_code = (1 << IVD_UNSUPPORTEDPARAM);
        ps_codec->i4_header_mode = 1;
        ret = IV_FAIL;
    }


    return ret;

}
/**
*******************************************************************************
*
* @brief
*  Resets the decoder state
*
* @par Description:
*  Resets the decoder state by calling ihevcd_init()
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_reset(iv_obj_t *ps_codec_obj, void *pv_api_ip, void *pv_api_op)
{
    codec_t *ps_codec;
    ivd_ctl_reset_op_t *s_ctl_reset_op = (ivd_ctl_reset_op_t *)pv_api_op;
    UNUSED(pv_api_ip);
    ps_codec = (codec_t *)(ps_codec_obj->pv_codec_handle);

    if(ps_codec != NULL)
    {
        DEBUG("\nReset called \n");
        ihevcd_init(ps_codec);
    }
    else
    {
        DEBUG("\nReset called without Initializing the decoder\n");
        s_ctl_reset_op->u4_error_code = IHEVCD_INIT_NOT_DONE;
    }

    return IV_SUCCESS;
}

/**
*******************************************************************************
*
* @brief
*  Releases display buffer from application to codec  to signal to the codec
* that it can write to this buffer  if required. Till release is called,
* codec can not write  to this buffer
*
* @par Description:
*  Marks the buffer as display done
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/

WORD32 ihevcd_rel_display_frame(iv_obj_t *ps_codec_obj,
                                void *pv_api_ip,
                                void *pv_api_op)
{

    ivd_rel_display_frame_ip_t *ps_dec_rel_disp_ip;
    ivd_rel_display_frame_op_t *ps_dec_rel_disp_op;

    codec_t *ps_codec = (codec_t *)ps_codec_obj->pv_codec_handle;

    ps_dec_rel_disp_ip = (ivd_rel_display_frame_ip_t *)pv_api_ip;
    ps_dec_rel_disp_op = (ivd_rel_display_frame_op_t *)pv_api_op;

    UNUSED(ps_dec_rel_disp_op);

    if(0 == ps_codec->i4_share_disp_buf)
    {
        return IV_SUCCESS;
    }

    ihevc_buf_mgr_release((buf_mgr_t *)ps_codec->pv_pic_buf_mgr, ps_dec_rel_disp_ip->u4_disp_buf_id, BUF_MGR_DISP);

    return IV_SUCCESS;
}
/**
*******************************************************************************
*
* @brief
*  Sets degrade params
*
* @par Description:
*  Sets degrade params.
*  Refer to ihevcd_cxa_ctl_degrade_ip_t definition for details
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/

WORD32 ihevcd_set_degrade(iv_obj_t *ps_codec_obj,
                          void *pv_api_ip,
                          void *pv_api_op)
{
    ihevcd_cxa_ctl_degrade_ip_t *ps_ip;
    ihevcd_cxa_ctl_degrade_op_t *ps_op;
    codec_t *ps_codec = (codec_t *)ps_codec_obj->pv_codec_handle;

    ps_ip = (ihevcd_cxa_ctl_degrade_ip_t *)pv_api_ip;
    ps_op = (ihevcd_cxa_ctl_degrade_op_t *)pv_api_op;

    ps_codec->i4_degrade_type = ps_ip->i4_degrade_type;
    ps_codec->i4_nondegrade_interval = ps_ip->i4_nondegrade_interval;
    ps_codec->i4_degrade_pics = ps_ip->i4_degrade_pics;

    ps_op->u4_error_code = 0;
    ps_codec->i4_degrade_pic_cnt = 0;

    return IV_SUCCESS;
}


/**
*******************************************************************************
*
* @brief
*  Gets frame dimensions/offsets
*
* @par Description:
*  Gets frame buffer chararacteristics such a x & y offsets  display and
* buffer dimensions
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/

WORD32 ihevcd_get_frame_dimensions(iv_obj_t *ps_codec_obj,
                                   void *pv_api_ip,
                                   void *pv_api_op)
{
    ihevcd_cxa_ctl_get_frame_dimensions_ip_t *ps_ip;
    ihevcd_cxa_ctl_get_frame_dimensions_op_t *ps_op;
    codec_t *ps_codec = (codec_t *)ps_codec_obj->pv_codec_handle;
    WORD32 disp_wd, disp_ht, buffer_wd, buffer_ht, x_offset, y_offset;
    ps_ip = (ihevcd_cxa_ctl_get_frame_dimensions_ip_t *)pv_api_ip;
    ps_op = (ihevcd_cxa_ctl_get_frame_dimensions_op_t *)pv_api_op;
    UNUSED(ps_ip);
    if(ps_codec->i4_sps_done)
    {
        disp_wd = ps_codec->i4_disp_wd;
        disp_ht = ps_codec->i4_disp_ht;

        if(0 == ps_codec->i4_share_disp_buf)
        {
            buffer_wd = disp_wd;
            buffer_ht = disp_ht;
        }
        else
        {
            buffer_wd = ps_codec->i4_strd;
            buffer_ht = ps_codec->i4_ht + PAD_HT;
        }
    }
    else
    {

        disp_wd = ps_codec->i4_max_wd;
        disp_ht = ps_codec->i4_max_ht;

        if(0 == ps_codec->i4_share_disp_buf)
        {
            buffer_wd = disp_wd;
            buffer_ht = disp_ht;
        }
        else
        {
            buffer_wd = ALIGN16(disp_wd) + PAD_WD;
            buffer_ht = ALIGN16(disp_ht) + PAD_HT;

        }
    }
    if(ps_codec->i4_strd > buffer_wd)
        buffer_wd = ps_codec->i4_strd;

    if(0 == ps_codec->i4_share_disp_buf)
    {
        x_offset = 0;
        y_offset = 0;
    }
    else
    {
        y_offset = PAD_TOP;
        x_offset = PAD_LEFT;
    }

    ps_op->u4_disp_wd[0] = disp_wd;
    ps_op->u4_disp_ht[0] = disp_ht;
    ps_op->u4_buffer_wd[0] = buffer_wd;
    ps_op->u4_buffer_ht[0] = buffer_ht;
    ps_op->u4_x_offset[0] = x_offset;
    ps_op->u4_y_offset[0] = y_offset;

    ps_op->u4_disp_wd[1] = ps_op->u4_disp_wd[2] = ((ps_op->u4_disp_wd[0] + 1)
                    >> 1);
    ps_op->u4_disp_ht[1] = ps_op->u4_disp_ht[2] = ((ps_op->u4_disp_ht[0] + 1)
                    >> 1);
    ps_op->u4_buffer_wd[1] = ps_op->u4_buffer_wd[2] = (ps_op->u4_buffer_wd[0]
                    >> 1);
    ps_op->u4_buffer_ht[1] = ps_op->u4_buffer_ht[2] = (ps_op->u4_buffer_ht[0]
                    >> 1);
    ps_op->u4_x_offset[1] = ps_op->u4_x_offset[2] = (ps_op->u4_x_offset[0]
                    >> 1);
    ps_op->u4_y_offset[1] = ps_op->u4_y_offset[2] = (ps_op->u4_y_offset[0]
                    >> 1);

    if((ps_codec->e_chroma_fmt == IV_YUV_420SP_UV)
                    || (ps_codec->e_chroma_fmt == IV_YUV_420SP_VU))
    {
        ps_op->u4_disp_wd[2] = 0;
        ps_op->u4_disp_ht[2] = 0;
        ps_op->u4_buffer_wd[2] = 0;
        ps_op->u4_buffer_ht[2] = 0;
        ps_op->u4_x_offset[2] = 0;
        ps_op->u4_y_offset[2] = 0;

        ps_op->u4_disp_wd[1] <<= 1;
        ps_op->u4_buffer_wd[1] <<= 1;
        ps_op->u4_x_offset[1] <<= 1;
    }

    return IV_SUCCESS;

}


/**
*******************************************************************************
*
* @brief
*  Gets vui parameters
*
* @par Description:
*  Gets VUI parameters
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/
WORD32 ihevcd_get_vui_params(iv_obj_t *ps_codec_obj,
                             void *pv_api_ip,
                             void *pv_api_op)
{
    ihevcd_cxa_ctl_get_vui_params_ip_t *ps_ip;
    ihevcd_cxa_ctl_get_vui_params_op_t *ps_op;
    codec_t *ps_codec = (codec_t *)ps_codec_obj->pv_codec_handle;
    sps_t *ps_sps;
    vui_t *ps_vui;
    WORD32 i;

    ps_ip = (ihevcd_cxa_ctl_get_vui_params_ip_t *)pv_api_ip;
    ps_op = (ihevcd_cxa_ctl_get_vui_params_op_t *)pv_api_op;

    if(0 == ps_codec->i4_sps_done)
    {
        ps_op->u4_error_code = IHEVCD_VUI_PARAMS_NOT_FOUND;
        return IV_FAIL;
    }

    ps_sps = ps_codec->s_parse.ps_sps;
    if(0 == ps_sps->i1_sps_valid || 0 == ps_sps->i1_vui_parameters_present_flag)
    {
        WORD32 sps_idx = 0;
        ps_sps = ps_codec->ps_sps_base;

        while((0 == ps_sps->i1_sps_valid) || (0 == ps_sps->i1_vui_parameters_present_flag))
        {
            sps_idx++;
            ps_sps++;

            if(sps_idx == MAX_SPS_CNT - 1)
            {
                ps_op->u4_error_code = IHEVCD_VUI_PARAMS_NOT_FOUND;
                return IV_FAIL;
            }
        }
    }

    ps_vui = &ps_sps->s_vui_parameters;
    UNUSED(ps_ip);

    ps_op->u1_aspect_ratio_info_present_flag         =  ps_vui->u1_aspect_ratio_info_present_flag;
    ps_op->u1_aspect_ratio_idc                       =  ps_vui->u1_aspect_ratio_idc;
    ps_op->u2_sar_width                              =  ps_vui->u2_sar_width;
    ps_op->u2_sar_height                             =  ps_vui->u2_sar_height;
    ps_op->u1_overscan_info_present_flag             =  ps_vui->u1_overscan_info_present_flag;
    ps_op->u1_overscan_appropriate_flag              =  ps_vui->u1_overscan_appropriate_flag;
    ps_op->u1_video_signal_type_present_flag         =  ps_vui->u1_video_signal_type_present_flag;
    ps_op->u1_video_format                           =  ps_vui->u1_video_format;
    ps_op->u1_video_full_range_flag                  =  ps_vui->u1_video_full_range_flag;
    ps_op->u1_colour_description_present_flag        =  ps_vui->u1_colour_description_present_flag;
    ps_op->u1_colour_primaries                       =  ps_vui->u1_colour_primaries;
    ps_op->u1_transfer_characteristics               =  ps_vui->u1_transfer_characteristics;
    ps_op->u1_matrix_coefficients                    =  ps_vui->u1_matrix_coefficients;
    ps_op->u1_chroma_loc_info_present_flag           =  ps_vui->u1_chroma_loc_info_present_flag;
    ps_op->u1_chroma_sample_loc_type_top_field       =  ps_vui->u1_chroma_sample_loc_type_top_field;
    ps_op->u1_chroma_sample_loc_type_bottom_field    =  ps_vui->u1_chroma_sample_loc_type_bottom_field;
    ps_op->u1_neutral_chroma_indication_flag         =  ps_vui->u1_neutral_chroma_indication_flag;
    ps_op->u1_field_seq_flag                         =  ps_vui->u1_field_seq_flag;
    ps_op->u1_frame_field_info_present_flag          =  ps_vui->u1_frame_field_info_present_flag;
    ps_op->u1_default_display_window_flag            =  ps_vui->u1_default_display_window_flag;
    ps_op->u4_def_disp_win_left_offset               =  ps_vui->u4_def_disp_win_left_offset;
    ps_op->u4_def_disp_win_right_offset              =  ps_vui->u4_def_disp_win_right_offset;
    ps_op->u4_def_disp_win_top_offset                =  ps_vui->u4_def_disp_win_top_offset;
    ps_op->u4_def_disp_win_bottom_offset             =  ps_vui->u4_def_disp_win_bottom_offset;
    ps_op->u1_vui_hrd_parameters_present_flag        =  ps_vui->u1_vui_hrd_parameters_present_flag;
    ps_op->u1_vui_timing_info_present_flag           =  ps_vui->u1_vui_timing_info_present_flag;
    ps_op->u4_vui_num_units_in_tick                  =  ps_vui->u4_vui_num_units_in_tick;
    ps_op->u4_vui_time_scale                         =  ps_vui->u4_vui_time_scale;
    ps_op->u1_poc_proportional_to_timing_flag        =  ps_vui->u1_poc_proportional_to_timing_flag;
    ps_op->u1_num_ticks_poc_diff_one_minus1          =  ps_vui->u1_num_ticks_poc_diff_one_minus1;
    ps_op->u1_bitstream_restriction_flag             =  ps_vui->u1_bitstream_restriction_flag;
    ps_op->u1_tiles_fixed_structure_flag             =  ps_vui->u1_tiles_fixed_structure_flag;
    ps_op->u1_motion_vectors_over_pic_boundaries_flag =  ps_vui->u1_motion_vectors_over_pic_boundaries_flag;
    ps_op->u1_restricted_ref_pic_lists_flag          =  ps_vui->u1_restricted_ref_pic_lists_flag;
    ps_op->u4_min_spatial_segmentation_idc           =  ps_vui->u4_min_spatial_segmentation_idc;
    ps_op->u1_max_bytes_per_pic_denom                =  ps_vui->u1_max_bytes_per_pic_denom;
    ps_op->u1_max_bits_per_mincu_denom               =  ps_vui->u1_max_bits_per_mincu_denom;
    ps_op->u1_log2_max_mv_length_horizontal          =  ps_vui->u1_log2_max_mv_length_horizontal;
    ps_op->u1_log2_max_mv_length_vertical            =  ps_vui->u1_log2_max_mv_length_vertical;


    /* HRD parameters */
    ps_op->u1_timing_info_present_flag                         =    ps_vui->s_vui_hrd_parameters.u1_timing_info_present_flag;
    ps_op->u4_num_units_in_tick                                =    ps_vui->s_vui_hrd_parameters.u4_num_units_in_tick;
    ps_op->u4_time_scale                                       =    ps_vui->s_vui_hrd_parameters.u4_time_scale;
    ps_op->u1_nal_hrd_parameters_present_flag                  =    ps_vui->s_vui_hrd_parameters.u1_nal_hrd_parameters_present_flag;
    ps_op->u1_vcl_hrd_parameters_present_flag                  =    ps_vui->s_vui_hrd_parameters.u1_vcl_hrd_parameters_present_flag;
    ps_op->u1_cpbdpb_delays_present_flag                       =    ps_vui->s_vui_hrd_parameters.u1_cpbdpb_delays_present_flag;
    ps_op->u1_sub_pic_cpb_params_present_flag                  =    ps_vui->s_vui_hrd_parameters.u1_sub_pic_cpb_params_present_flag;
    ps_op->u1_tick_divisor_minus2                              =    ps_vui->s_vui_hrd_parameters.u1_tick_divisor_minus2;
    ps_op->u1_du_cpb_removal_delay_increment_length_minus1     =    ps_vui->s_vui_hrd_parameters.u1_du_cpb_removal_delay_increment_length_minus1;
    ps_op->u1_sub_pic_cpb_params_in_pic_timing_sei_flag        =    ps_vui->s_vui_hrd_parameters.u1_sub_pic_cpb_params_in_pic_timing_sei_flag;
    ps_op->u1_dpb_output_delay_du_length_minus1                =    ps_vui->s_vui_hrd_parameters.u1_dpb_output_delay_du_length_minus1;
    ps_op->u4_bit_rate_scale                                   =    ps_vui->s_vui_hrd_parameters.u4_bit_rate_scale;
    ps_op->u4_cpb_size_scale                                   =    ps_vui->s_vui_hrd_parameters.u4_cpb_size_scale;
    ps_op->u4_cpb_size_du_scale                                =    ps_vui->s_vui_hrd_parameters.u4_cpb_size_du_scale;
    ps_op->u1_initial_cpb_removal_delay_length_minus1          =    ps_vui->s_vui_hrd_parameters.u1_initial_cpb_removal_delay_length_minus1;
    ps_op->u1_au_cpb_removal_delay_length_minus1               =    ps_vui->s_vui_hrd_parameters.u1_au_cpb_removal_delay_length_minus1;
    ps_op->u1_dpb_output_delay_length_minus1                   =    ps_vui->s_vui_hrd_parameters.u1_dpb_output_delay_length_minus1;

    for(i = 0; i < 6; i++)
    {
        ps_op->au1_fixed_pic_rate_general_flag[i]                  =    ps_vui->s_vui_hrd_parameters.au1_fixed_pic_rate_general_flag[i];
        ps_op->au1_fixed_pic_rate_within_cvs_flag[i]               =    ps_vui->s_vui_hrd_parameters.au1_fixed_pic_rate_within_cvs_flag[i];
        ps_op->au1_elemental_duration_in_tc_minus1[i]              =    ps_vui->s_vui_hrd_parameters.au1_elemental_duration_in_tc_minus1[i];
        ps_op->au1_low_delay_hrd_flag[i]                           =    ps_vui->s_vui_hrd_parameters.au1_low_delay_hrd_flag[i];
        ps_op->au1_cpb_cnt_minus1[i]                               =    ps_vui->s_vui_hrd_parameters.au1_cpb_cnt_minus1[i];
    }


    return IV_SUCCESS;
}

/**
*******************************************************************************
*
* @brief
*  Sets Processor type
*
* @par Description:
*  Sets Processor type
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/

WORD32 ihevcd_set_processor(iv_obj_t *ps_codec_obj,
                            void *pv_api_ip,
                            void *pv_api_op)
{
    ihevcd_cxa_ctl_set_processor_ip_t *ps_ip;
    ihevcd_cxa_ctl_set_processor_op_t *ps_op;
    codec_t *ps_codec = (codec_t *)ps_codec_obj->pv_codec_handle;

    ps_ip = (ihevcd_cxa_ctl_set_processor_ip_t *)pv_api_ip;
    ps_op = (ihevcd_cxa_ctl_set_processor_op_t *)pv_api_op;

    ps_codec->e_processor_arch = (IVD_ARCH_T)ps_ip->u4_arch;
    ps_codec->e_processor_soc = (IVD_SOC_T)ps_ip->u4_soc;

    ihevcd_init_function_ptr(ps_codec);

    ihevcd_update_function_ptr(ps_codec);

    if(ps_codec->e_processor_soc && (ps_codec->e_processor_soc <= SOC_HISI_37X))
    {
        /* 8th bit indicates if format conversion is to be done ahead */
        if(ps_codec->e_processor_soc & 0x80)
            ps_codec->u4_enable_fmt_conv_ahead = 1;

        /* Lower 7 bit indicate NCTB - if non-zero */
        ps_codec->e_processor_soc &= 0x7F;

        if(ps_codec->e_processor_soc)
            ps_codec->u4_nctb = ps_codec->e_processor_soc;


    }

    if((ps_codec->e_processor_soc == SOC_HISI_37X) && (ps_codec->i4_num_cores == 2))
    {
        ps_codec->u4_nctb = 2;
    }


    ps_op->u4_error_code = 0;
    return IV_SUCCESS;
}

/**
*******************************************************************************
*
* @brief
*  Sets Number of cores that can be used in the codec. Codec uses these many
* threads for decoding
*
* @par Description:
*  Sets number of cores
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/

WORD32 ihevcd_set_num_cores(iv_obj_t *ps_codec_obj,
                            void *pv_api_ip,
                            void *pv_api_op)
{
    ihevcd_cxa_ctl_set_num_cores_ip_t *ps_ip;
    ihevcd_cxa_ctl_set_num_cores_op_t *ps_op;
    codec_t *ps_codec = (codec_t *)ps_codec_obj->pv_codec_handle;

    ps_ip = (ihevcd_cxa_ctl_set_num_cores_ip_t *)pv_api_ip;
    ps_op = (ihevcd_cxa_ctl_set_num_cores_op_t *)pv_api_op;

#ifdef MULTICORE
    ps_codec->i4_num_cores = ps_ip->u4_num_cores;
#else
    ps_codec->i4_num_cores = 1;
#endif
    ps_op->u4_error_code = 0;
    return IV_SUCCESS;
}
/**
*******************************************************************************
*
* @brief
*  Codec control call
*
* @par Description:
*  Codec control call which in turn calls appropriate calls  based on
* subcommand
*
* @param[in] ps_codec_obj
*  Pointer to codec object at API level
*
* @param[in] pv_api_ip
*  Pointer to input argument structure
*
* @param[out] pv_api_op
*  Pointer to output argument structure
*
* @returns  Status
*
* @remarks
*
*
*******************************************************************************
*/

WORD32 ihevcd_ctl(iv_obj_t *ps_codec_obj, void *pv_api_ip, void *pv_api_op)
{
    ivd_ctl_set_config_ip_t *ps_ctl_ip;
    ivd_ctl_set_config_op_t *ps_ctl_op;
    WORD32 ret = 0;
    WORD32 subcommand;
    codec_t *ps_codec = (codec_t *)ps_codec_obj->pv_codec_handle;

    ps_ctl_ip = (ivd_ctl_set_config_ip_t *)pv_api_ip;
    ps_ctl_op = (ivd_ctl_set_config_op_t *)pv_api_op;

    if(ps_codec->i4_init_done != 1)
    {
        ps_ctl_op->u4_error_code |= 1 << IVD_FATALERROR;
        ps_ctl_op->u4_error_code |= IHEVCD_INIT_NOT_DONE;
        return IV_FAIL;
    }
    subcommand = ps_ctl_ip->e_sub_cmd;

    switch(subcommand)
    {
        case IVD_CMD_CTL_GETPARAMS:
            ret = ihevcd_get_status(ps_codec_obj, (void *)pv_api_ip,
                                    (void *)pv_api_op);
            break;
        case IVD_CMD_CTL_SETPARAMS:
            ret = ihevcd_set_params(ps_codec_obj, (void *)pv_api_ip,
                                    (void *)pv_api_op);
            break;
        case IVD_CMD_CTL_RESET:
            ret = ihevcd_reset(ps_codec_obj, (void *)pv_api_ip,
                               (void *)pv_api_op);
            break;
        case IVD_CMD_CTL_SETDEFAULT:
        {
            ivd_ctl_set_config_op_t *s_ctl_dynparams_op =
                            (ivd_ctl_set_config_op_t *)pv_api_op;

            ret = ihevcd_set_default_params(ps_codec);
            if(IV_SUCCESS == ret)
                s_ctl_dynparams_op->u4_error_code = 0;
            break;
        }
        case IVD_CMD_CTL_FLUSH:
            ret = ihevcd_set_flush_mode(ps_codec_obj, (void *)pv_api_ip,
                                        (void *)pv_api_op);
            break;
        case IVD_CMD_CTL_GETBUFINFO:
            ret = ihevcd_get_buf_info(ps_codec_obj, (void *)pv_api_ip,
                                      (void *)pv_api_op);
            break;
        case IVD_CMD_CTL_GETVERSION:
        {
            ivd_ctl_getversioninfo_ip_t *ps_ip;
            ivd_ctl_getversioninfo_op_t *ps_op;
            IV_API_CALL_STATUS_T ret;
            ps_ip = (ivd_ctl_getversioninfo_ip_t *)pv_api_ip;
            ps_op = (ivd_ctl_getversioninfo_op_t *)pv_api_op;

            ps_op->u4_error_code = IV_SUCCESS;

            if((WORD32)ps_ip->u4_version_buffer_size <= 0)
            {
                ps_op->u4_error_code = IHEVCD_CXA_VERS_BUF_INSUFFICIENT;
                ret = IV_FAIL;
            }
            else
            {
                ret = ihevcd_get_version((CHAR *)ps_ip->pv_version_buffer,
                                         ps_ip->u4_version_buffer_size);
                if(ret != IV_SUCCESS)
                {
                    ps_op->u4_error_code = IHEVCD_CXA_VERS_BUF_INSUFFICIENT;
                    ret = IV_FAIL;
                }
            }
        }
            break;
        case IHEVCD_CXA_CMD_CTL_DEGRADE:
            ret = ihevcd_set_degrade(ps_codec_obj, (void *)pv_api_ip,
                            (void *)pv_api_op);
            break;
        case IHEVCD_CXA_CMD_CTL_SET_NUM_CORES:
            ret = ihevcd_set_num_cores(ps_codec_obj, (void *)pv_api_ip,
                                       (void *)pv_api_op);
            break;
        case IHEVCD_CXA_CMD_CTL_GET_BUFFER_DIMENSIONS:
            ret = ihevcd_get_frame_dimensions(ps_codec_obj, (void *)pv_api_ip,
                                              (void *)pv_api_op);
            break;
        case IHEVCD_CXA_CMD_CTL_GET_VUI_PARAMS:
            ret = ihevcd_get_vui_params(ps_codec_obj, (void *)pv_api_ip,
                                        (void *)pv_api_op);
            break;
        case IHEVCD_CXA_CMD_CTL_SET_PROCESSOR:
            ret = ihevcd_set_processor(ps_codec_obj, (void *)pv_api_ip,
                            (void *)pv_api_op);
            break;
        default:
            DEBUG("\nDo nothing\n");
            break;
    }

    return ret;
}

/**
*******************************************************************************
*
* @brief
*  Codecs entry point function. All the function calls to  the codec are
* done using this function with different  values specified in command
*
* @par Description:
*  Arguments are tested for validity and then based on the  command
* appropriate function is called
*
* @param[in] ps_handle
*  API level handle for codec
*
* @param[in] pv_api_ip
*  Input argument structure
*
* @param[out] pv_api_op
*  Output argument structure
*
* @returns  Status of the function corresponding to command
*
* @remarks
*
*
*******************************************************************************
*/
IV_API_CALL_STATUS_T ihevcd_cxa_api_function(iv_obj_t *ps_handle,
                                             void *pv_api_ip,
                                             void *pv_api_op)
{
    WORD32 command;
    UWORD32 *pu4_ptr_cmd;
    WORD32 ret = 0;
    IV_API_CALL_STATUS_T e_status;
    e_status = api_check_struct_sanity(ps_handle, pv_api_ip, pv_api_op);

    if(e_status != IV_SUCCESS)
    {
        DEBUG("error code = %d\n", *((UWORD32 *)pv_api_op + 1));
        return IV_FAIL;
    }

    pu4_ptr_cmd = (UWORD32 *)pv_api_ip;
    pu4_ptr_cmd++;

    command = *pu4_ptr_cmd;

    switch(command)
    {
        case IV_CMD_GET_NUM_MEM_REC:
            ret = ihevcd_get_num_rec((void *)pv_api_ip, (void *)pv_api_op);

            break;
        case IV_CMD_FILL_NUM_MEM_REC:

            ret = ihevcd_fill_num_mem_rec((void *)pv_api_ip, (void *)pv_api_op);
            break;
        case IV_CMD_INIT:
            ret = ihevcd_init_mem_rec(ps_handle, (void *)pv_api_ip,
                                      (void *)pv_api_op);
            break;

        case IVD_CMD_VIDEO_DECODE:
            ret = ihevcd_decode(ps_handle, (void *)pv_api_ip, (void *)pv_api_op);
            break;

        case IVD_CMD_GET_DISPLAY_FRAME:
            //ret = ihevcd_get_display_frame(ps_handle,(void *)pv_api_ip,(void *)pv_api_op);
            break;

        case IVD_CMD_SET_DISPLAY_FRAME:
            ret = ihevcd_set_display_frame(ps_handle, (void *)pv_api_ip,
                                           (void *)pv_api_op);

            break;

        case IVD_CMD_REL_DISPLAY_FRAME:
            ret = ihevcd_rel_display_frame(ps_handle, (void *)pv_api_ip,
                                           (void *)pv_api_op);
            break;

        case IV_CMD_RETRIEVE_MEMREC:
            ret = ihevcd_retrieve_memrec(ps_handle, (void *)pv_api_ip,
                                         (void *)pv_api_op);
            break;

        case IVD_CMD_VIDEO_CTL:
            ret = ihevcd_ctl(ps_handle, (void *)pv_api_ip, (void *)pv_api_op);
            break;
        default:
            ret = IV_FAIL;
            break;
    }

    return (IV_API_CALL_STATUS_T)ret;
}

