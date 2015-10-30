/*
 * Copyright 2014, The Android Open Source Project
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

package com.android.managedprovisioning;

import android.content.Context;
import android.accounts.Account;
import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.Locale;

/**
 * Provisioning Parameters for Device owner and Profile owner Provisioning.
 */
public class ProvisioningParams implements Parcelable {
    public static final long DEFAULT_LOCAL_TIME = -1;
    public static final boolean DEFAULT_WIFI_HIDDEN = false;
    public static final boolean DEFAULT_LEAVE_ALL_SYSTEM_APPS_ENABLED = false;
    public static final int DEFAULT_WIFI_PROXY_PORT = 0;
    public static final boolean DEFAULT_EXTRA_PROVISIONING_SKIP_ENCRYPTION = false;
    // Always download packages if no minimum version given.
    public static final int DEFAULT_MINIMUM_VERSION = Integer.MAX_VALUE;

    public String timeZone;
    public long localTime = DEFAULT_LOCAL_TIME;
    public Locale locale;

    // Intent extra used internally for passing data between activities and service.
    /* package */ static final String EXTRA_PROVISIONING_PARAMS = "provisioningParams";

    public static class WifiInfo {
        public String ssid;
        public boolean hidden = DEFAULT_WIFI_HIDDEN;
        public String securityType;
        public String password;
        public String proxyHost;
        public int proxyPort = DEFAULT_WIFI_PROXY_PORT;
        public String proxyBypassHosts;
        public String pacUrl;

        public void writeToParcel(Parcel out) {
            out.writeString(ssid);
            out.writeInt(hidden ? 1 : 0);
            out.writeString(securityType);
            out.writeString(password);
            out.writeString(proxyHost);
            out.writeInt(proxyPort);
            out.writeString(proxyBypassHosts);
            out.writeString(pacUrl);
        }

        public void readFromParcel(Parcel in) {
            ssid = in.readString();
            hidden = in.readInt() == 1;
            securityType = in.readString();
            password = in.readString();
            proxyHost = in.readString();
            proxyPort = in.readInt();
            proxyBypassHosts = in.readString();
            pacUrl = in.readString();
        }
    }
    public WifiInfo wifiInfo = new WifiInfo();

    // At least one one of deviceAdminPackageName and deviceAdminComponentName should be non-null
    public String deviceAdminPackageName; // Package name of the device admin package.
    public ComponentName deviceAdminComponentName;
    public ComponentName deviceInitializerComponentName;
    public Account accountToMigrate;

    private ComponentName inferedDeviceAdminComponentName;

    public static class PackageDownloadInfo {
        // Url where the package (.apk) can be downloaded from
        public String location;
        // Cookie header for http request
        public String cookieHeader;
        // One of the following two checksums should be non empty.
        // SHA-256 or SHA-1 hash of the .apk file, or empty array if not used.
        public byte[] packageChecksum = new byte[0];
        // SHA-256 hash of the signature in the .apk file, or empty array if not used.
        public byte[] signatureChecksum = new byte[0];
        public int minVersion;
        // If this is false, packageChecksum can only be SHA-256 hash, otherwise SHA-1 is also
        // supported.
        public boolean packageChecksumSupportsSha1;

        public void writeToParcel(Parcel out) {
            out.writeInt(minVersion);
            out.writeString(location);
            out.writeString(cookieHeader);
            out.writeByteArray(packageChecksum);
            out.writeByteArray(signatureChecksum);
            out.writeInt(packageChecksumSupportsSha1 ? 1 : 0);
        }

        public void readFromParcel(Parcel in) {
            minVersion = in.readInt();
            location = in.readString();
            cookieHeader = in.readString();
            packageChecksum = in.createByteArray();
            signatureChecksum = in.createByteArray();
            packageChecksumSupportsSha1 = in.readInt() == 1;
        }
    }
    public PackageDownloadInfo deviceAdminDownloadInfo = new PackageDownloadInfo();
    public PackageDownloadInfo deviceInitializerDownloadInfo  = new PackageDownloadInfo();

    public PersistableBundle adminExtrasBundle;

    public boolean startedByNfc; // True iff provisioning flow was started by Nfc bump.

    public boolean leaveAllSystemAppsEnabled;
    public boolean skipEncryption;

    public String inferDeviceAdminPackageName() {
        if (deviceAdminComponentName != null) {
            return deviceAdminComponentName.getPackageName();
        }
        return deviceAdminPackageName;
    }

    public String getDeviceInitializerPackageName() {
        if (deviceInitializerComponentName != null) {
            return deviceInitializerComponentName.getPackageName();
        }
        return null;
    }

    // This should not be called if the app has not been installed yet.
    ComponentName inferDeviceAdminComponentName(Context c)
            throws Utils.IllegalProvisioningArgumentException {
        if (inferedDeviceAdminComponentName == null) {
            inferedDeviceAdminComponentName = Utils.findDeviceAdmin(
                    deviceAdminPackageName, deviceAdminComponentName, c);
        }
        return inferedDeviceAdminComponentName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(timeZone);
        out.writeLong(localTime);
        out.writeSerializable(locale);
        wifiInfo.writeToParcel(out);
        out.writeString(deviceAdminPackageName);
        out.writeParcelable(deviceAdminComponentName, 0 /* default */);
        deviceAdminDownloadInfo.writeToParcel(out);
        out.writeParcelable(deviceInitializerComponentName, 0 /* default */);
        deviceInitializerDownloadInfo.writeToParcel(out);
        out.writeParcelable(adminExtrasBundle, 0 /* default */);
        out.writeInt(startedByNfc ? 1 : 0);
        out.writeInt(leaveAllSystemAppsEnabled ? 1 : 0);
        out.writeInt(skipEncryption ? 1 : 0);
        out.writeParcelable(accountToMigrate, 0 /* default */);
    }

    public static final Parcelable.Creator<ProvisioningParams> CREATOR
        = new Parcelable.Creator<ProvisioningParams>() {
        @Override
        public ProvisioningParams createFromParcel(Parcel in) {
            ProvisioningParams params = new ProvisioningParams();
            params.timeZone = in.readString();
            params.localTime = in.readLong();
            params.locale = (Locale) in.readSerializable();
            params.wifiInfo.readFromParcel(in);
            params.deviceAdminPackageName = in.readString();
            params.deviceAdminComponentName = (ComponentName)
                    in.readParcelable(null /* use default classloader */);
            params.deviceAdminDownloadInfo.readFromParcel(in);
            params.deviceInitializerComponentName = (ComponentName)
                    in.readParcelable(null /* use default classloader */);
            params.deviceInitializerDownloadInfo.readFromParcel(in);
            params.adminExtrasBundle = in.readParcelable(null /* use default classloader */);
            params.startedByNfc = in.readInt() == 1;
            params.leaveAllSystemAppsEnabled = in.readInt() == 1;
            params.skipEncryption = in.readInt() == 1;
            params.accountToMigrate =
                    (Account) in.readParcelable(null /* use default classloader */);
            return params;
        }

        @Override
        public ProvisioningParams[] newArray(int size) {
            return new ProvisioningParams[size];
        }
    };
}
