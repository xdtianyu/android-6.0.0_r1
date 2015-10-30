/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.cts.readexternalstorageapp;

import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_NONE;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_READ;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_WRITE;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertFileReadOnlyAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertFileReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.buildGiftForPackage;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.readInt;

import android.test.AndroidTestCase;

import java.io.File;

public class ReadGiftTest extends AndroidTestCase {
    /**
     * Verify we can read all gifts.
     */
    public void testGifts() throws Exception {
        final File none = buildGiftForPackage(getContext(), PACKAGE_NONE);
        assertFileReadOnlyAccess(none);
        assertEquals(100, readInt(none));

        final File read = buildGiftForPackage(getContext(), PACKAGE_READ);
        assertFileReadWriteAccess(read);
        assertEquals(101, readInt(read));

        final File write = buildGiftForPackage(getContext(), PACKAGE_WRITE);
        assertFileReadOnlyAccess(write);
        assertEquals(102, readInt(write));
    }
}
