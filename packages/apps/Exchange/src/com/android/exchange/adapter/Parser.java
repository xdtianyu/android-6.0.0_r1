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

package com.android.exchange.adapter;

import android.content.Context;

import com.android.exchange.Eas;
import com.android.exchange.EasException;
import com.android.exchange.service.EasService;
import com.android.exchange.utility.FileLogger;
import com.android.mail.utils.LogUtils;
import com.google.common.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;

/**
 * Extremely fast and lightweight WBXML parser, implementing only the subset of WBXML that
 * EAS uses (as defined in the EAS specification).
 *
 * Supports:
 *      WBXML tokens to encode XML tags
 *      WBXML code pages to support multiple XML namespaces
 *      Inline strings
 *      Opaque data
 *
 * Does not support: (throws EasParserException)
 *      String tables
 *      Entities
 *      Processing instructions
 *      Attribute encoding
 *
 */
public abstract class Parser {
    private static final boolean LOG_VERBOSE = false;

    private static final String LOG_TAG = Eas.LOG_TAG;

    // The following constants are Wbxml standard
    public static final int START_DOCUMENT = 0;
    public static final int END_DOCUMENT = 1;
    private static final int DONE = 1;
    private static final int START = 2;
    public static final int END = 3;
    private static final int TEXT = 4;
    private static final int OPAQUE = 5;
    private static final int NOT_ENDED = Integer.MIN_VALUE;
    private static final int EOF_BYTE = -1;

    private boolean capture = false;

    private ArrayList<Integer> captureArray;

    // The input stream for this parser
    private InputStream in;

    // The stack of names of tags being processed; used when debug = true
    private String[] nameArray = new String[32];

    public class Tag {
        private final int mPage;
        private final int mIndex;
        // Whether the tag is associated with content (a value)
        public final boolean mNoContent;
        private final String mName;

        public Tag(final int page, final int id) {
            mPage = page;
            // The tag is in the low 6 bits
            mIndex = id & Tags.PAGE_MASK;
            // If the high bit is set, there is content (a value) to be read
            mNoContent = (id & Wbxml.WITH_CONTENT) == 0;
            if (Tags.isGlobalTag(mIndex)) {
                mName = "unsupported-WBXML";
            } else if (!Tags.isValidTag(mPage, mIndex)) {
                mName = "unknown";
            } else {
                mName = Tags.getTagName(mPage, mIndex);
            }
        }

