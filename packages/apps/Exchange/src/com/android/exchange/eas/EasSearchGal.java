package com.android.exchange.eas;

import android.content.Context;

import com.android.emailcommon.provider.Account;
import com.android.exchange.CommandStatusException;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.GalParser;
import com.android.exchange.adapter.Serializer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;

import com.android.exchange.adapter.Tags;
import com.android.exchange.provider.GalResult;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;

public class EasSearchGal extends EasOperation {

    public static final int RESULT_OK = 1;

    final private String mFilter;
    final private int mLimit;
    private GalResult mResult;

    public EasSearchGal(Context context, final Account account, final String filter,
                        final int limit) {
        super(context, account);
        mFilter = filter;
        mLimit = limit;
    }

    @Override
    protected String getCommand() {
        return "Search";
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException, MessageInvalidException {
        /*
         * TODO: shorter timeout for interactive lookup
         * TODO: make watchdog actually work (it doesn't understand our service w/Mailbox == 0)
         * TODO: figure out why sendHttpClientPost() hangs - possibly pool exhaustion
         */
        try {
            final Serializer s = new Serializer();
            s.start(Tags.SEARCH_SEARCH).start(Tags.SEARCH_STORE);
            s.data(Tags.SEARCH_NAME, "GAL").data(Tags.SEARCH_QUERY, mFilter);
            s.start(Tags.SEARCH_OPTIONS);
            s.data(Tags.SEARCH_RANGE, "0-" + Integer.toString(mLimit - 1));
            s.end().end().end().done();
            return makeEntity(s);
        } catch (final IOException e) {
            // TODO: what do we do for exceptions?
        } catch (final IllegalArgumentException e) {
        } catch (final IllegalStateException e) {
        }
        return null;
    }

    protected int handleResponse(final EasResponse response) throws
            IOException, CommandStatusException {
        final int code = response.getStatus();
        if (code == HttpStatus.SC_OK) {
            InputStream is = response.getInputStream();
            try {
                final GalParser gp = new GalParser(is);
                if (gp.parse()) {
                    mResult = gp.getGalResult();
                } else {
                    LogUtils.wtf(LogUtils.TAG, "Failure to parse GalResult");
                }
            } finally {
                is.close();
            }
            return RESULT_OK;
        } else {
            LogUtils.d(LogUtils.TAG, "GAL lookup returned %d", code);
            return RESULT_OTHER_FAILURE;
        }
    }

    public GalResult getResult() {
        return mResult;
    }

}
