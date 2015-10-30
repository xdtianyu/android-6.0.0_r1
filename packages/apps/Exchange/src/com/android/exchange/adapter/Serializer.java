/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */

//Contributors: Jonathan Cox, Bogdan Onoiu, Jerry Tian
// Greatly simplified for Google, Inc. by Marc Blank

package com.android.exchange.adapter;

import android.content.ContentValues;
import android.text.TextUtils;

import com.android.exchange.Eas;
import com.android.exchange.service.EasService;
import com.android.exchange.utility.FileLogger;
import com.android.mail.utils.LogUtils;
import com.google.common.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class Serializer {
    private static final String TAG = Eas.LOG_TAG;
    private static final int BUFFER_SIZE = 16*1024;
    private static final int NOT_PENDING = -1;

    private final OutputStream mOutput;
    private int mPendingTag = NOT_PENDING;
    private final Deque<String> mNameStack = new ArrayDeque<String>();
    private int mTagPage = 0;

    public Serializer() throws IOException {
        this(new ByteArrayOutputStream(), true);
    }

    public Serializer(OutputStream os) throws IOException {
        this(os, true);
    }

    @VisibleForTesting
    public Serializer(boolean startDocument) throws IOException {
        this(new ByteArrayOutputStream(), startDocument);
    }

    /**
     * Base constructor
     * @param outputStream the stream we're serializing to
     * @param startDocument whether or not to start a document
     * @throws IOException
     */
    public Serializer(final OutputStream outputStream, final boolean startDocument)
            throws IOException {
        super();
        mOutput = outputStream;
        if (startDocument) {
            startDocument();
        } else {
            mOutput.write(0);
        }
    }

    void log(final String str) {
        if (!EasService.getProtocolLogging()) {
            return;
        }
        final String logStr;
        final int cr = str.indexOf('\n');
        if (cr > 0) {
            logStr = str.substring(0, cr);
        } else {
            logStr = str;
        }
        final char [] charArray = new char[mNameStack.size() * 2];
        Arrays.fill(charArray, ' ');
        final String indent = new String(charArray);
        LogUtils.d(TAG, "%s%s", indent, logStr);
        if (EasService.getFileLogging()) {
            FileLogger.log(TAG, logStr);
        }
    }

    public void done() throws IOException {
        if (mNameStack.size() != 0 || mPendingTag != NOT_PENDING) {
            throw new IOException("Done received with unclosed tags");
        }
        mOutput.flush();
    }

    public void startDocument() throws IOException {
        mOutput.write(0x03); // version 1.3
        mOutput.write(0x01); // unknown or missing public identifier
        mOutput.write(106);  // UTF-8
        mOutput.write(0);    // 0 length string array
    }

    private void checkPendingTag(final boolean degenerated) throws IOException {
        if (mPendingTag == NOT_PENDING) {
            return;
        }

        final int page = mPendingTag >> Tags.PAGE_SHIFT;
        final int tag = mPendingTag & Tags.PAGE_MASK;
        if (page != mTagPage) {
            mTagPage = page;
            mOutput.write(Wbxml.SWITCH_PAGE);
            mOutput.write(page);
        }

        mOutput.write(degenerated ? tag : tag | Wbxml.WITH_CONTENT);
        String name = "unknown";
        if (!Tags.isValidPage(page)) {
            log("Unrecognized page " + page);
        } else if (!Tags.isValidTag(page, tag)) {
            log("Unknown tag " + tag + " on page " + page);
        } else {
            name = Tags.getTagName(page, tag);
        }
        log("<" + name + (degenerated ? "/>" : ">"));
        if (!degenerated) {
            mNameStack.addFirst(name);
        }
        mPendingTag = NOT_PENDING;
    }

    public Serializer start(final int tag) throws IOException {
        checkPendingTag(false);
        mPendingTag = tag;
        return this;
    }

    public Serializer end() throws IOException {
        if (mPendingTag >= 0) {
            checkPendingTag(true);
        } else {
            mOutput.write(Wbxml.END);
            final String tagName = mNameStack.removeFirst();
            log("</" + tagName + '>');
        }
        return this;
    }

    public Serializer tag(final int tag) throws IOException {
        start(tag);
        end();
        return this;
    }

    /**
     * Writes <tag>value</tag>. Throws IOException for null strings.
     */
    public Serializer data(final int tag, final String value) throws IOException {
        start(tag);
        text(value);
        end();
        return this;
    }

    /**
     * Writes out inline string. Throws IOException for null strings.
     */
    public Serializer text(final String text) throws IOException {
        if (text == null) {
            throw new IOException("Null text write for pending tag: " + mPendingTag);
        }
        checkPendingTag(false);
        writeInlineString(mOutput, text);
        log(text);
        return this;
    }

    /**
     * Writes out opaque data blocks. Throws IOException for negative buffer
     * sizes or if is unable to read sufficient bytes from input stream.
     */
    public Serializer opaque(final InputStream is, final int length) throws IOException {
        writeOpaqueHeader(length);
        log("opaque: " + length);
        // Now write out the opaque data in batches
        final byte[] buffer = new byte[BUFFER_SIZE];
        int totalBytesRead = 0;
        while (totalBytesRead < length) {
            final int bytesRead = is.read(buffer, 0, Math.min(BUFFER_SIZE, length));
            if (bytesRead == -1) {
                throw new IOException("Invalid opaque data block; read "
                        + totalBytesRead + " bytes but expected " + length);
            }
            mOutput.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
        }
        return this;
    }

    /**
     * Writes out opaque data header, without the actual opaque data bytes.
     * Used internally by opaque(), and externally to calculate content length
     * without having to allocate the memory for the data copy.
     * Throws IOException if length is negative; is a no-op for length 0.
     */
    public Serializer writeOpaqueHeader(final int length) throws IOException {
        if (length < 0) {
            throw new IOException("Invalid negative opaque data length " + length);
        }
        if (length == 0) {
            return this;
        }
        checkPendingTag(false);
        mOutput.write(Wbxml.OPAQUE);
        writeInteger(mOutput, length);
        return this;
    }

    @VisibleForTesting
    static void writeInteger(final OutputStream out, int i) throws IOException {
        final byte[] buf = new byte[5];
        int idx = 0;

        do {
            buf[idx++] = (byte) (i & 0x7f);
            // Use >>> to shift in 0s so loop terminates
            i = i >>> 7;
        } while (i != 0);

        while (idx > 1) {
            out.write(buf[--idx] | 0x80);
        }
        out.write(buf[0]);
    }

    private static void writeInlineString(final OutputStream out, final String s)
        throws IOException {
        out.write(Wbxml.STR_I);
        final byte[] data = s.getBytes("UTF-8");
        out.write(data);
        out.write(0);
    }

    /**
     * Looks up key in cv; if absent or empty writes out <tag/> otherwise
     * writes out <tag>value</tag>.
     */
    public void writeStringValue (final ContentValues cv, final String key,
            final int tag) throws IOException {
        final String value = cv.getAsString(key);
        if (!TextUtils.isEmpty(value)) {
            data(tag, value);
        } else {
            tag(tag);
        }
    }

    @Override
    public String toString() {
        if (mOutput instanceof ByteArrayOutputStream) {
            return ((ByteArrayOutputStream)mOutput).toString();
        }
        throw new IllegalStateException();
    }

    public byte[] toByteArray() {
        if (mOutput instanceof ByteArrayOutputStream) {
            return ((ByteArrayOutputStream)mOutput).toByteArray();
        }
        throw new IllegalStateException();
    }

}
