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

package android.graphics.drawable.cts;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.cts.graphics.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class VectorDrawableTest extends AndroidTestCase {
    private static final String LOGTAG = VectorDrawableTest.class.getSimpleName();
    private int[] mIconResIds = new int[] {
            R.drawable.vector_icon_create,
            R.drawable.vector_icon_delete,
            R.drawable.vector_icon_heart,
            R.drawable.vector_icon_schedule,
            R.drawable.vector_icon_settings,
            R.drawable.vector_icon_random_path_1,
            R.drawable.vector_icon_random_path_2,
            R.drawable.vector_icon_repeated_cq,
            R.drawable.vector_icon_repeated_st,
            R.drawable.vector_icon_repeated_a_1,
            R.drawable.vector_icon_repeated_a_2,
            R.drawable.vector_icon_clip_path_1,
            R.drawable.vector_icon_transformation_1,
            R.drawable.vector_icon_transformation_2,
            R.drawable.vector_icon_transformation_3,
            R.drawable.vector_icon_transformation_4,
            R.drawable.vector_icon_transformation_5,
            R.drawable.vector_icon_transformation_6,
            R.drawable.vector_icon_render_order_1,
            R.drawable.vector_icon_render_order_2,
            R.drawable.vector_icon_stroke_1,
            R.drawable.vector_icon_stroke_2,
            R.drawable.vector_icon_stroke_3,
            R.drawable.vector_icon_scale_1,
            R.drawable.vector_icon_scale_2,
    };

    private int[] mGoldenImages = new int[] {
            R.drawable.vector_icon_create_golden,
            R.drawable.vector_icon_delete_golden,
            R.drawable.vector_icon_heart_golden,
            R.drawable.vector_icon_schedule_golden,
            R.drawable.vector_icon_settings_golden,
            R.drawable.vector_icon_random_path_1_golden,
            R.drawable.vector_icon_random_path_2_golden,
            R.drawable.vector_icon_repeated_cq_golden,
            R.drawable.vector_icon_repeated_st_golden,
            R.drawable.vector_icon_repeated_a_1_golden,
            R.drawable.vector_icon_repeated_a_2_golden,
            R.drawable.vector_icon_clip_path_1_golden,
            R.drawable.vector_icon_transformation_1_golden,
            R.drawable.vector_icon_transformation_2_golden,
            R.drawable.vector_icon_transformation_3_golden,
            R.drawable.vector_icon_transformation_4_golden,
            R.drawable.vector_icon_transformation_5_golden,
            R.drawable.vector_icon_transformation_6_golden,
            R.drawable.vector_icon_render_order_1_golden,
            R.drawable.vector_icon_render_order_2_golden,
            R.drawable.vector_icon_stroke_1_golden,
            R.drawable.vector_icon_stroke_2_golden,
            R.drawable.vector_icon_stroke_3_golden,
            R.drawable.vector_icon_scale_1_golden,
            R.drawable.vector_icon_scale_2_golden,
    };

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;
    // A small value is actually making sure that the values are matching
    // exactly with the golden image.
    // We can increase the threshold if the Skia is drawing with some variance
    // on different devices. So far, the tests show they are matching correctly.
    private static final float PIXEL_ERROR_THRESHOLD = 0.02f;
    private static final float PIXEL_ERROR_COUNT_THRESHOLD = 0.005f;

    private static final boolean DBG_DUMP_PNG = false;

    private Resources mResources;
    private VectorDrawable mVectorDrawable;
    private Bitmap mBitmap;
    private Canvas mCanvas;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final int width = IMAGE_WIDTH;
        final int height = IMAGE_HEIGHT;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mVectorDrawable = new VectorDrawable();
        mVectorDrawable.setBounds(0, 0, width, height);

        mResources = mContext.getResources();
    }

    public void testSimpleVectorDrawables() throws Exception {
        verifyVectorDrawables(mIconResIds, mGoldenImages, 0);
    }

    private void verifyVectorDrawables(int[] resIds, int[] goldenImages, float fraction) throws Exception {
        for (int i = 0; i < resIds.length; i++) {
            // Setup VectorDrawable from xml file and draw into the bitmap.
            XmlPullParser parser = mResources.getXml(resIds[i]);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type=parser.next()) != XmlPullParser.START_TAG &&
                    type != XmlPullParser.END_DOCUMENT) {
                // Empty loop
            }

            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }

            mVectorDrawable.inflate(mResources, parser, attrs);

            mBitmap.eraseColor(0);
            mVectorDrawable.draw(mCanvas);

            if (DBG_DUMP_PNG) {
                saveVectorDrawableIntoPNG(mBitmap, resIds, i);
            } else {
                // Start to compare
                Bitmap golden = BitmapFactory.decodeResource(mResources, goldenImages[i]);
                compareImages(mBitmap, golden, mResources.getString(resIds[i]));
            }
        }
    }

    // This is only for debugging or golden image (re)generation purpose.
    private void saveVectorDrawableIntoPNG(Bitmap bitmap, int[] resIds, int index) throws IOException {
        // Save the image to the disk.
        FileOutputStream out = null;
        try {
            String outputFolder = "/sdcard/temp/";
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            String originalFilePath = mResources.getString(resIds[index]);
            File originalFile = new File(originalFilePath);
            String fileFullName = originalFile.getName();
            String fileTitle = fileFullName.substring(0, fileFullName.lastIndexOf("."));
            String outputFilename = outputFolder + fileTitle + "_golden.png";
            File outputFile = new File(outputFilename);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            out = new FileOutputStream(outputFile, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.v(LOGTAG, "Write test No." + index + " to file successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void compareImages(Bitmap ideal, Bitmap given, String filename) {
        int idealWidth = ideal.getWidth();
        int idealHeight = ideal.getHeight();

        assertTrue(idealWidth == given.getWidth());
        assertTrue(idealHeight == given.getHeight());

        int totalDiffPixelCount = 0;
        float totalPixelCount = idealWidth * idealHeight;
        for (int x = 0; x < idealWidth; x++) {
            for (int y = 0; y < idealHeight; y++) {
                int idealColor = ideal.getPixel(x, y);
                int givenColor = given.getPixel(x, y);
                if (idealColor == givenColor)
                    continue;

                float totalError = 0;
                totalError += Math.abs(Color.red(idealColor) - Color.red(givenColor));
                totalError += Math.abs(Color.green(idealColor) - Color.green(givenColor));
                totalError += Math.abs(Color.blue(idealColor) - Color.blue(givenColor));
                totalError += Math.abs(Color.alpha(idealColor) - Color.alpha(givenColor));

                if ((totalError / 1024.0f) >= PIXEL_ERROR_THRESHOLD) {
                    fail((filename + ": totalError is " + totalError));
                }

                totalDiffPixelCount++;
            }
        }
        if ((totalDiffPixelCount / totalPixelCount) >= PIXEL_ERROR_COUNT_THRESHOLD) {
            fail((filename +": totalDiffPixelCount is " + totalDiffPixelCount));
        }

    }

    public void testGetChangingConfigurations() {
        VectorDrawable vectorDrawable = new VectorDrawable();
        ConstantState constantState = vectorDrawable.getConstantState();

        // default
        assertEquals(0, constantState.getChangingConfigurations());
        assertEquals(0, vectorDrawable.getChangingConfigurations());

        // change the drawable's configuration does not affect the state's configuration
        vectorDrawable.setChangingConfigurations(0xff);
        assertEquals(0xff, vectorDrawable.getChangingConfigurations());
        assertEquals(0, constantState.getChangingConfigurations());

        // the state's configuration get refreshed
        constantState = vectorDrawable.getConstantState();
        assertEquals(0xff,  constantState.getChangingConfigurations());

        // set a new configuration to drawable
        vectorDrawable.setChangingConfigurations(0xff00);
        assertEquals(0xff,  constantState.getChangingConfigurations());
        assertEquals(0xffff,  vectorDrawable.getChangingConfigurations());
    }

    public void testGetConstantState() {
        VectorDrawable vectorDrawable = new VectorDrawable();
        ConstantState constantState = vectorDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(0, constantState.getChangingConfigurations());

        vectorDrawable.setChangingConfigurations(1);
        constantState = vectorDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(1, constantState.getChangingConfigurations());
    }

    public void testMutate() {
        Resources resources = mContext.getResources();
        // d1 will be mutated, while d2 / d3 will not.
        VectorDrawable d1 = (VectorDrawable) resources.getDrawable(R.drawable.vector_icon_create);
        VectorDrawable d2 = (VectorDrawable) resources.getDrawable(R.drawable.vector_icon_create);
        VectorDrawable d3 = (VectorDrawable) resources.getDrawable(R.drawable.vector_icon_create);
        int originalAlpha = d2.getAlpha();

        d1.setAlpha(0x80);
        assertEquals(0x80, d1.getAlpha());
        assertEquals(0x80, d2.getAlpha());
        assertEquals(0x80, d3.getAlpha());

        d1.mutate();
        d1.setAlpha(0x40);
        assertEquals(0x40, d1.getAlpha());
        assertEquals(0x80, d2.getAlpha());
        assertEquals(0x80, d3.getAlpha());

        d2.setAlpha(0x20);
        assertEquals(0x40, d1.getAlpha());
        assertEquals(0x20, d2.getAlpha());
        assertEquals(0x20, d3.getAlpha());

        d2.setAlpha(originalAlpha);
    }

    public void testColorFilter() {
        PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.RED, Mode.SRC_IN);
        VectorDrawable vectorDrawable = new VectorDrawable();
        vectorDrawable.setColorFilter(filter);

        assertEquals(filter, vectorDrawable.getColorFilter());
    }
}
