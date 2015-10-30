/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.compatibility.common.devicesetup;

/**
 * Constants for device info attributes to be sent as instrumentation keys.
 */
public interface DeviceInfoConstants {

    public static final String BUILD_ABI = "buildAbi";
    public static final String BUILD_ABI2 = "buildAbi2";
    public static final String BUILD_BOARD = "buildBoard";
    public static final String BUILD_BRAND = "buildBrand";
    public static final String BUILD_DEVICE = "buildDevice";
    public static final String BUILD_FINGERPRINT = "buildFingerprint";
    public static final String BUILD_ID = "buildId";
    public static final String BUILD_MANUFACTURER = "buildManufacturer";
    public static final String BUILD_MODEL = "buildModel";
    public static final String BUILD_TAGS = "buildTags";
    public static final String BUILD_TYPE = "buildType";
    public static final String BUILD_VERSION = "buildVersion";

    public static final String FEATURES = "features";

    public static final String GRAPHICS_RENDERER = "graphicsRenderer";
    public static final String GRAPHICS_VENDOR = "graphicsVendor";

    public static final String IMEI = "imei";
    public static final String IMSI = "imsi";

    public static final String KEYPAD = "keypad";

    public static final String LOCALES = "locales";

    public static final String MULTI_USER = "multiUser";

    public static final String NAVIGATION = "navigation";
    public static final String NETWORK = "network";

    public static final String OPEN_GL_ES_VERSION = "openGlEsVersion";
    public static final String OPEN_GL_EXTENSIONS = "openGlExtensions";
    public static final String OPEN_GL_COMPRESSED_TEXTURE_FORMATS =
            "openGlCompressedTextureFormats";

    public static final String PARTITIONS = "partitions";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String PROCESSES = "processes";
    public static final String PRODUCT_NAME = "productName";

    public static final String RESOLUTION = "resolution";

    public static final String SCREEN_DENSITY = "screenDensity";
    public static final String SCREEN_DENSITY_BUCKET = "screenDensityBucket";
    public static final String SCREEN_DENSITY_X = "screenDensityX";
    public static final String SCREEN_DENSITY_Y = "screenDensityY";
    public static final String SCREEN_SIZE = "screenSize";
    public static final String SERIAL_NUMBER = "deviceId";
    public static final String STORAGE_DEVICES = "storageDevices";
    public static final String SYS_LIBRARIES = "systemLibraries";

    public static final String TOUCH = "touch";

    public static final String VERSION_RELEASE = "versionRelease";
    public static final String VERSION_SDK_INT = "versionSdkInt";
}
