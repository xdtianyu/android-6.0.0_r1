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

package com.android.sysapp.janktests;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.jank.GfxMonitor;
import android.support.test.jank.JankTest;
import android.support.test.jank.JankTestBase;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.StaleObjectException;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.Until;
import android.view.View;

import junit.framework.Assert;

/**
 * Jank test for Calendar
 * open and fling
 * cal.jank.test1@gmail
 */

public class CalendarJankTests extends JankTestBase {
    private static final int LONG_TIMEOUT = 2000;
    private static final int SHORT_TIMEOUT = 100;
    private static final int INNER_LOOP = 5;
    private static final int EXPECTED_FRAMES = 100;
    private static final int TAB_MIN_WIDTH = 600;
    private static final String PACKAGE_NAME = "com.google.android.calendar";
    private static final String RES_PACKAGE_NAME = "com.android.calendar";
    private UiDevice mDevice;

    private BySelector mCalendarSelector = null;
    private Direction mScrollDirection = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.setOrientationNatural();
        if (mDevice.getDisplaySizeDp().x < TAB_MIN_WIDTH) {
            mCalendarSelector = By.res(PACKAGE_NAME, "timely_list");
            mScrollDirection = Direction.DOWN;
        } else {
            mCalendarSelector = By.res(PACKAGE_NAME, "main_pane");
            mScrollDirection = Direction.RIGHT;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        mDevice.unfreezeRotation();
        super.tearDown();
    }

    public void launchApp(String packageName) throws UiObjectNotFoundException{
        PackageManager pm = getInstrumentation().getContext().getPackageManager();
        Intent appIntent = pm.getLaunchIntentForPackage(packageName);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(appIntent);
        SystemClock.sleep(SHORT_TIMEOUT * 10);
    }

    public void launchCalendar () throws UiObjectNotFoundException {
        launchApp(PACKAGE_NAME);
        mDevice.waitForIdle();
        dismissCling();
        assertNotNull("Calendar can't be found",
                mDevice.wait(Until.findObject(mCalendarSelector), LONG_TIMEOUT));
    }

    // Measures jank of flinging calendar items
    @JankTest(beforeTest="launchCalendar", expectedFrames=EXPECTED_FRAMES)
    @GfxMonitor(processName=PACKAGE_NAME)
    public void testCalendarItemsFling() {
        UiObject2 calendarItems = null;
        calendarItems = mDevice.wait(Until.findObject(mCalendarSelector), LONG_TIMEOUT);
        for (int i = 0; i < INNER_LOOP; i++) {
            calendarItems.scroll(mScrollDirection, 1.0f);
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    private void dismissCling() {
        UiObject2 splashScreen = null;
      splashScreen = mDevice.wait(Until.findObject(
              By.pkg(PACKAGE_NAME).clazz(View.class).desc("Got it")), LONG_TIMEOUT);
        if (splashScreen != null) {
            splashScreen.clickAndWait(Until.newWindow(), SHORT_TIMEOUT);
        }

        UiObject2 rightArrow = null;
        short counter = 8;
        while ((rightArrow = mDevice.wait(Until.findObject(By.res(
                PACKAGE_NAME, "right_arrow_touch")), LONG_TIMEOUT)) != null && counter > 0) {
            rightArrow.click();
            --counter;
        }
        UiObject2 gotIt = mDevice.wait(Until.findObject(
              By.res(PACKAGE_NAME, "done_button").text("Got it")), LONG_TIMEOUT);
        if (gotIt != null) {
            gotIt.click();
        }
    }
}
