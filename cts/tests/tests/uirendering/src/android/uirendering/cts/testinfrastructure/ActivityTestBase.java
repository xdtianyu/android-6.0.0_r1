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
package android.uirendering.cts.testinfrastructure;

import android.annotation.Nullable;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.test.ActivityInstrumentationTestCase2;
import android.uirendering.cts.bitmapcomparers.BitmapComparer;
import android.uirendering.cts.bitmapverifiers.BitmapVerifier;
import android.uirendering.cts.differencevisualizers.DifferenceVisualizer;
import android.uirendering.cts.differencevisualizers.PassFailVisualizer;
import android.uirendering.cts.util.BitmapDumper;
import android.util.Log;

import android.support.test.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains the basis for the graphics hardware test classes. Contained within this class
 * are several methods that help with the execution of tests, and should be extended to gain the
 * functionality built in.
 */
public abstract class ActivityTestBase extends
        ActivityInstrumentationTestCase2<DrawActivity> {
    public static final String TAG = "ActivityTestBase";
    public static final boolean DEBUG = false;
    public static final boolean USE_RS = false;

    //The minimum height and width of a device
    public static final int TEST_WIDTH = 90;
    public static final int TEST_HEIGHT = 90;

    public static final int MAX_SCREEN_SHOTS = 100;

    private int[] mHardwareArray = new int[TEST_HEIGHT * TEST_WIDTH];
    private int[] mSoftwareArray = new int[TEST_HEIGHT * TEST_WIDTH];
    private DifferenceVisualizer mDifferenceVisualizer;
    private RenderScript mRenderScript;
    private TestCaseBuilder mTestCaseBuilder;

    /**
     * The default constructor creates the package name and sets the DrawActivity as the class that
     * we would use.
     */
    public ActivityTestBase() {
        super(DrawActivity.class);
        mDifferenceVisualizer = new PassFailVisualizer();

        // Create a location for the files to be held, if it doesn't exist already
        BitmapDumper.createSubDirectory(this.getClass().getSimpleName());

        // If we have a test currently, let's remove the older files if they exist
        if (getName() != null) {
            BitmapDumper.deleteFileInClassFolder(this.getClass().getSimpleName(), getName());
        }
    }

    /**
     * This method is called before each test case and should be called from the test class that
     * extends this class.
     */
    @Override
    public void setUp() {
        // As the way to access Instrumentation is changed in the new runner, we need to inject it
        // manually into ActivityInstrumentationTestCase2. ActivityInstrumentationTestCase2 will
        // be marked as deprecated and replaced with ActivityTestRule.
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mDifferenceVisualizer = new PassFailVisualizer();
        if (USE_RS) {
            mRenderScript = RenderScript.create(getActivity().getApplicationContext());
        }
    }

    /**
     * This method will kill the activity so that it can be reset depending on the test.
     */
    @Override
    public void tearDown() {
        if (mTestCaseBuilder != null) {
            List<TestCase> testCases = mTestCaseBuilder.getTestCases();

            if (testCases.size() == 0) {
                throw new IllegalStateException("Must have at least one test case");
            }


            for (TestCase testCase : testCases) {
                if (!testCase.wasTestRan) {
                    Log.w(TAG, getName() + " not all of the tests ran");
                    break;
                }
            }
            mTestCaseBuilder = null;
        }

        Runnable finishRunnable = new Runnable() {

            @Override
            public void run() {
                getActivity().finish();
            }
        };

        getActivity().runOnUiThread(finishRunnable);
    }

    static int[] getBitmapPixels(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(),
                0, 0, bitmap.getWidth(), bitmap.getHeight());
        return pixels;
    }

    private Bitmap takeScreenshotImpl(Point testOffset) {
        Bitmap source = getInstrumentation().getUiAutomation().takeScreenshot();
        return Bitmap.createBitmap(source, testOffset.x, testOffset.y, TEST_WIDTH, TEST_HEIGHT);
    }

    public Bitmap takeScreenshot(Point testOffset) {
        getInstrumentation().waitForIdleSync();
        Bitmap bitmap1 = takeScreenshotImpl(testOffset);
        Bitmap bitmap2;
        int count = 0;
        do  {
            bitmap2 = bitmap1;
            bitmap1 = takeScreenshotImpl(testOffset);
            count++;
        } while (count < MAX_SCREEN_SHOTS &&
                !Arrays.equals(getBitmapPixels(bitmap2), getBitmapPixels(bitmap1)));
        return bitmap1;
    }

    /**
     * Sets the current DifferenceVisualizer for use in current test.
     */
    public void setDifferenceVisualizer(DifferenceVisualizer differenceVisualizer) {
        mDifferenceVisualizer = differenceVisualizer;
    }

    /**
     * Used to execute a specific part of a test and get the resultant bitmap
     */
    protected Bitmap captureRenderSpec(TestCase testCase) {
        Point testOffset = getActivity().enqueueRenderSpecAndWait(
                testCase.layoutID, testCase.canvasClient,
                testCase.webViewUrl, testCase.viewInitializer, testCase.useHardware);
        testCase.wasTestRan = true;
        return takeScreenshot(testOffset);
    }

    /**
     * Compares the two bitmaps saved using the given test. If they fail, the files are saved using
     * the test name.
     */
    protected void assertBitmapsAreSimilar(Bitmap bitmap1, Bitmap bitmap2,
            BitmapComparer comparer, String debugMessage) {
        boolean success;

        if (USE_RS && comparer.supportsRenderScript()) {
            Allocation idealAllocation = Allocation.createFromBitmap(mRenderScript, bitmap1,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            Allocation givenAllocation = Allocation.createFromBitmap(mRenderScript, bitmap2,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            success = comparer.verifySameRS(getActivity().getResources(), idealAllocation,
                    givenAllocation, 0, TEST_WIDTH, TEST_WIDTH, TEST_HEIGHT, mRenderScript);
        } else {
            bitmap1.getPixels(mSoftwareArray, 0, TEST_WIDTH, 0, 0, TEST_WIDTH, TEST_HEIGHT);
            bitmap2.getPixels(mHardwareArray, 0, TEST_WIDTH, 0, 0, TEST_WIDTH, TEST_HEIGHT);
            success = comparer.verifySame(mSoftwareArray, mHardwareArray, 0, TEST_WIDTH, TEST_WIDTH,
                    TEST_HEIGHT);
        }

        if (!success) {
            BitmapDumper.dumpBitmaps(bitmap1, bitmap2, getName(), this.getClass().getSimpleName(),
                    mDifferenceVisualizer);
        }

        assertTrue(debugMessage, success);
    }

    /**
     * Tests to see if a bitmap passes a verifier's test. If it doesn't the bitmap is saved to the
     * sdcard.
     */
    protected void assertBitmapIsVerified(Bitmap bitmap, BitmapVerifier bitmapVerifier,
            String debugMessage) {
        bitmap.getPixels(mSoftwareArray, 0, TEST_WIDTH, 0, 0,
                TEST_WIDTH, TEST_HEIGHT);
        boolean success = bitmapVerifier.verify(mSoftwareArray, 0, TEST_WIDTH, TEST_WIDTH, TEST_HEIGHT);
        if (!success) {
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, TEST_WIDTH, TEST_HEIGHT);
            BitmapDumper.dumpBitmap(croppedBitmap, getName(), this.getClass().getSimpleName());
            BitmapDumper.dumpBitmap(bitmapVerifier.getDifferenceBitmap(), getName() + "_verifier",
                    this.getClass().getSimpleName());
        }
        assertTrue(debugMessage, success);
    }

    protected TestCaseBuilder createTest() {
        mTestCaseBuilder = new TestCaseBuilder();
        return mTestCaseBuilder;
    }

    /**
     * Defines a group of CanvasClients, XML layouts, and WebView html files for testing.
     */
    protected class TestCaseBuilder {
        private List<TestCase> mTestCases;

        private TestCaseBuilder() {
            mTestCases = new ArrayList<TestCase>();
        }

        /**
         * Runs a test where the first test case is considered the "ideal" image and from there,
         * every test case is tested against it.
         */
        public void runWithComparer(BitmapComparer bitmapComparer) {
            if (mTestCases.size() == 0) {
                throw new IllegalStateException("Need at least one test to run");
            }

            Bitmap idealBitmap = captureRenderSpec(mTestCases.remove(0));

            for (TestCase testCase : mTestCases) {
                Bitmap testCaseBitmap = captureRenderSpec(testCase);
                assertBitmapsAreSimilar(idealBitmap, testCaseBitmap, bitmapComparer,
                        testCase.getDebugString());
            }
        }

        /**
         * Runs a test where each testcase is independent of the others and each is checked against
         * the verifier given.
         */
        public void runWithVerifier(BitmapVerifier bitmapVerifier) {
            if (mTestCases.size() == 0) {
                throw new IllegalStateException("Need at least one test to run");
            }

            for (TestCase testCase : mTestCases) {
                Bitmap testCaseBitmap = captureRenderSpec(testCase);
                assertBitmapIsVerified(testCaseBitmap, bitmapVerifier, testCase.getDebugString());
            }
        }

        /**
         * Runs a test where each testcase is run without verification. Should only be used
         * where custom CanvasClients, Views, or ViewInitializers do their own internal
         * test assertions.
         */
        public void runWithoutVerification() {
            runWithVerifier(new BitmapVerifier() {
                @Override
                public boolean verify(int[] bitmap, int offset, int stride, int width, int height) {
                    return true;
                }
            });
        }

        public TestCaseBuilder addWebView(String webViewUrl,
                @Nullable ViewInitializer viewInitializer) {
            return addWebView(webViewUrl, viewInitializer, false)
                    .addWebView(webViewUrl, viewInitializer, true);
        }

        public TestCaseBuilder addLayout(int layoutId, @Nullable ViewInitializer viewInitializer) {
            return addLayout(layoutId, viewInitializer, false)
                    .addLayout(layoutId, viewInitializer, true);
        }

        public TestCaseBuilder addCanvasClient(CanvasClient canvasClient) {
            return addCanvasClient(canvasClient, false)
                    .addCanvasClient(canvasClient, true);
        }

        public TestCaseBuilder addWebView(String webViewUrl,
                @Nullable ViewInitializer viewInitializer, boolean useHardware) {
            mTestCases.add(new TestCase(null, 0, webViewUrl, viewInitializer, useHardware));
            return this;
        }

        public TestCaseBuilder addLayout(int layoutId, @Nullable ViewInitializer viewInitializer,
                boolean useHardware) {
            mTestCases.add(new TestCase(null, layoutId, null, viewInitializer, useHardware));
            return this;
        }

        public TestCaseBuilder addCanvasClient(CanvasClient canvasClient, boolean useHardware) {
            mTestCases.add(new TestCase(canvasClient, 0, null, null, useHardware));
            return this;
        }

        private List<TestCase> getTestCases() {
            return mTestCases;
        }
    }

    private class TestCase {
        public int layoutID;
        public CanvasClient canvasClient;
        public String webViewUrl;
        public ViewInitializer viewInitializer;
        public boolean useHardware;
        public boolean wasTestRan;

        public TestCase(CanvasClient client, int id, String viewUrl,
                ViewInitializer viewInitializer, boolean useHardware) {
            int count = 0;
            count += (client == null ? 0 : 1);
            count += (viewUrl == null ? 0 : 1);
            count += (id == 0 ? 0 : 1);
            assert(count == 1);
            assert(client == null || viewInitializer == null);
            this.layoutID = id;
            this.canvasClient = client;
            this.webViewUrl = viewUrl;
            this.viewInitializer = viewInitializer;
            this.useHardware = useHardware;
            this.wasTestRan = false;
        }

        public String getDebugString() {
            String debug = "";
            if (canvasClient != null) {
                debug += "CanvasClient : ";
                if (canvasClient.getDebugString() != null) {
                    debug += canvasClient.getDebugString();
                } else {
                    debug += "no debug string given";
                }
            } else if (webViewUrl != null) {
                debug += "WebView URL : " + webViewUrl;
            } else {
                debug += "Layout resource : " +
                        getActivity().getResources().getResourceName(layoutID);
            }
            debug += "\nTest ran in " + (useHardware ? "hardware" : "software") + "\n";
            return debug;
        }
    }
}
