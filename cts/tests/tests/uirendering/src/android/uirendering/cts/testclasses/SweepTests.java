/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE2.0
*
* Unless required by applicable law or agreed to in riting, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package android.uirendering.cts.testclasses;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.bitmapcomparers.BitmapComparer;
import android.uirendering.cts.bitmapcomparers.MSSIMComparer;
import android.uirendering.cts.bitmapverifiers.BitmapVerifier;
import android.uirendering.cts.bitmapverifiers.SamplePointVerifier;
import android.uirendering.cts.testinfrastructure.ActivityTestBase;
import android.uirendering.cts.testinfrastructure.CanvasClient;
import android.uirendering.cts.testinfrastructure.DisplayModifier;
import android.uirendering.cts.testinfrastructure.ResourceModifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test cases of all combination of resource modifications.
 */
public class SweepTests extends ActivityTestBase {
    private static final String TAG = "SweepTests";

    public static final int BG_COLOR = 0xFFFFFFFF;
    public static final int DST_COLOR = 0xFFFFCC44;
    public static final int SRC_COLOR = 0xFF66AAFF;
    public static final int MULTIPLY_COLOR = 0xFF668844;
    public static final int SCREEN_COLOR = 0xFFFFEEFF;

    // These points are in pairs, the first being the lower left corner, the second is only in the
    // Destination bitmap, the third is the intersection of the two bitmaps, and the fourth is in
    // the Source bitmap.
    private final static Point[] XFERMODE_TEST_POINTS = new Point[] {
            new Point(1, 80), new Point(25, 25), new Point(35, 35), new Point(70, 70)
    };