        public int getTagNum() {
            if (Tags.isGlobalTag(mIndex)) {
                return mIndex;
            }
            return (mPage << Tags.PAGE_SHIFT) | mIndex;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    // The stack of tags being processed
    private final Deque<Tag> startTagArray = new ArrayDeque<Tag>();

    private Tag startTag;

    // The type of the last token read (eg, TEXT, OPAQUE, END, etc).
    private int type;

    // The current page. As of EAS 14.1, this is a value 0-24.
    private int page;

    // The current tag. The low order 6 bits contain the tag index and the
    // higher order bits the page number. The format matches that used for
    // the tag enums defined in Tags.java.
    public int tag;

    // Whether the current tag is associated with content (a value)
    public boolean noContent;

    // The value read, as a String
    private String text;

    // The value read, as bytes
    private byte[] bytes;

    // TODO: Define a new parse exception type rather than lumping these in as IOExceptions.

    /**
     * Generated when the parser comes to EOF prematurely during parsing (i.e. in error)
     */
    public class EofException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * An EmptyStreamException is an EofException that occurs reading the first byte in the parser's
     * input stream; in other words, the stream had no content.
     */
    public class EmptyStreamException extends EofException {
        private static final long serialVersionUID = 1L;
    }

    public class EodException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    public class EasParserException extends IOException {
        private static final long serialVersionUID = 1L;

        EasParserException() {
            super("WBXML format error");
        }

        EasParserException(final String reason) {
            super(reason);
        }
    }

    public boolean parse() throws IOException, EasException {
        return false;
    }

    public Parser(final InputStream in) throws IOException {
        setInput(in, true);
    }

    /**
     * Constructor for use when switching parsers within a input stream
     * @param parser an existing, initialized parser
     * @throws IOException
     */
    public Parser(final Parser parser) throws IOException {
        setInput(parser.in, false);
    }

    protected InputStream getInput() {
        return in;
    }

    /**
     * Turns on data capture; this is used to create test streams that represent "live" data and
     * can be used against the various parsers.
     */
    public void captureOn() {
        capture = true;
        captureArray = new ArrayList<Integer>();
    }

    /**
     * Turns off data capture; writes the captured data to a specified file.
     */
    public void captureOff(final Context context, final String file) {
        try {
            final FileOutputStream out = context.openFileOutput(file,
                    Context.MODE_WORLD_WRITEABLE);
            out.write(captureArray.toString().getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            // This is debug code; exceptions aren't interesting.
        } catch (IOException e) {
            // This is debug code; exceptions aren't interesting.
        }
    }

    /**
     * Return the value of the current tag, as a byte array. Throws EasParserException
     * if neither opaque nor text data is present. Never returns null--returns
     * an empty byte[] array for empty data.
     *
     * @return the byte array value of the current tag
     * @throws IOException
     */
    public byte[] getValueBytes() throws IOException {
        final String name = startTag.toString();

        getNext();
        // This means there was no value given, just <Foo/>; we'll return empty array
        if (type == END) {
            log("No value for tag: " + name);
            return new byte[0];
        } else if (type != OPAQUE && type != TEXT) {
            throw new EasParserException("Expected OPAQUE or TEXT data for tag " + name);
        }

        // Save the value
        final byte[] val = type == OPAQUE ? bytes : text.getBytes("UTF-8");
        // Read the next token; it had better be the end of the current tag
        getNext();
        // If not, throw an exception
        if (type != END) {
            throw new EasParserException("No END found for tag " + name);
        }
        return val;
    }

    /**
     * Return the value of the current tag, as a String. Throws EasParserException
     * for non-text data. Never returns null--returns an empty string if no data.
     *
     * @return the String value of the current tag
     * @throws IOException
     */
    public String getValue() throws IOException {
        final String name = startTag.toString();

        getNext();
        // This means there was no value given, just <Foo/>; we'll return empty string for now
        if (type == END) {
            log("No value for tag: " + name);
            return "";
        } else if (type != TEXT) {
            throw new EasParserException("Expected TEXT data for tag " + name);
        }

        // Save the value
        final String val = text;
        // Read the next token; it had better be the end of the current tag
        getNext();
        // If not, throw an exception
        if (type != END) {
            throw new EasParserException("No END found for tag " + name);
        }
        return val;
    }

    /**
     * Return the value of the current tag, as an integer. Throws EasParserException
     * for non text data, and text data that doesn't parse as an integer. Returns
     * 0 for empty data.
     *
     * @return the integer value of the current tag
     * @throws IOException
     */
    public int getValueInt() throws IOException {
        final String val = getValue();
        if (val.length() == 0) {
            return 0;
        }

        int num;
        try {
            num = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new EasParserException("Tag " + startTag + ": " + e.getMessage());
        }
        return num;
    }

    /**
     * Return the next tag found in the stream; special tags END and END_DOCUMENT are used to
     * mark the end of the current tag and end of document.  If we hit end of document without
     * looking for it, generate an EodException.  The tag returned consists of the page number
     * shifted PAGE_SHIFT bits OR'd with the tag retrieved from the stream.  Thus, all tags returned
     * are unique.
     *
     * @param endingTag the tag that would represent the end of the tag we're processing
     * @return the next tag found
     * @throws IOException
     */
    public int nextTag(final int endingTag) throws IOException {
        while (getNext() != DONE) {
            // If we're a start, set tag to include the page and return it
            if (type == START) {
                tag = startTag.getTagNum();
                return tag;
            // If we're at the ending tag we're looking for, return the END signal
            } else if (type == END && startTag.getTagNum() == endingTag) {
                return END;
            }
        }
        // We're at end of document here.  If we're looking for it, return END_DOCUMENT
        if (endingTag == START_DOCUMENT) {
            return END_DOCUMENT;
        }
        // Otherwise, we've prematurely hit end of document, so exception out
        // EodException is a subclass of IOException; this will be treated as an IO error by
        // EasService
        throw new EodException();
    }

    /**
     * Skip anything found in the stream until the end of the current tag is reached.  This can be
     * used to ignore stretches of xml that aren't needed by the parser.
     *
     * @throws IOException
     */
    public void skipTag() throws IOException {
        final int thisTag = startTag.getTagNum();
        // Just loop until we hit the end of the current tag
        while (getNext() != DONE) {
            if (type == END && startTag.getTagNum() == thisTag) {
                return;
            }
        }

        // If we're at end of document, that's bad
        throw new EofException();
    }

    /**
     * Initializes the parser with an input stream; reads the first 4 bytes (which are always the
     * same in EAS, and then sets the tag table to point to page 0 (by definition, the starting
     * page).
     *
     * @param in the InputStream associated with this parser
     * @throws IOException
     */
    public void setInput(final InputStream in, final boolean initialize) throws IOException {
        this.in = in;
        if ((in != null) && initialize) {
            // If we fail on the very first byte, report an empty stream
            try {
                final int version = readByte(); // version
            } catch (EofException e) {
                throw new EmptyStreamException();
            }
            readInt();  // public identifier
            readInt();  // 106 (UTF-8)
            final int stringTableLength = readInt();  // string table length
            if (stringTableLength != 0) {
                throw new EasParserException("WBXML string table unsupported");
            }
        }
    }

    @VisibleForTesting
    void resetInput(final InputStream in) {
        this.in = in;
        try {
            // Read leading zero
            read();
        } catch (IOException e) {
        }
    }

    void log(final String str) {
        if (!EasService.getProtocolLogging()) {
            return;
        }
        final String logStr;
        int cr = str.indexOf('\n');
        if (cr > 0) {
            logStr = str.substring(0, cr);
        } else {
            logStr = str;
        }
        final char [] charArray = new char[startTagArray.size() * 2];
        Arrays.fill(charArray, ' ');
        final String indent = new String(charArray);
        LogUtils.d(LOG_TAG, "%s", indent + logStr);
        if (EasService.getFileLogging()) {
            FileLogger.log(LOG_TAG, logStr);
        }
    }

    void logVerbose(final String str) {
        if (LOG_VERBOSE) {
            log(str);
        }
    }

    protected void pushTag(final int id) {
        page = id >>> Tags.PAGE_SHIFT;
        push(id);
    }

    protected void pop() {
        // Retrieve the now-current startTag from our stack
        startTag = startTagArray.removeFirst();
        log("</" + startTag + '>');
    }

    private void push(final int id) {
        startTag = new Tag(page, id);
        noContent = startTag.mNoContent;
        log("<" + startTag + (noContent ? '/' : "") + '>');
        // Save the startTag to our stack
        startTagArray.addFirst(startTag);
    }

    /**
     * Return the next piece of data from the stream.  The return value indicates the type of data
     * that has been retrieved - START (start of tag), END (end of tag), DONE (end of stream), or
     * TEXT (the value of a tag)
     *
     * @return the type of data retrieved
     * @throws IOException
     */
    private final int getNext() throws IOException {
        bytes = null;
        text = null;

        if (noContent) {
            startTagArray.removeFirst();
            type = END;
            noContent = false;
            return type;
        }

        int id = read();
        while (id == Wbxml.SWITCH_PAGE) {
            // Get the new page number
            page = readByte();
            // Retrieve the current tag table
            if (!Tags.isValidPage(page)) {
                // Unknown code page. These seem to happen mostly because of
                // invalid data from the server so throw an exception here.
                throw new EasParserException("Unknown code page " + page);
            }
            logVerbose("Page: " + page);
            id = read();
        }

        switch (id) {
            case EOF_BYTE:
                // End of document
                type = DONE;
                break;

            case Wbxml.END:
                type = END;
                pop();
                break;

            case Wbxml.STR_I:
                // Inline string
                type = TEXT;
                text = readInlineString();
                log(startTag + ": " + text);
                break;

            case Wbxml.OPAQUE:
                // Integer length + opaque data
                type = OPAQUE;
                final int length = readInt();
                bytes = new byte[length];
                for (int i = 0; i < length; i++) {
                    bytes[i] = (byte)readByte();
                }
                log(startTag + ": (opaque:" + length + ") ");
                break;

            default:
                if (Tags.isGlobalTag(id & Tags.PAGE_MASK)) {
                    throw new EasParserException(String.format(
                                    "Unhandled WBXML global token 0x%02X", id));
                }
                if ((id & Wbxml.WITH_ATTRIBUTES) != 0) {
                    throw new EasParserException(String.format(
                                    "Attributes unsupported, tag 0x%02X", id));
                }
                type = START;
                push(id);
        }

        // Return the type of data we're dealing with
        return type;
    }

    /**
     * Read an int from the input stream, and capture it if necessary for debugging.  Seems a small
     * price to pay...
     *
     * @return the int read
     * @throws IOException
     */
    private int read() throws IOException {
        int i;
        i = in.read();
        if (capture) {
            captureArray.add(i);
        }
        logVerbose("Byte: " + i);
        return i;
    }

    private int readByte() throws IOException {
        int i = read();
        if (i == EOF_BYTE) {
            throw new EofException();
        }
        return i;
    }

    /**
     * Throws EasParserException if detects integer encoded with more than 5
     * bytes. A uint_32 needs 5 bytes to fully encode 32 bits so if the high
     * bit is set for more than 4 bytes, something is wrong with the data
     * stream.
     */
    private int readInt() throws IOException {
        int result = 0;
        int i;
        int numBytes = 0;

        do {
            if (++numBytes > 5) {
                throw new EasParserException("Invalid integer encoding, too many bytes");
            }
            i = readByte();
            result = (result << 7) | (i & 0x7f);
        } while ((i & 0x80) != 0);

        return result;
    }

    /**
     * Read an inline string from the stream
     *
     * @return the String as parsed from the stream
     * @throws IOException
     */
    private String readInlineString() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        while (true) {
            final int i = read();
            if (i == 0) {
                break;
            } else if (i == EOF_BYTE) {
                throw new EofException();
            }
            outputStream.write(i);
        }
        outputStream.flush();
        final String res = outputStream.toString("UTF-8");
        outputStream.close();
        return res;
    }
}
