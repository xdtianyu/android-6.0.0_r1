/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.text.cts;

import android.test.AndroidTestCase;
import android.text.Editable;
import android.text.GetChars;
import android.text.GraphicsOperations;
import android.text.Layout.Alignment;
import android.text.TextUtils.TruncateAt;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class StaticLayoutTest extends AndroidTestCase {
    private static final float SPACE_MULTI = 1.0f;
    private static final float SPACE_ADD = 0.0f;
    private static final int DEFAULT_OUTER_WIDTH = 150;

    private static final int LAST_LINE = 5;
    private static final int LINE_COUNT = 6;
    private static final int LARGER_THAN_LINE_COUNT  = 50;

    /* the first line must have one tab. the others not. totally 6 lines
     */
    private static final CharSequence LAYOUT_TEXT = "CharSe\tq\nChar"
            + "Sequence\nCharSequence\nHelllo\n, world\nLongLongLong";

    private static final CharSequence LAYOUT_TEXT_SINGLE_LINE = "CharSequence";

    private static final int VERTICAL_BELOW_TEXT = 1000;

    private static final Alignment DEFAULT_ALIGN = Alignment.ALIGN_CENTER;

    private static final int ELLIPSIZE_WIDTH = 8;

    private StaticLayout mDefaultLayout;
    private TextPaint mDefaultPaint;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (mDefaultPaint == null) {
            mDefaultPaint = new TextPaint();
        }
        if (mDefaultLayout == null) {
            mDefaultLayout = createDefaultStaticLayout();
        }
    }

    private StaticLayout createDefaultStaticLayout() {
        return new StaticLayout(LAYOUT_TEXT, mDefaultPaint,
                DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);
    }

    private StaticLayout createEllipsizeStaticLayout() {
        return new StaticLayout(LAYOUT_TEXT, 0, LAYOUT_TEXT.length(), mDefaultPaint,
                DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true,
                TextUtils.TruncateAt.MIDDLE, ELLIPSIZE_WIDTH);
    }

    private StaticLayout createEllipsizeStaticLayout(CharSequence text,
            TextUtils.TruncateAt ellipsize, int maxLines) {
        return new StaticLayout(text, 0, text.length(),
                mDefaultPaint, DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN,
                TextDirectionHeuristics.FIRSTSTRONG_LTR,
                SPACE_MULTI, SPACE_ADD, true /* include pad */,
                ellipsize,
                ELLIPSIZE_WIDTH,
                maxLines);
    }



    /**
     * Constructor test
     */
    public void testConstructor() {
        new StaticLayout(LAYOUT_TEXT, mDefaultPaint, DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

        new StaticLayout(LAYOUT_TEXT, 0, LAYOUT_TEXT.length(), mDefaultPaint,
                DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

        new StaticLayout(LAYOUT_TEXT, 0, LAYOUT_TEXT.length(), mDefaultPaint,
                DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, false, null, 0);

        try {
            new StaticLayout(null, null, -1, null, 0, 0, true);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
        }
    }

    public void testBuilder() {
        {
            // Obtain.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            StaticLayout layout = builder.build();
            // Check values passed to obtain().
            assertEquals(LAYOUT_TEXT, layout.getText());
            assertEquals(mDefaultPaint, layout.getPaint());
            assertEquals(DEFAULT_OUTER_WIDTH, layout.getWidth());
            // Check default values.
            assertEquals(TextDirectionHeuristics.FIRSTSTRONG_LTR,
                    layout.getTextDirectionHeuristic());
            assertEquals(Alignment.ALIGN_NORMAL, layout.getAlignment());
            assertEquals(0.0f, layout.getSpacingAdd());
            assertEquals(1.0f, layout.getSpacingMultiplier());
            assertEquals(DEFAULT_OUTER_WIDTH, layout.getEllipsizedWidth());
        }
        {
            // Obtain with null objects.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(null, 0, 0, null, 0);
            try {
                StaticLayout layout = builder.build();
                fail("should throw NullPointerException here");
            } catch (NullPointerException e) {
            }
        }
        {
            // setText.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            builder.setText(LAYOUT_TEXT_SINGLE_LINE);
            StaticLayout layout = builder.build();
            assertEquals(LAYOUT_TEXT_SINGLE_LINE, layout.getText());
        }
        {
            // setAlignment.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            builder.setAlignment(DEFAULT_ALIGN);
            StaticLayout layout = builder.build();
            assertEquals(DEFAULT_ALIGN, layout.getAlignment());
        }
        {
            // setTextDirection.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            builder.setTextDirection(TextDirectionHeuristics.RTL);
            StaticLayout layout = builder.build();
            // Always returns TextDirectionHeuristics.FIRSTSTRONG_LTR.
            assertEquals(TextDirectionHeuristics.FIRSTSTRONG_LTR,
                    layout.getTextDirectionHeuristic());
        }
        {
            // setLineSpacing.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            builder.setLineSpacing(1.0f, 2.0f);
            StaticLayout layout = builder.build();
            assertEquals(1.0f, layout.getSpacingAdd());
            assertEquals(2.0f, layout.getSpacingMultiplier());
        }
        {
            // setEllipsizedWidth and setEllipsize.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            builder.setEllipsize(TruncateAt.END);
            builder.setEllipsizedWidth(ELLIPSIZE_WIDTH);
            StaticLayout layout = builder.build();
            assertEquals(ELLIPSIZE_WIDTH, layout.getEllipsizedWidth());
            assertEquals(DEFAULT_OUTER_WIDTH, layout.getWidth());
            assertTrue(layout.getEllipsisCount(0) == 0);
            assertTrue(layout.getEllipsisCount(5) > 0);
        }
        {
            // setMaxLines.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            builder.setMaxLines(1);
            builder.setEllipsize(TruncateAt.END);
            StaticLayout layout = builder.build();
            assertTrue(layout.getEllipsisCount(0) > 0);
            assertEquals(1, layout.getLineCount());
        }
        {
            // Setter methods that cannot be directly tested.
            // setBreakStrategy, setHyphenationFrequency, setIncludePad, and setIndents.
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(LAYOUT_TEXT, 0,
                    LAYOUT_TEXT.length(), mDefaultPaint, DEFAULT_OUTER_WIDTH);
            builder.setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY);
            builder.setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_FULL);
            builder.setIncludePad(true);
            builder.setIndents(null, null);
            StaticLayout layout = builder.build();
            assertNotNull(layout);
        }
    }

    /*
     * Get the line number corresponding to the specified vertical position.
     *  If you ask for a position above 0, you get 0. above 0 means pixel above the fire line
     *  if you ask for a position in the range of the height, return the pixel in line
     *  if you ask for a position below the bottom of the text, you get the last line.
     *  Test 4 values containing -1, 0, normal number and > count
     */
    public void testGetLineForVertical() {
        assertEquals(0, mDefaultLayout.getLineForVertical(-1));
        assertEquals(0, mDefaultLayout.getLineForVertical(0));
        assertTrue(mDefaultLayout.getLineForVertical(50) > 0);
        assertEquals(LAST_LINE, mDefaultLayout.getLineForVertical(VERTICAL_BELOW_TEXT));
    }

    /**
     * Return the number of lines of text in this layout.
     */
    public void testGetLineCount() {
        assertEquals(LINE_COUNT, mDefaultLayout.getLineCount());
    }

    /*
     * Return the vertical position of the top of the specified line.
     * If the specified line is one beyond the last line, returns the bottom of the last line.
     * A line of text contains top and bottom in height. this method just get the top of a line
     * Test 4 values containing -1, 0, normal number and > count
     */
    public void testGetLineTop() {
        assertTrue(mDefaultLayout.getLineTop(0) >= 0);
        assertTrue(mDefaultLayout.getLineTop(1) > mDefaultLayout.getLineTop(0));

        try {
            mDefaultLayout.getLineTop(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getLineTop(LARGER_THAN_LINE_COUNT );
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Return the descent of the specified line.
     * This method just like getLineTop, descent means the bottom pixel of the line
     * Test 4 values containing -1, 0, normal number and > count
     */
    public void testGetLineDescent() {
        assertTrue(mDefaultLayout.getLineDescent(0) > 0);
        assertTrue(mDefaultLayout.getLineDescent(1) > 0);

        try {
            mDefaultLayout.getLineDescent(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getLineDescent(LARGER_THAN_LINE_COUNT );
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Returns the primary directionality of the paragraph containing the specified line.
     * By default, each line should be same
     */
    public void testGetParagraphDirection() {
        assertEquals(mDefaultLayout.getParagraphDirection(0),
                mDefaultLayout.getParagraphDirection(1));
        try {
            mDefaultLayout.getParagraphDirection(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getParagraphDirection(LARGER_THAN_LINE_COUNT);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Return the text offset of the beginning of the specified line.
     * If the specified line is one beyond the last line, returns the end of the last line.
     * Test 4 values containing -1, 0, normal number and > count
     * Each line's offset must >= 0
     */
    public void testGetLineStart() {
        assertTrue(mDefaultLayout.getLineStart(0) >= 0);
        assertTrue(mDefaultLayout.getLineStart(1) >= 0);

        try {
            mDefaultLayout.getLineStart(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getLineStart(LARGER_THAN_LINE_COUNT);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /*
     * Returns whether the specified line contains one or more tabs.
     */
    public void testGetContainsTab() {
        assertTrue(mDefaultLayout.getLineContainsTab(0));
        assertFalse(mDefaultLayout.getLineContainsTab(1));

        try {
            mDefaultLayout.getLineContainsTab(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getLineContainsTab(LARGER_THAN_LINE_COUNT );
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Returns an array of directionalities for the specified line.
     * The array alternates counts of characters in left-to-right
     * and right-to-left segments of the line.
     * We can not check the return value, for Directions's field is package private
     * So only check it not null
     */
    public void testGetLineDirections() {
        assertNotNull(mDefaultLayout.getLineDirections(0));
        assertNotNull(mDefaultLayout.getLineDirections(1));

        try {
            mDefaultLayout.getLineDirections(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getLineDirections(LARGER_THAN_LINE_COUNT);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /**
     * Returns the (negative) number of extra pixels of ascent padding
     * in the top line of the Layout.
     */
    public void testGetTopPadding() {
        assertTrue(mDefaultLayout.getTopPadding() < 0);
    }

    /**
     * Returns the number of extra pixels of descent padding in the bottom line of the Layout.
     */
    public void testGetBottomPadding() {
        assertTrue(mDefaultLayout.getBottomPadding() > 0);
    }

    /*
     * Returns the number of characters to be ellipsized away, or 0 if no ellipsis is to take place.
     * So each line must >= 0
     */
    public void testGetEllipsisCount() {
        // Multilines (6 lines) and TruncateAt.START so no ellipsis at all
        mDefaultLayout = createEllipsizeStaticLayout(LAYOUT_TEXT,
                TextUtils.TruncateAt.MIDDLE,
                Integer.MAX_VALUE /* maxLines */);

        assertTrue(mDefaultLayout.getEllipsisCount(0) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(1) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(2) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(3) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(4) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(5) == 0);

        try {
            mDefaultLayout.getEllipsisCount(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getEllipsisCount(LARGER_THAN_LINE_COUNT);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        // Multilines (6 lines) and TruncateAt.MIDDLE so no ellipsis at all
        mDefaultLayout = createEllipsizeStaticLayout(LAYOUT_TEXT,
                TextUtils.TruncateAt.MIDDLE,
                Integer.MAX_VALUE /* maxLines */);

        assertTrue(mDefaultLayout.getEllipsisCount(0) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(1) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(2) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(3) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(4) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(5) == 0);

        // Multilines (6 lines) and TruncateAt.END so ellipsis only on the last line
        mDefaultLayout = createEllipsizeStaticLayout(LAYOUT_TEXT,
                TextUtils.TruncateAt.END,
                Integer.MAX_VALUE /* maxLines */);

        assertTrue(mDefaultLayout.getEllipsisCount(0) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(1) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(2) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(3) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(4) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(5) > 0);

        // Multilines (6 lines) and TruncateAt.MARQUEE so ellipsis only on the last line
        mDefaultLayout = createEllipsizeStaticLayout(LAYOUT_TEXT,
                TextUtils.TruncateAt.END,
                Integer.MAX_VALUE /* maxLines */);

        assertTrue(mDefaultLayout.getEllipsisCount(0) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(1) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(2) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(3) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(4) == 0);
        assertTrue(mDefaultLayout.getEllipsisCount(5) > 0);
    }

    /*
     * Return the offset of the first character to be ellipsized away
     * relative to the start of the line.
     * (So 0 if the beginning of the line is ellipsized, not getLineStart().)
     */
    public void testGetEllipsisStart() {
        mDefaultLayout = createEllipsizeStaticLayout();
        assertTrue(mDefaultLayout.getEllipsisStart(0) >= 0);
        assertTrue(mDefaultLayout.getEllipsisStart(1) >= 0);

        try {
            mDefaultLayout.getEllipsisStart(-1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        try {
            mDefaultLayout.getEllipsisStart(LARGER_THAN_LINE_COUNT);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    /*
     * Return the width to which this Layout is ellipsizing
     * or getWidth() if it is not doing anything special.
     * The constructor's Argument TextUtils.TruncateAt defines which EllipsizedWidth to use
     * ellipsizedWidth if argument is not null
     * outerWidth if argument is null
     */
    public void testGetEllipsizedWidth() {
        int ellipsizedWidth = 60;
        int outerWidth = 100;
        StaticLayout layout = new StaticLayout(LAYOUT_TEXT, 0, LAYOUT_TEXT.length(),
                mDefaultPaint, outerWidth, DEFAULT_ALIGN, SPACE_MULTI,
                SPACE_ADD, false, TextUtils.TruncateAt.END, ellipsizedWidth);
        assertEquals(ellipsizedWidth, layout.getEllipsizedWidth());

        layout = new StaticLayout(LAYOUT_TEXT, 0, LAYOUT_TEXT.length(),
                mDefaultPaint, outerWidth, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD,
                false, null, ellipsizedWidth);
        assertEquals(outerWidth, layout.getEllipsizedWidth());
    }

    public void testEllipsis_singleLine() {
        {
            // Single line case and TruncateAt.END so that we have some ellipsis
            StaticLayout layout = createEllipsizeStaticLayout(LAYOUT_TEXT_SINGLE_LINE,
                    TextUtils.TruncateAt.END, 1);
            assertTrue(layout.getEllipsisCount(0) > 0);
        }
        {
            // Single line case and TruncateAt.MIDDLE so that we have some ellipsis
            StaticLayout layout = createEllipsizeStaticLayout(LAYOUT_TEXT_SINGLE_LINE,
                    TextUtils.TruncateAt.MIDDLE, 1);
            assertTrue(layout.getEllipsisCount(0) > 0);
        }
        {
            // Single line case and TruncateAt.END so that we have some ellipsis
            StaticLayout layout = createEllipsizeStaticLayout(LAYOUT_TEXT_SINGLE_LINE,
                    TextUtils.TruncateAt.END, 1);
            assertTrue(layout.getEllipsisCount(0) > 0);
        }
        {
            // Single line case and TruncateAt.MARQUEE so that we have NO ellipsis
            StaticLayout layout = createEllipsizeStaticLayout(LAYOUT_TEXT_SINGLE_LINE,
                    TextUtils.TruncateAt.MARQUEE, 1);
            assertTrue(layout.getEllipsisCount(0) == 0);
        }

        final String text = "\u3042" // HIRAGANA LETTER A
                + "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        final float textWidth = mDefaultPaint.measureText(text);
        final int halfWidth = (int)(textWidth / 2.0f);
        {
            StaticLayout layout = new StaticLayout(text, 0, text.length(), mDefaultPaint,
                    halfWidth, DEFAULT_ALIGN, TextDirectionHeuristics.FIRSTSTRONG_LTR,
                    SPACE_MULTI, SPACE_ADD, false, TextUtils.TruncateAt.END, halfWidth, 1);
            assertTrue(layout.getEllipsisCount(0) > 0);
            assertTrue(layout.getEllipsisStart(0) > 0);
        }
        {
            StaticLayout layout = new StaticLayout(text, 0, text.length(), mDefaultPaint,
                    halfWidth, DEFAULT_ALIGN, TextDirectionHeuristics.FIRSTSTRONG_LTR,
                    SPACE_MULTI, SPACE_ADD, false, TextUtils.TruncateAt.START, halfWidth, 1);
            assertTrue(layout.getEllipsisCount(0) > 0);
            assertEquals(0, mDefaultLayout.getEllipsisStart(0));
        }
        {
            StaticLayout layout = new StaticLayout(text, 0, text.length(), mDefaultPaint,
                    halfWidth, DEFAULT_ALIGN, TextDirectionHeuristics.FIRSTSTRONG_LTR,
                    SPACE_MULTI, SPACE_ADD, false, TextUtils.TruncateAt.MIDDLE, halfWidth, 1);
            assertTrue(layout.getEllipsisCount(0) > 0);
            assertTrue(layout.getEllipsisStart(0) > 0);
        }
        {
            StaticLayout layout = new StaticLayout(text, 0, text.length(), mDefaultPaint,
                    halfWidth, DEFAULT_ALIGN, TextDirectionHeuristics.FIRSTSTRONG_LTR,
                    SPACE_MULTI, SPACE_ADD, false, TextUtils.TruncateAt.MARQUEE, halfWidth, 1);
            assertEquals(0, layout.getEllipsisCount(0));
        }
    }

    /**
     * scenario description:
     * 1. set the text.
     * 2. change the text
     * 3. Check the text won't change to the StaticLayout
    */
    public void testImmutableStaticLayout() {
        Editable editable =  Editable.Factory.getInstance().newEditable("123\t\n555");
        StaticLayout layout = new StaticLayout(editable, mDefaultPaint,
                DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

        assertEquals(2, layout.getLineCount());
        assertTrue(mDefaultLayout.getLineContainsTab(0));

        // change the text
        editable.delete(0, editable.length() - 1);

        assertEquals(2, layout.getLineCount());
        assertTrue(layout.getLineContainsTab(0));

    }

    // String wrapper for testing not well known implementation of CharSequence.
    private class FakeCharSequence implements CharSequence {
        private String mStr;

        public FakeCharSequence(String str) {
            mStr = str;
        }

        @Override
        public char charAt(int index) {
            return mStr.charAt(index);
        }

        @Override
        public int length() {
            return mStr.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return mStr.subSequence(start, end);
        }

        @Override
        public String toString() {
            return mStr;
        }
    };

    private List<CharSequence> buildTestCharSequences(String testString, Normalizer.Form[] forms) {
        List<CharSequence> result = new ArrayList<CharSequence>();

        List<String> normalizedStrings = new ArrayList<String>();
        for (Normalizer.Form form: forms) {
            normalizedStrings.add(Normalizer.normalize(testString, form));
        }

        for (String str: normalizedStrings) {
            result.add(str);
            result.add(new SpannedString(str));
            result.add(new SpannableString(str));
            result.add(new SpannableStringBuilder(str));  // as a GraphicsOperations implementation.
            result.add(new FakeCharSequence(str));  // as a not well known implementation.
        }
        return result;
    }

    private String buildTestMessage(CharSequence seq) {
        String normalized;
        if (Normalizer.isNormalized(seq, Normalizer.Form.NFC)) {
            normalized = "NFC";
        } else if (Normalizer.isNormalized(seq, Normalizer.Form.NFD)) {
            normalized = "NFD";
        } else if (Normalizer.isNormalized(seq, Normalizer.Form.NFKC)) {
            normalized = "NFKC";
        } else if (Normalizer.isNormalized(seq, Normalizer.Form.NFKD)) {
            normalized = "NFKD";
        } else {
            throw new IllegalStateException("Normalized form is not NFC/NFD/NFKC/NFKD");
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < seq.length(); ++i) {
            builder.append(String.format("0x%04X ", Integer.valueOf(seq.charAt(i))));
        }

        return "testString: \"" + seq.toString() + "\"[" + builder.toString() + "]" +
                ", class: " + seq.getClass().getName() +
                ", Normalization: " + normalized;
    }

    public void testGetOffset_ASCII() {
        String testStrings[] = { "abcde", "ab\ncd", "ab\tcd", "ab\n\nc", "ab\n\tc" };

        for (String testString: testStrings) {
            for (CharSequence seq: buildTestCharSequences(testString, Normalizer.Form.values())) {
                StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                        DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

                String testLabel = buildTestMessage(seq);

                assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
                assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
                assertEquals(testLabel, 1, layout.getOffsetToLeftOf(2));
                assertEquals(testLabel, 2, layout.getOffsetToLeftOf(3));
                assertEquals(testLabel, 3, layout.getOffsetToLeftOf(4));
                assertEquals(testLabel, 4, layout.getOffsetToLeftOf(5));

                assertEquals(testLabel, 1, layout.getOffsetToRightOf(0));
                assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
                assertEquals(testLabel, 3, layout.getOffsetToRightOf(2));
                assertEquals(testLabel, 4, layout.getOffsetToRightOf(3));
                assertEquals(testLabel, 5, layout.getOffsetToRightOf(4));
                assertEquals(testLabel, 5, layout.getOffsetToRightOf(5));
            }
        }

        String testString = "ab\r\nde";
        for (CharSequence seq: buildTestCharSequences(testString, Normalizer.Form.values())) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 1, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(5));
            assertEquals(testLabel, 5, layout.getOffsetToLeftOf(6));

            assertEquals(testLabel, 1, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 5, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(5));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(6));
        }
    }

    public void testGetOffset_UNICODE() {
        String testStrings[] = new String[] {
              // Cyrillic alphabets.
              "\u0410\u0411\u0412\u0413\u0414",
              // Japanese Hiragana Characters.
              "\u3042\u3044\u3046\u3048\u304A",
        };

        for (String testString: testStrings) {
            for (CharSequence seq: buildTestCharSequences(testString, Normalizer.Form.values())) {
                StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                        DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

                String testLabel = buildTestMessage(seq);

                assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
                assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
                assertEquals(testLabel, 1, layout.getOffsetToLeftOf(2));
                assertEquals(testLabel, 2, layout.getOffsetToLeftOf(3));
                assertEquals(testLabel, 3, layout.getOffsetToLeftOf(4));
                assertEquals(testLabel, 4, layout.getOffsetToLeftOf(5));

                assertEquals(testLabel, 1, layout.getOffsetToRightOf(0));
                assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
                assertEquals(testLabel, 3, layout.getOffsetToRightOf(2));
                assertEquals(testLabel, 4, layout.getOffsetToRightOf(3));
                assertEquals(testLabel, 5, layout.getOffsetToRightOf(4));
                assertEquals(testLabel, 5, layout.getOffsetToRightOf(5));
            }
        }
    }

    public void testGetOffset_UNICODE_Normalization() {
        // "A" with acute, circumflex, tilde, diaeresis, ring above.
        String testString = "\u00C1\u00C2\u00C3\u00C4\u00C5";
        Normalizer.Form[] oneUnicodeForms = { Normalizer.Form.NFC, Normalizer.Form.NFKC };
        for (CharSequence seq: buildTestCharSequences(testString, oneUnicodeForms)) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 1, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 3, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(5));

            assertEquals(testLabel, 1, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 3, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 5, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 5, layout.getOffsetToRightOf(5));
        }

        Normalizer.Form[] twoUnicodeForms = { Normalizer.Form.NFD, Normalizer.Form.NFKD };
        for (CharSequence seq: buildTestCharSequences(testString, twoUnicodeForms)) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(5));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(6));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(7));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(8));
            assertEquals(testLabel, 8, layout.getOffsetToLeftOf(9));
            assertEquals(testLabel, 8, layout.getOffsetToLeftOf(10));

            assertEquals(testLabel, 2, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(5));
            assertEquals(testLabel, 8, layout.getOffsetToRightOf(6));
            assertEquals(testLabel, 8, layout.getOffsetToRightOf(7));
            assertEquals(testLabel, 10, layout.getOffsetToRightOf(8));
            assertEquals(testLabel, 10, layout.getOffsetToRightOf(9));
            assertEquals(testLabel, 10, layout.getOffsetToRightOf(10));
        }
    }

    public void testGetOffset_UNICODE_SurrogatePairs() {
        // Emoticons for surrogate pairs tests.
        String testString =
                "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02\uD83D\uDE03\uD83D\uDE04";
        for (CharSequence seq: buildTestCharSequences(testString, Normalizer.Form.values())) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(5));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(6));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(7));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(8));
            assertEquals(testLabel, 8, layout.getOffsetToLeftOf(9));
            assertEquals(testLabel, 8, layout.getOffsetToLeftOf(10));

            assertEquals(testLabel, 2, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(5));
            assertEquals(testLabel, 8, layout.getOffsetToRightOf(6));
            assertEquals(testLabel, 8, layout.getOffsetToRightOf(7));
            assertEquals(testLabel, 10, layout.getOffsetToRightOf(8));
            assertEquals(testLabel, 10, layout.getOffsetToRightOf(9));
            assertEquals(testLabel, 10, layout.getOffsetToRightOf(10));
        }
    }

    public void testGetOffset_UNICODE_Thai() {
        // Thai Characters. The expected cursorable boundary is
        // | \u0E02 | \u0E2D | \u0E1A | \u0E04\u0E38 | \u0E13 |
        String testString = "\u0E02\u0E2D\u0E1A\u0E04\u0E38\u0E13";
        for (CharSequence seq: buildTestCharSequences(testString, Normalizer.Form.values())) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 1, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 3, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 3, layout.getOffsetToLeftOf(5));
            assertEquals(testLabel, 5, layout.getOffsetToLeftOf(6));

            assertEquals(testLabel, 1, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 3, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 5, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 5, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(5));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(6));
        }
    }

    public void testGetOffset_UNICODE_Hebrew() {
        String testString = "\u05DE\u05E1\u05E2\u05D3\u05D4"; // Hebrew Characters
        for (CharSequence seq: buildTestCharSequences(testString, Normalizer.Form.values())) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN,
                    TextDirectionHeuristics.RTL, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 1, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 3, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 5, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 5, layout.getOffsetToLeftOf(5));

            assertEquals(testLabel, 0, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 1, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 3, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(5));
        }
    }

    public void testGetOffset_UNICODE_Arabic() {
        // Arabic Characters. The expected cursorable boundary is
        // | \u0623 \u064F | \u0633 \u0652 | \u0631 \u064E | \u0629 \u064C |";
        String testString = "\u0623\u064F\u0633\u0652\u0631\u064E\u0629\u064C";

        Normalizer.Form[] oneUnicodeForms = { Normalizer.Form.NFC, Normalizer.Form.NFKC };
        for (CharSequence seq: buildTestCharSequences(testString, oneUnicodeForms)) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(5));
            assertEquals(testLabel, 8, layout.getOffsetToLeftOf(6));
            assertEquals(testLabel, 8, layout.getOffsetToLeftOf(7));
            assertEquals(testLabel, 8, layout.getOffsetToLeftOf(8));

            assertEquals(testLabel, 0, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 0, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(5));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(6));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(7));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(8));
        }
    }

    public void testGetOffset_UNICODE_Bidi() {
        // String having RTL characters and LTR characters

        // LTR Context
        // The first and last two characters are LTR characters.
        String testString = "\u0061\u0062\u05DE\u05E1\u05E2\u0063\u0064";
        // Logical order: [L1] [L2] [R1] [R2] [R3] [L3] [L4]
        //               0    1    2    3    4    5    6    7
        // Display order: [L1] [L2] [R3] [R2] [R1] [L3] [L4]
        //               0    1    2    4    3    5    6    7
        // [L?] means ?th LTR character and [R?] means ?th RTL character.
        for (CharSequence seq: buildTestCharSequences(testString, Normalizer.Form.values())) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 1, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 3, layout.getOffsetToLeftOf(5));
            assertEquals(testLabel, 5, layout.getOffsetToLeftOf(6));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(7));

            assertEquals(testLabel, 1, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 5, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 3, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(5));
            assertEquals(testLabel, 7, layout.getOffsetToRightOf(6));
            assertEquals(testLabel, 7, layout.getOffsetToRightOf(7));
        }

        // RTL Context
        // The first and last two characters are RTL characters.
        String testString2 = "\u05DE\u05E1\u0063\u0064\u0065\u05DE\u05E1";
        // Logical order: [R1] [R2] [L1] [L2] [L3] [R3] [R4]
        //               0    1    2    3    4    5    6    7
        // Display order: [R4] [R3] [L1] [L2] [L3] [R2] [R1]
        //               7    6    5    3    4    2    1    0
        // [L?] means ?th LTR character and [R?] means ?th RTL character.
        for (CharSequence seq: buildTestCharSequences(testString2, Normalizer.Form.values())) {
            StaticLayout layout = new StaticLayout(seq, mDefaultPaint,
                    DEFAULT_OUTER_WIDTH, DEFAULT_ALIGN, SPACE_MULTI, SPACE_ADD, true);

            String testLabel = buildTestMessage(seq);

            assertEquals(testLabel, 1, layout.getOffsetToLeftOf(0));
            assertEquals(testLabel, 2, layout.getOffsetToLeftOf(1));
            assertEquals(testLabel, 4, layout.getOffsetToLeftOf(2));
            assertEquals(testLabel, 5, layout.getOffsetToLeftOf(3));
            assertEquals(testLabel, 3, layout.getOffsetToLeftOf(4));
            assertEquals(testLabel, 6, layout.getOffsetToLeftOf(5));
            assertEquals(testLabel, 7, layout.getOffsetToLeftOf(6));
            assertEquals(testLabel, 7, layout.getOffsetToLeftOf(7));

            assertEquals(testLabel, 0, layout.getOffsetToRightOf(0));
            assertEquals(testLabel, 0, layout.getOffsetToRightOf(1));
            assertEquals(testLabel, 1, layout.getOffsetToRightOf(2));
            assertEquals(testLabel, 4, layout.getOffsetToRightOf(3));
            assertEquals(testLabel, 2, layout.getOffsetToRightOf(4));
            assertEquals(testLabel, 3, layout.getOffsetToRightOf(5));
            assertEquals(testLabel, 5, layout.getOffsetToRightOf(6));
            assertEquals(testLabel, 6, layout.getOffsetToRightOf(7));
        }
    }
}
