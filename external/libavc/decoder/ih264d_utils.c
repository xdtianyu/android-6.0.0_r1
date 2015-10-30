/******************************************************************************
 *
 * Copyright (C) 2015 The Android Open Source Project
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
 *****************************************************************************
 * Originally developed and contributed by Ittiam Systems Pvt. Ltd, Bangalore
*/
/*!
 **************************************************************************
 * \file ih264d_utils.c
 *
 * \brief
 *    Contains routines that handle of start and end of pic processing
 *
 * \date
 *    19/12/2002
 *
 * \author  AI
 **************************************************************************
 */

#include <string.h>
#include "ih264_typedefs.h"
#include "ithread.h"
#include "ih264d_deblocking.h"
#include "ih264d_parse_slice.h"
#include "ih264d_parse_cavlc.h"
#include "ih264d_dpb_manager.h"
#include "ih264d_defs.h"
#include "ih264d_structs.h"
#include "ih264d_mem_request.h"
#include "ih264_typedefs.h"
#include "ih264_macros.h"
#include "ih264_platform_macros.h"
#include "ih264d_tables.h"
#include "ih264d_debug.h"
#include "ih264d_mb_utils.h"
#include "ih264d_error_handler.h"
#include "ih264d_dpb_manager.h"
#include "ih264d_utils.h"
#include "ih264d_defs.h"
#include "ih264d_tables.h"
#include "ih264d_inter_pred.h"
#include "ih264d_dpb_manager.h"
#include "iv.h"
#include "ivd.h"
#include "ih264d_format_conv.h"
#include "ih264_error.h"
#include "ih264_disp_mgr.h"
#include "ih264_buf_mgr.h"
#include "ih264d_utils.h"

/*!
 **************************************************************************
 * \if Function name : ih264d_is_end_of_pic \endif
 *
 * \brief
 *    Determines whether current slice is first slice of a new picture as
 *    defined in 7.4.1.2.4 of 14496-10.
 *
 * \return
 *    Return 1 if current slice is first slice of a new picture
 *    Otherwise it returns 0
 **************************************************************************
 */
UWORD8 ih264d_is_end_of_pic(UWORD16 u2_frame_num,
                            UWORD8 u1_nal_ref_idc,
                            pocstruct_t *ps_cur_poc,
                            pocstruct_t *ps_prev_poc,
                            dec_slice_params_t * ps_prev_slice, /*!< Previous slice parameters*/
                            UWORD8 u1_pic_order_cnt_type,
                            UWORD8 u1_nal_unit_type,
                            UWORD32 u4_idr_pic_id,
                            UWORD8 u1_field_pic_flag,
                            UWORD8 u1_bottom_field_flag)
{
    WORD8 i1_is_end_of_pic;
    WORD8 a, b, c, d, e, f, g, h;

    a = b = c = d = e = f = g = h = 0;
    a = (ps_prev_slice->u2_frame_num != u2_frame_num);
    b = (ps_prev_slice->u1_field_pic_flag != u1_field_pic_flag);
    if(u1_field_pic_flag && ps_prev_slice->u1_field_pic_flag)
        c = (u1_bottom_field_flag != ps_prev_slice->u1_bottom_field_flag);
    d =
                    (u1_nal_ref_idc == 0 && ps_prev_slice->u1_nal_ref_idc != 0)
                                    || (u1_nal_ref_idc != 0
                                                    && ps_prev_slice->u1_nal_ref_idc
                                                                    == 0);
    if(!a)
    {
        if((u1_pic_order_cnt_type == 0)
                        && (ps_prev_slice->u1_pic_order_cnt_type == 0))
        {
            e =
                            ((ps_cur_poc->i4_pic_order_cnt_lsb
                                            != ps_prev_poc->i4_pic_order_cnt_lsb)
                                            || (ps_cur_poc->i4_delta_pic_order_cnt_bottom
                                                            != ps_prev_poc->i4_delta_pic_order_cnt_bottom));
        }

        if((u1_pic_order_cnt_type == 1)
                        && (ps_prev_slice->u1_pic_order_cnt_type == 1))
        {
            f =
                            ((ps_cur_poc->i4_delta_pic_order_cnt[0]
                                            != ps_prev_poc->i4_delta_pic_order_cnt[0])
                                            || (ps_cur_poc->i4_delta_pic_order_cnt[1]
                                                            != ps_prev_poc->i4_delta_pic_order_cnt[1]));
        }
    }

    if((u1_nal_unit_type == IDR_SLICE_NAL)
                    && (ps_prev_slice->u1_nal_unit_type == IDR_SLICE_NAL))
    {
        g = (u4_idr_pic_id != ps_prev_slice->u4_idr_pic_id);
    }

    if((u1_nal_unit_type == IDR_SLICE_NAL)
                    && (ps_prev_slice->u1_nal_unit_type != IDR_SLICE_NAL))
    {
        h = 1;
    }
    i1_is_end_of_pic = a + b + c + d + e + f + g + h;
    return (i1_is_end_of_pic);
}

/*!
 **************************************************************************
 * \if Function name : ih264d_decode_pic_order_cnt \endif
 *
 * \brief
 *    Calculates picture order count of picture.
 *
 * \return
 *    Returns the pic order count of the picture to which current
 *    Slice belongs.
 *
 **************************************************************************
 */
WORD32 ih264d_decode_pic_order_cnt(UWORD8 u1_is_idr_slice,
                                   UWORD32 u2_frame_num,
                                   pocstruct_t *ps_prev_poc,
                                   pocstruct_t *ps_cur_poc,
                                   dec_slice_params_t *ps_cur_slice, /*!< Pointer to current slice Params*/
                                   dec_pic_params_t * ps_pps,
                                   UWORD8 u1_nal_ref_idc,
                                   UWORD8 u1_bottom_field_flag,
                                   UWORD8 u1_field_pic_flag,
                                   WORD32 *pi4_poc)
{
    WORD16 i1_pic_msb;
    WORD32 i4_top_field_order_cnt = 0, i4_bottom_field_order_cnt = 0;
    dec_seq_params_t *ps_seq = ps_pps->ps_sps;
    WORD32 i4_prev_frame_num_ofst;

    switch(ps_seq->u1_pic_order_cnt_type)
    {
        case 0:
            /* POC TYPE 0 */
            if(u1_is_idr_slice)
            {
                ps_prev_poc->i4_pic_order_cnt_msb = 0;
                ps_prev_poc->i4_pic_order_cnt_lsb = 0;
            }
            if(ps_prev_poc->u1_mmco_equalto5)
            {
                if(ps_prev_poc->u1_bot_field != 1)
                {
                    ps_prev_poc->i4_pic_order_cnt_msb = 0;
                    ps_prev_poc->i4_pic_order_cnt_lsb =
                                    ps_prev_poc->i4_top_field_order_count;
                }
                else
                {
                    ps_prev_poc->i4_pic_order_cnt_msb = 0;
                    ps_prev_poc->i4_pic_order_cnt_lsb = 0;
                }
            }

            if((ps_cur_poc->i4_pic_order_cnt_lsb
                            < ps_prev_poc->i4_pic_order_cnt_lsb)
                            && ((ps_prev_poc->i4_pic_order_cnt_lsb
                                            - ps_cur_poc->i4_pic_order_cnt_lsb)
                                            >= (ps_seq->i4_max_pic_order_cntLsb
                                                            >> 1)))
            {
                i1_pic_msb = ps_prev_poc->i4_pic_order_cnt_msb
                                + ps_seq->i4_max_pic_order_cntLsb;
            }
            else if((ps_cur_poc->i4_pic_order_cnt_lsb
                            > ps_prev_poc->i4_pic_order_cnt_lsb)
                            && ((ps_cur_poc->i4_pic_order_cnt_lsb
                                            - ps_prev_poc->i4_pic_order_cnt_lsb)
                                            >= (ps_seq->i4_max_pic_order_cntLsb
                                                            >> 1)))
            {
                i1_pic_msb = ps_prev_poc->i4_pic_order_cnt_msb
                                - ps_seq->i4_max_pic_order_cntLsb;
            }
            else
            {
                i1_pic_msb = ps_prev_poc->i4_pic_order_cnt_msb;
            }

            if(!u1_field_pic_flag || !u1_bottom_field_flag)
                i4_top_field_order_cnt = i1_pic_msb
                                + ps_cur_poc->i4_pic_order_cnt_lsb;

            if(!u1_field_pic_flag)
            {
                i4_bottom_field_order_cnt = i4_top_field_order_cnt
                                + ps_cur_poc->i4_delta_pic_order_cnt_bottom;
            }
            else if(u1_bottom_field_flag)
            {
                i4_bottom_field_order_cnt = i1_pic_msb
                                + ps_cur_poc->i4_pic_order_cnt_lsb;
            }
            ps_cur_poc->i4_pic_order_cnt_msb = i1_pic_msb;
            break;

        case 1:
        {
            /* POC TYPE 1 */
            UWORD8 i;
            WORD32 prev_frame_num;
            WORD32 frame_num_ofst;
            WORD32 abs_frm_num;
            WORD32 poc_cycle_cnt, frame_num_in_poc_cycle;
            WORD32 expected_delta_poc_cycle;
            WORD32 expected_poc;

            prev_frame_num = (WORD32)ps_cur_slice->u2_frame_num;
            if(!u1_is_idr_slice)
            {
                if(ps_cur_slice->u1_mmco_equalto5)
                {
                    prev_frame_num = 0;
                    i4_prev_frame_num_ofst = 0;
                }
                else
                {
                    i4_prev_frame_num_ofst = ps_prev_poc->i4_prev_frame_num_ofst;
                }
            }
            else
                i4_prev_frame_num_ofst = 0;

            /* 1. Derivation for FrameNumOffset */
            if(u1_is_idr_slice)
            {
                frame_num_ofst = 0;
                ps_cur_poc->i4_delta_pic_order_cnt[0] = 0;
                ps_cur_poc->i4_delta_pic_order_cnt[1] = 0;
            }
            else if(prev_frame_num > ((WORD32)u2_frame_num))
            {
                frame_num_ofst = i4_prev_frame_num_ofst
                                + ps_seq->u2_u4_max_pic_num_minus1 + 1;
            }
            else
                frame_num_ofst = i4_prev_frame_num_ofst;

            /* 2. Derivation for absFrameNum */
            if(0 != ps_seq->u1_num_ref_frames_in_pic_order_cnt_cycle)
                abs_frm_num = frame_num_ofst + u2_frame_num;
            else
                abs_frm_num = 0;
            if((u1_nal_ref_idc == 0) && (abs_frm_num > 0))
                abs_frm_num = abs_frm_num - 1;

            /* 4. expectedDeltaPerPicOrderCntCycle is derived as */
            expected_delta_poc_cycle = 0;
            for(i = 0; i < ps_seq->u1_num_ref_frames_in_pic_order_cnt_cycle;
                            i++)
            {
                expected_delta_poc_cycle +=
                                ps_seq->i4_ofst_for_ref_frame[i];
            }

            /* 3. When absFrameNum > 0, picOrderCntCycleCnt and
             frame_num_in_poc_cycle are derived as : */
            /* 5. expectedPicOrderCnt is derived as : */
            if(abs_frm_num > 0)
            {
                poc_cycle_cnt =
                                DIV((abs_frm_num - 1),
                                    ps_seq->u1_num_ref_frames_in_pic_order_cnt_cycle);
                frame_num_in_poc_cycle =
                                MOD((abs_frm_num - 1),
                                    ps_seq->u1_num_ref_frames_in_pic_order_cnt_cycle);

                expected_poc = poc_cycle_cnt
                                * expected_delta_poc_cycle;
                for(i = 0; i <= frame_num_in_poc_cycle; i++)
                {
                    expected_poc = expected_poc
                                    + ps_seq->i4_ofst_for_ref_frame[i];
                }
            }
            else
                expected_poc = 0;

            if(u1_nal_ref_idc == 0)
            {
                expected_poc = expected_poc
                                + ps_seq->i4_ofst_for_non_ref_pic;
            }

            /* 6. TopFieldOrderCnt or BottomFieldOrderCnt are derived as */
            if(!u1_field_pic_flag)
            {
                i4_top_field_order_cnt = expected_poc
                                + ps_cur_poc->i4_delta_pic_order_cnt[0];
                i4_bottom_field_order_cnt = i4_top_field_order_cnt
                                + ps_seq->i4_ofst_for_top_to_bottom_field
                                + ps_cur_poc->i4_delta_pic_order_cnt[1];
            }
            else if(!u1_bottom_field_flag)
            {
                i4_top_field_order_cnt = expected_poc
                                + ps_cur_poc->i4_delta_pic_order_cnt[0];
            }
            else
            {
                i4_bottom_field_order_cnt = expected_poc
                                + ps_seq->i4_ofst_for_top_to_bottom_field
                                + ps_cur_poc->i4_delta_pic_order_cnt[0];
            }
            /* Copy the current POC info into Previous POC structure */
            ps_cur_poc->i4_prev_frame_num_ofst = frame_num_ofst;
        }

            break;
        case 2:
        {
            /* POC TYPE 2 */
            WORD32 prev_frame_num;
            WORD32 frame_num_ofst;
            WORD32 tmp_poc;

            prev_frame_num = (WORD32)ps_cur_slice->u2_frame_num;
            if(!u1_is_idr_slice)
            {
                if(ps_cur_slice->u1_mmco_equalto5)
                {
                    prev_frame_num = 0;
                    i4_prev_frame_num_ofst = 0;
                }
                else
                    i4_prev_frame_num_ofst = ps_prev_poc->i4_prev_frame_num_ofst;
            }
            else
                i4_prev_frame_num_ofst = 0;

            /* 1. Derivation for FrameNumOffset */
            if(u1_is_idr_slice)
            {
                frame_num_ofst = 0;
                ps_cur_poc->i4_delta_pic_order_cnt[0] = 0;
                ps_cur_poc->i4_delta_pic_order_cnt[1] = 0;
            }
            else if(prev_frame_num > ((WORD32)u2_frame_num))
            {
                frame_num_ofst = i4_prev_frame_num_ofst
                                + ps_seq->u2_u4_max_pic_num_minus1 + 1;
            }
            else
                frame_num_ofst = i4_prev_frame_num_ofst;

            /* 2. Derivation for tempPicOrderCnt */
            if(u1_is_idr_slice)
                tmp_poc = 0;
            else if(u1_nal_ref_idc == 0)
                tmp_poc = ((frame_num_ofst + u2_frame_num) << 1)
                                - 1;
            else
                tmp_poc = ((frame_num_ofst + u2_frame_num) << 1);

            /* 6. TopFieldOrderCnt or BottomFieldOrderCnt are derived as */
            if(!u1_field_pic_flag)
            {
                i4_top_field_order_cnt = tmp_poc;
                i4_bottom_field_order_cnt = tmp_poc;
            }
            else if(!u1_bottom_field_flag)
                i4_top_field_order_cnt = tmp_poc;
            else
                i4_bottom_field_order_cnt = tmp_poc;

            /* Copy the current POC info into Previous POC structure */
            ps_prev_poc->i4_prev_frame_num_ofst = frame_num_ofst;
            ps_cur_poc->i4_prev_frame_num_ofst = frame_num_ofst;
        }
            break;
        default:
            return ERROR_INV_POC_TYPE_T;
            break;
    }

    if(!u1_field_pic_flag) // or a complementary field pair
    {
        *pi4_poc = MIN(i4_top_field_order_cnt, i4_bottom_field_order_cnt);
        ps_pps->i4_top_field_order_cnt = i4_top_field_order_cnt;
        ps_pps->i4_bottom_field_order_cnt = i4_bottom_field_order_cnt;
    }
    else if(!u1_bottom_field_flag)
    {
        *pi4_poc = i4_top_field_order_cnt;
        ps_pps->i4_top_field_order_cnt = i4_top_field_order_cnt;
    }
    else
    {
        *pi4_poc = i4_bottom_field_order_cnt;
        ps_pps->i4_bottom_field_order_cnt = i4_bottom_field_order_cnt;
    }

    ps_pps->i4_avg_poc = *pi4_poc;

    return OK;
}