    /**
     * There are 4 locations we care about in any filter testing.
     *
     * 1) Both empty
     * 2) Only src, dst empty
     * 3) Both src + dst
     * 4) Only dst, src empty
     */
    private final Map<PorterDuff.Mode, int[]> XFERMODE_COLOR_MAP = new LinkedHashMap<PorterDuff.Mode, int[]>() {
        {
            put(PorterDuff.Mode.SRC, new int[] {
                    BG_COLOR, BG_COLOR, SRC_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.DST, new int[] {
                    BG_COLOR, DST_COLOR, DST_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SRC_OVER, new int[] {
                    BG_COLOR, DST_COLOR, SRC_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.DST_OVER, new int[] {
                    BG_COLOR, DST_COLOR, DST_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.SRC_IN, new int[] {
                    BG_COLOR, BG_COLOR, SRC_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.DST_IN, new int[] {
                    BG_COLOR, BG_COLOR, DST_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SRC_OUT, new int[] {
                    BG_COLOR, BG_COLOR, BG_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.DST_OUT, new int[] {
                    BG_COLOR, DST_COLOR, BG_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SRC_ATOP, new int[] {
                    BG_COLOR, DST_COLOR, SRC_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.DST_ATOP, new int[] {
                    BG_COLOR, BG_COLOR, DST_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.XOR, new int[] {
                    BG_COLOR, DST_COLOR, BG_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.MULTIPLY, new int[] {
                    BG_COLOR, BG_COLOR, MULTIPLY_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SCREEN, new int[] {
                    BG_COLOR, DST_COLOR, SCREEN_COLOR, SRC_COLOR
            });
        }
    };

    private final static DisplayModifier XFERMODE_MODIFIER = new DisplayModifier() {
        private final RectF mSrcRect = new RectF(30, 30, 80, 80);
        private final RectF mDstRect = new RectF(10, 10, 60, 60);
        private final Bitmap mSrcBitmap = createSrc();
        private final Bitmap mDstBitmap = createDst();

        @Override
        public void modifyDrawing(Paint paint, Canvas canvas) {
            int sc = canvas.saveLayer(0, 0, TEST_WIDTH, TEST_HEIGHT, null);

            canvas.drawBitmap(mDstBitmap, 0, 0, null);
            canvas.drawBitmap(mSrcBitmap, 0, 0, paint);

            canvas.restoreToCount(sc);
        }

        private Bitmap createSrc() {
            Bitmap srcB = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas srcCanvas = new Canvas(srcB);
            Paint srcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            srcPaint.setColor(SRC_COLOR);
            srcCanvas.drawRect(mSrcRect, srcPaint);
            return srcB;
        }

        private Bitmap createDst() {
            Bitmap dstB = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas dstCanvas = new Canvas(dstB);
            Paint dstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dstPaint.setColor(DST_COLOR);
            dstCanvas.drawOval(mDstRect, dstPaint);
            return dstB;
        }
    };

    // We care about one point in each of the four rectangles of different alpha values, as well as
    // the area outside the rectangles
    private final static Point[] COLOR_FILTER_ALPHA_POINTS = new Point[] {
            new Point(9, 45),
            new Point(27, 45),
            new Point(45, 45),
            new Point(63, 45),
            new Point(81, 45)
    };

    public static final int FILTER_COLOR = 0xFFBB0000;
    private final Map<PorterDuff.Mode, int[]> COLOR_FILTER_ALPHA_MAP
            = new LinkedHashMap<PorterDuff.Mode, int[]>() {
        {
            put(PorterDuff.Mode.SRC, new int[] {
                    FILTER_COLOR, FILTER_COLOR, FILTER_COLOR, FILTER_COLOR, FILTER_COLOR
            });

            put(PorterDuff.Mode.DST, new int[] {
                    0xFFE6E6E6, 0xFFCCCCCC, 0xFFB3B3B3, 0xFF999999, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SRC_OVER, new int[] {
                    0xFFBB0000, 0xFFBB0000, 0xFFBB0000, 0xFFBB0000, 0xFFBB0000
            });

            put(PorterDuff.Mode.DST_OVER, new int[] {
                    0xFFAF1A1A, 0xFFA33333, 0xFF984D4D, 0xFF8B6666, 0xFFBB0000
            });

            put(PorterDuff.Mode.SRC_IN, new int[] {
                    0xFFF1CCCC, 0xFFE49999, 0xFFD66666, 0xFFC83333, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.DST_IN, new int[] {
                    0xFFE6E6E6, 0xFFCCCCCC, 0xFFB3B3B3, 0xFF999999, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SRC_OUT, new int[] {
                    0xFFC83333, 0xFFD66666, 0xFFE49999, 0xFFF1CCCC, 0xFFBB0000
            });

            put(PorterDuff.Mode.DST_OUT, new int[] {
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SRC_ATOP, new int[] {
                    0xFFF1CCCC, 0xFFE49999, 0xFFD66666, 0xFFC93333, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.DST_ATOP, new int[] {
                    0xFFB01A1A, 0xFFA33333, 0xFF984D4D, 0xFF8B6666, 0xFFBB0000
            });

            put(PorterDuff.Mode.XOR, new int[] {
                    0xFFC93333, 0xFFD66666, 0xFFE49999, 0xFFF1CCCC, 0xFFBB0000
            });

            put(PorterDuff.Mode.MULTIPLY, new int[] {
                    0xFFDFCCCC, 0xFFBE9999, 0xFF9E6666, 0xFF7E3333, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SCREEN, new int[] {
                    0xFFC21A1A, 0xFFC93333, 0xFFD04D4D, 0xFFD66666, 0xFFBB0000
            });
        }
    };

    /**
     * Draws 5 blocks of different color/opacity to be blended against
     */
    private final static DisplayModifier COLOR_FILTER_ALPHA_MODIFIER = new DisplayModifier() {
        private final int[] BLOCK_COLORS = new int[] {
                0x33808080,
                0x66808080,
                0x99808080,
                0xCC808080,
                0x00000000
        };

        private final Bitmap mBitmap = createQuadRectBitmap();

        public void modifyDrawing(Paint paint, Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, paint);
        }

        private Bitmap createQuadRectBitmap() {
            Bitmap bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            final int blockCount = BLOCK_COLORS.length;
            final int blockWidth = TEST_WIDTH / blockCount;
            for (int i = 0 ; i < blockCount; i++) {
                paint.setColor(BLOCK_COLORS[i]);
                canvas.drawRect(i * blockWidth, 0, (i + 1) * blockWidth, TEST_HEIGHT, paint);
            }
            return bitmap;
        }
    };

    private final static DisplayModifier COLOR_FILTER_GRADIENT_MODIFIER = new DisplayModifier() {
        private final Rect mBounds = new Rect(30, 30, 150, 150);
        private final int[] mColors = new int[] {
                Color.RED, Color.GREEN, Color.BLUE
        };

        private final Bitmap mBitmap = createGradient();

        @Override
        public void modifyDrawing(Paint paint, Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, paint);
        }

        private Bitmap createGradient() {
            LinearGradient gradient = new LinearGradient(15, 45, 75, 45, mColors, null,
                    Shader.TileMode.REPEAT);
            Bitmap bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.ARGB_8888);
            Paint p = new Paint();
            p.setShader(gradient);
            Canvas c = new Canvas(bitmap);
            c.drawRect(mBounds, p);
            return bitmap;
        }
    };

    public static final DisplayModifier mCircleDrawModifier = new DisplayModifier() {
        @Override
        public void modifyDrawing(Paint paint, Canvas canvas) {
            canvas.drawCircle(TEST_WIDTH / 2, TEST_HEIGHT / 2, TEST_HEIGHT / 2, paint);
        }
    };

    /**
     * 0.5 defines minimum similarity as 50%
     */
    private static final float HIGH_THRESHOLD = 0.5f;

    private static final BitmapComparer[] DEFAULT_MSSIM_COMPARER = new BitmapComparer[] {
            new MSSIMComparer(HIGH_THRESHOLD)
    };

    @SmallTest
    public void testBasicDraws() {
        sweepModifiersForMask(DisplayModifier.Accessor.SHAPES_MASK, null, DEFAULT_MSSIM_COMPARER,
                null);
    }

    @SmallTest
    public void testBasicShaders() {
        sweepModifiersForMask(DisplayModifier.Accessor.SHADER_MASK, mCircleDrawModifier,
                DEFAULT_MSSIM_COMPARER, null);
    }

    @SmallTest
    public void testColorFilterUsingGradient() {
        sweepModifiersForMask(DisplayModifier.Accessor.COLOR_FILTER_MASK,
                COLOR_FILTER_GRADIENT_MODIFIER, DEFAULT_MSSIM_COMPARER, null);
    }

    @SmallTest
    public void testColorFiltersAlphas() {
        BitmapVerifier[] bitmapVerifiers =
                new BitmapVerifier[DisplayModifier.PORTERDUFF_MODES.length];
        int index = 0;
        for (PorterDuff.Mode mode : DisplayModifier.PORTERDUFF_MODES) {
            bitmapVerifiers[index] = new SamplePointVerifier(COLOR_FILTER_ALPHA_POINTS,
                    COLOR_FILTER_ALPHA_MAP.get(mode));
            index++;
        }
        sweepModifiersForMask(DisplayModifier.Accessor.COLOR_FILTER_MASK,
                COLOR_FILTER_ALPHA_MODIFIER, null, bitmapVerifiers);
    }

    @SmallTest
    public void testXfermodes() {
        BitmapVerifier[] bitmapVerifiers =
                new BitmapVerifier[DisplayModifier.PORTERDUFF_MODES.length];
        int index = 0;
        for (PorterDuff.Mode mode : DisplayModifier.PORTERDUFF_MODES) {
            bitmapVerifiers[index] = new SamplePointVerifier(XFERMODE_TEST_POINTS,
                    XFERMODE_COLOR_MAP.get(mode));
            index++;
        }
        sweepModifiersForMask(DisplayModifier.Accessor.XFERMODE_MASK, XFERMODE_MODIFIER,
                null, bitmapVerifiers);
    }

    protected void sweepModifiersForMask(int mask, final DisplayModifier drawOp,
            BitmapComparer[] bitmapComparers, BitmapVerifier[] bitmapVerifiers) {
        if ((mask & DisplayModifier.Accessor.ALL_OPTIONS_MASK) == 0) {
            throw new IllegalArgumentException("Attempt to test with a mask that is invalid");
        }
        // Get the accessor of all the different modifications possible
        final DisplayModifier.Accessor modifierAccessor = new DisplayModifier.Accessor(mask);
        // Initialize the resources that we will need to access
        ResourceModifier.init(getActivity().getResources());
        // For each modification combination, we will get the CanvasClient associated with it and
        // from there execute a normal canvas test with that.
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint paint = new Paint();
                modifierAccessor.modifyDrawing(canvas, paint);
                if (drawOp != null) {
                    drawOp.modifyDrawing(paint, canvas);
                }
            }
        };

        int index = 0;
        // Create the test cases with each combination
        do {
            canvasClient.setDebugString(modifierAccessor.getDebugString());
            if (bitmapComparers != null) {
                int arrIndex = Math.min(index, bitmapComparers.length - 1);
                createTest().addCanvasClient(canvasClient).runWithComparer(bitmapComparers[arrIndex]);
            } else {
                int arrIndex = Math.min(index, bitmapVerifiers.length - 1);
                createTest().addCanvasClient(canvasClient).runWithVerifier(bitmapVerifiers[arrIndex]);
            }
            index++;
        } while (modifierAccessor.step());
    }
}
