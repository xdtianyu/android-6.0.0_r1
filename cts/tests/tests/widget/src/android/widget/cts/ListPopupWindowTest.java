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

package android.widget.cts;

import com.android.cts.widget.R;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

public class ListPopupWindowTest extends
        ActivityInstrumentationTestCase2<MockPopupWindowCtsActivity> {
    private Instrumentation mInstrumentation;
    private Activity mActivity;

    /** The list popup window. */
    private ListPopupWindow mPopupWindow;

    /**
     * Instantiates a new popup window test.
     */
    public ListPopupWindowTest() {
        super(MockPopupWindowCtsActivity.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.test.ActivityInstrumentationTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
    }

    public void testConstructor() {
        new ListPopupWindow(mActivity);

        new ListPopupWindow(mActivity, null);

        new ListPopupWindow(mActivity, null, android.R.attr.popupWindowStyle);

        new ListPopupWindow(mActivity, null, 0, android.R.style.Widget_Material_ListPopupWindow);
    }

    public void testAccessBackground() {
        mPopupWindow = new ListPopupWindow(mActivity);

        Drawable drawable = new ColorDrawable();
        mPopupWindow.setBackgroundDrawable(drawable);
        assertSame(drawable, mPopupWindow.getBackground());

        mPopupWindow.setBackgroundDrawable(null);
        assertNull(mPopupWindow.getBackground());
    }

    public void testAccessAnimationStyle() {
        mPopupWindow = new ListPopupWindow(mActivity);
        assertEquals(0, mPopupWindow.getAnimationStyle());

        mPopupWindow.setAnimationStyle(android.R.style.Animation_Toast);
        assertEquals(android.R.style.Animation_Toast, mPopupWindow.getAnimationStyle());

        // abnormal values
        mPopupWindow.setAnimationStyle(-100);
        assertEquals(-100, mPopupWindow.getAnimationStyle());
    }

    public void testAccessHeight() {
        mPopupWindow = new ListPopupWindow(mActivity);
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, mPopupWindow.getHeight());

        int height = getDisplay().getHeight() / 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        height = getDisplay().getHeight();
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        mPopupWindow.setHeight(0);
        assertEquals(0, mPopupWindow.getHeight());

        height = getDisplay().getHeight() * 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        height = -getDisplay().getHeight() / 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());
    }

    /**
     * Gets the display.
     *
     * @return the display
     */
    private Display getDisplay() {
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
    }

    public void testAccessWidth() {
        mPopupWindow = new ListPopupWindow(mActivity);
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, mPopupWindow.getWidth());

        int width = getDisplay().getWidth() / 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        width = getDisplay().getWidth();
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        mPopupWindow.setWidth(0);
        assertEquals(0, mPopupWindow.getWidth());

        width = getDisplay().getWidth() * 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        width = - getDisplay().getWidth() / 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());
    }

    public void testShow() {
        int[] anchorXY = new int[2];
        int[] viewOnScreenXY = new int[2];
        int[] viewInWindowXY = new int[2];

        mPopupWindow = new ListPopupWindow(mActivity);

        final View upperAnchor = mActivity.findViewById(R.id.anchor_upper);
        mPopupWindow.setAnchorView(upperAnchor);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.show();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());

        mPopupWindow.getListView().getLocationOnScreen(viewOnScreenXY);
        upperAnchor.getLocationOnScreen(anchorXY);
        mPopupWindow.getListView().getLocationInWindow(viewInWindowXY);
        assertEquals(anchorXY[0] + viewInWindowXY[0], viewOnScreenXY[0]);
        assertEquals(anchorXY[1] + viewInWindowXY[1] + upperAnchor.getHeight(), viewOnScreenXY[1]);

        dismissPopup();
    }

    public void testSetWindowLayoutType() {
        mPopupWindow = new ListPopupWindow(mActivity);

        final View upperAnchor = mActivity.findViewById(R.id.anchor_upper);
        mPopupWindow.setAnchorView(upperAnchor);
        mPopupWindow.setWindowLayoutType(
                WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.show();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());

        WindowManager.LayoutParams p = (WindowManager.LayoutParams)
                mPopupWindow.getListView().getRootView().getLayoutParams();
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL, p.type);

        dismissPopup();
    }

    @UiThreadTest
    public void testDismiss() {
        mPopupWindow = new ListPopupWindow(mActivity);
        assertFalse(mPopupWindow.isShowing());
        View anchorView = mActivity.findViewById(R.id.anchor_upper);
        mPopupWindow.setAnchorView(anchorView);
        mPopupWindow.show();

        mPopupWindow.dismiss();
        assertFalse(mPopupWindow.isShowing());

        mPopupWindow.dismiss();
        assertFalse(mPopupWindow.isShowing());
    }

    public void testSetOnDismissListener() {
        mPopupWindow = new ListPopupWindow(mActivity);
        mPopupWindow.setOnDismissListener(null);

        MockOnDismissListener onDismissListener = new MockOnDismissListener();
        mPopupWindow.setOnDismissListener(onDismissListener);
        showPopup();
        dismissPopup();
        assertEquals(1, onDismissListener.getOnDismissCalledCount());

        showPopup();
        dismissPopup();
        assertEquals(2, onDismissListener.getOnDismissCalledCount());

        mPopupWindow.setOnDismissListener(null);
        showPopup();
        dismissPopup();
        assertEquals(2, onDismissListener.getOnDismissCalledCount());
    }

    public void testAccessInputMethodMode() {
        mPopupWindow = new ListPopupWindow(mActivity);
        assertEquals(PopupWindow.INPUT_METHOD_NEEDED, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_FROM_FOCUSABLE);
        assertEquals(PopupWindow.INPUT_METHOD_FROM_FOCUSABLE, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        assertEquals(PopupWindow.INPUT_METHOD_NEEDED, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        assertEquals(PopupWindow.INPUT_METHOD_NOT_NEEDED, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(-1);
        assertEquals(-1, mPopupWindow.getInputMethodMode());
    }

    /**
     * The listener interface for receiving OnDismiss events. The class that is
     * interested in processing a OnDismiss event implements this interface, and
     * the object created with that class is registered with a component using
     * the component's <code>setOnDismissListener<code> method. When
     * the OnDismiss event occurs, that object's appropriate
     * method is invoked.
     */
    private static class MockOnDismissListener implements OnDismissListener {

        /** The Ondismiss called count. */
        private int mOnDismissCalledCount;

        /**
         * Gets the onDismiss() called count.
         *
         * @return the on dismiss called count
         */
        public int getOnDismissCalledCount() {
            return mOnDismissCalledCount;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.PopupWindow.OnDismissListener#onDismiss()
         */
        public void onDismiss() {
            mOnDismissCalledCount++;
        }

    }

    /**
     * Show PopupWindow.
     */
    // FIXME: logcat info complains that there is window leakage due to that mPopupWindow is not
    // clean up. Need to fix it.
    private void showPopup() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                if (mPopupWindow == null || mPopupWindow.isShowing()) {
                    return;
                }
                View anchor = mActivity.findViewById(R.id.anchor_upper);
                mPopupWindow.setAnchorView(anchor);
                mPopupWindow.show();
                assertTrue(mPopupWindow.isShowing());
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    /**
     * Dismiss PopupWindow.
     */
    private void dismissPopup() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                if (mPopupWindow == null || !mPopupWindow.isShowing())
                    return;
                mPopupWindow.dismiss();
            }
        });
        mInstrumentation.waitForIdleSync();
    }
}