/*!
 **************************************************************************
 * \if Function name : ih264d_end_of_pic_processing \endif
 *
 * \brief
 *    Performs the end of picture processing.
 *
 * It performs deblocking on the current picture and sets the i4_status of
 * current picture as decoded.
 *
 * \return
 *    0 on Success and Error code otherwise.
 **************************************************************************
 */
WORD32 ih264d_end_of_pic_processing(dec_struct_t *ps_dec)
{
    UWORD8 u1_pic_type, u1_nal_ref_idc;
    dec_slice_params_t *ps_cur_slice = ps_dec->ps_cur_slice;
    WORD32 ret;

    /* If nal_ref_idc is equal to 0 for one slice or slice data partition NAL
     unit of a particular picture, it shall be equal to 0 for all slice and
     slice data partition NAL units of the picture. nal_ref_idc greater
     than 0 indicates that the content of the NAL unit belongs to a decoded
     picture that is stored and marked for use as a reference picture in the
     decoded picture buffer. */

    /* 1. Do MMCO
     2. Add Cur Pic to list of reference pics.
     */

    /* Call MMCO */
    u1_pic_type = 0;
    u1_nal_ref_idc = ps_cur_slice->u1_nal_ref_idc;

    if(u1_nal_ref_idc)
    {
        if(ps_cur_slice->u1_nal_unit_type == IDR_SLICE_NAL)
        {
            if(ps_dec->ps_dpb_cmds->u1_long_term_reference_flag == 0)
            {
                ih264d_reset_ref_bufs(ps_dec->ps_dpb_mgr);

                {
                    ret = ih264d_insert_st_node(ps_dec->ps_dpb_mgr,
                                          ps_dec->ps_cur_pic,
                                          ps_dec->u1_pic_buf_id,
                                          ps_cur_slice->u2_frame_num);
                    if(ret != OK)
                        return ret;
                }
            }
            else
            {
                /* Equivalent of inserting a pic directly as longterm Pic */

                {
                    ret = ih264d_insert_st_node(ps_dec->ps_dpb_mgr,
                                          ps_dec->ps_cur_pic,
                                          ps_dec->u1_pic_buf_id,
                                          ps_cur_slice->u2_frame_num);
                    if(ret != OK)
                        return ret;
                    /* Set longTermIdx = 0, MaxLongTermFrameIdx = 0 */
                    ret = ih264d_delete_st_node_or_make_lt(
                                    ps_dec->ps_dpb_mgr,
                                    ps_cur_slice->u2_frame_num, 0,
                                    ps_cur_slice->u1_field_pic_flag);
                    if(ret != OK)
                        return ret;
                    ps_dec->ps_dpb_mgr->u1_max_lt_pic_idx_plus1 = 1;
                }
            }
        }
        else
        {

            {
                UWORD16 u2_pic_num = ps_cur_slice->u2_frame_num;



                ret = ih264d_do_mmco_buffer(
                                ps_dec->ps_dpb_cmds, ps_dec->ps_dpb_mgr,
                                ps_dec->ps_cur_sps->u1_num_ref_frames,
                                u2_pic_num,
                                (ps_dec->ps_cur_sps->u2_u4_max_pic_num_minus1),
                                ps_dec->u1_nal_unit_type, ps_dec->ps_cur_pic,
                                ps_dec->u1_pic_buf_id,
                                ps_cur_slice->u1_field_pic_flag,
                                ps_dec->e_dec_status);
                if(ret != OK)
                    return ret;
            }
        }
        ih264d_update_default_index_list(ps_dec->ps_dpb_mgr);
    }

    if(ps_cur_slice->u1_field_pic_flag)
    {
        if(ps_cur_slice->u1_bottom_field_flag)
        {
            if(u1_nal_ref_idc)
                u1_pic_type = u1_pic_type | BOT_REF;
            u1_pic_type = u1_pic_type | BOT_FLD;
        }
        else
        {
            if(u1_nal_ref_idc)
                u1_pic_type = u1_pic_type | TOP_REF;
            u1_pic_type = u1_pic_type | TOP_FLD;
        }
    }
    else
        u1_pic_type = TOP_REF | BOT_REF;
    ps_dec->ps_cur_pic->u1_pic_type |= u1_pic_type;

#if ROW_ACCESSES_STAT
    {
        H264_DEC_DEBUG_PRINT("Row_Accesses_BeforeBB = %6d, Row_Accesses_AfterBB = %6d \n\n",
                        gui_Row_Accesses_BeforeBB, gui_Row_Accesses_AfterBB);
        gui_Row_Accesses_BeforeBBTotal += gui_Row_Accesses_BeforeBB;
        gui_Row_Accesses_AfterBBTotal += gui_Row_Accesses_AfterBB;
        gui_Row_Accesses_AfterBB = 0;
        gui_Row_Accesses_BeforeBB = 0;
    }
#endif

    if(ps_cur_slice->u1_field_pic_flag)
    {
        H264_DEC_DEBUG_PRINT("Toggling secondField\n");
        ps_dec->u1_second_field = 1 - ps_dec->u1_second_field;
    }

    return OK;
}

/*****************************************************************************/
/*                                                                           */
/*  Function Name : init_dpb_size                                            */
/*                                                                           */
/*  Description   : This function calculates the DBP i4_size in frames          */
/*  Inputs        : ps_seq - current sequence params                         */
/*                                                                           */
/*  Globals       : None                                                     */
/*                                                                           */
/*  Outputs       : None                                                     */
/*                                                                           */
/*  Returns       : DPB in frames                                            */
/*                                                                           */
/*  Issues        : None                                                     */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         28 04 2005   NS              Draft                                */
/*                                                                           */
/*****************************************************************************/
WORD32 ih264d_get_dpb_size(dec_seq_params_t *ps_seq, dec_struct_t *ps_dec)
{
    WORD32 i4_size;
    UWORD8 u1_level_idc;

    u1_level_idc = ps_seq->u1_level_idc; //harcode for the time being
    u1_level_idc = MIN(u1_level_idc, ps_dec->u4_level_at_init);

    switch(u1_level_idc)
    {
        case 10:
            i4_size = 152064;
            break;
        case 11:
            i4_size = 345600;
            break;
        case 12:
            i4_size = 912384;
            break;
        case 13:
            i4_size = 912384;
            break;
        case 20:
            i4_size = 912384;
            break;
        case 21:
            i4_size = 1824768;
            break;
        case 22:
            i4_size = 3110400;
            break;
        case 30:
            i4_size = 3110400;
            break;
        case 31:
            i4_size = 6912000;
            break;
        case 32:
            i4_size = 7864320;
            break;
        case 40:
            i4_size = 12582912;
            break;
        case 41:
            i4_size = 12582912;
            break;
        case 42:
            i4_size = 12582912;
            break;
        case 50:
            i4_size = 42393600;
            break;
        case 51:
            i4_size = 70778880;
            break;
        case 52:
            i4_size = 70778880;
            break;
        default:
            i4_size = 70778880;
            break;
    }

    i4_size /= (ps_seq->u2_frm_wd_in_mbs * (ps_seq->u2_frm_ht_in_mbs << (1 - ps_seq->u1_frame_mbs_only_flag)));
    i4_size /= 384;
    i4_size = MIN(i4_size, 16);
    i4_size = MAX(i4_size, 1);
    return (i4_size);
}

WORD32 ih264d_get_dpb_size_new(UWORD32 u4_level_idc,
                               UWORD32 u2_frm_wd_in_mbs,
                               UWORD32 u2_frm_ht_in_mbs)
{

    UWORD32 i4_size = 0;

    switch(u4_level_idc)
    {
        case 10:
            i4_size = 152064;
            break;
        case 11:
            i4_size = 345600;
            break;
        case 12:
            i4_size = 912384;
            break;
        case 13:
            i4_size = 912384;
            break;
        case 20:
            i4_size = 912384;
            break;
        case 21:
            i4_size = 1824768;
            break;
        case 22:
            i4_size = 3110400;
            break;
        case 30:
            i4_size = 3110400;
            break;
        case 31:
            i4_size = 6912000;
            break;
        case 32:
            i4_size = 7864320;
            break;
        case 40:
            i4_size = 12582912;
            break;
        case 41:
            i4_size = 12582912;
            break;
        case 42:
            i4_size = 12582912;
            break;
        case 50:
            i4_size = 42393600;
            break;
        case 51:
            i4_size = 70778880;
            break;
        case 52:
            i4_size = 70778880;
            break;
        default:
        {
            i4_size = 70778880;
        }
            break;
    }

    i4_size = i4_size / (u2_frm_wd_in_mbs * (u2_frm_ht_in_mbs));
    i4_size = (i4_size + 383) / 384;
    i4_size = MIN(i4_size, 16);
    i4_size = MAX(i4_size, 1);
    return (i4_size);
}

/*****************************************************************************/
/*                                                                           */
/*  Function Name : ih264d_max_possible_ref_pics                                    */
/*                                                                           */
/*  Description   : This function returns the maximum number of              */
/*                  reference buffers corresponding to the current Level     */
/*                  in accordance to "Table A-1  Level limits" in standard.  */
/*                  Please refer to Annex A - Profiles and Levels            */
/*                  Maximum Number of reference buffers are derived from     */
/*                  the dbpsize and max_mbs_in frame given in the table      */
/*  Inputs        : level number                                             */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         19 05 2005   SWRN            Draft                                */
/*                                                                           */
/*****************************************************************************/

UWORD8 ih264d_max_possible_ref_pics(UWORD8 u1_level)
{
    switch(u1_level)
    {
        case H264_LEVEL_1_0:
            return (MAX_REF_LEVEL_1_0);
        case H264_LEVEL_1_1:
            return (MAX_REF_LEVEL_1_1);
        case H264_LEVEL_1_2:
            return (MAX_REF_LEVEL_1_2);
        case H264_LEVEL_1_3:
            return (MAX_REF_LEVEL_1_3);
        case H264_LEVEL_2_0:
            return (MAX_REF_LEVEL_2_0);
        case H264_LEVEL_2_1:
            return (MAX_REF_LEVEL_2_1);
        case H264_LEVEL_2_2:
            return (MAX_REF_LEVEL_2_2);
        case H264_LEVEL_3_0:
            return (MAX_REF_LEVEL_3_0);
    }

    return (H264_MAX_REF_PICS);
}

/***************************************************************************/
/* If change in Level or the required PicBuffers i4_size is more than the  */
/* current one FREE the current PicBuffers and allocate affresh            */
/***************************************************************************/
UWORD8 ih264d_is_sps_changed(prev_seq_params_t * ps_prv,
                             dec_seq_params_t * ps_cur)
{

    if((ps_prv->u2_frm_wd_in_mbs != ps_cur->u2_frm_wd_in_mbs)
                    || (ps_prv->u1_level_idc != ps_cur->u1_level_idc)
                    || (ps_prv->u1_profile_idc != ps_cur->u1_profile_idc)
                    || (ps_cur->u2_frm_ht_in_mbs != ps_prv->u2_frm_ht_in_mbs)
                    || (ps_cur->u1_frame_mbs_only_flag
                                    != ps_prv->u1_frame_mbs_only_flag)
                    || (ps_cur->u1_direct_8x8_inference_flag
                                    != ps_prv->u1_direct_8x8_inference_flag))
        return 1;

    return 0;
}

