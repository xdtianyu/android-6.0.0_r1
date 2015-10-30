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
package android.telephony.cts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.List;

/**
 * Test TelephonyManager.getAllCellInfo()
 * <p>
 * TODO(chesnutt): test onCellInfoChanged() once the implementation
 * of async callbacks is complete (see http://b/13788638)
 */
public class CellInfoTest extends AndroidTestCase{
    private final Object mLock = new Object();
    private TelephonyManager mTelephonyManager;
    private static ConnectivityManager mCm;
    private static final String TAG = "android.telephony.cts.CellInfoTest";
    // Maximum and minimum possible RSSI values(in dbm).
    private static final int MAX_RRSI = -10;
    private static final int MIN_RSSI = -150;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTelephonyManager =
                (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
        mCm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void testCellInfo() throws Throwable {
        if (mCm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) == null) {
            Log.d(TAG, "Skipping test that requires ConnectivityManager.TYPE_MOBILE");
            return;
        }

        // getAllCellInfo should never return null, and there should
        // be at least one entry.
        List<CellInfo> allCellInfo = mTelephonyManager.getAllCellInfo();
        assertNotNull("TelephonyManager.getAllCellInfo() returned NULL!", allCellInfo);
        assertTrue("TelephonyManager.getAllCellInfo() returned zero-length list!",
            allCellInfo.size() > 0);

        int numRegisteredCells = 0;
        for (CellInfo cellInfo : allCellInfo) {
            if (cellInfo.isRegistered()) {
                ++numRegisteredCells;
            }
            if (cellInfo instanceof CellInfoLte) {
                verifyLteInfo((CellInfoLte) cellInfo);
            } else if (cellInfo instanceof CellInfoWcdma) {
                verifyWcdmaInfo((CellInfoWcdma) cellInfo);
            } else if (cellInfo instanceof CellInfoGsm) {
                verifyGsmInfo((CellInfoGsm) cellInfo);
            }
        }
        // At most two cells could be registered.
        assertTrue("None or too many registered cells : " + numRegisteredCells,
                numRegisteredCells > 0 && numRegisteredCells <= 2);
    }

    // Verify lte cell information is within correct range.
    private void verifyLteInfo(CellInfoLte lte) {
        verifyRssiDbm(lte.getCellSignalStrength().getDbm());
        // Verify LTE neighbor information.
        if (!lte.isRegistered()) {
            // Only physical cell id is available for LTE neighbor.
            int pci = lte.getCellIdentity().getPci();
            // Physical cell id should be within [0, 503].
            assertTrue("getPci() out of range [0, 503]", pci >= 0 && pci <= 503);
        }
    }

    // Verify wcdma cell information is within correct range.
    private void verifyWcdmaInfo(CellInfoWcdma wcdma) {
        verifyRssiDbm(wcdma.getCellSignalStrength().getDbm());
        // Verify wcdma neighbor.
        if (!wcdma.isRegistered()) {
            // For wcdma neighbor, only primary scrambling code is available.
            // Primary scrambling code should be within [0, 511].
            int psc = wcdma.getCellIdentity().getPsc();
            assertTrue("getPsc() out of range [0, 511]", psc >= 0 && psc <= 511);
        }
    }

    // Verify gsm cell information is within correct range.
    private void verifyGsmInfo(CellInfoGsm gsm) {
        verifyRssiDbm(gsm.getCellSignalStrength().getDbm());
        // Verify gsm neighbor.
        if (!gsm.isRegistered()) {
            // lac and cid are available in GSM neighbor information.
            // Local area code and cellid should be with [0, 65535].
            int lac = gsm.getCellIdentity().getLac();
            assertTrue("getLac() out of range [0, 65535]", lac >= 0 && lac <= 65535);
            int cid = gsm.getCellIdentity().getCid();
            assertTrue("getCid() out range [0, 65535]", cid >= 0 && cid <= 65535);
        }
    }

    // Rssi(in dbm) should be within [MIN_RSSI, MAX_RSSI].
    private void verifyRssiDbm(int dbm) {
        assertTrue("getCellSignalStrength().getDbm() out of range",
                dbm >= MIN_RSSI && dbm <= MAX_RRSI);
    }
}
