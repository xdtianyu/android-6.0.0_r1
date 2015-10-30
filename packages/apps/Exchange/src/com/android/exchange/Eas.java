/*
 * Copyright (C) 2008-2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceProxy;
import com.android.mail.utils.LogUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Constants used throughout the EAS implementation are stored here.
 *
 */
public class Eas {

    // For logging.
    public static final String LOG_TAG = "Exchange";

    // For debugging
    public static boolean DEBUG = false;         // DO NOT CHECK IN WITH THIS SET TO TRUE

    // The following two are for user logging (the second providing more detail)
    public static boolean USER_LOG = false;     // DO NOT CHECK IN WITH THIS SET TO TRUE

    public static final String CLIENT_VERSION = "EAS-2.0";
    public static final String ACCOUNT_MAILBOX_PREFIX = "__eas";

    // Define our default protocol version as 2.5 (Exchange 2003)
    public static final String SUPPORTED_PROTOCOL_EX2003 = "2.5";
    public static final double SUPPORTED_PROTOCOL_EX2003_DOUBLE = 2.5;
    public static final String SUPPORTED_PROTOCOL_EX2007 = "12.0";
    public static final double SUPPORTED_PROTOCOL_EX2007_DOUBLE = 12.0;
    public static final String SUPPORTED_PROTOCOL_EX2007_SP1 = "12.1";
    public static final double SUPPORTED_PROTOCOL_EX2007_SP1_DOUBLE = 12.1;
    public static final String SUPPORTED_PROTOCOL_EX2010 = "14.0";
    public static final double SUPPORTED_PROTOCOL_EX2010_DOUBLE = 14.0;
    public static final String SUPPORTED_PROTOCOL_EX2010_SP1 = "14.1";
    public static final double SUPPORTED_PROTOCOL_EX2010_SP1_DOUBLE = 14.1;
    public static final String DEFAULT_PROTOCOL_VERSION = SUPPORTED_PROTOCOL_EX2003;
    public static final boolean DEFAULT_PROTOCOL_IS_EAS14 = false;

    public static final String EXCHANGE_ACCOUNT_MANAGER_TYPE =
            com.android.exchange.Configuration.EXCHANGE_ACCOUNT_MANAGER_TYPE;
    public static final String PROTOCOL = com.android.exchange.Configuration.EXCHANGE_PROTOCOL;
    public static final String EXCHANGE_SERVICE_INTENT_ACTION =
            com.android.exchange.Configuration.EXCHANGE_SERVICE_INTENT_ACTION;

    // From EAS spec
    //                Mail Cal
    // 0 No filter    Yes  Yes
    // 1 1 day ago    Yes  No
    // 2 3 days ago   Yes  No
    // 3 1 week ago   Yes  No
    // 4 2 weeks ago  Yes  Yes
    // 5 1 month ago  Yes  Yes
    // 6 3 months ago No   Yes
    // 7 6 months ago No   Yes

    // TODO Rationalize this with SYNC_WINDOW_ALL
    public static final String FILTER_ALL = "0";
    public static final String FILTER_1_DAY = "1";
    public static final String FILTER_3_DAYS =  "2";
    public static final String FILTER_1_WEEK =  "3";
    public static final String FILTER_2_WEEKS =  "4";
    public static final String FILTER_1_MONTH =  "5";
    public static final String FILTER_3_MONTHS = "6";
    public static final String FILTER_6_MONTHS = "7";

    public static final String BODY_PREFERENCE_TEXT = "1";
    public static final String BODY_PREFERENCE_HTML = "2";

    public static final String MIME_BODY_PREFERENCE_TEXT = "0";
    public static final String MIME_BODY_PREFERENCE_MIME = "2";

