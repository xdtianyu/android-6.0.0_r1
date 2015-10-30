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

package android.assist.cts;

import android.assist.cts.TestStartActivity;
import android.assist.common.Utils;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.app.assist.AssistStructure.WindowNode;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.cts.util.SystemUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AssistTestBase extends ActivityInstrumentationTestCase2<TestStartActivity> {
    private static final String TAG = "AssistTestBase";

    protected TestStartActivity mTestActivity;
    protected AssistContent mAssistContent;
    protected AssistStructure mAssistStructure;
    protected boolean mScreenshot;
    protected Bitmap mAppScreenshot;
    protected BroadcastReceiver mReceiver;
    protected Bundle mAssistBundle;
    protected Context mContext;
    protected CountDownLatch mLatch, mScreenshotLatch, mHasResumedLatch;
    protected boolean mScreenshotMatches;
    private Point mDisplaySize;
    private String mTestName;
    private View mView;

    public AssistTestBase() {
        super(TestStartActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        SystemUtil.runShellCommand(getInstrumentation(),
                "settings put secure assist_structure_enabled 1");
        SystemUtil.runShellCommand(getInstrumentation(),
                "settings put secure assist_screenshot_enabled 1");
        logContextAndScreenshotSetting();

        // reset old values
        mScreenshotMatches = false;
        mScreenshot = false;
        mAssistStructure = null;
        mAssistContent = null;
        mAssistBundle = null;

        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        mReceiver = new TestResultsReceiver();
        mContext.registerReceiver(mReceiver,
            new IntentFilter(Utils.BROADCAST_ASSIST_DATA_INTENT));
    }

    @Override
    protected void tearDown() throws Exception {
        mTestActivity.finish();
        mContext.sendBroadcast(new Intent(Utils.HIDE_SESSION));
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.tearDown();
    }

    /**
     * Starts the shim service activity
     */
    protected void startTestActivity(String testName) {
        Intent intent = new Intent();
        mTestName = testName;
        intent.setAction("android.intent.action.TEST_START_ACTIVITY_" + testName);
        intent.setComponent(new ComponentName(getInstrumentation().getContext(),
                TestStartActivity.class));
        intent.putExtra(Utils.TESTCASE_TYPE, testName);
        setActivityIntent(intent);
        mTestActivity = getActivity();
    }

    /**
     * Called when waiting for Assistant's Broadcast Receiver to be setup
     */
    public void waitForAssistantToBeReady(CountDownLatch latch) throws Exception {
        Log.i(TAG, "waiting for assistant to be ready before continuing");
        if (!latch.await(Utils.TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Assistant was not ready before timeout of: " + Utils.TIMEOUT_MS + "msec");
        }
    }

    /**
     * Send broadcast to MainInteractionService to start a session
     */
    protected void startSession() {
        startSession(mTestName, new Bundle());
    }

    protected void startSession(String testName, Bundle extras) {
        Intent intent = new Intent(Utils.BROADCAST_INTENT_START_ASSIST);
        Log.i(TAG, "passed in class test name is: " + testName);
        intent.putExtra(Utils.TESTCASE_TYPE, testName);
        addDimensionsToIntent(intent);
        intent.putExtras(extras);
        mContext.sendBroadcast(intent);
    }

    /**
     * Calculate display dimensions (including navbar) to pass along in the given intent.
     */
    private void addDimensionsToIntent(Intent intent) {
        if (mDisplaySize == null) {
            Display display = mTestActivity.getWindowManager().getDefaultDisplay();
            mDisplaySize = new Point();
            display.getRealSize(mDisplaySize);
        }
        intent.putExtra(Utils.DISPLAY_WIDTH_KEY, mDisplaySize.x);
        intent.putExtra(Utils.DISPLAY_HEIGHT_KEY, mDisplaySize.y);
    }

    /**
     * Called after startTestActivity. Includes check for receiving context.
     */
    protected boolean waitForBroadcast() throws Exception {
        mTestActivity.start3pApp(mTestName);
        mTestActivity.startTest(mTestName);
        return waitForContext();
    }

    protected boolean waitForContext() throws Exception {
        mLatch = new CountDownLatch(1);

        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        mReceiver = new TestResultsReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.BROADCAST_ASSIST_DATA_INTENT);
        mContext.registerReceiver(mReceiver, filter);
        if (!mLatch.await(Utils.getAssistDataTimeout(mTestName), TimeUnit.MILLISECONDS)) {
            fail("Fail to receive broadcast in " + Utils.getAssistDataTimeout(mTestName) + "msec");
        }
        Log.i(TAG, "Received broadcast with all information.");
        return true;
    }

    /**
     * Checks that the nullness of values are what we expect.
     *
     * @param isBundleNull True if assistBundle should be null.
     * @param isStructureNull True if assistStructure should be null.
     * @param isContentNull True if assistContent should be null.
     * @param isScreenshotNull True if screenshot should be null.
     */
    protected void verifyAssistDataNullness(boolean isBundleNull, boolean isStructureNull,
            boolean isContentNull, boolean isScreenshotNull) {

        if ((mAssistContent == null) != isContentNull) {
            fail(String.format("Should %s have been null - AssistContent: %s",
                    isContentNull? "":"not", mAssistContent));
        }

        if ((mAssistStructure == null) != isStructureNull) {
            fail(String.format("Should %s have been null - AssistStructure: %s",
                isStructureNull ? "" : "not", mAssistStructure));
        }

        if ((mAssistBundle == null) != isBundleNull) {
            fail(String.format("Should %s have been null - AssistBundle: %s",
                    isBundleNull? "":"not", mAssistBundle));
        }

        if (mScreenshot == isScreenshotNull) {
            fail(String.format("Should %s have been null - Screenshot: %s",
                    isScreenshotNull? "":"not", mScreenshot));
        }
    }

    /**
     * Verifies the view hierarchy of the backgroundApp matches the assist structure.
     *
     * @param backgroundApp ComponentName of app the assistant is invoked upon
     * @param isSecureWindow Denotes whether the activity has FLAG_SECURE set
     */
    protected void verifyAssistStructure(ComponentName backgroundApp, boolean isSecureWindow) {
        // Check component name matches
        assertEquals(backgroundApp.flattenToString(),
            mAssistStructure.getActivityComponent().flattenToString());

        Log.i(TAG, "Traversing down structure for: " + backgroundApp.flattenToString());
        mView = mTestActivity.findViewById(android.R.id.content).getRootView();
        verifyHierarchy(mAssistStructure, isSecureWindow);
    }

    protected void logContextAndScreenshotSetting() {
        Log.i(TAG, "Context is: " + Settings.Secure.getString(
                mContext.getContentResolver(), "assist_structure_enabled"));
        Log.i(TAG, "Screenshot is: " + Settings.Secure.getString(
                mContext.getContentResolver(), "assist_screenshot_enabled"));
    }

    /**
     * Recursively traverse and compare properties in the View hierarchy with the Assist Structure.
     */
    public void verifyHierarchy(AssistStructure structure, boolean isSecureWindow) {
        Log.i(TAG, "verifyHierarchy");
        Window mWindow = mTestActivity.getWindow();

        int numWindows = structure.getWindowNodeCount();
        // TODO: multiple windows?
        assertEquals("Number of windows don't match", 1, numWindows);

        for (int i = 0; i < numWindows; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            Log.i(TAG, "Title: " + windowNode.getTitle());
            // verify top level window bounds are as big as the screen and pinned to 0,0
            assertEquals("Window left position wrong: was " + windowNode.getLeft(),
                    windowNode.getLeft(), 0);
            assertEquals("Window top position wrong: was " + windowNode.getTop(),
                    windowNode.getTop(), 0);

            traverseViewAndStructure(
                    mView,
                    windowNode.getRootViewNode(),
                    isSecureWindow);
        }
    }

    private void traverseViewAndStructure(View parentView, ViewNode parentNode,
            boolean isSecureWindow) {
        ViewGroup parentGroup;

        if (parentView == null && parentNode == null) {
            Log.i(TAG, "Views are null, done traversing this branch.");
            return;
        } else if (parentNode == null || parentView == null) {
            fail(String.format("Views don't match. View: %s, Node: %s", parentView, parentNode));
        }

        // Debugging
        Log.i(TAG, "parentView is of type: " + parentView.getClass().getName());
        if (parentView instanceof ViewGroup) {
            for (int childInt = 0; childInt < ((ViewGroup) parentView).getChildCount();
                    childInt++) {
                Log.i(TAG,
                        "viewchild" + childInt + " is of type: "
                        + ((ViewGroup) parentView).getChildAt(childInt).getClass().getName());
            }
        }
        if (parentView.getId() > 0) {
            Log.i(TAG, "View ID: "
                    + mTestActivity.getResources().getResourceEntryName(parentView.getId()));
        }

        Log.i(TAG, "parentNode is of type: " + parentNode.getClassName());
        for (int nodeInt = 0; nodeInt < parentNode.getChildCount(); nodeInt++) {
            Log.i(TAG,
                    "nodechild" + nodeInt + " is of type: "
                    + parentNode.getChildAt(nodeInt).getClassName());
        }
            Log.i(TAG, "Node ID: " + parentNode.getIdEntry());

        int numViewChildren = 0;
        int numNodeChildren = 0;
        if (parentView instanceof ViewGroup) {
            numViewChildren = ((ViewGroup) parentView).getChildCount();
        }
        numNodeChildren = parentNode.getChildCount();

        if (isSecureWindow) {
            assertEquals("Secure window should only traverse root node.", 0, numNodeChildren);
            isSecureWindow = false;
            return;
        } else {
            assertEquals("Number of children did not match.", numViewChildren, numNodeChildren);
        }

        verifyViewProperties(parentView, parentNode);

        if (parentView instanceof ViewGroup) {
            parentGroup = (ViewGroup) parentView;

            // TODO: set a max recursion level
            for (int i = numNodeChildren - 1; i >= 0; i--) {
                View childView = parentGroup.getChildAt(i);
                ViewNode childNode = parentNode.getChildAt(i);

                // if isSecureWindow, should not have reached this point.
                assertFalse(isSecureWindow);
                traverseViewAndStructure(childView, childNode, isSecureWindow);
            }
        }
    }

    /**
     * Compare view properties of the view hierarchy with that reported in the assist structure.
     */
    private void verifyViewProperties(View parentView, ViewNode parentNode) {
        assertEquals("Left positions do not match.", parentView.getLeft(), parentNode.getLeft());
        assertEquals("Top positions do not match.", parentView.getTop(), parentNode.getTop());

        int viewId = parentView.getId();

        if (viewId > 0) {
            if (parentNode.getIdEntry() != null) {
                assertEquals("View IDs do not match.",
                        mTestActivity.getResources().getResourceEntryName(viewId),
                        parentNode.getIdEntry());
            }
        } else {
            assertNull("View Node should not have an ID.", parentNode.getIdEntry());
        }

        Log.i(TAG, "parent text: " + parentNode.getText());
        if (parentView instanceof TextView) {
            Log.i(TAG, "view text: " + ((TextView) parentView).getText());
        }

        assertEquals("Scroll X does not match.",
                parentView.getScrollX(), parentNode.getScrollX());

        assertEquals("Scroll Y does not match.",
                parentView.getScrollY(), parentNode.getScrollY());

        assertEquals("Heights do not match.", parentView.getHeight(),
                parentNode.getHeight());

        assertEquals("Widths do not match.", parentView.getWidth(), parentNode.getWidth());

        // TODO: handle unicode/i18n
        if (parentView instanceof TextView) {
            assertEquals("Text in TextView does not match.",
                    ((TextView) parentView).getText().toString(), parentNode.getText());
        } else {
            assertNull(parentNode.getText());
        }
    }

    class TestResultsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Utils.BROADCAST_ASSIST_DATA_INTENT)) {
                Log.i(TAG, "Received broadcast with assist data.");
                Bundle assistData = intent.getExtras();
                AssistTestBase.this.mAssistBundle = assistData.getBundle(Utils.ASSIST_BUNDLE_KEY);
                AssistTestBase.this.mAssistStructure = assistData.getParcelable(
                        Utils.ASSIST_STRUCTURE_KEY);
                AssistTestBase.this.mAssistContent = assistData.getParcelable(
                        Utils.ASSIST_CONTENT_KEY);

                AssistTestBase.this.mScreenshot =
                        assistData.getBoolean(Utils.ASSIST_SCREENSHOT_KEY, false);

                AssistTestBase.this.mScreenshotMatches = assistData.getBoolean(
                        Utils.COMPARE_SCREENSHOT_KEY, false);

                if (mLatch != null) {
                    Log.i(AssistTestBase.TAG, "counting down latch. received assist data.");
                    mLatch.countDown();
                }
            } else if (intent.getAction().equals(Utils.APP_3P_HASRESUMED)) {
                if (mHasResumedLatch != null) {
                    mHasResumedLatch.countDown();
                }
            }
        }
    }
}
