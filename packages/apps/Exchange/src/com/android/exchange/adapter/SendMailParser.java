package com.android.exchange.adapter;

import java.io.IOException;
import java.io.InputStream;

public class SendMailParser extends Parser {
    private final int mStartTag;
    private int mStatus;

    public SendMailParser(final InputStream in, final int startTag) throws IOException {
        super(in);
        mStartTag = startTag;
    }

    public int getStatus() {
        return mStatus;
    }

    /**
     * The only useful info in the SendMail response is the status; we capture and save it
     */
    @Override
    public boolean parse() throws IOException {
        if (nextTag(START_DOCUMENT) != mStartTag) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.COMPOSE_STATUS) {
                mStatus = getValueInt();
            } else {
                skipTag();
            }
        }
        return true;
    }
}