/**************************************************************************/
/* This function initialises the value of ps_dec->u1_recon_mb_grp         */
/* ps_dec->u1_recon_mb_grp must satisfy the following criteria            */
/*  - multiple of 2 (required for N/2 parse-mvpred design)                */
/*  - multiple of 4 (if it is not a frame_mbs_only sequence),             */
/*         in this case N/2 itself needs to be even for mbpair processing */
/*  - lesser than ps_dec->u2_frm_wd_in_mbs/2 (at least 3 N-Chunks       */
/*         should make a row to ensure proper MvTop transferring)         */
/**************************************************************************/
WORD32 ih264d_init_dec_mb_grp(dec_struct_t *ps_dec)
{
    dec_seq_params_t *ps_seq = ps_dec->ps_cur_sps;
    UWORD8 u1_frm = ps_seq->u1_frame_mbs_only_flag;

    ps_dec->u1_recon_mb_grp = PARSE_MB_GROUP_4;

    //NMB set to width in MBs for non-mbaff cases
    if(0 == ps_seq->u1_mb_aff_flag)
        ps_dec->u1_recon_mb_grp = ps_dec->u2_frm_wd_in_mbs;

    ps_dec->u1_recon_mb_grp_pair = ps_dec->u1_recon_mb_grp >> 1;

    if(!ps_dec->u1_recon_mb_grp)
    {
        return ERROR_MB_GROUP_ASSGN_T;
    }

    ps_dec->u4_num_mbs_prev_nmb = ps_dec->u1_recon_mb_grp;

    return OK;
}

/*!
 **************************************************************************
 * \if Function name : ih264d_get_numbuf_dpb_bank \endif
 *
 * \brief
 *    Initializes the picture.
 *
 * \return
 *    0 on Success and Error code otherwise
 *
 * \note
 *    This function is called when first slice of the
 *    NON -IDR picture is encountered.
 **************************************************************************
 */
WORD32 ih264d_get_numbuf_dpb_bank(dec_struct_t *ps_dec, UWORD32 u4_frame_wd, UWORD32 u4_frame_ht)
{
    WORD32 i4_DPB_size;
    WORD32 i4_pic_size;
    WORD32 i4_num_buf_alloc;
    UWORD32 Ysize;
    UWORD32 UVsize;
    UWORD32 one_frm_size;


    i4_DPB_size = ps_dec->ps_mem_tab[MEM_REC_REF_PIC].u4_mem_size;

    Ysize = u4_frame_wd * u4_frame_ht;

    UVsize = Ysize >> 2;

    {
        if(ps_dec->u4_share_disp_buf == 1)
        {
            /* In case of buffers getting shared between application and library
             there is no need of reference memtabs. Instead of setting the i4_size
             to zero, it is reduced to a small i4_size to ensure that changes
             in the code are minimal */
            if((ps_dec->u1_chroma_format == IV_YUV_420SP_UV)
                            || (ps_dec->u1_chroma_format == IV_YUV_420SP_VU)
                            || (ps_dec->u1_chroma_format == IV_YUV_420P))
            {
                Ysize = 64;
            }
            if(ps_dec->u1_chroma_format == IV_YUV_420SP_UV)
            {
                UVsize = 64;
            }

        }
    }

    one_frm_size = (((Ysize + 127) >> 7) << 7)
                    + ((((UVsize << 1) + 127) >> 7) << 7);
    i4_num_buf_alloc = i4_DPB_size / (one_frm_size);

    return i4_num_buf_alloc;
}

/*!
 **************************************************************************
 * \if Function name : ih264d_get_numbuf_mv_bank \endif
 *
 * \brief
 *    Computes number of MVbank buffers that can be allocated.
 *
 * \return
 *    Number of MV bank buffers that can be allocated.
 *
 * \note
 **************************************************************************
 */
UWORD32 ih264d_get_numbuf_mv_bank(dec_struct_t *ps_dec, UWORD32 width,
                                  UWORD32 height)
{
    UWORD32 u4_mv_bank_size,one_frame_size;
    UWORD32 u4_num_buf_alloc;

    u4_mv_bank_size = ps_dec->ps_mem_tab[MEM_REC_MVBANK].u4_mem_size;
    one_frame_size = sizeof(mv_pred_t)
                    * ((width * (height + PAD_MV_BANK_ROW)) >> 4);
    u4_num_buf_alloc = u4_mv_bank_size / one_frame_size;
    return u4_num_buf_alloc;
}
/*!
 **************************************************************************
 * \if Function name : ih264d_init_pic \endif
 *
 * \brief
 *    Initializes the picture.
 *
 * \return
 *    0 on Success and Error code otherwise
 *
 * \note
 *    This function is called when first slice of the
 *    NON -IDR picture is encountered.
 **************************************************************************
 */
WORD32 ih264d_init_pic(dec_struct_t *ps_dec,
                       UWORD16 u2_frame_num,
                       WORD32 i4_poc,
                       dec_pic_params_t *ps_pps)
{
    dec_seq_params_t *ps_seq = ps_pps->ps_sps;
    prev_seq_params_t * ps_prev_seq_params = &ps_dec->s_prev_seq_params;
    WORD32 i4_pic_bufs;
    WORD32 ret;

    ps_dec->ps_cur_slice->u2_frame_num = u2_frame_num;
    ps_dec->ps_cur_slice->i4_poc = i4_poc;
    ps_dec->ps_cur_pps = ps_pps;
    ps_dec->ps_cur_pps->pv_codec_handle = ps_dec;

    ps_dec->ps_cur_sps = ps_seq;
    ps_dec->ps_dpb_mgr->i4_max_frm_num = ps_seq->u2_u4_max_pic_num_minus1
                    + 1;

    ps_dec->ps_dpb_mgr->u2_pic_ht = ps_dec->u2_pic_ht;
    ps_dec->ps_dpb_mgr->u2_pic_wd = ps_dec->u2_pic_wd;
    ps_dec->i4_pic_type = -1;
    ps_dec->i4_frametype = -1;
    ps_dec->i4_content_type = -1;

    /*--------------------------------------------------------------------*/
    /* Get the value of MaxMbAddress and frmheight in Mbs                 */
    /*--------------------------------------------------------------------*/
    ps_seq->u2_max_mb_addr =
                    (ps_seq->u2_frm_wd_in_mbs
                                    * (ps_dec->u2_pic_ht
                                                    >> (4
                                                                    + ps_dec->ps_cur_slice->u1_field_pic_flag)))
                                    - 1;
    ps_dec->u2_frm_ht_in_mbs = (ps_dec->u2_pic_ht
                    >> (4 + ps_dec->ps_cur_slice->u1_field_pic_flag));

    /***************************************************************************/
    /* If change in Level or the required PicBuffers i4_size is more than the  */
    /* current one FREE the current PicBuffers and allocate affresh            */
    /***************************************************************************/
    if(!ps_dec->u1_init_dec_flag
                    || ih264d_is_sps_changed(ps_prev_seq_params, ps_seq))
    {


        ivd_video_decode_ip_t *ps_dec_in = ps_dec->pv_dec_in;
        ivd_video_decode_op_t *ps_dec_out = ps_dec->pv_dec_out;

        if(ps_dec->u4_share_disp_buf == 0)
        {
            i4_pic_bufs = ih264d_get_numbuf_dpb_bank(ps_dec, ps_dec->u2_frm_wd_y,
                                              ps_dec->u2_frm_ht_y);
        }
        else
        {
            i4_pic_bufs = (WORD32)ps_dec->u4_num_disp_bufs;
        }

        ps_dec->u1_pic_bufs = CLIP_U8(i4_pic_bufs);

        if(ps_dec->u4_share_disp_buf == 0)
            ps_dec->u1_pic_bufs = MIN(ps_dec->u1_pic_bufs,
                                      (H264_MAX_REF_PICS * 2));

        ps_dec->u1_max_dec_frame_buffering = ih264d_get_dpb_size(ps_seq,
                                                                 ps_dec);

        ps_dec->u1_max_dec_frame_buffering = MIN(
                        ps_dec->u1_max_dec_frame_buffering,
                        ps_dec->u4_num_ref_frames_at_init);
        ps_dec->u1_max_dec_frame_buffering = MIN(
                        ps_dec->u1_max_dec_frame_buffering,
                        ps_dec->u1_pic_bufs);

//   ps_dec->u1_pic_bufs = ps_dec->i1_max_dec_frame_buffering;

        /* Fix is for handling one pic in and one pic out incase of */
        /* MMCO 5 or IDR                                            */

        ps_dec->i4_display_delay = MIN(ps_dec->u4_num_reorder_frames_at_init,
                                       ps_dec->u1_max_dec_frame_buffering);

        if(1 == ps_seq->u1_vui_parameters_present_flag)
        {
            if(ps_seq->u1_frame_mbs_only_flag == 1)
                ps_dec->i4_display_delay = MIN(
                                (UWORD32 )ps_dec->i4_display_delay,
                                ((UWORD32 )ps_seq->s_vui.u4_num_reorder_frames
                                                + 1));
            else
                ps_dec->i4_display_delay = MIN(
                                (UWORD32 )ps_dec->i4_display_delay,
                                ((UWORD32 )ps_seq->s_vui.u4_num_reorder_frames
                                                + 1) * 2);
        }

        /* Temporary hack to run Tractor Cav/Cab/MbAff Profiler streams  also for CAFI1_SVA_C.264 in conformance*/
        if(ps_dec->u1_init_dec_flag)
        {
            ih264d_release_pics_in_dpb((void *)ps_dec,
                                       ps_dec->u1_pic_bufs);
            ih264d_release_display_bufs(ps_dec);
            ih264d_reset_ref_bufs(ps_dec->ps_dpb_mgr);
        }

        /*********************************************************************/
        /* Configuring decoder parameters based on level and then            */
        /* fresh pointer initialisation in decoder scratch and state buffers */
        /*********************************************************************/
        if(!ps_dec->u1_init_dec_flag ||
                ((ps_seq->u1_level_idc < H264_LEVEL_3_0) ^ (ps_prev_seq_params->u1_level_idc < H264_LEVEL_3_0)))
        {
            ret = ih264d_init_dec_mb_grp(ps_dec);
            if(ret != OK)
                return ret;
        }

        ret = ih264d_create_pic_buffers(ps_dec->u1_pic_bufs,
                                        ps_dec);
        if(ret != OK)
            return ret;

        ih264d_get_memory_dec_params(ps_dec);

        ret = ih264d_create_mv_bank(ps_dec, ps_dec->u2_pic_wd,
                                    ps_dec->u2_pic_ht);
        if(ret != OK)
            return ret;

        /* In shared mode, set all of them as used by display */
        if(ps_dec->u4_share_disp_buf == 1)
        {
            WORD32 i;

            for(i = 0; i < ps_dec->u1_pic_bufs; i++)
            {
                ih264_buf_mgr_set_status((buf_mgr_t *)ps_dec->pv_pic_buf_mgr, i,
                                         BUF_MGR_IO);
            }
        }

        ps_dec->u1_init_dec_flag = 1;
        ps_prev_seq_params->u2_frm_wd_in_mbs = ps_seq->u2_frm_wd_in_mbs;
        ps_prev_seq_params->u1_level_idc = ps_seq->u1_level_idc;
        ps_prev_seq_params->u1_profile_idc = ps_seq->u1_profile_idc;
        ps_prev_seq_params->u2_frm_ht_in_mbs = ps_seq->u2_frm_ht_in_mbs;
        ps_prev_seq_params->u1_frame_mbs_only_flag =
                        ps_seq->u1_frame_mbs_only_flag;
        ps_prev_seq_params->u1_direct_8x8_inference_flag =
                        ps_seq->u1_direct_8x8_inference_flag;

        ps_dec->i4_cur_display_seq = 0;
        ps_dec->i4_prev_max_display_seq = 0;
        ps_dec->i4_max_poc = 0;

        {
            /* 0th entry of CtxtIncMbMap will be always be containing default values
             for CABAC context representing MB not available */
            ctxt_inc_mb_info_t *p_DefCtxt = ps_dec->p_ctxt_inc_mb_map - 1;
            UWORD8 *pu1_temp;
            WORD8 i;
            p_DefCtxt->u1_mb_type = CAB_SKIP;

            p_DefCtxt->u1_cbp = 0x0f;
            p_DefCtxt->u1_intra_chroma_pred_mode = 0;

            p_DefCtxt->u1_yuv_dc_csbp = 0x7;

            p_DefCtxt->u1_transform8x8_ctxt = 0;

            pu1_temp = (UWORD8*)p_DefCtxt->i1_ref_idx;
            for(i = 0; i < 4; i++, pu1_temp++)
                (*pu1_temp) = 0;
            pu1_temp = (UWORD8*)p_DefCtxt->u1_mv;
            for(i = 0; i < 16; i++, pu1_temp++)
                (*pu1_temp) = 0;
            ps_dec->ps_def_ctxt_mb_info = p_DefCtxt;
        }

    }
    /* reset DBP commands read u4_flag */
    ps_dec->ps_dpb_cmds->u1_dpb_commands_read = 0;

    return OK;
}

