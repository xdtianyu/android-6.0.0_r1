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

import com.android.exchange.Eas;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parse the result of a MoveItems command.
 */
public class MoveItemsParser extends Parser {
    private static final String TAG = Eas.LOG_TAG;
    private int mStatusCode = 0;
    private String mNewServerId;
    private String mSourceServerId;

    // These are the EAS status codes for MoveItems
    private static final int STATUS_NO_SOURCE_FOLDER = 1;
    private static final int STATUS_NO_DESTINATION_FOLDER = 2;
    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_SOURCE_DESTINATION_SAME = 4;
    private static final int STATUS_INTERNAL_ERROR = 5;
    private static final int STATUS_ALREADY_EXISTS = 6;
    private static final int STATUS_LOCKED = 7;

    // These are the status values we return to callers
    public static final int STATUS_CODE_SUCCESS = 1;
    public static final int STATUS_CODE_REVERT = 2;
    public static final int STATUS_CODE_RETRY = 3;

    public MoveItemsParser(InputStream in) throws IOException {
        super(in);
    }

    public int getStatusCode() {
        if (mStatusCode == 0) {
            LogUtils.e(TAG, "Trying to get status for MoveItems, but no status was set");
            // TODO: We currently treat empty responses as retry, so for now we'll do the same for
            // partially empty responses.
            return STATUS_CODE_RETRY;
        }
        return mStatusCode;
    }

    public String getNewServerId() {
        return mNewServerId;
    }

    public String getSourceServerId() {
        return mSourceServerId;
    }

    private void parseResponse() throws IOException {
        while (nextTag(Tags.MOVE_RESPONSE) != END) {
            if (tag == Tags.MOVE_STATUS) {
                int status = getValueInt();
                // Convert the EAS status code with our external codes
                switch(status) {
                    case STATUS_SUCCESS:
                    case STATUS_SOURCE_DESTINATION_SAME:
                    case STATUS_ALREADY_EXISTS:
                        // Same destination and already exists are ok with us; we'll continue as
                        // if the move succeeded
                        mStatusCode = STATUS_CODE_SUCCESS;
                        break;
                    case STATUS_LOCKED:
                        // This sounds like a transient error, so we can safely retry
                        mStatusCode = STATUS_CODE_RETRY;
                        break;
                    case STATUS_NO_SOURCE_FOLDER:
                    case STATUS_NO_DESTINATION_FOLDER:
                    case STATUS_INTERNAL_ERROR:
                    default:
                        // These are non-recoverable, so we'll revert the message to its original
                        // mailbox.  If there's an unknown response, revert
                        mStatusCode = STATUS_CODE_REVERT;
                        break;
                }
                if (status != STATUS_SUCCESS) {
                    // There's not much to be done if this fails
                    LogUtils.w(TAG, "Error in MoveItems: %d", status);
                }
            } else if (tag == Tags.MOVE_DSTMSGID) {
                mNewServerId = getValue();
                LogUtils.d(TAG, "Moved message id is now: %s", mNewServerId);
            } else if (tag == Tags.MOVE_SRCMSGID) {
                mSourceServerId = getValue();
                LogUtils.d(TAG, "Source message id is: %s", mNewServerId);
            } else {
                skipTag();
            }
        }
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.MOVE_MOVE_ITEMS) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.MOVE_RESPONSE) {
                parseResponse();
            } else {
                skipTag();
            }
        }
        return res;
    }
}

