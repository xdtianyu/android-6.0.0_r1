/* Copyright (C) 2010 The Android Open Source Project.
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

import com.android.exchange.provider.GalResult;
import com.android.exchange.provider.GalResult.GalData;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parse the result of a GAL command.
 */
public class GalParser extends Parser {
    // TODO: make this controllable through the debug screen.
    private static final boolean DEBUG_GAL_SERVICE = false;

    GalResult mGalResult = new GalResult();

    public GalParser(final InputStream in) throws IOException {
        super(in);
    }

    public GalResult getGalResult() {
        return mGalResult;
    }

    @Override
    public boolean parse() throws IOException {
        if (nextTag(START_DOCUMENT) != Tags.SEARCH_SEARCH) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.SEARCH_RESPONSE) {
                parseResponse(mGalResult);
            } else {
                skipTag();
            }
         }
        // TODO: IO Exception if we can't get the result to parse and a false should be returned
        // here if we can't parse the input stream in the case where the schema may have changed.
        // To implement this, we'll have to dig a bit deeper into the parsing code.
        return true;
     }

    private void parseProperties(final GalResult galResult) throws IOException {
        final GalData galData = new GalData();
        while (nextTag(Tags.SEARCH_PROPERTIES) != END) {
            switch(tag) {
                // Display name and email address use both legacy and new code for galData
                case Tags.GAL_DISPLAY_NAME:
                    String displayName = getValue();
                    galData.put(GalData.DISPLAY_NAME, displayName);
                    galData.displayName = displayName;
                    break;
                case Tags.GAL_EMAIL_ADDRESS:
                    String emailAddress = getValue();
                    galData.put(GalData.EMAIL_ADDRESS, emailAddress);
                    galData.emailAddress = emailAddress;
                    break;
                case Tags.GAL_PHONE:
                    galData.put(GalData.WORK_PHONE, getValue());
                    break;
                case Tags.GAL_OFFICE:
                    galData.put(GalData.OFFICE, getValue());
                    break;
                case Tags.GAL_TITLE:
                    galData.put(GalData.TITLE, getValue());
                    break;
                case Tags.GAL_COMPANY:
                    galData.put(GalData.COMPANY, getValue());
                    break;
                case Tags.GAL_ALIAS:
                    galData.put(GalData.ALIAS, getValue());
                    break;
                case Tags.GAL_FIRST_NAME:
                    galData.put(GalData.FIRST_NAME, getValue());
                    break;
                case Tags.GAL_LAST_NAME:
                    galData.put(GalData.LAST_NAME, getValue());
                    break;
                case Tags.GAL_HOME_PHONE:
                    galData.put(GalData.HOME_PHONE, getValue());
                    break;
                case Tags.GAL_MOBILE_PHONE:
                    galData.put(GalData.MOBILE_PHONE, getValue());
                    break;
                default:
                    skipTag();
            }
        }
        galResult.addGalData(galData);
    }

     private void parseResult(final GalResult galResult) throws IOException {
         while (nextTag(Tags.SEARCH_RESULT) != END) {
             if (tag == Tags.SEARCH_PROPERTIES) {
                 parseProperties(galResult);
             } else {
                 skipTag();
             }
         }
     }

     private void parseResponse(final GalResult galResult) throws IOException {
         while (nextTag(Tags.SEARCH_RESPONSE) != END) {
             if (tag == Tags.SEARCH_STORE) {
                 parseStore(galResult);
             } else {
                 skipTag();
             }
         }
     }

     private void parseStore(final GalResult galResult) throws IOException {
         while (nextTag(Tags.SEARCH_STORE) != END) {
             if (tag == Tags.SEARCH_RESULT) {
                 parseResult(galResult);
             } else if (tag == Tags.SEARCH_RANGE) {
                 // Retrieve value, even if we're not using it for debug logging
                 String range = getValue();
                 if (DEBUG_GAL_SERVICE) {
                     LogUtils.d(LogUtils.TAG, "GAL result range: " + range);
                 }
             } else if (tag == Tags.SEARCH_TOTAL) {
                 galResult.total = getValueInt();
             } else {
                 skipTag();
             }
         }
     }
}

