/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.compatibility.common.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Utility class to add results to the report.
 */
public class ReportLog implements Serializable {

    private static final String LOG_SEPARATOR = "+++";
    private static final String SUMMARY_SEPARATOR = "++++";
    private static final String LOG_ELEM_SEPARATOR = "|";
    private static final String EMPTY_CHAR = " ";
    private Result mSummary;
    private final List<Result> mDetails = new ArrayList<Result>();

    static class Result implements Serializable {
        private String mLocation;
        private String mMessage;
        private double[] mValues;
        private ResultType mType;
        private ResultUnit mUnit;
        private Double mTarget;


        private Result(String location, String message, double[] values,
                ResultType type, ResultUnit unit) {
            this(location, message, values, null /*target*/, type, unit);
        }

        /**
         * Creates a result object to be included in the report. Each object has a message
         * describing its values and enums to interpret them. In addition, each result also includes
         * class, method and line number information about the test which added this result which is
         * collected by looking at the stack trace.
         *
         * @param message A string describing the values
         * @param values An array of the values
         * @param target Nullable. The target value.
         * @param type Represents how to interpret the values (eg. A lower score is better)
         * @param unit Represents the unit in which the values are (eg. Milliseconds)
         */
        private Result(String location, String message, double[] values,
                Double target, ResultType type, ResultUnit unit) {
            mLocation = location;
            mMessage = message;
            mValues = values;
            mType = type;
            mUnit = unit;
            mTarget = target;
        }

        public double getTarget() {
            return mTarget;
        }

        public String getLocation() {
            return mLocation;
        }

        public String getMessage() {
            return mMessage;
        }

        public double[] getValues() {
            return mValues;
        }

        public ResultType getType() {
            return mType;
        }

        public ResultUnit getUnit() {
            return mUnit;
        }

        /**
         * Format:
         * location|message|target|type|unit|value[s], target can be " " if there is no target set.
         * log for array = classMethodName:line_number|message|unit|type|space separated values
         */
        String toEncodedString() {
            StringBuilder builder = new StringBuilder()
                    .append(mLocation)
                    .append(LOG_ELEM_SEPARATOR)
                    .append(mMessage)
                    .append(LOG_ELEM_SEPARATOR)
                    .append(mTarget != null ? mTarget : EMPTY_CHAR)
                    .append(LOG_ELEM_SEPARATOR)
                    .append(mType.name())
                    .append(LOG_ELEM_SEPARATOR)
                    .append(mUnit.name())
                    .append(LOG_ELEM_SEPARATOR);
            for (double value : mValues) {
                builder.append(value).append(" ");
            }
            return builder.toString();
        }

        static Result fromEncodedString(String encodedString) {
            String[] elems = encodedString.split(Pattern.quote(LOG_ELEM_SEPARATOR));
            if (elems.length < 5) {
                return null;
            }

            String[] valueStrArray = elems[5].split(" ");
            double[] valueArray = new double[valueStrArray.length];
            for (int i = 0; i < valueStrArray.length; i++) {
                valueArray[i] = Double.parseDouble(valueStrArray[i]);
            }
            return new Result(
                    elems[0], /*location*/
                    elems[1], /*message*/
                    valueArray, /*values*/
                    elems[2].equals(EMPTY_CHAR) ? null : Double.parseDouble(elems[2]), /*target*/
                    ResultType.valueOf(elems[3]), /*type*/
                    ResultUnit.valueOf(elems[4])  /*unit*/);
        }
    }

    /**
     * Adds an array of values to the report.
     */
    public void addValues(String message, double[] values, ResultType type, ResultUnit unit) {
        mDetails.add(new Result(Stacktrace.getTestCallerClassMethodNameLineNumber(),
                message, values, type, unit));
    }

    /**
     * Adds an array of values to the report.
     */
    public void addValues(
            String message, double[] values, ResultType type, ResultUnit unit, String location) {
        mDetails.add(new Result(location, message, values, type, unit));
    }

    /**
     * Adds a value to the report.
     */
    public void addValue(String message, double value, ResultType type, ResultUnit unit) {
        mDetails.add(new Result(Stacktrace.getTestCallerClassMethodNameLineNumber(), message,
                new double[] {value}, type, unit));
    }

    /**
     * Adds a value to the report.
     */
    public void addValue(String message, double value, ResultType type,
            ResultUnit unit, String location) {
        mDetails.add(new Result(location, message, new double[] {value}, type, unit));
    }

    /**
     * Sets the summary of the report.
     */
    public void setSummary(String message, double value, ResultType type, ResultUnit unit) {
        mSummary = new Result(Stacktrace.getTestCallerClassMethodNameLineNumber(),
                message, new double[] {value}, type, unit);
    }

    public Result getSummary() {
        return mSummary;
    }

    public List<Result> getDetailedMetrics() {
        return new ArrayList<Result>(mDetails);
    }

    /**
     * Parse a String encoded {@link com.android.compatibility.common.util.ReportLog}
     */
    public static ReportLog fromEncodedString(String encodedString) {
        ReportLog reportLog = new ReportLog();
        StringTokenizer tok = new StringTokenizer(encodedString, SUMMARY_SEPARATOR);
        if (tok.hasMoreTokens()) {
            // Extract the summary
            reportLog.mSummary = Result.fromEncodedString(tok.nextToken());
        }
        if (tok.hasMoreTokens()) {
            // Extract the detailed results
            StringTokenizer detailedTok = new StringTokenizer(tok.nextToken(), LOG_SEPARATOR);
            while (detailedTok.hasMoreTokens()) {
                reportLog.mDetails.add(Result.fromEncodedString(detailedTok.nextToken()));
            }
        }
        return reportLog;
    }

    /**
     * @return a String representation of this report or null if not collected
     */
    protected String toEncodedString() {
        if ((mSummary == null) && mDetails.isEmpty()) {
            // just return empty string
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(mSummary.toEncodedString());
        builder.append(SUMMARY_SEPARATOR);
        for (Result result : mDetails) {
            builder.append(result.toEncodedString());
            builder.append(LOG_SEPARATOR);
        }
        // delete the last separator
        if (builder.length() >= LOG_SEPARATOR.length()) {
            builder.delete(builder.length() - LOG_SEPARATOR.length(), builder.length());
        }
        return builder.toString();
    }
}
