/* Copyright (C) 2011 The Android Open Source Project.
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

import com.android.exchange.eas.EasLoadAttachment.ProgressCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Parse the result of an ItemOperations command; we use this to load attachments in EAS 14.0
 */
public class ItemOperationsParser extends Parser {
    private static final int CHUNK_SIZE = 16*1024;

    private int mStatusCode = 0;
    private final OutputStream mAttachmentOutputStream;
    private final long mAttachmentSize;
    private final ProgressCallback mCallback;

    public ItemOperationsParser(final InputStream in, final OutputStream out, final long size,
            final ProgressCallback callback) throws IOException {
        super(in);
        mAttachmentOutputStream = out;
        mAttachmentSize = size;
        mCallback = callback;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    private void parseProperties() throws IOException {
        while (nextTag(Tags.ITEMS_PROPERTIES) != END) {
            if (tag == Tags.ITEMS_DATA) {
                // Wrap the input stream in our custom base64 input stream
                Base64InputStream bis = new Base64InputStream(getInput());
                // Read the attachment
                readChunked(bis, mAttachmentOutputStream, mAttachmentSize, mCallback);
            } else {
                skipTag();
            }
        }
    }

    private void parseFetch() throws IOException {
        while (nextTag(Tags.ITEMS_FETCH) != END) {
            if (tag == Tags.ITEMS_PROPERTIES) {
                parseProperties();
            } else {
                skipTag();
            }
        }
    }

    private void parseResponse() throws IOException {
        while (nextTag(Tags.ITEMS_RESPONSE) != END) {
            if (tag == Tags.ITEMS_FETCH) {
                parseFetch();
            } else {
                skipTag();
            }
        }
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.ITEMS_ITEMS) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.ITEMS_STATUS) {
                // Save the status code
                mStatusCode = getValueInt();
            } else if (tag == Tags.ITEMS_RESPONSE) {
                parseResponse();
            } else {
                skipTag();
            }
        }
        return res;
    }

    /**
     * Read the attachment data in chunks and write the data back out to our attachment file
     * @param inputStream the InputStream we're reading the attachment from
     * @param outputStream the OutputStream the attachment will be written to
     * @param length the number of expected bytes we're going to read
     * @param callback A {@link ProgressCallback} to use to send progress updates to the UI.
     * @throws IOException
     */
    public static void readChunked(final InputStream inputStream, final OutputStream outputStream,
            final long length, final ProgressCallback callback) throws IOException {
        final byte[] bytes = new byte[CHUNK_SIZE];
        // Loop terminates 1) when EOF is reached or 2) IOException occurs
        // One of these is guaranteed to occur
        int totalRead = 0;
        long lastCallbackPct = -1;
        int lastCallbackTotalRead = 0;
        while (true) {
            final int read = inputStream.read(bytes, 0, CHUNK_SIZE);
            if (read < 0) {
                // -1 means EOF
                break;
            }

            // Keep track of how much we've read for progress callback
            totalRead += read;
            // Write these bytes out
            outputStream.write(bytes, 0, read);

            // We can't report percentage if data is chunked; the length of incoming data is unknown
            if (length > 0) {
                final int pct = (int)((totalRead * 100) / length);
                // Callback only if we've read at least 1% more and have read more than CHUNK_SIZE
                // We don't want to spam the Email app
                if ((pct > lastCallbackPct) && (totalRead > (lastCallbackTotalRead + CHUNK_SIZE))) {
                    // Report progress back to the UI
                    callback.doCallback(pct);

                    // TODO: Fix this.
                    //doProgressCallback(pct);
                    lastCallbackTotalRead = totalRead;
                    lastCallbackPct = pct;
                }
            }
        }
    }
}