/*****************************************************************************/
/*                                                                           */
/*  Function Name : ih264d_get_next_display_field                                   */
/*                                                                           */
/*  Description   : Application calls this module to get the next field      */
/*                  to be displayed                                          */
/*                                                                           */
/*  Inputs        : 1.   IBUFAPI_Handle Hnadle to the Display buffer         */
/*                  2.   IH264DEC_DispUnit    Pointer to the display struct  */
/*                                                                           */
/*  Globals       :                                                          */
/*                                                                           */
/*                                                                           */
/*  Processing    : None                                                     */
/*  Outputs       : None                                                     */
/*  Returns       : None                                                     */
/*  Issues        : None                                                     */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         27 05 2005   Ittiam          Draft                                */
/*                                                                           */
/*****************************************************************************/

WORD32 ih264d_get_next_display_field(dec_struct_t * ps_dec,
                                  ivd_out_bufdesc_t *ps_out_buffer,
                                  ivd_get_display_frame_op_t *pv_disp_op)
{
    pic_buffer_t *pic_buf;

    UWORD8 i1_cur_fld;
    WORD32 u4_api_ret = -1;
    WORD32 i4_disp_buf_id;
    iv_yuv_buf_t *ps_op_frm;



    ps_op_frm = &(ps_dec->s_disp_frame_info);
    H264_MUTEX_LOCK(&ps_dec->process_disp_mutex);
    pic_buf = (pic_buffer_t *)ih264_disp_mgr_get(
                    (disp_mgr_t *)ps_dec->pv_disp_buf_mgr, &i4_disp_buf_id);
    ps_dec->u4_num_fld_in_frm = 0;
    u4_api_ret = -1;
    pv_disp_op->u4_ts = -1;
    pv_disp_op->e_output_format = ps_dec->u1_chroma_format;

    pv_disp_op->s_disp_frm_buf.pv_y_buf = ps_out_buffer->pu1_bufs[0];
    pv_disp_op->s_disp_frm_buf.pv_u_buf = ps_out_buffer->pu1_bufs[1];
    pv_disp_op->s_disp_frm_buf.pv_v_buf = ps_out_buffer->pu1_bufs[2];
    if(pic_buf != NULL)
    {
        pv_disp_op->e4_fld_type = 0;
        pv_disp_op->u4_disp_buf_id = i4_disp_buf_id;

        ps_op_frm->u4_y_ht = pic_buf->u2_disp_height << 1;
        ps_op_frm->u4_u_ht = ps_op_frm->u4_v_ht = ps_op_frm->u4_y_ht >> 1;
        ps_op_frm->u4_y_wd = pic_buf->u2_disp_width;

        ps_op_frm->u4_u_wd = ps_op_frm->u4_v_wd = ps_op_frm->u4_y_wd >> 1;

        ps_op_frm->u4_y_strd = pic_buf->u2_frm_wd_y;
        ps_op_frm->u4_u_strd = ps_op_frm->u4_v_strd = pic_buf->u2_frm_wd_uv;

        /* ! */
        pv_disp_op->u4_ts = pic_buf->u4_ts;

        /* set the start of the Y, U and V buffer pointer for display    */
        ps_op_frm->pv_y_buf = pic_buf->pu1_buf1 + pic_buf->u2_crop_offset_y;
        ps_op_frm->pv_u_buf = pic_buf->pu1_buf2 + pic_buf->u2_crop_offset_uv;
        ps_op_frm->pv_v_buf = pic_buf->pu1_buf3 + pic_buf->u2_crop_offset_uv;
        ps_dec->u4_num_fld_in_frm++;
        ps_dec->u4_num_fld_in_frm++;
        u4_api_ret = 0;

        if(pic_buf->u1_picturetype == 0)
            pv_disp_op->u4_progressive_frame_flag = 1;
        else
            pv_disp_op->u4_progressive_frame_flag = 0;

    } H264_MUTEX_UNLOCK(&ps_dec->process_disp_mutex);
    pv_disp_op->u4_error_code = u4_api_ret;
    pv_disp_op->e_pic_type = 0xFFFFFFFF; //Junk;

    if(u4_api_ret)
    {
        pv_disp_op->u4_error_code = 1; //put a proper error code here
    }
    else
    {

        //Release the buffer if being sent for display
        UWORD32 temp;
        UWORD32 dest_inc_Y = 0, dest_inc_UV = 0;

        pv_disp_op->s_disp_frm_buf.u4_y_wd = temp = MIN(ps_op_frm->u4_y_wd,
                                                        ps_op_frm->u4_y_strd);
        pv_disp_op->s_disp_frm_buf.u4_u_wd = pv_disp_op->s_disp_frm_buf.u4_y_wd
                        >> 1;
        pv_disp_op->s_disp_frm_buf.u4_v_wd = pv_disp_op->s_disp_frm_buf.u4_y_wd
                        >> 1;

        pv_disp_op->s_disp_frm_buf.u4_y_ht = ps_op_frm->u4_y_ht;
        pv_disp_op->s_disp_frm_buf.u4_u_ht = pv_disp_op->s_disp_frm_buf.u4_y_ht
                        >> 1;
        pv_disp_op->s_disp_frm_buf.u4_v_ht = pv_disp_op->s_disp_frm_buf.u4_y_ht
                        >> 1;
        if(0 == ps_dec->u4_share_disp_buf)
        {
            pv_disp_op->s_disp_frm_buf.u4_y_strd =
                            pv_disp_op->s_disp_frm_buf.u4_y_wd;
            pv_disp_op->s_disp_frm_buf.u4_u_strd =
                            pv_disp_op->s_disp_frm_buf.u4_y_wd >> 1;
            pv_disp_op->s_disp_frm_buf.u4_v_strd =
                            pv_disp_op->s_disp_frm_buf.u4_y_wd >> 1;

        }
        else
        {
            pv_disp_op->s_disp_frm_buf.u4_y_strd = ps_op_frm->u4_y_strd;
        }

        if(ps_dec->u4_app_disp_width)
        {
            pv_disp_op->s_disp_frm_buf.u4_y_strd = MAX(
                            ps_dec->u4_app_disp_width,
                            pv_disp_op->s_disp_frm_buf.u4_y_strd);
        }

        pv_disp_op->u4_error_code = 0;
        if(pv_disp_op->e_output_format == IV_YUV_420P)
        {
            UWORD32 i;
            pv_disp_op->s_disp_frm_buf.u4_u_strd =
                            pv_disp_op->s_disp_frm_buf.u4_y_strd >> 1;
            pv_disp_op->s_disp_frm_buf.u4_v_strd =
                            pv_disp_op->s_disp_frm_buf.u4_y_strd >> 1;

            pv_disp_op->s_disp_frm_buf.u4_u_wd = ps_op_frm->u4_y_wd >> 1;
            pv_disp_op->s_disp_frm_buf.u4_v_wd = ps_op_frm->u4_y_wd >> 1;

            if(1 == ps_dec->u4_share_disp_buf)
            {
                pv_disp_op->s_disp_frm_buf.pv_y_buf = ps_op_frm->pv_y_buf;

                for(i = 0; i < MAX_DISP_BUFS_NEW; i++)
                {
                    UWORD8 *buf = ps_dec->disp_bufs[i].buf[0];
                    buf += ps_dec->disp_bufs[i].u4_ofst[0];
                    if(((UWORD8 *)pv_disp_op->s_disp_frm_buf.pv_y_buf
                                    - pic_buf->u2_crop_offset_y) == buf)
                    {
                        buf = ps_dec->disp_bufs[i].buf[1];
                        buf += ps_dec->disp_bufs[i].u4_ofst[1];
                        pv_disp_op->s_disp_frm_buf.pv_u_buf = buf
                                        + pic_buf->u2_crop_offset_uv;

                        buf = ps_dec->disp_bufs[i].buf[2];
                        buf += ps_dec->disp_bufs[i].u4_ofst[2];
                        pv_disp_op->s_disp_frm_buf.pv_v_buf = buf
                                        + pic_buf->u2_crop_offset_uv;
                    }
                }
            }

        }
        else if((pv_disp_op->e_output_format == IV_YUV_420SP_UV)
                        || (pv_disp_op->e_output_format == IV_YUV_420SP_VU))
        {
            pv_disp_op->s_disp_frm_buf.u4_u_strd =
                            pv_disp_op->s_disp_frm_buf.u4_y_strd;
            pv_disp_op->s_disp_frm_buf.u4_v_strd = 0;

            if(1 == ps_dec->u4_share_disp_buf)
            {
                UWORD32 i;

                pv_disp_op->s_disp_frm_buf.pv_y_buf = ps_op_frm->pv_y_buf;

                for(i = 0; i < MAX_DISP_BUFS_NEW; i++)
                {
                    UWORD8 *buf = ps_dec->disp_bufs[i].buf[0];
                    buf += ps_dec->disp_bufs[i].u4_ofst[0];
                    if((UWORD8 *)pv_disp_op->s_disp_frm_buf.pv_y_buf
                                    - pic_buf->u2_crop_offset_y == buf)
                    {
                        buf = ps_dec->disp_bufs[i].buf[1];
                        buf += ps_dec->disp_bufs[i].u4_ofst[1];
                        pv_disp_op->s_disp_frm_buf.pv_u_buf = buf
                                        + pic_buf->u2_crop_offset_uv;
                        ;

                        buf = ps_dec->disp_bufs[i].buf[2];
                        buf += ps_dec->disp_bufs[i].u4_ofst[2];
                        pv_disp_op->s_disp_frm_buf.pv_v_buf = buf
                                        + pic_buf->u2_crop_offset_uv;
                        ;
                    }
                }
            }
            pv_disp_op->s_disp_frm_buf.u4_u_wd =
                            pv_disp_op->s_disp_frm_buf.u4_y_wd;
            pv_disp_op->s_disp_frm_buf.u4_v_wd = 0;

        }
        else if((pv_disp_op->e_output_format == IV_RGB_565)
                        || (pv_disp_op->e_output_format == IV_YUV_422ILE))
        {

            pv_disp_op->s_disp_frm_buf.u4_u_strd = 0;
            pv_disp_op->s_disp_frm_buf.u4_v_strd = 0;
            pv_disp_op->s_disp_frm_buf.u4_u_wd = 0;
            pv_disp_op->s_disp_frm_buf.u4_v_wd = 0;
            pv_disp_op->s_disp_frm_buf.u4_u_ht = 0;
            pv_disp_op->s_disp_frm_buf.u4_v_ht = 0;

        }


    }

    return u4_api_ret;
}


/*****************************************************************************/
/*  Function Name : ih264d_release_display_field                                         */
/*                                                                           */
/*  Description   : This function releases the display field that was returned   */
/*                  here.                                                    */
/*  Inputs        : ps_dec - Decoder parameters                              */
/*  Globals       : None                                                     */
/*  Processing    : Refer bumping process in the standard                    */
/*  Outputs       : Assigns display sequence number.                         */
/*  Returns       : None                                                     */
/*                                                                           */
/*  Issues        : None                                                     */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         27 04 2005   NS              Draft                                */
/*                                                                           */
/*****************************************************************************/
void ih264d_release_display_field(dec_struct_t *ps_dec,
                                  ivd_get_display_frame_op_t *pv_disp_op)
{
    if(1 == pv_disp_op->u4_error_code)
    {
        if(1 == ps_dec->u1_flushfrm)
        {
            UWORD32 i;

            if(1 == ps_dec->u4_share_disp_buf)
            {
                H264_MUTEX_LOCK(&ps_dec->process_disp_mutex);
                for(i = 0; i < (MAX_DISP_BUFS_NEW); i++)
                {
                    if(1 == ps_dec->u4_disp_buf_mapping[i])
                    {
                        ih264_buf_mgr_release(
                                        (buf_mgr_t *)ps_dec->pv_pic_buf_mgr, i,
                                        BUF_MGR_IO);
                        ps_dec->u4_disp_buf_mapping[i] = 0;
                    }
                } H264_MUTEX_UNLOCK(&ps_dec->process_disp_mutex);

                memset(ps_dec->u4_disp_buf_to_be_freed, 0,
                       (MAX_DISP_BUFS_NEW) * sizeof(UWORD32));
                for(i = 0; i < ps_dec->u1_pic_bufs; i++)
                    ps_dec->u4_disp_buf_mapping[i] = 1;
            }
            ps_dec->u1_flushfrm = 0;

        }
    }
    else
    {
        H264_MUTEX_LOCK(&ps_dec->process_disp_mutex);

        if(0 == ps_dec->u4_share_disp_buf)
        {
            ih264_buf_mgr_release((buf_mgr_t *)ps_dec->pv_pic_buf_mgr,
                                  pv_disp_op->u4_disp_buf_id,
                                  BUF_MGR_IO);

        }
        else
        {
            ps_dec->u4_disp_buf_mapping[pv_disp_op->u4_disp_buf_id] = 1;
        } H264_MUTEX_UNLOCK(&ps_dec->process_disp_mutex);

    }
}
/*****************************************************************************/
/*  Function Name : ih264d_assign_display_seq                                         */
/*                                                                           */
/*  Description   : This function implments bumping process. Every outgoing  */
/*                  frame from DPB is assigned a display sequence number     */
/*                  which increases monotonically. System looks for this     */
/*                  number to display a frame.                              */
/*                  here.                                                    */
/*  Inputs        : ps_dec - Decoder parameters                              */
/*  Globals       : None                                                     */
/*  Processing    : Refer bumping process in the standard                    */
/*  Outputs       : Assigns display sequence number.                         */
/*  Returns       : None                                                     */
/*                                                                           */
/*  Issues        : None                                                     */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         27 04 2005   NS              Draft                                */
/*                                                                           */
/*****************************************************************************/
WORD32 ih264d_assign_display_seq(dec_struct_t *ps_dec)
{
    WORD32 i;
    WORD32 i4_min_poc;
    WORD32 i4_min_poc_buf_id;
    WORD32 i4_min_index;
    dpb_manager_t *ps_dpb_mgr = ps_dec->ps_dpb_mgr;
    WORD32 (*i4_poc_buf_id_map)[3] = ps_dpb_mgr->ai4_poc_buf_id_map;

    i4_min_poc = 0x7fffffff;
    i4_min_poc_buf_id = -1;
    i4_min_index = -1;

    if(ps_dpb_mgr->i1_poc_buf_id_entries >= ps_dec->i4_display_delay)
    {
        for(i = 0; i < MAX_FRAMES; i++)
        {
            if((i4_poc_buf_id_map[i][0] != -1)
                            && (DO_NOT_DISP
                                            != ps_dpb_mgr->ai4_poc_buf_id_map[i][0]))
            {
                if(i4_poc_buf_id_map[i][1] < i4_min_poc)
                {
                    i4_min_poc = i4_poc_buf_id_map[i][1];
                    i4_min_poc_buf_id = i4_poc_buf_id_map[i][0];
                    i4_min_index = i;
                }
            }
        }

        if((i4_min_index != -1) && (DO_NOT_DISP != i4_min_poc_buf_id))
        {
            ps_dec->i4_cur_display_seq++;
            ih264_disp_mgr_add(
                            (disp_mgr_t *)ps_dec->pv_disp_buf_mgr,
                            i4_min_poc_buf_id, ps_dec->i4_cur_display_seq,
                            ps_dec->apv_buf_id_pic_buf_map[i4_min_poc_buf_id]);
            i4_poc_buf_id_map[i4_min_index][0] = -1;
            i4_poc_buf_id_map[i4_min_index][1] = 0x7fffffff;
            ps_dpb_mgr->i1_poc_buf_id_entries--;
        }
        else if(DO_NOT_DISP == i4_min_poc_buf_id)
        {
            WORD32 i4_error_code;
            i4_error_code = ERROR_GAPS_IN_FRM_NUM;
//          i4_error_code |= 1<<IVD_CORRUPTEDDATA;
            return i4_error_code;
        }
    }
    return OK;
}