    // Mailbox Types
    // Section 2.2.3.170.3 Type (FolderSync)
    // http://msdn.microsoft.com/en-us/library/gg650877(v=exchg.80).aspx
    public static final int MAILBOX_TYPE_USER_GENERIC = 1;
    public static final int MAILBOX_TYPE_INBOX = 2;
    public static final int MAILBOX_TYPE_DRAFTS = 3;
    public static final int MAILBOX_TYPE_DELETED = 4;
    public static final int MAILBOX_TYPE_SENT = 5;
    public static final int MAILBOX_TYPE_OUTBOX = 6;
//    public static final int MAILBOX_TYPE_TASKS = 7;
    public static final int MAILBOX_TYPE_CALENDAR = 8;
    public static final int MAILBOX_TYPE_CONTACTS = 9;
//    public static final int MAILBOX_TYPE_NOTES = 10;
//    public static final int MAILBOX_TYPE_JOURNAL = 11;
    public static final int MAILBOX_TYPE_USER_MAIL = 12;
    public static final int MAILBOX_TYPE_USER_CALENDAR = 13;
    public static final int MAILBOX_TYPE_USER_CONTACTS = 14;
//    public static final int MAILBOX_TYPE_USER_TASKS = 15;
//    public static final int MAILBOX_TYPE_USER_JOURNAL = 16;
//    public static final int MAILBOX_TYPE_USER_NOTES = 17;
//    public static final int MAILBOX_TYPE_UNKNOWN = 18;
//    public static final int MAILBOX_TYPE_RECIPIENT_INFORMATION_CACHE = 19;


    // These limits must never exceed about 500k which is half the max size of a Binder IPC buffer.

    // For EAS 12, we use HTML, so we want a larger size than in EAS 2.5
    public static final String EAS12_TRUNCATION_SIZE = "200000";
    // For EAS 2.5, truncation is a code; the largest is "7", which is 100k
    public static final String EAS2_5_TRUNCATION_SIZE = "7";

    public static final int FOLDER_STATUS_OK = 1;
    public static final int FOLDER_STATUS_INVALID_KEY = 9;

    public static final int EXCHANGE_ERROR_NOTIFICATION = 0x10;

    static public Double getProtocolVersionDouble(String version) {
        if (SUPPORTED_PROTOCOL_EX2003.equals(version)) {
            return SUPPORTED_PROTOCOL_EX2003_DOUBLE;
        } else if (SUPPORTED_PROTOCOL_EX2007.equals(version)) {
            return SUPPORTED_PROTOCOL_EX2007_DOUBLE;
        } if (SUPPORTED_PROTOCOL_EX2007_SP1.equals(version)) {
            return SUPPORTED_PROTOCOL_EX2007_SP1_DOUBLE;
        } if (SUPPORTED_PROTOCOL_EX2010.equals(version)) {
            return SUPPORTED_PROTOCOL_EX2010_DOUBLE;
        } if (SUPPORTED_PROTOCOL_EX2010_SP1.equals(version)) {
            return SUPPORTED_PROTOCOL_EX2010_SP1_DOUBLE;
        }
        throw new IllegalArgumentException("illegal protocol version");
    }

    static public boolean isProtocolEas14(String version) {
        if (version == null) {
            return DEFAULT_PROTOCOL_IS_EAS14;
        }
        return getProtocolVersionDouble(version) >= SUPPORTED_PROTOCOL_EX2010_DOUBLE;
    }

    /**
     * Gets the Exchange folder class for a mailbox type (PIM collections have different values
     * from email), needed when forming the request.
     * @param mailboxType The type of the mailbox we're interested in, from {@link Mailbox}.
     * @return The folder class for the mailbox we're interested in.
     */
    public static String getFolderClass(final int mailboxType) {
        switch (mailboxType) {
            case Mailbox.TYPE_CALENDAR:
                return "Calendar";
            case Mailbox.TYPE_CONTACTS:
                return "Contacts";
            default:
                return "Email";
        }
    }

    // Time format documented at http://msdn.microsoft.com/en-us/library/ee201818(v=exchg.80).aspx
    public static final SimpleDateFormat DATE_FORMAT;
    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.US);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
}
