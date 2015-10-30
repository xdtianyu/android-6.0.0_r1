/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.exchange;

import android.app.Application;

import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.MailboxUtilities;
import com.android.mail.utils.LogTag;

public class Exchange extends Application {
    static {
        LogTag.setLogTag(Eas.LOG_TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EmailContent.init(this);
        try {
            getContentResolver().call(EmailContent.CONTENT_URI,
                    MailboxUtilities.FIX_PARENT_KEYS_METHOD, "", null);
        } catch (IllegalArgumentException e) {
            // If there is no Email provider (which happens if eg the
            // Email app is disabled), ignore.
        }
    }
}