/*****************************************************************************/
/*                                                                           */
/*  Function Name : ih264d_release_display_bufs                                       */
/*                                                                           */
/*  Description   : This function implments bumping process when mmco = 5.   */
/*                  Each outgoing frame from DPB is assigned a display       */
/*                  sequence number which increases monotonically. System    */
/*                  looks for this number to display a frame.                */
/*  Inputs        : ps_dec - Decoder parameters                              */
/*  Globals       : None                                                     */
/*  Processing    : Refer bumping process in the standard for mmco = 5       */
/*  Outputs       : Assigns display sequence number.                         */
/*  Returns       : None                                                     */
/*                                                                           */
/*  Issues        : None                                                     */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         27 04 2005   NS              Draft                                */
/*                                                                           */
/*****************************************************************************/
void ih264d_release_display_bufs(dec_struct_t *ps_dec)
{
    WORD32 i, j;
    WORD32 i4_min_poc;
    WORD32 i4_min_poc_buf_id;
    WORD32 i4_min_index;
    dpb_manager_t *ps_dpb_mgr = ps_dec->ps_dpb_mgr;
    WORD32 (*i4_poc_buf_id_map)[3] = ps_dpb_mgr->ai4_poc_buf_id_map;

    i4_min_poc = 0x7fffffff;
    i4_min_poc_buf_id = -1;
    i4_min_index = -1;

    ih264d_delete_nonref_nondisplay_pics(ps_dpb_mgr);

    for(j = 0; j < ps_dpb_mgr->i1_poc_buf_id_entries; j++)
    {
        i4_min_poc = 0x7fffffff;
        for(i = 0; i < MAX_FRAMES; i++)
        {
            if(i4_poc_buf_id_map[i][0] != -1)
            {
                if(i4_poc_buf_id_map[i][1] < i4_min_poc)
                {
                    i4_min_poc = i4_poc_buf_id_map[i][1];
                    i4_min_poc_buf_id = i4_poc_buf_id_map[i][0];
                    i4_min_index = i;
                }
            }
        }

        if(DO_NOT_DISP != i4_min_poc_buf_id)
        {
            ps_dec->i4_cur_display_seq++;
            ih264_disp_mgr_add(
                            (disp_mgr_t *)ps_dec->pv_disp_buf_mgr,
                            i4_min_poc_buf_id, ps_dec->i4_cur_display_seq,
                            ps_dec->apv_buf_id_pic_buf_map[i4_min_poc_buf_id]);
            i4_poc_buf_id_map[i4_min_index][0] = -1;
            i4_poc_buf_id_map[i4_min_index][1] = 0x7fffffff;
            ps_dpb_mgr->ai4_poc_buf_id_map[i4_min_index][2] = 0;
        }
        else
        {
            i4_poc_buf_id_map[i4_min_index][0] = -1;
            i4_poc_buf_id_map[i4_min_index][1] = 0x7fffffff;
            ps_dpb_mgr->ai4_poc_buf_id_map[i4_min_index][2] = 0;
        }
    }
    ps_dpb_mgr->i1_poc_buf_id_entries = 0;
    ps_dec->i4_prev_max_display_seq = ps_dec->i4_prev_max_display_seq
                    + ps_dec->i4_max_poc + ps_dec->u1_max_dec_frame_buffering
                    + 1;
    ps_dec->i4_max_poc = 0;
}

/*****************************************************************************/
/*                                                                           */
/*  Function Name : ih264d_assign_pic_num                                           */
/*                                                                           */
/*  Description   : This function assigns pic num to each reference frame    */
/*                  depending on the cur_frame_num as speified in section    */
/*                  8.2.4.1                                                  */
/*                                                                           */
/*  Inputs        : ps_dec                                                   */
/*                                                                           */
/*  Globals       : NO globals used                                          */
/*                                                                           */
/*  Processing    : for all ST pictures                                      */
/*                    if( FrameNum > cur_frame_num)                          */
/*                    PicNum = FrameNum - MaxFrameNum                        */
/*                    else                                                   */
/*                    PicNum = FrameNum                                      */
/*                                                                           */
/*  Returns       : void                                                     */
/*                                                                           */
/*  Issues        : NO                                                       */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         13 07 2002   Jay             Draft                                */
/*                                                                           */
/*****************************************************************************/

void ih264d_assign_pic_num(dec_struct_t *ps_dec)
{
    dpb_manager_t *ps_dpb_mgr;
    struct dpb_info_t *ps_next_dpb;
    WORD8 i;
    WORD32 i4_cur_frame_num, i4_max_frame_num;
    WORD32 i4_ref_frame_num;
    UWORD8 u1_fld_pic_flag = ps_dec->ps_cur_slice->u1_field_pic_flag;

    i4_max_frame_num = ps_dec->ps_cur_sps->u2_u4_max_pic_num_minus1 + 1;
    i4_cur_frame_num = ps_dec->ps_cur_pic->i4_frame_num;
    ps_dpb_mgr = ps_dec->ps_dpb_mgr;

    /* Start from ST head */
    ps_next_dpb = ps_dpb_mgr->ps_dpb_st_head;
    for(i = 0; i < ps_dpb_mgr->u1_num_st_ref_bufs; i++)
    {
        WORD32 i4_pic_num;

        i4_ref_frame_num = ps_next_dpb->ps_pic_buf->i4_frame_num;
        if(i4_ref_frame_num > i4_cur_frame_num)
        {
            /* RefPic Buf frame_num is before Current frame_num in decode order */
            i4_pic_num = i4_ref_frame_num - i4_max_frame_num;
        }
        else
        {
            /* RefPic Buf frame_num is after Current frame_num in decode order */
            i4_pic_num = i4_ref_frame_num;
        }

        ps_next_dpb->ps_pic_buf->i4_pic_num = i4_pic_num;
        ps_next_dpb->i4_frame_num = i4_pic_num;
        ps_next_dpb->ps_pic_buf->u1_long_term_frm_idx = MAX_REF_BUFS + 1;
        if(u1_fld_pic_flag)
        {
            /* Assign the pic num to top fields and bot fields */

            ps_next_dpb->s_top_field.i4_pic_num = i4_pic_num * 2
                            + !(ps_dec->ps_cur_slice->u1_bottom_field_flag);
            ps_next_dpb->s_bot_field.i4_pic_num = i4_pic_num * 2
                            + ps_dec->ps_cur_slice->u1_bottom_field_flag;
        }
        /* Chase the next link */
        ps_next_dpb = ps_next_dpb->ps_prev_short;
    }

    if(ps_dec->ps_cur_sps->u1_gaps_in_frame_num_value_allowed_flag
                    && ps_dpb_mgr->u1_num_gaps)
    {
        WORD32 i4_start_frm, i4_end_frm;
        /* Assign pic numbers for gaps */
        for(i = 0; i < MAX_FRAMES; i++)
        {
            i4_start_frm = ps_dpb_mgr->ai4_gaps_start_frm_num[i];
            if(i4_start_frm != INVALID_FRAME_NUM)
            {
                if(i4_start_frm > i4_cur_frame_num)
                {
                    /* gap's frame_num is before Current frame_num in
                     decode order */
                    i4_start_frm -= i4_max_frame_num;
                }
                ps_dpb_mgr->ai4_gaps_start_frm_num[i] = i4_start_frm;
                i4_end_frm = ps_dpb_mgr->ai4_gaps_end_frm_num[i];

                if(i4_end_frm > i4_cur_frame_num)
                {
                    /* gap's frame_num is before Current frame_num in
                     decode order */
                    i4_end_frm -= i4_max_frame_num;
                }
                ps_dpb_mgr->ai4_gaps_end_frm_num[i] = i4_end_frm;
            }
        }
    }
}

/*!
 **************************************************************************
 * \if Function name : ih264d_update_qp \endif
 *
 * \brief
 *    Updates the values of QP and its related entities
 *
 * \return
 *    0 on Success and Error code otherwise
 *
 **************************************************************************
 */
WORD32 ih264d_update_qp(dec_struct_t * ps_dec, const WORD8 i1_qp)
{
    WORD32 i_temp;
    i_temp = (ps_dec->u1_qp + i1_qp + 52) % 52;

    if((i_temp < 0) || (i_temp > 51) || (i1_qp < -26) || (i1_qp > 25))
        return ERROR_INV_RANGE_QP_T;

    ps_dec->u1_qp = i_temp;
    ps_dec->u1_qp_y_rem6 = ps_dec->u1_qp % 6;
    ps_dec->u1_qp_y_div6 = ps_dec->u1_qp / 6;
    i_temp = CLIP3(0, 51, ps_dec->u1_qp + ps_dec->ps_cur_pps->i1_chroma_qp_index_offset);
    ps_dec->u1_qp_u_rem6 = MOD(gau1_ih264d_qp_scale_cr[12 + i_temp], 6);
    ps_dec->u1_qp_u_div6 = DIV(gau1_ih264d_qp_scale_cr[12 + i_temp], 6);

    i_temp = CLIP3(0, 51, ps_dec->u1_qp + ps_dec->ps_cur_pps->i1_second_chroma_qp_index_offset);
    ps_dec->u1_qp_v_rem6 = MOD(gau1_ih264d_qp_scale_cr[12 + i_temp], 6);
    ps_dec->u1_qp_v_div6 = DIV(gau1_ih264d_qp_scale_cr[12 + i_temp], 6);

    ps_dec->pu2_quant_scale_y =
                    gau2_ih264_iquant_scale_4x4[ps_dec->u1_qp_y_rem6];
    ps_dec->pu2_quant_scale_u =
                    gau2_ih264_iquant_scale_4x4[ps_dec->u1_qp_u_rem6];
    ps_dec->pu2_quant_scale_v =
                    gau2_ih264_iquant_scale_4x4[ps_dec->u1_qp_v_rem6];
    return OK;
}

