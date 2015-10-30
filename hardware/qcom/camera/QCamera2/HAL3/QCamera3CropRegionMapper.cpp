/* Copyright (c) 2015, The Linux Foundataion. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are
* met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above
*       copyright notice, this list of conditions and the following
*       disclaimer in the documentation and/or other materials provided
*       with the distribution.
*     * Neither the name of The Linux Foundation nor the names of its
*       contributors may be used to endorse or promote products derived
*       from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
* ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
* BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
* IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/


#define ATRACE_TAG ATRACE_TAG_CAMERA
#define LOG_TAG "QCamera3CropRegionMapper"

#include "QCamera3CropRegionMapper.h"
#include "QCamera3HWI.h"

using namespace android;

namespace qcamera {

/*===========================================================================
 * FUNCTION   : QCamera3CropRegionMapper
 *
 * DESCRIPTION: Constructor
 *
 * PARAMETERS : None
 *
 * RETURN     : None
 *==========================================================================*/
QCamera3CropRegionMapper::QCamera3CropRegionMapper()
        : mSensorW(0),
          mSensorH(0),
          mActiveArrayW(0),
          mActiveArrayH(0),
          mSensorCropW(0),
          mSensorCropH(0),
          mSensorScale(1.0)
{
}

/*===========================================================================
 * FUNCTION   : ~QCamera3CropRegionMapper
 *
 * DESCRIPTION: destructor
 *
 * PARAMETERS : none
 *
 * RETURN     : none
 *==========================================================================*/

QCamera3CropRegionMapper::~QCamera3CropRegionMapper()
{
}

/*===========================================================================
 * FUNCTION   : update
 *
 * DESCRIPTION: update sensor active array size and sensor output size
 *
 * PARAMETERS :
 *   @active_array_w : active array width
 *   @active_array_h : active array height
 *   @sensor_w       : sensor output width
 *   @sensor_h       : sensor output height
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3CropRegionMapper::update(uint32_t active_array_w,
        uint32_t active_array_h, uint32_t sensor_w,
        uint32_t sensor_h)
{
    // Sanity check
    if (active_array_w == 0 || active_array_h == 0 ||
            sensor_w == 0 || sensor_h == 0) {
        ALOGE("%s: active_array size and sensor output size must be non zero",
                __func__);
        return;
    }
    if (active_array_w < sensor_w || active_array_h < sensor_h) {
        ALOGE("%s: invalid input: active_array [%d, %d], sensor size [%d, %d]",
                __func__, active_array_w, active_array_h, sensor_w, sensor_h);
        return;
    }
    mSensorW = sensor_w;
    mSensorH = sensor_h;
    mActiveArrayW = active_array_w;
    mActiveArrayH = active_array_h;

    // Derive mapping from active array to sensor output size
    // Assume the sensor first crops top/bottom, or left/right, (not both),
    // before doing downscaling.
    // sensor_w = (active_array_w - 2 * crop_w) / scale;
    // sensor_h = (active_array_h - 2 * crop_y) / scale;
    float scale_w = 1.0 * active_array_w / sensor_w;
    float scale_h = 1.0 * active_array_h / sensor_h;
    float scale = MIN(scale_w, scale_h);
    uint32_t crop_w = 0, crop_h = 0;
    if (scale_w > scale_h) {
        crop_w = (active_array_w - sensor_w * active_array_h / sensor_h)/2;
    } else {
        crop_h = (active_array_h - sensor_h * active_array_w / sensor_w)/2;
    }
    mSensorCropW = crop_w;
    mSensorCropH = crop_h;
    mSensorScale = scale;

    ALOGI("%s: active_array: %d x %d, sensor size %d x %d", __func__,
            mActiveArrayW, mActiveArrayH, mSensorW, mSensorH);
    ALOGI("%s: mSensorCrop is [%d, %d], and scale is %f", __func__,
            mSensorCropW, mSensorCropH, mSensorScale);
}

/*===========================================================================
 * FUNCTION   : toActiveArray
 *
 * DESCRIPTION: Map crop rectangle from sensor output space to active array space
 *
 * PARAMETERS :
 *   @crop_left   : x coordinate of top left corner of rectangle
 *   @crop_top    : y coordinate of top left corner of rectangle
 *   @crop_width  : width of rectangle
 *   @crop_height : height of rectangle
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3CropRegionMapper::toActiveArray(int32_t& crop_left, int32_t& crop_top,
        int32_t& crop_width, int32_t& crop_height)
{
    if (mSensorW == 0 || mSensorH == 0 ||
            mActiveArrayW == 0 || mActiveArrayH == 0) {
        ALOGE("%s: sensor/active array sizes are not initialized!", __func__);
        return;
    }

    // For each crop region in sensor space, it's mapping to active array space is:
    // left = left' * scale + crop_w;
    // top  = top' + scale * crop_h;
    // width = width' * scale;
    // height = height' * scale;
    crop_left = mSensorCropW + crop_left * mSensorScale;
    crop_top = mSensorCropH + crop_top * mSensorScale;
    crop_width = crop_width * mSensorScale;
    crop_height = crop_height * mSensorScale;

    boundToSize(crop_left, crop_top, crop_width, crop_height,
            mActiveArrayW, mActiveArrayH);
}

/*===========================================================================
 * FUNCTION   : toSensor
 *
 * DESCRIPTION: Map crop rectangle from active array space to sensor output space
 *
 * PARAMETERS :
 *   @crop_left   : x coordinate of top left corner of rectangle
 *   @crop_top    : y coordinate of top left corner of rectangle
 *   @crop_width  : width of rectangle
 *   @crop_height : height of rectangle
 *
 * RETURN     : none
 *==========================================================================*/

