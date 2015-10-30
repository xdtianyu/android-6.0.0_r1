/* Copyright (C) 2014 The Android Open Source Project
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

package com.android.exchange.utility;

import android.test.suitebuilder.annotation.SmallTest;

import org.apache.http.message.BasicHeader;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Test for {@link WbxmlResponseLogger}.
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.utility.WbxmlResponseLoggerTests exchange
 */
@SmallTest
public class WbxmlResponseLoggerTests extends TestCase {
    private static final byte testArray[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x11,};

    public void testShouldLogResponseTooBig() {
        final long contentSize = WbxmlResponseLogger.MAX_LENGTH + 1;
        assertEquals(false, WbxmlResponseLogger.shouldLogResponse(contentSize));
    }

    public void testShouldLogResponseSmallEnough() {
        final long contentSize = WbxmlResponseLogger.MAX_LENGTH - 1;
        assertEquals(true, WbxmlResponseLogger.shouldLogResponse(contentSize));
    }

    public void testProcessContentEncoding() {
        final String encoding = "US-ASCII";
        final BasicHeader header = new BasicHeader("content-encoding", encoding);
        final String outputEncoding = WbxmlResponseLogger.processContentEncoding(header);
        assertEquals(true, encoding.equals(outputEncoding));
    }

    public void testProcessContentEncodingNullHeader() {
        final String encoding = "UTF-8";
        final String outputEncoding = WbxmlResponseLogger.processContentEncoding(null);
        assertEquals(true, encoding.equals(outputEncoding));
    }

    public void testProcessContentEncodingNullValue() {
        final String encoding = "UTF-8";
        final BasicHeader header = new BasicHeader("content-encoding", null);
        final String outputEncoding = WbxmlResponseLogger.processContentEncoding(header);
        assertEquals(true, encoding.equals(outputEncoding));
    }

    public void testGetContentAsByteArraySingleBatch() throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(testArray);
        final byte outputBytes[] = WbxmlResponseLogger.getContentAsByteArray(bis,
            testArray.length);
        assertEquals(true, Arrays.equals(testArray, outputBytes));
    }

    public void testGetContentAsByteArrayMultipleBatches() throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(testArray);
        // If we cut the batch size to be half the length of testArray, we force
        // 2 batches of processing.
        final byte outputBytes[] = WbxmlResponseLogger.getContentAsByteArray(bis,
                testArray.length / 2);
        assertEquals(true, Arrays.equals(testArray, outputBytes));
    }
}