/*****************************************************************************/
/*                                                                           */
/*  Function Name : ih264d_decode_gaps_in_frame_num                                 */
/*                                                                           */
/*  Description   : This function decodes gaps in frame number               */
/*                                                                           */
/*  Inputs        : ps_dec          Decoder parameters                       */
/*                  u2_frame_num   current frame number                     */
/*                                                                           */
/*  Globals       : None                                                     */
/*  Processing    : This functionality needs to be implemented               */
/*  Outputs       : None                                                     */
/*  Returns       : None                                                     */
/*                                                                           */
/*  Issues        : Not implemented                                          */
/*                                                                           */
/*  Revision History:                                                        */
/*                                                                           */
/*         DD MM YYYY   Author(s)       Changes (Describe the changes made)  */
/*         06 05 2002   NS              Draft                                */
/*                                                                           */
/*****************************************************************************/
WORD32 ih264d_decode_gaps_in_frame_num(dec_struct_t *ps_dec,
                                       UWORD16 u2_frame_num)
{
    UWORD32 u4_next_frm_num, u4_start_frm_num;
    UWORD32 u4_max_frm_num;
    pocstruct_t s_tmp_poc;
    WORD32 i4_poc;
    dec_slice_params_t *ps_cur_slice;

    dec_pic_params_t *ps_pic_params;
    WORD8 i1_gap_idx;
    WORD32 *i4_gaps_start_frm_num;
    dpb_manager_t *ps_dpb_mgr;
    WORD32 i4_frame_gaps;
    WORD8 *pi1_gaps_per_seq;
    WORD32 ret;

    ps_cur_slice = ps_dec->ps_cur_slice;
    if(ps_cur_slice->u1_field_pic_flag)
    {
        if(ps_dec->u2_prev_ref_frame_num == u2_frame_num)
            return 0;
    }

    u4_next_frm_num = ps_dec->u2_prev_ref_frame_num + 1;
    u4_max_frm_num = ps_dec->ps_cur_sps->u2_u4_max_pic_num_minus1 + 1;

    // check
    if(u4_next_frm_num >= u4_max_frm_num)
    {
        u4_next_frm_num -= u4_max_frm_num;
    }

    if(u4_next_frm_num == u2_frame_num)
    {
        return (0);
    }

    // check
    if((ps_dec->u1_nal_unit_type == IDR_SLICE_NAL)
                    && (u4_next_frm_num >= u2_frame_num))
    {
        return (0);
    }
    u4_start_frm_num = u4_next_frm_num;

    s_tmp_poc.i4_pic_order_cnt_lsb = 0;
    s_tmp_poc.i4_delta_pic_order_cnt_bottom = 0;
    s_tmp_poc.i4_pic_order_cnt_lsb = 0;
    s_tmp_poc.i4_delta_pic_order_cnt_bottom = 0;
    s_tmp_poc.i4_delta_pic_order_cnt[0] = 0;
    s_tmp_poc.i4_delta_pic_order_cnt[1] = 0;

    ps_cur_slice = ps_dec->ps_cur_slice;
    ps_pic_params = ps_dec->ps_cur_pps;
    ps_cur_slice->u1_field_pic_flag = 0;

    i4_frame_gaps = 0;
    ps_dpb_mgr = ps_dec->ps_dpb_mgr;

    /* Find a empty slot to store gap seqn info */
    i4_gaps_start_frm_num = ps_dpb_mgr->ai4_gaps_start_frm_num;
    for(i1_gap_idx = 0; i1_gap_idx < MAX_FRAMES; i1_gap_idx++)
    {
        if(INVALID_FRAME_NUM == i4_gaps_start_frm_num[i1_gap_idx])
            break;
    }
    if(MAX_FRAMES == i1_gap_idx)
    {
        UWORD32 i4_error_code;
        i4_error_code = ERROR_DBP_MANAGER_T;
//          i4_error_code |= 1<<IVD_CORRUPTEDDATA;
        return i4_error_code;
    }

    i4_poc = 0;
    i4_gaps_start_frm_num[i1_gap_idx] = u4_start_frm_num;
    ps_dpb_mgr->ai4_gaps_end_frm_num[i1_gap_idx] = u2_frame_num - 1;
    pi1_gaps_per_seq = ps_dpb_mgr->ai1_gaps_per_seq;
    pi1_gaps_per_seq[i1_gap_idx] = 0;
    while(u4_next_frm_num != u2_frame_num)
    {
        ih264d_delete_nonref_nondisplay_pics(ps_dpb_mgr);
        if(ps_pic_params->ps_sps->u1_pic_order_cnt_type)
        {
            /* allocate a picture buffer and insert it as ST node */
            ret = ih264d_decode_pic_order_cnt(0, u4_next_frm_num,
                                              &ps_dec->s_prev_pic_poc,
                                              &s_tmp_poc, ps_cur_slice,
                                              ps_pic_params, 1, 0, 0,
                                              &i4_poc);
            if(ret != OK)
                return ret;

            /* Display seq no calculations */
            if(i4_poc >= ps_dec->i4_max_poc)
                ps_dec->i4_max_poc = i4_poc;
            /* IDR Picture or POC wrap around */
            if(i4_poc == 0)
            {
                ps_dec->i4_prev_max_display_seq =
                                ps_dec->i4_prev_max_display_seq
                                                + ps_dec->i4_max_poc
                                                + ps_dec->u1_max_dec_frame_buffering
                                                + 1;
                ps_dec->i4_max_poc = 0;
            }

            ps_cur_slice->u1_mmco_equalto5 = 0;
            ps_cur_slice->u2_frame_num = u4_next_frm_num;
        }

        // check
        if(ps_dpb_mgr->i1_poc_buf_id_entries
                        >= ps_dec->u1_max_dec_frame_buffering)
        {
            ret = ih264d_assign_display_seq(ps_dec);
            if(ret != OK)
                return ret;
        }

        ret = ih264d_insert_pic_in_display_list(
                        ps_dec->ps_dpb_mgr, (WORD8) DO_NOT_DISP,
                        (WORD32)(ps_dec->i4_prev_max_display_seq + i4_poc),
                        u4_next_frm_num);
        if(ret != OK)
            return ret;

        pi1_gaps_per_seq[i1_gap_idx]++;
        ret = ih264d_do_mmco_for_gaps(ps_dpb_mgr,
                                ps_dec->ps_cur_sps->u1_num_ref_frames);
        if(ret != OK)
            return ret;

        ih264d_delete_nonref_nondisplay_pics(ps_dpb_mgr);

        u4_next_frm_num++;
        if(u4_next_frm_num >= u4_max_frm_num)
        {
            u4_next_frm_num -= u4_max_frm_num;
        }

        i4_frame_gaps++;
    }

    return OK;
}

/*!
 **************************************************************************
 * \if Function name : ih264d_create_pic_buffers \endif
 *
 * \brief
 *    This function creates Picture Buffers.
 *
 * \return
 *    0 on Success and -1 on error
 **************************************************************************
 */
WORD32 ih264d_create_pic_buffers(UWORD8 u1_num_of_buf,
                               dec_struct_t *ps_dec)
{
    struct pic_buffer_t *ps_pic_buf;
    UWORD8 i;
    UWORD32 u4_luma_size, u4_chroma_size;
    UWORD8 u1_frm = ps_dec->ps_cur_sps->u1_frame_mbs_only_flag;
    WORD32 j;
    UWORD32 u4_pic_buf_mem_used, u4_ref_buf_mem_used;
    UWORD8 *pu1_pic_buf_mem_base, *pu1_ref_buf_mem_base;

    u4_pic_buf_mem_used = 0;
    pu1_pic_buf_mem_base = ps_dec->ps_mem_tab[MEM_REC_PIC_BUF_MGR].pv_base;

    ps_dec->pv_disp_buf_mgr = (void *)(pu1_pic_buf_mem_base
                    + u4_pic_buf_mem_used);
    u4_pic_buf_mem_used += sizeof(disp_mgr_t);
    ih264_disp_mgr_init((disp_mgr_t *)ps_dec->pv_disp_buf_mgr);

    ps_dec->pv_pic_buf_mgr =
                    (void *)(pu1_pic_buf_mem_base + u4_pic_buf_mem_used);
    u4_pic_buf_mem_used += sizeof(buf_mgr_t) + ithread_get_mutex_lock_size();
    ih264_buf_mgr_init((buf_mgr_t *)ps_dec->pv_pic_buf_mgr);

    ps_pic_buf = (pic_buffer_t *)(pu1_pic_buf_mem_base + u4_pic_buf_mem_used);
    u4_pic_buf_mem_used += sizeof(struct pic_buffer_t)
                    * (H264_MAX_REF_PICS * 2);

    u4_luma_size = ps_dec->u2_frm_wd_y * ps_dec->u2_frm_ht_y;
    u4_chroma_size = ps_dec->u2_frm_wd_uv * ps_dec->u2_frm_ht_uv;

    {
        if(ps_dec->u4_share_disp_buf == 1)
        {
            /* In case of buffers getting shared between application and library
             there is no need of reference memtabs. Instead of setting the i4_size
             to zero, it is reduced to a small i4_size to ensure that changes
             in the code are minimal */
            if((ps_dec->u1_chroma_format == IV_YUV_420SP_UV)
                            || (ps_dec->u1_chroma_format == IV_YUV_420SP_VU)
                            || (ps_dec->u1_chroma_format == IV_YUV_420P))
            {
                u4_luma_size = 64;
            }

            if(ps_dec->u1_chroma_format == IV_YUV_420SP_UV)

            {
                u4_chroma_size = 64;
            }

        }
    }

    pu1_ref_buf_mem_base = ps_dec->ps_mem_tab[MEM_REC_REF_PIC].pv_base;
    u4_ref_buf_mem_used = 0;

    /* Allocate memory for refernce buffers */
    for(i = 0; i < u1_num_of_buf; i++)
    {
        UWORD32 u4_offset;
        WORD32 buf_ret;
        UWORD8 *pu1_luma, *pu1_chroma;

        pu1_luma = pu1_ref_buf_mem_base + u4_ref_buf_mem_used;
        u4_ref_buf_mem_used += u4_luma_size;
        pu1_chroma = pu1_ref_buf_mem_base + u4_ref_buf_mem_used;
        u4_ref_buf_mem_used += u4_chroma_size;

        /* Offset to the start of the pic from the top left corner of the frame
         buffer */

        if((0 == ps_dec->u4_share_disp_buf)
                        || (NULL == ps_dec->disp_bufs[i].buf[0]))
        {
            UWORD32 pad_len_h, pad_len_v;

            u4_offset = ps_dec->u2_frm_wd_y * (PAD_LEN_Y_V << 1) + PAD_LEN_Y_H;
            ps_pic_buf->pu1_buf1 = (UWORD8 *)(pu1_luma) + u4_offset;

            pad_len_h = MAX(PAD_LEN_UV_H, (PAD_LEN_Y_H >> 1));
            pad_len_v = MAX(PAD_LEN_UV_V, PAD_LEN_Y_V);

            u4_offset = ps_dec->u2_frm_wd_uv * pad_len_v + pad_len_h;

            ps_pic_buf->pu1_buf2 = (UWORD8 *)(pu1_chroma) + u4_offset;
            ps_pic_buf->pu1_buf3 = (UWORD8 *)(NULL) + u4_offset;

        }
        else
        {
            UWORD32 pad_len_h, pad_len_v;
            u4_offset = ps_dec->u2_frm_wd_y * (PAD_LEN_Y_V << 1) + PAD_LEN_Y_H;
            ps_pic_buf->pu1_buf1 = (UWORD8 *)ps_dec->disp_bufs[i].buf[0]
                            + u4_offset;

            ps_dec->disp_bufs[i].u4_ofst[0] = u4_offset;

            if(ps_dec->u1_chroma_format == IV_YUV_420P)
            {
                pad_len_h = MAX(PAD_LEN_UV_H * YUV420SP_FACTOR,
                                (PAD_LEN_Y_H >> 1));
                pad_len_v = MAX(PAD_LEN_UV_V, PAD_LEN_Y_V);

                u4_offset = ps_dec->u2_frm_wd_uv * pad_len_v + pad_len_h;
                ps_pic_buf->pu1_buf2 = (UWORD8 *)(pu1_chroma) + u4_offset;
                ps_pic_buf->pu1_buf3 = (UWORD8 *)(NULL) + u4_offset;

                ps_dec->disp_bufs[i].u4_ofst[1] = u4_offset;
                ps_dec->disp_bufs[i].u4_ofst[2] = u4_offset;

            }
            else
            {
                pad_len_h = MAX(PAD_LEN_UV_H * YUV420SP_FACTOR,
                                (PAD_LEN_Y_H >> 1));
                pad_len_v = MAX(PAD_LEN_UV_V, PAD_LEN_Y_V);

                u4_offset = ps_dec->u2_frm_wd_uv * pad_len_v + pad_len_h;
                ps_pic_buf->pu1_buf2 = (UWORD8 *)(ps_dec->disp_bufs[i].buf[1])
                                + u4_offset;
                ps_pic_buf->pu1_buf3 = (UWORD8 *)(ps_dec->disp_bufs[i].buf[1])
                                + u4_offset;

                ps_dec->disp_bufs[i].u4_ofst[1] = u4_offset;
                ps_dec->disp_bufs[i].u4_ofst[2] = u4_offset;

            }

        }

        ps_pic_buf->u2_frm_ht_y = ps_dec->u2_frm_ht_y;
        ps_pic_buf->u2_frm_ht_uv = ps_dec->u2_frm_ht_uv;
        ps_pic_buf->u2_frm_wd_y = ps_dec->u2_frm_wd_y;
        ps_pic_buf->u2_frm_wd_uv = ps_dec->u2_frm_wd_uv;

        ps_pic_buf->u1_pic_buf_id = i;

        buf_ret = ih264_buf_mgr_add((buf_mgr_t *)ps_dec->pv_pic_buf_mgr,
                                    ps_pic_buf, i);
        if(0 != buf_ret)
        {
            ps_dec->i4_error_code = ERROR_BUF_MGR;
            return ERROR_BUF_MGR;
        }

        ps_dec->apv_buf_id_pic_buf_map[i] = (void *)ps_pic_buf;
        ps_pic_buf++;
    }

    if((u4_ref_buf_mem_used > ps_dec->ps_mem_tab[MEM_REC_REF_PIC].u4_mem_size) ||
                    (u4_pic_buf_mem_used > ps_dec->ps_mem_tab[MEM_REC_PIC_BUF_MGR].u4_mem_size))
    {
        ps_dec->i4_error_code = ERROR_BUF_MGR;
        return ERROR_BUF_MGR;
    }

    if(1 == ps_dec->u4_share_disp_buf)
    {
        for(i = 0; i < u1_num_of_buf; i++)
            ps_dec->u4_disp_buf_mapping[i] = 1;
    }
    return OK;
}