void QCamera3CropRegionMapper::toSensor(int32_t& crop_left, int32_t& crop_top,
        int32_t& crop_width, int32_t& crop_height)
{
    if (mSensorW == 0 || mSensorH == 0 ||
            mActiveArrayW == 0 || mActiveArrayH == 0) {
        ALOGE("%s: sensor/active array sizes are not initialized!", __func__);
        return;
    }

    // For each crop region in active array space, it's mapping to sensor space is:
    // left' = (left - crop_w) / scale;
    // top'  = (top - crop_h) / scale;
    // width' = width / scale;
    // height' = height / scale;
    crop_left = (crop_left - mSensorCropW) / mSensorScale;
    crop_top = (crop_top - mSensorCropH) / mSensorScale;
    crop_width = crop_width / mSensorScale;
    crop_height = crop_height / mSensorScale;

    CDBG("%s: before bounding left %d, top %d, width %d, height %d",
        __func__, crop_left, crop_top, crop_width, crop_height);
    boundToSize(crop_left, crop_top, crop_width, crop_height,
            mSensorW, mSensorH);
    CDBG("%s: after bounding left %d, top %d, width %d, height %d",
        __func__, crop_left, crop_top, crop_width, crop_height);
}

/*===========================================================================
 * FUNCTION   : boundToSize
 *
 * DESCRIPTION: Bound a particular rectangle inside a bounding box
 *
 * PARAMETERS :
 *   @left    : x coordinate of top left corner of rectangle
 *   @top     : y coordinate of top left corner of rectangle
 *   @width   : width of rectangle
 *   @height  : height of rectangle
 *   @bound_w : width of bounding box
 *   @bound_y : height of bounding box
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3CropRegionMapper::boundToSize(int32_t& left, int32_t& top,
            int32_t& width, int32_t& height, int32_t bound_w, int32_t bound_h)
{
    if (left < 0) {
        left = 0;
    }
    if (top < 0) {
        top = 0;
    }

    if ((left + width) > bound_w) {
        width = bound_w - left;
    }
    if ((top + height) > bound_h) {
        height = bound_h - top;
    }
}

}; //end namespace android
