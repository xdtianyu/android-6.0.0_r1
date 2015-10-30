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

package com.android.exchange.adapter;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import com.android.exchange.service.EasService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Arrays;

@SmallTest
public class ParserTest extends AndroidTestCase {
    public class TestParser extends Parser {
        private Deque<Object> mExpectedData;

        public TestParser(InputStream in, Object[] expectedData) throws IOException{
            super(in);
            EasService.setProtocolLogging(true);
            mExpectedData = expectedData == null ? null
                : new ArrayDeque<Object>(Arrays.asList(expectedData));
        }

        @Override
        public boolean parse() throws IOException {
            int tag;
            while((tag = nextTag(START_DOCUMENT)) != END_DOCUMENT) {
                if (tag == 0x0B) {
                    final String strVal = getValue();
                    if (mExpectedData != null) {
                        final String expectedStrVal = (String) mExpectedData.removeFirst();
                        assertEquals(expectedStrVal, strVal);
                    }
                } else if (tag == 0x0C) {
                    final int intVal = getValueInt();
                    if (mExpectedData != null) {
                        final Integer expectedIntVal = (Integer) mExpectedData.removeFirst();
                        assertEquals(expectedIntVal.intValue(), intVal);
                    }
                } else if (tag == 0x0D) {
                    final byte[] bytes = getValueBytes();
                    if (mExpectedData != null) {
                        final String expectedHexStr = stripString((String) mExpectedData.removeFirst());
                        final String hexStr = byteArrayToHexString(bytes);
                        assertEquals(expectedHexStr, hexStr);
                    }
                }
            }
            return true;
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String stripString(String str) {
        return TextUtils.join("", str.split(" "));
    }

    private static byte[] hexStringToByteArray(String hexStr) {
        final String s = TextUtils.join("", hexStr.split(" "));
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static InputStream getTestInputStream(final String hexStr) {
        final byte[] byteArray = hexStringToByteArray(hexStr);
        return new ByteArrayInputStream(byteArray);
    }

    private void testParserHelper(String wbxmlStr) throws Exception {
        testParserHelper(wbxmlStr, null);
    }

    private void testParserHelper(String wbxmlStr, Object[] expectedData) throws Exception {
        final Parser parser = new TestParser(getTestInputStream(wbxmlStr), expectedData);
        parser.parse();
    }

    @SmallTest
    public void testUnsupportedWbxmlTag() throws Exception {
        // Test parser with unsupported Wbxml tag (EXT_2 = 0xC2)
        final String unsupportedWbxmlTag = "03 01 6A 00 45 5F C2 05 11 22 33 44 00 01 01";
        try {
            testParserHelper(unsupportedWbxmlTag);
            fail("Expected EasParserException for unsupported tag 0xC2");
        } catch (Parser.EasParserException e) {
            // expected
        }
    }

    @SmallTest
    public void testUnknownCodePage() throws Exception {
        // Test parser with non existent code page 64 (0x40)
        final String unknownCodePage = "03 01 6A 00 45 00 40 4A 03 31 00 01 01";
        try {
            testParserHelper(unknownCodePage);
            fail("Expected EasParserException for unknown code page 64");
        } catch (Parser.EasParserException e) {
            // expected
        }
    }

    @SmallTest
    public void testUnknownTag() throws Exception {
        // Test parser with valid code page (0x00) but non existent tag (0x3F)
        final String unknownTag = "03 01 6A 00 45 7F 03 31 00 01 01";
        testParserHelper(unknownTag);
    }

    @SmallTest
    public void testTextParsing() throws Exception {
        // Expect text; has text data "DF"
        final String textTagWithTextData = "03 01 6A 00 45 4B 03 44 46 00 01 01";
        testParserHelper(textTagWithTextData, new Object[] {"DF"});

        // Expect text; has tag with no content: <Tag/>
        final String textTagNoContent = "03 01 6A 00 45 0B 01";
        testParserHelper(textTagNoContent, new Object[] {""});

        // Expect text; has tag and end tag with no value: <Tag></Tag>
        final String emptyTextTag = "03 01 6A 00 45 4B 01 01";
        testParserHelper(emptyTextTag, new Object[] {""});

        // Expect text; has opaque data {0x11, 0x22, 0x33}
        final String textTagWithOpaqueData = "03 01 6A 00 45 4B C3 03 11 22 33 01 01";
        try {
            testParserHelper(textTagWithOpaqueData);
            fail("Expected EasParserException for trying to read opaque data as text");
        } catch (Parser.EasParserException e) {
            // expected
        }
    }

    @SmallTest
    public void testIntegerStringParsing() throws Exception {
        // Expect int; has text data "1"
        final String intTagWithIntData = "03 01 6A 00 45 4C 03 31 00 01 01";
        testParserHelper(intTagWithIntData, new Object[] {1});

        // Expect int; has tag with no content: <Tag/>
        final String intTagNoContent = "03 01 6A 00 45 0C 01";
        testParserHelper(intTagNoContent, new Object[] {0});

        // Expect int; has tag and end tag with no value: <Tag></Tag>
        final String emptyIntTag = "03 01 6A 00 45 4C 01 01";
        testParserHelper(emptyIntTag, new Object[] {0});

        // Expect int; has text data "DF"
        final String intTagWithTextData = "03 01 6A 00 45 4C 03 44 46 00 01 01";
        try {
            testParserHelper(intTagWithTextData);
            fail("Expected EasParserException for nonnumeric char 'D'");
        } catch (Parser.EasParserException e) {
            // expected
        }
    }

    @SmallTest
    public void testOpaqueDataParsing() throws Exception {
        // Expect opaque; has opaque data {0x11, 0x22, 0x33}
        final String opaqueTagWithOpaqueData = "03 01 6A 00 45 4D C3 03 11 22 33 01 01";
        testParserHelper(opaqueTagWithOpaqueData, new Object[] {"11 22 33"});

        // Expect opaque; has tag with no content: <Tag/>
        final String opaqueTagNoContent = "03 01 6A 00 45 0D 01";
        testParserHelper(opaqueTagNoContent, new Object[] {""});

        // Expect opaque; has tag and end tag with no value: <Tag></Tag>
        final String emptyOpaqueTag = "03 01 6A 00 45 4D 01 01";
        testParserHelper(emptyOpaqueTag, new Object[] {""});

        // Expect opaque; has text data "DF"
        final String opaqueTagWithTextData = "03 01 6A 00 45 4D 03 44 46 00 01 01";
        testParserHelper(opaqueTagWithTextData, new Object[] {"44 46"});
    }

    @SmallTest
    public void testMalformedData() throws Exception {
        final String malformedData = "03 01 6A 00 45 4B 03 11 22 00 00 33 00 01 01";
        try {
            testParserHelper(malformedData);
            fail("Expected EasParserException for improperly escaped text data");
        } catch (Parser.EasParserException e) {
            // expected
        }
    }

    @SmallTest
    public void testRunOnInteger() throws Exception {
        final String runOnIntegerEncoding = "03 01 6A 00 45 4D C3 81 82 83 84 85 06 11 22 33 01 01";
        try {
            testParserHelper(runOnIntegerEncoding);
            fail("Expected EasParserException for improperly encoded integer");
        } catch (Parser.EasParserException e) {
            // expected
        }
    }

    @SmallTest
    public void testAttributeTag() throws Exception {
        // Test parser with known tag with attributes
        final String tagWithAttributes = "03 01 6A 00 45 DF 06 01 03 31 00 01 01";
        try {
            testParserHelper(tagWithAttributes);
            fail("Expected EasParserException for tag with attributes 0xDF");
        } catch (Parser.EasParserException e) {
            // expected
        }
    }
}