/*!
 **************************************************************************
 * \if Function name : ih264d_get_memory_dec_params \endif
 *
 * \brief
 *    This function allocates memory required by Decoder.
 *
 * \param ps_dec: Pointer to dec_struct_t.
 *
 * \return
 *    Returns i4_status as returned by MemManager.
 *
 **************************************************************************
 */
WORD16 ih264d_get_memory_dec_params(dec_struct_t * ps_dec)
{
    struct MemReq s_MemReq;
    struct MemBlock *p_MemBlock;

    pred_info_t *ps_pred_frame;
    dec_mb_info_t *ps_frm_mb_info;
    dec_slice_struct_t *ps_dec_slice_buf;
    UWORD8 *pu1_dec_mb_map, *pu1_recon_mb_map;
    UWORD16 *pu2_slice_num_map;

    WORD16 *pi16_res_coeff;
    WORD16 i16_status = 0;
    UWORD8 uc_frmOrFld = (1 - ps_dec->ps_cur_sps->u1_frame_mbs_only_flag);
    UWORD16 u4_luma_wd = ps_dec->u2_frm_wd_y;
    UWORD16 u4_chroma_wd = ps_dec->u2_frm_wd_uv;
    WORD8 c_i = 0;
    dec_seq_params_t *ps_sps = ps_dec->ps_cur_sps;
    UWORD32 u4_total_mbs = ps_sps->u2_total_num_of_mbs << uc_frmOrFld;
    UWORD32 u4_wd_mbs = ps_dec->u2_frm_wd_in_mbs;
    UWORD32 u4_ht_mbs = ps_dec->u2_frm_ht_in_mbs;
    UWORD32 u4_blk_wd;
    UWORD32 ui_size = 0;
    UWORD32 u4_int_scratch_size = 0, u4_ref_pred_size = 0;
    UWORD8 *pu1_buf;

    ps_dec->ps_deblk_pic = ps_dec->ps_mem_tab[MEM_REC_DEBLK_MB_INFO].pv_base;
    memset(ps_dec->ps_deblk_pic, 0, ps_dec->ps_mem_tab[MEM_REC_DEBLK_MB_INFO].u4_mem_size);

    ps_dec->pu1_dec_mb_map = ps_dec->ps_mem_tab[MEM_REC_PARSE_MAP].pv_base;

    ps_dec->pu1_recon_mb_map = ps_dec->ps_mem_tab[MEM_REC_PROC_MAP].pv_base;

    ps_dec->pu2_slice_num_map =
                    ps_dec->ps_mem_tab[MEM_REC_SLICE_NUM_MAP].pv_base;

    ps_dec->ps_dec_slice_buf = ps_dec->ps_mem_tab[MEM_REC_SLICE_HDR].pv_base;
    memset(ps_dec->ps_mem_tab[MEM_REC_SLICE_HDR].pv_base, 0,
           ps_dec->ps_mem_tab[MEM_REC_SLICE_HDR].u4_mem_size);
    pu1_buf = (UWORD8 *)ps_dec->ps_dec_slice_buf;
    pu1_buf += sizeof(dec_slice_struct_t) * u4_total_mbs;
    ps_dec->pv_map_ref_idx_to_poc_buf = (void *)pu1_buf;

    ps_dec->ps_frm_mb_info = ps_dec->ps_mem_tab[MEM_REC_MB_INFO].pv_base;
    memset(ps_dec->ps_frm_mb_info, 0, ps_dec->ps_mem_tab[MEM_REC_MB_INFO].u4_mem_size);

    ps_dec->ps_pred = ps_dec->ps_mem_tab[MEM_REC_PRED_INFO].pv_base;

    ps_dec->pi2_coeff_data = ps_dec->ps_mem_tab[MEM_REC_COEFF_DATA].pv_base;

    ps_dec->pv_pic_tu_coeff_data = (void *)(ps_dec->pi2_coeff_data + MB_LUM_SIZE);

    /*scratch memory allocations*/
    {
        UWORD8 *pu1_scratch_mem_base;
        UWORD32 u4_scratch_mem_used;

        pu1_scratch_mem_base =
                        ps_dec->ps_mem_tab[MEM_REC_INTERNAL_SCRATCH].pv_base;
        u4_scratch_mem_used = 0;

        ps_dec->ppv_map_ref_idx_to_poc = (void *)(pu1_scratch_mem_base
                        + u4_scratch_mem_used);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);
        u4_scratch_mem_used += ((TOTAL_LIST_ENTRIES + PAD_MAP_IDX_POC)
                        * sizeof(void *));
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);
        memset(ps_dec->ppv_map_ref_idx_to_poc, 0, (TOTAL_LIST_ENTRIES + PAD_MAP_IDX_POC)
                                * sizeof(void *));

        ps_dec->p_cabac_ctxt_table_t = (void *)(pu1_scratch_mem_base
                        + u4_scratch_mem_used);
        u4_scratch_mem_used += (sizeof(bin_ctxt_model_t) * NUM_CABAC_CTXTS);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);

        ps_dec->ps_left_mb_ctxt_info = (void *)(pu1_scratch_mem_base
                        + u4_scratch_mem_used);
        u4_scratch_mem_used += sizeof(ctxt_inc_mb_info_t);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);

        ps_dec->pu4_defI_wts_ofsts = (void *)(pu1_scratch_mem_base
                        + u4_scratch_mem_used);
        u4_scratch_mem_used +=
                        sizeof(UWORD32)
                                        * (ps_sps->u1_num_ref_frames
                                                        * ps_sps->u1_num_ref_frames);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);

        ps_dec->pu1_ref_buff = pu1_scratch_mem_base + u4_scratch_mem_used + MAX_REF_BUF_SIZE;
        u4_scratch_mem_used += MAX_REF_BUF_SIZE * 2;
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);
        ps_dec->pi2_pred1 =
                        (void *)(pu1_scratch_mem_base + u4_scratch_mem_used);
        u4_scratch_mem_used += ((sizeof(WORD16)) * PRED_BUFFER_WIDTH
                        * PRED_BUFFER_HEIGHT * 2);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);

        ps_dec->pu1_temp_mc_buffer = (void *)(pu1_scratch_mem_base
                        + u4_scratch_mem_used);
        u4_scratch_mem_used += sizeof(UWORD8) * (MB_LUM_SIZE);

        ps_dec->ps_parse_mb_data = (void *)(pu1_scratch_mem_base
                        + u4_scratch_mem_used);
        u4_scratch_mem_used += sizeof(parse_pmbarams_t)
                        * (ps_dec->u1_recon_mb_grp);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);

        ps_dec->ps_parse_part_params = (void *)(pu1_scratch_mem_base
                        + u4_scratch_mem_used);
        u4_scratch_mem_used += sizeof(parse_part_params_t)
                        * ((ps_dec->u1_recon_mb_grp) << 4);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);

        ps_dec->ps_dpb_mgr->ps_init_dpb[0][0] =
                        (struct pic_buffer_t*)(pu1_scratch_mem_base
                                        + u4_scratch_mem_used);
        u4_scratch_mem_used += 2 * MAX_REF_BUFS * sizeof(struct pic_buffer_t);

        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);
        ps_dec->ps_dpb_mgr->ps_init_dpb[1][0] =
                        (struct pic_buffer_t*)(pu1_scratch_mem_base + u4_scratch_mem_used);
        u4_scratch_mem_used += 2 * MAX_REF_BUFS * sizeof(struct pic_buffer_t);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);
        ps_dec->pu4_mbaff_wt_mat = (UWORD32 *)(pu1_scratch_mem_base + u4_scratch_mem_used);

        u4_scratch_mem_used += (sizeof(UWORD32) * 3
                        * (MAX_FRAMES * MAX_FRAMES))
                        << 3;
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);

        ps_dec->pu4_wts_ofsts_mat = (UWORD32 *)(pu1_scratch_mem_base + u4_scratch_mem_used);
        u4_scratch_mem_used += sizeof(UWORD32) * 2 * 3
                        * (MAX_FRAMES * MAX_FRAMES);
        u4_scratch_mem_used = ALIGN64(u4_scratch_mem_used);
    }
    /********************************************************************/
    /* check whether deblk memory used is less than the scratch buffer  */
    /* and assign deblocking pointers in the the reference buffers      */
    /********************************************************************/
    {
        /************************************************************/
        /* Post allocation Initialisations                          */
        /************************************************************/
        memset(ps_dec->ppv_map_ref_idx_to_poc, 0,
               (TOTAL_LIST_ENTRIES + PAD_MAP_IDX_POC) * sizeof(void *));
        ps_dec->ppv_map_ref_idx_to_poc += OFFSET_MAP_IDX_POC;

        {
            ps_dec->ps_parse_cur_slice = &(ps_dec->ps_dec_slice_buf[0]);
            ps_dec->ps_decode_cur_slice = &(ps_dec->ps_dec_slice_buf[0]);
            ps_dec->ps_computebs_cur_slice = &(ps_dec->ps_dec_slice_buf[0]);

            ps_dec->ps_pred_start = ps_dec->ps_pred;
            ps_dec->u4_ref_buf_size = MAX_REF_BUF_SIZE;
        }

        {
            UWORD8 i;
            struct pic_buffer_t *ps_init_dpb;
            ps_init_dpb = ps_dec->ps_dpb_mgr->ps_init_dpb[0][0];
            for(i = 0; i < 2 * MAX_REF_BUFS; i++)
            {
                ps_init_dpb->pu1_buf1 = NULL;
                ps_init_dpb->u1_long_term_frm_idx = MAX_REF_BUFS + 1;
                ps_dec->ps_dpb_mgr->ps_init_dpb[0][i] = ps_init_dpb;
                ps_dec->ps_dpb_mgr->ps_mod_dpb[0][i] = ps_init_dpb;
                ps_init_dpb++;
            }

            ps_init_dpb = ps_dec->ps_dpb_mgr->ps_init_dpb[1][0];
            for(i = 0; i < 2 * MAX_REF_BUFS; i++)
            {
                ps_init_dpb->pu1_buf1 = NULL;
                ps_init_dpb->u1_long_term_frm_idx = MAX_REF_BUFS + 1;
                ps_dec->ps_dpb_mgr->ps_init_dpb[1][i] = ps_init_dpb;
                ps_dec->ps_dpb_mgr->ps_mod_dpb[1][i] = ps_init_dpb;
                ps_init_dpb++;
            }
        }
    }

    /*persistent memory allocations*/

    {
        UWORD8 *pu1_persitent_mem_base;
        UWORD32 u4_persistent_mem_used;

        pu1_persitent_mem_base =
                        ps_dec->ps_mem_tab[MEM_REC_INTERNAL_PERSIST].pv_base;
        u4_persistent_mem_used = 0;

        ps_dec->ps_deblk_top_mb = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ((u4_wd_mbs
                        * sizeof(deblkmb_neighbour_t)) << uc_frmOrFld);
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->ps_left_mvpred_addr = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += (sizeof(neighbouradd_t) << 2);
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->p_ctxt_inc_mb_map = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ((sizeof(ctxt_inc_mb_info_t))
                        * (((u4_wd_mbs + 1) << uc_frmOrFld) + 1));
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->ps_mv_p[0] = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += (sizeof(mv_pred_t) * ps_dec->u1_recon_mb_grp
                        * 16);
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->ps_mv_p[1] = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += (sizeof(mv_pred_t) * ps_dec->u1_recon_mb_grp
                        * 16);
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        {
            UWORD8 i;
            for(i = 0; i < MV_SCRATCH_BUFS; i++)
            {

                ps_dec->ps_mv_top_p[i] = (void *)(pu1_persitent_mem_base
                                + u4_persistent_mem_used);
                u4_persistent_mem_used += (sizeof(mv_pred_t)
                                * ps_dec->u1_recon_mb_grp * 4);
                u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

            }
        }

        {
            UWORD32 u4_numRows = MB_SIZE << 1;

            /* Allocate memory for ping, pong and left reconstruction buffers */
            u4_blk_wd = ((ps_dec->u1_recon_mb_grp << 4) >> 1) + 8;

            ps_dec->pu1_y_scratch[0] = (void *)(pu1_persitent_mem_base
                            + u4_persistent_mem_used);
            u4_persistent_mem_used += sizeof(UWORD8) * u4_numRows * u4_blk_wd;
            u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

            ps_dec->pu1_y_scratch[1] = (void *)(pu1_persitent_mem_base
                            + u4_persistent_mem_used);
            u4_persistent_mem_used += sizeof(UWORD8) * u4_numRows * u4_blk_wd;
            u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

            u4_numRows = BLK8x8SIZE << 1;
            u4_blk_wd = ((ps_dec->u1_recon_mb_grp << 3) >> 1) + 8;

            ps_dec->pu1_u_scratch[0] = (void *)(pu1_persitent_mem_base
                            + u4_persistent_mem_used);
            u4_persistent_mem_used += sizeof(UWORD8) * u4_numRows * u4_blk_wd;
            u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

            ps_dec->pu1_v_scratch[0] = (void *)(pu1_persitent_mem_base
                            + u4_persistent_mem_used);
            u4_persistent_mem_used += sizeof(UWORD8) * u4_numRows * u4_blk_wd;
            u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

            ps_dec->pu1_u_scratch[1] = (void *)(pu1_persitent_mem_base
                            + u4_persistent_mem_used);
            u4_persistent_mem_used += sizeof(UWORD8) * u4_numRows * u4_blk_wd;
            u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

            ps_dec->pu1_v_scratch[1] = (void *)(pu1_persitent_mem_base
                            + u4_persistent_mem_used);
            u4_persistent_mem_used += sizeof(UWORD8) * u4_numRows * u4_blk_wd;
            u4_persistent_mem_used += 32;
            u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        }

        ps_dec->pu1_y_intra_pred_line = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += sizeof(UWORD8) * ((u4_wd_mbs + 1) * MB_SIZE) * 2;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->pu1_u_intra_pred_line = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += sizeof(UWORD8) * ((u4_wd_mbs + 1) * MB_SIZE) * 2;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->pu1_v_intra_pred_line = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += sizeof(UWORD8) * ((u4_wd_mbs + 1) * MB_SIZE) * 2;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->ps_nbr_mb_row = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        if(ps_dec->u1_separate_parse)
        {
            u4_persistent_mem_used += sizeof(mb_neigbour_params_t)
                            * ((u4_wd_mbs + 1) * u4_ht_mbs);
            memset(ps_dec->ps_nbr_mb_row, 0, sizeof(mb_neigbour_params_t)
                            * ((u4_wd_mbs + 1) * u4_ht_mbs));
        }
        else
        {
            u4_persistent_mem_used += sizeof(mb_neigbour_params_t)
                            * ((u4_wd_mbs + 1) << uc_frmOrFld);
            memset(ps_dec->ps_nbr_mb_row, 0, sizeof(mb_neigbour_params_t)
                            * ((u4_wd_mbs + 1) << uc_frmOrFld));

        }
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);
        ps_dec->s_pad_mgr.pu1_row_y = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ps_dec->u2_frm_wd_y;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->s_pad_mgr.pu1_row_u = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ps_dec->u2_frm_wd_uv;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->s_pad_mgr.pu1_row_v = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ps_dec->u2_frm_wd_uv;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->s_pad_mgr.pu1_mb_y = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ((MB_SIZE + 4) << uc_frmOrFld) * PAD_LEN_Y_H;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->s_pad_mgr.pu1_mb_u = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ((BLK8x8SIZE + 2) << uc_frmOrFld)
                        * PAD_LEN_UV_H;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);

        ps_dec->s_pad_mgr.pu1_mb_v = (void *)(pu1_persitent_mem_base
                        + u4_persistent_mem_used);
        u4_persistent_mem_used += ((BLK8x8SIZE + 2) << uc_frmOrFld)
                        * PAD_LEN_UV_H;
        u4_persistent_mem_used = ALIGN64(u4_persistent_mem_used);
    }

    /*Post allocation initializations*/
    memset(ps_dec->pu1_y_intra_pred_line, 0,
           sizeof(UWORD8) * u4_luma_wd + PAD_LEN_Y_H);
    memset(ps_dec->pu1_u_intra_pred_line, 0,
           sizeof(UWORD8) * u4_chroma_wd + PAD_LEN_UV_H);
    memset(ps_dec->pu1_v_intra_pred_line, 0,
           sizeof(UWORD8) * u4_chroma_wd + PAD_LEN_UV_H);

    /* 0th entry of CtxtIncMbMap will be always be containing default values
     for CABAC context representing MB not available */
    ps_dec->p_ctxt_inc_mb_map += 1;
    /* Post allocation Increment Actions */

    /***************************************************************************/
    /*Initialize cabac context pointers for every SE that has fixed contextIdx */
    /***************************************************************************/
    {
        bin_ctxt_model_t * const p_cabac_ctxt_table_t =
                        ps_dec->p_cabac_ctxt_table_t;
        bin_ctxt_model_t * * p_coeff_abs_level_minus1_t =
                        ps_dec->p_coeff_abs_level_minus1_t;
        bin_ctxt_model_t * * p_cbf_t = ps_dec->p_cbf_t;

        ps_dec->p_mb_field_dec_flag_t = p_cabac_ctxt_table_t
                        + MB_FIELD_DECODING_FLAG;
        ps_dec->p_prev_intra4x4_pred_mode_flag_t = p_cabac_ctxt_table_t
                        + PREV_INTRA4X4_PRED_MODE_FLAG;
        ps_dec->p_rem_intra4x4_pred_mode_t = p_cabac_ctxt_table_t
                        + REM_INTRA4X4_PRED_MODE;
        ps_dec->p_intra_chroma_pred_mode_t = p_cabac_ctxt_table_t
                        + INTRA_CHROMA_PRED_MODE;
        ps_dec->p_mb_qp_delta_t = p_cabac_ctxt_table_t + MB_QP_DELTA;
        ps_dec->p_ref_idx_t = p_cabac_ctxt_table_t + REF_IDX;
        ps_dec->p_mvd_x_t = p_cabac_ctxt_table_t + MVD_X;
        ps_dec->p_mvd_y_t = p_cabac_ctxt_table_t + MVD_Y;
        p_cbf_t[0] = p_cabac_ctxt_table_t + CBF + 0;
        p_cbf_t[1] = p_cabac_ctxt_table_t + CBF + 4;
        p_cbf_t[2] = p_cabac_ctxt_table_t + CBF + 8;
        p_cbf_t[3] = p_cabac_ctxt_table_t + CBF + 12;
        p_cbf_t[4] = p_cabac_ctxt_table_t + CBF + 16;
        ps_dec->p_cbp_luma_t = p_cabac_ctxt_table_t + CBP_LUMA;
        ps_dec->p_cbp_chroma_t = p_cabac_ctxt_table_t + CBP_CHROMA;

        p_coeff_abs_level_minus1_t[LUMA_DC_CTXCAT] = p_cabac_ctxt_table_t
                        + COEFF_ABS_LEVEL_MINUS1 + COEFF_ABS_LEVEL_CAT_0_OFFSET;

        p_coeff_abs_level_minus1_t[LUMA_AC_CTXCAT] = p_cabac_ctxt_table_t
                        + COEFF_ABS_LEVEL_MINUS1 + COEFF_ABS_LEVEL_CAT_1_OFFSET;

        p_coeff_abs_level_minus1_t[LUMA_4X4_CTXCAT] = p_cabac_ctxt_table_t
                        + COEFF_ABS_LEVEL_MINUS1 + COEFF_ABS_LEVEL_CAT_2_OFFSET;

        p_coeff_abs_level_minus1_t[CHROMA_DC_CTXCAT] = p_cabac_ctxt_table_t
                        + COEFF_ABS_LEVEL_MINUS1 + COEFF_ABS_LEVEL_CAT_3_OFFSET;

        p_coeff_abs_level_minus1_t[CHROMA_AC_CTXCAT] = p_cabac_ctxt_table_t
                        + COEFF_ABS_LEVEL_MINUS1 + COEFF_ABS_LEVEL_CAT_4_OFFSET;

        p_coeff_abs_level_minus1_t[LUMA_8X8_CTXCAT] = p_cabac_ctxt_table_t
                        + COEFF_ABS_LEVEL_MINUS1_8X8
                        + COEFF_ABS_LEVEL_CAT_5_OFFSET;

        /********************************************************/
        /* context for the high profile related syntax elements */
        /* This is maintained seperately in s_high_profile     */
        /********************************************************/
        {

            ps_dec->s_high_profile.ps_transform8x8_flag = p_cabac_ctxt_table_t
                            + TRANSFORM_SIZE_8X8_FLAG;

            ps_dec->s_high_profile.ps_sigcoeff_8x8_frame = p_cabac_ctxt_table_t
                            + SIGNIFICANT_COEFF_FLAG_8X8_FRAME;

            ps_dec->s_high_profile.ps_last_sigcoeff_8x8_frame =
                            p_cabac_ctxt_table_t
                                            + LAST_SIGNIFICANT_COEFF_FLAG_8X8_FRAME;

            ps_dec->s_high_profile.ps_coeff_abs_levelminus1 =
                            p_cabac_ctxt_table_t + COEFF_ABS_LEVEL_MINUS1_8X8;

            ps_dec->s_high_profile.ps_sigcoeff_8x8_field = p_cabac_ctxt_table_t
                            + SIGNIFICANT_COEFF_FLAG_8X8_FIELD;

            ps_dec->s_high_profile.ps_last_sigcoeff_8x8_field =
                            p_cabac_ctxt_table_t
                                            + LAST_SIGNIFICANT_COEFF_FLAG_8X8_FIELD;

        }

    }
    return (i16_status);
}

