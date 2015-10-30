/*
 * Copyright (C) 2015 The Android Open Source Project
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

//
// This implements the calculator evaluation logic.
// An evaluation is started with a call to evaluateAndShowResult().
// This starts an asynchronous computation, which requests display
// of the initial result, when available.  When initial evaluation is
// complete, it calls the calculator onEvaluate() method.
// This occurs in a separate event, and may happen quite a bit
// later.  Once a result has been computed, and before the underlying
// expression is modified, the getString method may be used to produce
// Strings that represent approximations to various precisions.
//
// Actual expressions being evaluated are represented as CalculatorExprs,
// which are just slightly preprocessed sequences of keypresses.
//
// The Evaluator owns the expression being edited and associated
// state needed for evaluating it. It provides functionality for
// saving and restoring this state.  However the current
// CalculatorExpr is exposed to the client, and may be directly modified
// after cancelling any in-progress computations by invoking the
// cancelAll() method.
//
// When evaluation is requested by the user, we invoke the eval
// method on the CalculatorExpr from a background AsyncTask.
// A subsequent getString() callback returns immediately, though it may
// return a result containing placeholder '?' characters.
// In that case we start a background task, which invokes the
// onReevaluate() callback when it completes.
// In both cases, the background task
// computes the appropriate result digits by evaluating
// the constructive real (CR) returned by CalculatorExpr.eval()
// to the required precision.
//
// We cache the best approximation we have already computed.
// We compute generously to allow for
// some scrolling without recomputation and to minimize the chance of
// digits flipping from "0000" to "9999".  The best known
// result approximation is maintained as a string by mCache (and
// in a different format by the CR representation of the result).
// When we are in danger of not having digits to display in response
// to further scrolling, we initiate a background computation to higher
// precision.  If we actually do fall behind, we display placeholder
// characters, e.g. blanks, and schedule a display update when the computation
// completes.
// The code is designed to ensure that the error in the displayed
// result (excluding any placeholder characters) is always strictly less than 1 in
// the last displayed digit.  Typically we actually display a prefix
// of a result that has this property and additionally is computed to
// a significantly higher precision.  Thus we almost always round correctly
// towards zero.  (Fully correct rounding towards zero is not computable.)
//
// Initial expression evaluation may time out.  This may happen in the
// case of domain errors such as division by zero, or for large computations.
// We do not currently time out reevaluations to higher precision, since
// the original evaluation prevcluded a domain error that could result
// in non-termination.  (We may discover that a presumed zero result is
// actually slightly negative when re-evaluated; but that results in an
// exception, which we can handle.)  The user can abort either kind
// of computation.
//
// We ensure that only one evaluation of either kind (AsyncReevaluator
// or AsyncDisplayResult) is running at a time.

package com.android.calculator2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hp.creals.CR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

class Evaluator {

    private static final String KEY_PREF_DEGREE_MODE = "degree_mode";

    private final Calculator mCalculator;
    private final CalculatorResult mResult;  // The result display View
    private CalculatorExpr mExpr;      // Current calculator expression
    private CalculatorExpr mSaved;     // Last saved expression.
                                       // Either null or contains a single
                                       // preevaluated node.
    private String mSavedName;         // A hopefully unique name associated
                                       // with mSaved.
    // The following are valid only if an evaluation
    // completed successfully.
        private CR mVal;               // value of mExpr as constructive real
        private BoundedRational mRatVal; // value of mExpr as rational or null
        private int mLastDigs;   // Last digit argument passed to getString()
                                 // for this result, or the initial preferred
                                 // precision.
    private boolean mDegreeMode;       // Currently in degree (not radian) mode
    private final Handler mTimeoutHandler;

    static final BigInteger BIG_MILLION = BigInteger.valueOf(1000000);

    private static final int EXTRA_DIGITS = 20;
                // Extra computed digits to minimize probably we will have
                // to change our minds about digits we already displayed.
                // (The correct digits are technically not computable using our
                // representation:  An off by one error in the last digits
                // can affect earlier ones, even though the display is
                // always within one in the lsd.  This is only visible
                // for results that end in EXTRA_DIGITS 9s or 0s, but are
                // not integers.)
                // We do use these extra digits to display while we are
                // computing the correct answer.  Thus they may be
                // temporarily visible.
   private static final int EXTRA_DIVISOR = 5;
                // We add the length of the previous result divided by
                // EXTRA_DIVISOR to try to recover recompute latency when
                // scrolling through a long result.
   private static final int PRECOMPUTE_DIGITS = 30;
   private static final int PRECOMPUTE_DIVISOR = 5;
                // When we have to reevaluate, we compute an extra
                // PRECOMPUTE_DIGITS
                // + <current_result_length>/PRECOMPUTE_DIVISOR digits.
                // The last term is dropped if prec < 0.

    // We cache the result as a string to accelerate scrolling.
    // The cache is filled in by the UI thread, but this may
    // happen asynchronously, much later than the request.
    private String mCache;       // Current best known result, which includes
    private int mCacheDigs = 0;  // mCacheDigs digits to the right of the
                                 // decimal point.  Always positive.
                                 // mCache is valid when non-null
                                 // unless the expression has been
                                 // changed since the last evaluation call.
    private int mCacheDigsReq;  // Number of digits that have been
                                // requested.  Only touched by UI
                                // thread.
    public static final int INVALID_MSD = Integer.MAX_VALUE;
    private int mMsd = INVALID_MSD;  // Position of most significant digit
                                     // in current cached result, if determined.
                                     // This is just the index in mCache
                                     // holding the msd.
    private static final int INIT_PREC = 50;
                             // Initial evaluation precision.  Enough to guarantee
                             // that we can compute the short representation, and that
                             // we rarely have to evaluate nonzero results to
                             // MAX_MSD_PREC.  It also helps if this is at least
                             // EXTRA_DIGITS + display width, so that we don't
                             // immediately need a second evaluation.
    private static final int MAX_MSD_PREC = 320;
                             // The largest number of digits to the right
                             // of the decimal point to which we will
                             // evaluate to compute proper scientific
                             // notation for values close to zero.
                             // Chosen to ensure that we always to better than
                             // IEEE double precision at identifying nonzeros.
    private static final int EXP_COST = 3;
                             // If we can replace an exponent by this many leading zeroes,
                             // we do so.  Also used in estimating exponent size for
                             // truncating short representation.

    private AsyncReevaluator mCurrentReevaluator;
        // The one and only un-cancelled and currently running reevaluator.
        // Touched only by UI thread.

    private AsyncDisplayResult mEvaluator;
        // Currently running expression evaluator, if any.

    private boolean mChangedValue;
        // The expression may have changed since the last evaluation in ways that would
        // affect its value.

    private SharedPreferences mSharedPrefs;

    Evaluator(Calculator calculator,
              CalculatorResult resultDisplay) {
        mCalculator = calculator;
        mResult = resultDisplay;
        mExpr = new CalculatorExpr();
        mSaved = new CalculatorExpr();
        mSavedName = "none";
        mTimeoutHandler = new Handler();

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(calculator);
        mDegreeMode = mSharedPrefs.getBoolean(KEY_PREF_DEGREE_MODE, false);
    }

    // Result of asynchronous reevaluation
    class ReevalResult {
        ReevalResult(String s, int p) {
            mNewCache = s;
            mNewCacheDigs = p;
        }
        final String mNewCache;
        final int mNewCacheDigs;
    }

    // Compute new cache contents accurate to prec digits to the right
    // of the decimal point.  Ensure that redisplay() is called after
    // doing so.  If the evaluation fails for reasons other than a
    // timeout, ensure that DisplayError() is called.
    class AsyncReevaluator extends AsyncTask<Integer, Void, ReevalResult> {
        @Override
        protected ReevalResult doInBackground(Integer... prec) {
            try {
                int eval_prec = prec[0].intValue();
                return new ReevalResult(mVal.toString(eval_prec), eval_prec);
            } catch(ArithmeticException e) {
                return null;
            } catch(CR.PrecisionOverflowException e) {
                return null;
            } catch(CR.AbortedException e) {
                // Should only happen if the task was cancelled,
                // in which case we don't look at the result.
                return null;
            }
        }
        @Override
        protected void onPostExecute(ReevalResult result) {
            if (result == null) {
                // This should only be possible in the extremely rare
                // case of encountering a domain error while reevaluating
                // or in case of a precision overflow.  We don't know of
                // a way to get the latter with a plausible amount of
                // user input.
                mCalculator.onError(R.string.error_nan);
            } else {
                if (result.mNewCacheDigs < mCacheDigs) {
                    throw new AssertionError("Unexpected onPostExecute timing");
                }
                mCache = result.mNewCache;
                mCacheDigs = result.mNewCacheDigs;
                mCalculator.onReevaluate();
            }
            mCurrentReevaluator = null;
        }
        // On cancellation we do nothing; invoker should have
        // left no trace of us.
    }

    // Result of initial asynchronous computation
    private static class InitialResult {
        InitialResult(CR val, BoundedRational ratVal, String s, int p, int idp) {
            mErrorResourceId = Calculator.INVALID_RES_ID;
            mVal = val;
            mRatVal = ratVal;
            mNewCache = s;
            mNewCacheDigs = p;
            mInitDisplayPrec = idp;
        }
        InitialResult(int errorResourceId) {
            mErrorResourceId = errorResourceId;
            mVal = CR.valueOf(0);
            mRatVal = BoundedRational.ZERO;
            mNewCache = "BAD";
            mNewCacheDigs = 0;
            mInitDisplayPrec = 0;
        }
        boolean isError() {
            return mErrorResourceId != Calculator.INVALID_RES_ID;
        }
        final int mErrorResourceId;
        final CR mVal;
        final BoundedRational mRatVal;
        final String mNewCache;       // Null iff it can't be computed.
        final int mNewCacheDigs;
        final int mInitDisplayPrec;
    }

    private void displayCancelledMessage() {
        new AlertDialog.Builder(mCalculator)
            .setMessage(R.string.cancelled)
            .setPositiveButton(R.string.dismiss,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) { }
                })
            .create()
            .show();
    }

    private final long MAX_TIMEOUT = 15000;
                                   // Milliseconds.
                                   // Longer is unlikely to help unless
                                   // we get more heap space.
    private long mTimeout = 2000;  // Timeout for requested evaluations,
                                   // in milliseconds.
                                   // This is currently not saved and restored
                                   // with the state; we reset
                                   // the timeout when the
                                   // calculator is restarted.
                                   // We'll call that a feature; others
                                   // might argue it's a bug.
    private final long QUICK_TIMEOUT = 1000;
                                   // Timeout for unrequested, speculative
                                   // evaluations, in milliseconds.
    private int mMaxResultBits = 120000;             // Don't try to display a larger result.
    private final int MAX_MAX_RESULT_BITS = 350000;  // Long timeout version.
    private final int QUICK_MAX_RESULT_BITS = 50000; // Instant result version.

    private void displayTimeoutMessage() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mCalculator)
                .setMessage(R.string.timeout)
                .setNegativeButton(R.string.dismiss, null /* listener */);
        if (mTimeout != MAX_TIMEOUT) {
            builder.setPositiveButton(R.string.ok_remove_timeout,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int which) {
                            mTimeout = MAX_TIMEOUT;
                            mMaxResultBits = MAX_MAX_RESULT_BITS;
                        }
                    });
        }
        builder.show();
    }

    // Compute initial cache contents and result when we're good and ready.
    // We leave the expression display up, with scrolling
    // disabled, until this computation completes.
    // Can result in an error display if something goes wrong.
    // By default we set a timeout to catch runaway computations.
    class AsyncDisplayResult extends AsyncTask<Void, Void, InitialResult> {
        private boolean mDm;  // degrees
        private boolean mRequired; // Result was requested by user.
        private boolean mQuiet;  // Suppress cancellation message.
        private Runnable mTimeoutRunnable = null;
        AsyncDisplayResult(boolean dm, boolean required) {
            mDm = dm;
            mRequired = required;
            mQuiet = !required;
        }
        private void handleTimeOut() {
            boolean running = (getStatus() != AsyncTask.Status.FINISHED);
            if (running && cancel(true)) {
                mEvaluator = null;
                // Replace mExpr with clone to avoid races if task
                // still runs for a while.
                mExpr = (CalculatorExpr)mExpr.clone();
                if (mRequired) {
                    suppressCancelMessage();
                    displayTimeoutMessage();
                }
            }
        }
        private void suppressCancelMessage() {
            mQuiet = true;
        }
        @Override
        protected void onPreExecute() {
            long timeout = mRequired ? mTimeout : QUICK_TIMEOUT;
            mTimeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    handleTimeOut();
                }
            };
            mTimeoutHandler.postDelayed(mTimeoutRunnable, timeout);
        }
        private boolean isTooBig(CalculatorExpr.EvalResult res) {
            int maxBits = mRequired ? mMaxResultBits : QUICK_MAX_RESULT_BITS;
            if (res.mRatVal != null) {
                return res.mRatVal.wholeNumberBits() > maxBits;
            } else {
                return res.mVal.get_appr(maxBits).bitLength() > 2;
            }
        }
        @Override
        protected InitialResult doInBackground(Void... nothing) {
            try {
                CalculatorExpr.EvalResult res = mExpr.eval(mDm);
                if (isTooBig(res)) {
                    // Avoid starting a long uninterruptible decimal conversion.
                    return new InitialResult(R.string.timeout);
                }
                int prec = INIT_PREC;
                String initCache = res.mVal.toString(prec);
                int msd = getMsdPos(initCache);
                if (BoundedRational.asBigInteger(res.mRatVal) == null
                        && msd == INVALID_MSD) {
                    prec = MAX_MSD_PREC;
                    initCache = res.mVal.toString(prec);
                    msd = getMsdPos(initCache);
                }
                int lsd = getLsd(res.mRatVal, initCache, initCache.indexOf('.'));
                int initDisplayPrec = getPreferredPrec(initCache, msd, lsd);
                int newPrec = initDisplayPrec + EXTRA_DIGITS;
                if (newPrec > prec) {
                    prec = newPrec;
                    initCache = res.mVal.toString(prec);
                }
                return new InitialResult(res.mVal, res.mRatVal,
                                         initCache, prec, initDisplayPrec);
            } catch (CalculatorExpr.SyntaxException e) {
                return new InitialResult(R.string.error_syntax);
            } catch (BoundedRational.ZeroDivisionException e) {
                // Division by zero caught by BoundedRational;
                // the easy and more common case.
                return new InitialResult(R.string.error_zero_divide);
            } catch(ArithmeticException e) {
                return new InitialResult(R.string.error_nan);
            } catch(CR.PrecisionOverflowException e) {
                // Extremely unlikely unless we're actually dividing by
                // zero or the like.
                return new InitialResult(R.string.error_overflow);
            } catch(CR.AbortedException e) {
                return new InitialResult(R.string.error_aborted);
            }
        }
        @Override
        protected void onPostExecute(InitialResult result) {
            mEvaluator = null;
            mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
            if (result.isError()) {
                if (result.mErrorResourceId == R.string.timeout) {
                    if (mRequired) {
                        displayTimeoutMessage();
                    }
                    mCalculator.onCancelled();
                } else {
                    mCalculator.onError(result.mErrorResourceId);
                }
                return;
            }
            mVal = result.mVal;
            mRatVal = result.mRatVal;
            mCache = result.mNewCache;
            mCacheDigs = result.mNewCacheDigs;
            mLastDigs = result.mInitDisplayPrec;
            int dotPos = mCache.indexOf('.');
            String truncatedWholePart = mCache.substring(0, dotPos);
            // Recheck display precision; it may change, since
            // display dimensions may have been unknow the first time.
            // In that case the initial evaluation precision should have
            // been conservative.
            // TODO: Could optimize by remembering display size and
            // checking for change.
            int init_prec = result.mInitDisplayPrec;
            int msd = getMsdPos(mCache);
            int leastDigPos = getLsd(mRatVal, mCache, dotPos);
            int new_init_prec = getPreferredPrec(mCache, msd, leastDigPos);
            if (new_init_prec < init_prec) {
                init_prec = new_init_prec;
            } else {
                // They should be equal.  But nothing horrible should
                // happen if they're not. e.g. because
                // CalculatorResult.MAX_WIDTH was too small.
            }
            mCalculator.onEvaluate(init_prec, msd, leastDigPos, truncatedWholePart);
        }
        @Override
        protected void onCancelled(InitialResult result) {
            // Invoker resets mEvaluator.
            mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
            if (mRequired && !mQuiet) {
                displayCancelledMessage();
            } // Otherwise timeout processing displayed message.
            mCalculator.onCancelled();
            // Just drop the evaluation; Leave expression displayed.
            return;
        }
    }


    // Start an evaluation to prec, and ensure that the
    // display is redrawn when it completes.
    private void ensureCachePrec(int prec) {
        if (mCache != null && mCacheDigs >= prec
                || mCacheDigsReq >= prec) return;
        if (mCurrentReevaluator != null) {
            // Ensure we only have one evaluation running at a time.
            mCurrentReevaluator.cancel(true);
            mCurrentReevaluator = null;
        }
        mCurrentReevaluator = new AsyncReevaluator();
        mCacheDigsReq = prec + PRECOMPUTE_DIGITS;
        if (mCache != null) {
            mCacheDigsReq += mCacheDigsReq / PRECOMPUTE_DIVISOR;
        }
        mCurrentReevaluator.execute(mCacheDigsReq);
    }

    /**
     * Return the rightmost nonzero digit position, if any.
     * @param ratVal Rational value of result or null.
     * @param cache Current cached decimal string representation of result.
     * @param decPos Index of decimal point in cache.
     * @result Position of rightmost nonzero digit relative to decimal point.
     *         Integer.MIN_VALUE if ratVal is zero.  Integer.MAX_VALUE if there is no lsd,
     *         or we cannot determine it.
     */
    int getLsd(BoundedRational ratVal, String cache, int decPos) {
        if (ratVal != null && ratVal.signum() == 0) return Integer.MIN_VALUE;
        int result = BoundedRational.digitsRequired(ratVal);
        if (result == 0) {
            int i;
            for (i = -1; decPos + i > 0 && cache.charAt(decPos + i) == '0'; --i) { }
            result = i;
        }
        return result;
    }

    /**
     * Retrieve the preferred precision for the currently displayed result.
     * May be called from non-UI thread.
     * @param cache Current approximation as string.
     * @param msd Position of most significant digit in result.  Index in cache.
     *            Can be INVALID_MSD if we haven't found it yet.
     * @param lastDigit Position of least significant digit (1 = tenths digit)
     *                  or Integer.MAX_VALUE.
     */
    int getPreferredPrec(String cache, int msd, int lastDigit) {
        int lineLength = mResult.getMaxChars();
        int wholeSize = cache.indexOf('.');
        int negative = cache.charAt(0) == '-' ? 1 : 0;
        // Don't display decimal point if result is an integer.
        if (lastDigit == 0) lastDigit = -1;
        if (lastDigit != Integer.MAX_VALUE) {
            if (wholeSize <= lineLength && lastDigit <= 0) {
                // Exact integer.  Prefer to display as integer, without decimal point.
                return -1;
            }
            if (lastDigit >= 0 && wholeSize + lastDigit + 1 /* dec.pt. */ <= lineLength) {
                // Display full exact number wo scientific notation.
                return lastDigit;
            }
        }
        if (msd > wholeSize && msd <= wholeSize + EXP_COST + 1) {
            // Display number without scientific notation.  Treat leading zero as msd.
            msd = wholeSize - 1;
        }
        if (msd > wholeSize + MAX_MSD_PREC) {
            // Display a probable but uncertain 0 as "0.000000000",
            // without exponent.  That's a judgment call, but less likely
            // to confuse naive users.  A more informative and confusing
            // option would be to use a large negative exponent.
            return lineLength - 2;
        }
        // Return position corresponding to having msd at left, effectively
        // presuming scientific notation that preserves the left part of the
        // result.
        return msd - wholeSize + lineLength - negative - 1;
    }

    private static final int SHORT_TARGET_LENGTH  = 8;
    private static final String SHORT_UNCERTAIN_ZERO = "0.00000" + KeyMaps.ELLIPSIS;

    /**
     * Get a short representation of the value represented by the string cache.
     * We try to match the CalculatorResult code when the result is finite
     * and small enough to suit our needs.
     * The result is not internationalized.
     * @param cache String approximation of value.  Assumed to be long enough
     *              that if it doesn't contain enough significant digits, we can
     *              reasonably abbreviate as SHORT_UNCERTAIN_ZERO.
     * @param msdIndex Index of most significant digit in cache, or INVALID_MSD.
     * @param lsd Position of least significant digit in finite representation,
     *            relative to decimal point, or MAX_VALUE.
     */
    private String getShortString(String cache, int msdIndex, int lsd) {
        // This somewhat mirrors the display formatting code, but
        // - The constants are different, since we don't want to use the whole display.
        // - This is an easier problem, since we don't support scrolling and the length
        //   is a bit flexible.
        // TODO: Think about refactoring this to remove partial redundancy with CalculatorResult.
        final int dotIndex = cache.indexOf('.');
        final int negative = cache.charAt(0) == '-' ? 1 : 0;
        final String negativeSign = negative == 1 ? "-" : "";

        // Ensure we don't have to worry about running off the end of cache.
        if (msdIndex >= cache.length() - SHORT_TARGET_LENGTH) {
            msdIndex = INVALID_MSD;
        }
        if (msdIndex == INVALID_MSD) {
            if (lsd < INIT_PREC) {
                return "0";
            } else {
                return SHORT_UNCERTAIN_ZERO;
            }
        }
        // Avoid scientific notation for small numbers of zeros.
        // Instead stretch significant digits to include decimal point.
        if (lsd < -1 && dotIndex - msdIndex + negative <= SHORT_TARGET_LENGTH
            && lsd >= -CalculatorResult.MAX_TRAILING_ZEROES - 1) {
            // Whole number that fits in allotted space.
            // CalculatorResult would not use scientific notation either.
            lsd = -1;
        }
        if (msdIndex > dotIndex) {
            if (msdIndex <= dotIndex + EXP_COST + 1) {
                // Preferred display format inthis cases is with leading zeroes, even if
                // it doesn't fit entirely.  Replicate that here.
                msdIndex = dotIndex - 1;
            } else if (lsd <= SHORT_TARGET_LENGTH - negative - 2
                    && lsd <= CalculatorResult.MAX_LEADING_ZEROES + 1) {
                // Fraction that fits entirely in allotted space.
                // CalculatorResult would not use scientific notation either.
                msdIndex = dotIndex -1;
            }
        }
        int exponent = dotIndex - msdIndex;
        if (exponent > 0) {
            // Adjust for the fact that the decimal point itself takes space.
            exponent--;
        }
        if (lsd != Integer.MAX_VALUE) {
            int lsdIndex = dotIndex + lsd;
            int totalDigits = lsdIndex - msdIndex + negative + 1;
            if (totalDigits <= SHORT_TARGET_LENGTH && dotIndex > msdIndex && lsd >= -1) {
                // Fits, no exponent needed.
                return negativeSign + cache.substring(msdIndex, lsdIndex + 1);
            }
            if (totalDigits <= SHORT_TARGET_LENGTH - 3) {
                return negativeSign + cache.charAt(msdIndex) + "."
                        + cache.substring(msdIndex + 1, lsdIndex + 1) + "E" + exponent;
            }
        }
        // We need to abbreviate.
        if (dotIndex > msdIndex && dotIndex < msdIndex + SHORT_TARGET_LENGTH - negative - 1) {
            return negativeSign + cache.substring(msdIndex,
                    msdIndex + SHORT_TARGET_LENGTH - negative - 1) + KeyMaps.ELLIPSIS;
        }
        // Need abbreviation + exponent
        return negativeSign + cache.charAt(msdIndex) + "."
                + cache.substring(msdIndex + 1, msdIndex + SHORT_TARGET_LENGTH - negative - 4)
                + KeyMaps.ELLIPSIS + "E" + exponent;
    }

    // Return the most significant digit position in the given string
    // or INVALID_MSD.
    public static int getMsdPos(String s) {
        int len = s.length();
        int nonzeroPos = -1;
        for (int i = 0; i < len; ++i) {
            char c = s.charAt(i);
            if (c != '-' && c != '.' && c != '0') {
                nonzeroPos = i;
                break;
            }
        }
        if (nonzeroPos >= 0 &&
            (nonzeroPos < len - 1 || s.charAt(nonzeroPos) != '1')) {
                return nonzeroPos;
        } else {
            // Unknown, or could change on reevaluation
            return INVALID_MSD;
        }
    }

    // Return most significant digit position in the cache, if determined,
    // INVALID_MSD ow.
    // If unknown, and we've computed less than DESIRED_PREC,
    // schedule reevaluation and redisplay, with higher precision.
    int getMsd() {
        if (mMsd != INVALID_MSD) return mMsd;
        if (mRatVal != null && mRatVal.signum() == 0) {
            return INVALID_MSD;  // None exists
        }
        int res = INVALID_MSD;
        if (mCache != null) {
            res = getMsdPos(mCache);
        }
        if (res == INVALID_MSD && mEvaluator == null
            && mCurrentReevaluator == null && mCacheDigs < MAX_MSD_PREC) {
            // We assert that mCache is not null, since there is no
            // evaluator running.
            ensureCachePrec(MAX_MSD_PREC);
            // Could reevaluate more incrementally, but we suspect that if
            // we have to reevaluate at all, the result is probably zero.
        }
        return res;
    }

    // Return a string with n placeholder characters.
    private String getPadding(int n) {
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < n; ++i) {
            padding.append(' ');   // To be replaced during final translation.
        }
        return padding.toString();
    }

    // Return the number of zero characters at the beginning of s
    private int leadingZeroes(String s) {
        int res = 0;
        int len = s.length();
        for (res = 0; res < len && s.charAt(res) == '0'; ++res) {}
        return res;
    }

    private static final int MIN_DIGS = 5;
            // Leave at least this many digits from the whole number
            // part on the screen, to avoid silly displays like 1E1.
    // Return result to exactly prec[0] digits to the right of the
    // decimal point.
    // The result should be no longer than maxDigs.
    // No exponent or other indication of precision is added.
    // The result is returned immediately, based on the
    // current cache contents, but it may contain question
    // marks for unknown digits.  It may also use uncertain
    // digits within EXTRA_DIGITS.  If either of those occurred,
    // schedule a reevaluation and redisplay operation.
    // Uncertain digits never appear to the left of the decimal point.
    // digs may be negative to only retrieve digits to the left
    // of the decimal point.  (prec[0] = 0 means we include
    // the decimal point, but nothing to the right.  prec[0] = -1
    // means we drop the decimal point and start at the ones
    // position.  Should not be invoked if mVal is null.
    // This essentially just returns a substring of the full result;
    // a leading minus sign or leading digits can be dropped.
    // Result uses US conventions; is NOT internationalized.
    // We set negative[0] if the number as a whole is negative,
    // since we may drop the minus sign.
    // We set truncated[0] if leading nonzero digits were dropped.
    // getRational() can be used to determine whether the result
    // is exact, or whether we dropped trailing digits.
    // If the requested prec[0] value is out of range, we update
    // it in place and use the updated value.  But we do not make it
    // greater than maxPrec.
    public String getString(int[] prec, int maxPrec, int maxDigs,
                            boolean[] truncated, boolean[] negative) {
        int digs = prec[0];
        mLastDigs = digs;
        // Make sure we eventually get a complete answer
            if (mCache == null) {
                ensureCachePrec(digs + EXTRA_DIGITS);
                // Nothing else to do now; seems to happen on rare occasion
                // with weird user input timing;
                // Will repair itself in a jiffy.
                return getPadding(1);
            } else {
                ensureCachePrec(digs + EXTRA_DIGITS
                        + mCache.length() / EXTRA_DIVISOR);
            }
        // Compute an appropriate substring of mCache.
        // We avoid returning a huge string to minimize string
        // allocation during scrolling.
        // Pad as needed.
            final int len = mCache.length();
            final boolean myNegative = mCache.charAt(0) == '-';
            negative[0] = myNegative;
            // Don't scroll left past leftmost digits in mCache
            // unless that still leaves an integer.
                int integralDigits = len - mCacheDigs;
                                // includes 1 for dec. pt
                if (myNegative) --integralDigits;
                int minDigs = Math.min(-integralDigits + MIN_DIGS, -1);
                digs = Math.min(Math.max(digs, minDigs), maxPrec);
                prec[0] = digs;
            int offset = mCacheDigs - digs; // trailing digits to drop
            int deficit = 0;  // The number of digits we're short
            if (offset < 0) {
                offset = 0;
                deficit = Math.min(digs - mCacheDigs, maxDigs);
            }
            int endIndx = len - offset;
            if (endIndx < 1) return " ";
            int startIndx = (endIndx + deficit <= maxDigs) ?
                                0
                                : endIndx + deficit - maxDigs;
            truncated[0] = (startIndx > getMsd());
            String res = mCache.substring(startIndx, endIndx);
            if (deficit > 0) {
                res = res + getPadding(deficit);
                // Since we always compute past the decimal point,
                // this never fills in the spot where the decimal point
                // should go, and the rest of this can treat the
                // made-up symbols as though they were digits.
            }
            return res;
    }

    // Return rational representation of current result, if any.
    public BoundedRational getRational() {
        return mRatVal;
    }

    private void clearCache() {
        mCache = null;
        mCacheDigs = mCacheDigsReq = 0;
        mMsd = INVALID_MSD;
    }

    void clear() {
        mExpr.clear();
        clearCache();
    }

    /**
     * Start asynchronous result evaluation of formula.
     * Will result in display on completion.
     * @param required result was explicitly requested by user.
     */
    private void reevaluateResult(boolean required) {
        clearCache();
        mEvaluator = new AsyncDisplayResult(mDegreeMode, required);
        mEvaluator.execute();
        mChangedValue = false;
    }

    // Begin evaluation of result and display when ready.
    // We assume this is called after each insertion and deletion.
    // Thus if we are called twice with the same effective end of
    // the formula, the evaluation is redundant.
    void evaluateAndShowResult() {
        if (!mChangedValue) {
            // Already done or in progress.
            return;
        }
        // In very odd cases, there can be significant latency to evaluate.
        // Don't show obsolete result.
        mResult.clear();
        reevaluateResult(false);
    }

    // Ensure that we either display a result or complain.
    // Does not invalidate a previously computed cache.
    // We presume that any prior result was computed using the same
    // expression.
    void requireResult() {
        if (mCache == null || mChangedValue) {
            // Restart evaluator in requested mode, i.e. with longer timeout.
            cancelAll(true);
            reevaluateResult(true);
        } else {
            // Notify immediately, reusing existing result.
            int dotPos = mCache.indexOf('.');
            String truncatedWholePart = mCache.substring(0, dotPos);
            int leastDigOffset = getLsd(mRatVal, mCache, dotPos);
            int msdIndex = getMsd();
            int preferredPrecOffset = getPreferredPrec(mCache, msdIndex, leastDigOffset);
            mCalculator.onEvaluate(preferredPrecOffset, msdIndex, leastDigOffset,
                    truncatedWholePart);
        }
    }

    /**
     * Cancel all current background tasks.
     * @param quiet suppress cancellation message
     * @return      true if we cancelled an initial evaluation
     */
    boolean cancelAll(boolean quiet) {
        if (mCurrentReevaluator != null) {
            mCurrentReevaluator.cancel(true);
            mCacheDigsReq = mCacheDigs;
            // Backgound computation touches only constructive reals.
            // OK not to wait.
            mCurrentReevaluator = null;
        }
        if (mEvaluator != null) {
            if (quiet) {
                mEvaluator.suppressCancelMessage();
            }
            mEvaluator.cancel(true);
            // There seems to be no good way to wait for cancellation
            // to complete, and the evaluation continues to look at
            // mExpr, which we will again modify.
            // Give ourselves a new copy to work on instead.
            mExpr = (CalculatorExpr)mExpr.clone();
            // Approximation of constructive reals should be thread-safe,
            // so we can let that continue until it notices the cancellation.
            mEvaluator = null;
            mChangedValue = true;    // Didn't do the expected evaluation.
            return true;
        }
        return false;
    }

    void restoreInstanceState(DataInput in) {
        mChangedValue = true;
        try {
            CalculatorExpr.initExprInput();
            mDegreeMode = in.readBoolean();
            mExpr = new CalculatorExpr(in);
            mSavedName = in.readUTF();
            mSaved = new CalculatorExpr(in);
        } catch (IOException e) {
            Log.v("Calculator", "Exception while restoring:\n" + e);
        }
    }

    void saveInstanceState(DataOutput out) {
        try {
            CalculatorExpr.initExprOutput();
            out.writeBoolean(mDegreeMode);
            mExpr.write(out);
            out.writeUTF(mSavedName);
            mSaved.write(out);
        } catch (IOException e) {
            Log.v("Calculator", "Exception while saving state:\n" + e);
        }
    }

    // Append a button press to the current expression.
    // Return false if we rejected the insertion due to obvious
    // syntax issues, and the expression is unchanged.
    // Return true otherwise.
    boolean append(int id) {
        if (id == R.id.fun_10pow) {
            add10pow();  // Handled as macro expansion.
            return true;
        } else {
            mChangedValue = mChangedValue || !KeyMaps.isBinary(id);
            return mExpr.add(id);
        }
    }

    void delete() {
        mChangedValue = true;
        mExpr.delete();
    }

    void setDegreeMode(boolean degreeMode) {
        mChangedValue = true;
        mDegreeMode = degreeMode;

        mSharedPrefs.edit()
                .putBoolean(KEY_PREF_DEGREE_MODE, degreeMode)
                .apply();
    }

    boolean getDegreeMode() {
        return mDegreeMode;
    }

    /**
     * @return the {@link CalculatorExpr} representation of the current result
     */
    CalculatorExpr getResultExpr() {
        final int dotPos = mCache.indexOf('.');
        final int leastDigPos = getLsd(mRatVal, mCache, dotPos);
        return mExpr.abbreviate(mVal, mRatVal, mDegreeMode,
               getShortString(mCache, getMsdPos(mCache), leastDigPos));
    }

    // Abbreviate the current expression to a pre-evaluated
    // expression node, which will display as a short number.
    // This should not be called unless the expression was
    // previously evaluated and produced a non-error result.
    // Pre-evaluated expressions can never represent an
    // expression for which evaluation to a constructive real
    // diverges.  Subsequent re-evaluation will also not diverge,
    // though it may generate errors of various kinds.
    // E.g. sqrt(-10^-1000)
    void collapse() {
        final CalculatorExpr abbrvExpr = getResultExpr();
        clear();
        mExpr.append(abbrvExpr);
        mChangedValue = true;
    }

    // Same as above, but put result in mSaved, leaving mExpr alone.
    // Return false if result is unavailable.
    boolean collapseToSaved() {
        if (mCache == null) {
            return false;
        }

        final CalculatorExpr abbrvExpr = getResultExpr();
        mSaved.clear();
        mSaved.append(abbrvExpr);
        return true;
    }

    Uri uriForSaved() {
        return new Uri.Builder().scheme("tag")
                                .encodedOpaquePart(mSavedName)
                                .build();
    }

    // Collapse the current expression to mSaved and return a URI
    // describing this particular result, so that we can refer to it
    // later.
    Uri capture() {
        if (!collapseToSaved()) return null;
        // Generate a new (entirely private) URI for this result.
        // Attempt to conform to RFC4151, though it's unclear it matters.
        Date date = new Date();
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(tz);
        String isoDate = df.format(new Date());
        mSavedName = "calculator2.android.com," + isoDate + ":"
                     + (new Random().nextInt() & 0x3fffffff);
        Uri tag = uriForSaved();
        return tag;
    }

    boolean isLastSaved(Uri uri) {
        return uri.equals(uriForSaved());
    }

    void addSaved() {
        mChangedValue = true;
        mExpr.append(mSaved);
    }

    // Add the power of 10 operator to the expression.  This is treated
    // essentially as a macro expansion.
    private void add10pow() {
        CalculatorExpr ten = new CalculatorExpr();
        ten.add(R.id.digit_1);
        ten.add(R.id.digit_0);
        mChangedValue = true;  // For consistency.  Reevaluation is probably not useful.
        mExpr.append(ten);
        mExpr.add(R.id.op_pow);
    }

    // Retrieve the main expression being edited.
    // It is the callee's reponsibility to call cancelAll to cancel
    // ongoing concurrent computations before modifying the result.
    // TODO: Perhaps add functionality so we can keep this private?
    CalculatorExpr getExpr() {
        return mExpr;
    }

    private static final int MAX_EXP_CHARS = 8;

    /**
     * Return the index of the character after the exponent starting at s[offset].
     * Return offset if there is no exponent at that position.
     * Exponents have syntax E[-]digit* .
     * "E2" and "E-2" are valid.  "E+2" and "e2" are not.
     * We allow any Unicode digits, and either of the commonly used minus characters.
     */
    static int exponentEnd(String s, int offset) {
        int i = offset;
        int len = s.length();
        if (i >= len - 1 || s.charAt(i) != 'E') {
            return offset;
        }
        ++i;
        if (KeyMaps.keyForChar(s.charAt(i)) == R.id.op_sub) {
            ++i;
        }
        if (i == len || i > offset + MAX_EXP_CHARS || !Character.isDigit(s.charAt(i))) {
            return offset;
        }
        ++i;
        while (i < len && Character.isDigit(s.charAt(i))) {
            ++i;
        }
        return i;
    }

    /**
     * Add the exponent represented by s[begin..end) to the constant at the end of current
     * expression.
     * The end of the current expression must be a constant.
     * Exponents have the same syntax as for exponentEnd().
     */
    void addExponent(String s, int begin, int end) {
        int sign = 1;
        int exp = 0;
        int i = begin + 1;
        // We do the decimal conversion ourselves to exactly match exponentEnd() conventions
        // and handle various kinds of digits on input.  Also avoids allocation.
        if (KeyMaps.keyForChar(s.charAt(i)) == R.id.op_sub) {
            sign = -1;
            ++i;
        }
        for (; i < end; ++i) {
            exp = 10 * exp + Character.digit(s.charAt(i), 10);
        }
        mExpr.addExponent(sign * exp);
        mChangedValue = true;
    }
}
