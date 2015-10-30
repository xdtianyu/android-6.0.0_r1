/*
 * Copyright (C) 2009 The Android Open Source Project
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

package vogar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ModeId {
    /** ART (works >= L) */
    DEVICE,
    /** Dalvik (works <= KitKat) */
    DEVICE_DALVIK,
    /** ART (for KitKat only, use DEVICE_DALVIK otherwise) */
    DEVICE_ART_KITKAT,

    /** ART (works >= L) */
    HOST,
    /** Dalvik (works <= KitKat) */
    HOST_DALVIK,
    /** ART for KitKat only */
    HOST_ART_KITKAT,
    /** Local Java */
    JVM,
    /** Device, execution as an Android app with Zygote */
    ACTIVITY,
    /** Device using app_process binary */
    APP_PROCESS;

    // $BOOTCLASSPATH defined by system/core/rootdir/init.rc
    private static final String[] DALVIK_DEVICE_JARS = new String[] {"core"};
    private static final String[] ART_DEVICE_JARS = new String[] {"core-libart"};
    private static final String[] COMMON_DEVICE_JARS = new String[] {
            "conscrypt",
            "okhttp",
            "core-junit",
            "bouncycastle",
            "ext",
            "framework",
            "telephony-common",
            "mms-common",
            "framework",
            "android.policy",
            "services",
            "apache-xml"};

    private static final String[] DALVIK_HOST_JARS = new String[] {"core-hostdex"};
    private static final String[] ART_HOST_JARS = new String[] {"core-libart-hostdex"};
    private static final String[] COMMON_HOST_JARS = new String[] {
            "conscrypt-hostdex",
            "okhttp-hostdex",
            "bouncycastle-hostdex",
            "apache-xml-hostdex",
    };

    public boolean acceptsVmArgs() {
        return this != ACTIVITY;
    }

    /**
     * Returns {@code true} if execution happens on the local machine. e.g. host-mode android or a
     * JVM.
     */
    public boolean isLocal() {
        return isHost() || this == ModeId.JVM;
    }

    /** Returns {@code true} if execution takes place with a host-mode Android runtime */
    public boolean isHost() {
        return this == HOST || this == HOST_DALVIK || this == ModeId.HOST_ART_KITKAT;
    }

    /** Returns {@code true} if execution takes place with a device-mode Android runtime */
    public boolean isDevice() {
        return this == ModeId.DEVICE || this == ModeId.DEVICE_ART_KITKAT
                || this == ModeId.DEVICE_DALVIK || this == ModeId.APP_PROCESS;
    }

    public boolean requiresAndroidSdk() {
        return this != JVM;
    }

    public boolean supportsVariant(Variant variant) {
        return (variant == Variant.X32)
                || ((this == HOST || this == DEVICE) && (variant == Variant.X64));
    }

    /** The default command to use for the mode unless overridden by --vm-command */
    public String defaultVmCommand(Variant variant) {
        if (!supportsVariant(variant)) {
            throw new AssertionError("Unsupported variant: " + variant + " for " + this);
        }
        switch (this) {
            case DEVICE:
            case HOST:
                if (variant == Variant.X32) {
                    return "dalvikvm32";
                } else {
                    return "dalvikvm64";
                }
            case DEVICE_DALVIK:
            case DEVICE_ART_KITKAT:
            case HOST_DALVIK:
            case HOST_ART_KITKAT:
                return "dalvikvm";
            case JVM:
                return "java";
            case APP_PROCESS:
                return "app_process";
            case ACTIVITY:
                return null;
            default:
                throw new IllegalArgumentException("Unknown mode: " + this);
        }
    }

    /**
     * Return the names of jars required to compile in this mode when android.jar is not being used.
     * Also used to generated the classpath in HOST* and DEVICE* modes.
     */
    public String[] getJarNames() {
        List<String> jarNames = new ArrayList<String>();
        switch (this) {
            case DEVICE_DALVIK:
                jarNames.addAll(Arrays.asList(DALVIK_DEVICE_JARS));
                jarNames.addAll(Arrays.asList(COMMON_DEVICE_JARS));
                break;
            case ACTIVITY:
            case APP_PROCESS:
            case DEVICE:
            case DEVICE_ART_KITKAT:
                jarNames.addAll(Arrays.asList(ART_DEVICE_JARS));
                jarNames.addAll(Arrays.asList(COMMON_DEVICE_JARS));
                break;
            case HOST_DALVIK:
                jarNames.addAll(Arrays.asList(DALVIK_HOST_JARS));
                jarNames.addAll(Arrays.asList(COMMON_HOST_JARS));
                break;
            case HOST:
            case HOST_ART_KITKAT:
                jarNames.addAll(Arrays.asList(ART_HOST_JARS));
                jarNames.addAll(Arrays.asList(COMMON_HOST_JARS));
                break;
            default:
                throw new IllegalArgumentException("Unsupported mode: " + this);
        }
        return jarNames.toArray(new String[jarNames.size()]);
    }
}