/*!
 **************************************************************************
 * \if Function name : ih264d_create_mv_bank \endif
 *
 * \brief
 *    This function creates MV bank.
 *
 * \param memType  : Type of memory being handled
 *                   0: Display Buffer
 *                   1: Decoder Buffer
 *                   2: Internal Buffer
 * \param u1_num_of_buf: Number of decode or display buffers.
 * \param u4_wd : Frame width.
 * \param u4_ht : Frame Height.
 * \param ps_pic_buf_api : Pointer to Picture Buffer API.
 * \param ih264d_dec_mem_manager  : Memory manager utility supplied by system.
 *
 * \return
 *    0 on Success and -1 on error
 *
 **************************************************************************
 */
WORD32 ih264d_create_mv_bank(void *pv_dec,
                             UWORD32 ui_width,
                             UWORD32 ui_height)
{
    UWORD8  i;
    UWORD32 col_flag_buffer_size, mvpred_buffer_size;
    UWORD8 *pu1_mv_buf_mgr_base, *pu1_mv_bank_base;
    UWORD32 u4_mv_buf_mgr_mem_used, u4_mv_bank_mem_used;
    col_mv_buf_t *ps_col_mv;
    mv_pred_t *ps_mv;
    UWORD8 *pu1_col_zero_flag_buf;
    dec_struct_t *ps_dec = (dec_struct_t *)pv_dec;
    WORD32 buf_ret;
    UWORD32 u4_num_bufs;

    pu1_mv_buf_mgr_base = ps_dec->ps_mem_tab[MEM_REC_MV_BUF_MGR].pv_base;
    u4_mv_buf_mgr_mem_used = 0;
    col_flag_buffer_size = ((ui_width * ui_height) >> 4);

    pu1_mv_bank_base = ps_dec->ps_mem_tab[MEM_REC_MVBANK].pv_base;
    u4_mv_bank_mem_used = 0;
    mvpred_buffer_size = sizeof(mv_pred_t)
                    * ((ui_width * (ui_height + PAD_MV_BANK_ROW)) >> 4);

    ps_dec->pv_mv_buf_mgr = (void *)(pu1_mv_buf_mgr_base + u4_mv_buf_mgr_mem_used);
    u4_mv_buf_mgr_mem_used += sizeof(buf_mgr_t) + ithread_get_mutex_lock_size();
    ih264_buf_mgr_init((buf_mgr_t *)ps_dec->pv_mv_buf_mgr);

    ps_col_mv = (col_mv_buf_t *)(pu1_mv_buf_mgr_base + u4_mv_buf_mgr_mem_used);
    u4_mv_buf_mgr_mem_used += sizeof(col_mv_buf_t) * (H264_MAX_REF_PICS * 2);
    u4_mv_buf_mgr_mem_used = ALIGN128(u4_mv_buf_mgr_mem_used);

    u4_num_bufs = ih264d_get_numbuf_mv_bank(ps_dec, ui_width, ui_height);

    u4_num_bufs = MIN(u4_num_bufs, ps_dec->u1_pic_bufs);

    for(i = 0 ; i < u4_num_bufs ; i++)
    {
        pu1_col_zero_flag_buf = pu1_mv_buf_mgr_base + u4_mv_buf_mgr_mem_used;
        u4_mv_buf_mgr_mem_used +=  col_flag_buffer_size;

        ps_mv = (mv_pred_t *)(pu1_mv_bank_base + u4_mv_bank_mem_used);
        u4_mv_bank_mem_used +=  mvpred_buffer_size;

        memset(ps_mv, 0, ((ui_width*OFFSET_MV_BANK_ROW) >> 4) * sizeof(mv_pred_t));
        ps_mv += (ui_width*OFFSET_MV_BANK_ROW) >> 4;

        ps_col_mv->pv_col_zero_flag = (void *)pu1_col_zero_flag_buf;
        ps_col_mv->pv_mv = (void *)ps_mv;
        buf_ret = ih264_buf_mgr_add((buf_mgr_t *)ps_dec->pv_mv_buf_mgr, ps_col_mv, i);
        if(0 != buf_ret)
        {
            ps_dec->i4_error_code = ERROR_BUF_MGR;
            return ERROR_BUF_MGR;
        }
        ps_col_mv++;
    }

    if((u4_mv_buf_mgr_mem_used > ps_dec->ps_mem_tab[MEM_REC_MV_BUF_MGR].u4_mem_size) ||
                    (u4_mv_bank_mem_used > ps_dec->ps_mem_tab[MEM_REC_MVBANK].u4_mem_size))
    {
        ps_dec->i4_error_code = ERROR_BUF_MGR;
        return ERROR_BUF_MGR;
    }

    return OK;

}


void ih264d_unpack_coeff4x4_dc_4x4blk(tu_sblk4x4_coeff_data_t *ps_tu_4x4,
                                      WORD16 *pi2_out_coeff_data,
                                      UWORD8 *pu1_inv_scan)
{
    UWORD16 u2_sig_coeff_map = ps_tu_4x4->u2_sig_coeff_map;
    WORD32 idx;
    WORD16 *pi2_coeff_data = &ps_tu_4x4->ai2_level[0];

    while(u2_sig_coeff_map)
    {
        idx = CLZ(u2_sig_coeff_map);

        idx = 31 - idx;
        RESET_BIT(u2_sig_coeff_map,idx);

        idx = pu1_inv_scan[idx];
        pi2_out_coeff_data[idx] = *pi2_coeff_data++;

    }
}
