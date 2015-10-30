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
import android.widget.ImageButton;

import junit.framework.Assert;

/**
 * Jank test for scrolling gmail inbox mails
 */

public class GMailJankTests extends JankTestBase {
    private static final int SHORT_TIMEOUT = 1000;
    private static final int LONG_TIMEOUT = 5000;
    private static final int INNER_LOOP = 5;
    private static final int EXPECTED_FRAMES = 100;
    private static final int TAB_MIN_WIDTH = 600;
    private static final String PACKAGE_NAME = "com.google.android.gm";
    private static final String RES_PACKAGE_NAME = "android";
    private UiDevice mDevice;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.setOrientationNatural();
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
        SystemClock.sleep(SHORT_TIMEOUT);
    }

    public void launchGMail () throws UiObjectNotFoundException {
        launchApp(PACKAGE_NAME);
        dismissClings();
        // Need any check for account-name??
        waitForEmailSync();
    }

    public void prepGMailInboxFling() throws UiObjectNotFoundException {
      launchGMail();
      // Ensure test is ready to be executed
      UiObject2 list = mDevice.wait(
              Until.findObject(By.res(PACKAGE_NAME, "conversation_list_view")), SHORT_TIMEOUT);
      Assert.assertNotNull("Failed to locate 'conversation_list_view'", list);
    }

    // Measures jank while scrolling gmail inbox
    @JankTest(beforeTest="prepGMailInboxFling", expectedFrames=EXPECTED_FRAMES)
    @GfxMonitor(processName=PACKAGE_NAME)
    public void testGMailInboxFling() {
        UiObject2 list = mDevice.wait(
                Until.findObject(By.res(PACKAGE_NAME, "conversation_list_view")), LONG_TIMEOUT);
        Assert.assertNotNull("Failed to locate 'conversation_list_view'", list);
        for (int i = 0; i < INNER_LOOP; i++) {
            list.scroll(Direction.DOWN, 1.0f);
            SystemClock.sleep(SHORT_TIMEOUT);
            list.scroll(Direction.UP, 1.0f);
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    public void prepOpenNavDrawer() throws UiObjectNotFoundException {
      launchGMail();
      // Ensure test is ready to be executed
      Assert.assertNotNull("Failed to locate Nav Drawer Openner", openNavigationDrawer());
    }

    // Measures jank while opening Navigation Drawer
    @JankTest(beforeTest="prepOpenNavDrawer", expectedFrames=EXPECTED_FRAMES)
    @GfxMonitor(processName=PACKAGE_NAME)
    public void testOpenNavDrawer() {
        UiObject2 navDrawer = openNavigationDrawer();
        for (int i = 0; i < INNER_LOOP; i++) {
            navDrawer.click();
            SystemClock.sleep(SHORT_TIMEOUT);
            mDevice.pressBack();
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    public void prepFlingNavDrawer() throws UiObjectNotFoundException{
        launchGMail();
        UiObject2 navDrawer = openNavigationDrawer();
        Assert.assertNotNull("Failed to locate Nav Drawer Openner", navDrawer);
        navDrawer.click();
        // Ensure test is ready to be executed
        UiObject2 container = getNavigationDrawerContainer();
        Assert.assertNotNull("Failed to locate Nav drawer container", container);
    }

    // Measures jank while flinging Navigation Drawer
    @JankTest(beforeTest="prepFlingNavDrawer", expectedFrames=EXPECTED_FRAMES)
    @GfxMonitor(processName=PACKAGE_NAME)
    public void testFlingNavDrawer() {
        UiObject2 container = getNavigationDrawerContainer();
        for (int i = 0; i < INNER_LOOP; i++) {
            container.fling(Direction.DOWN);
            SystemClock.sleep(SHORT_TIMEOUT);
            container.fling(Direction.UP);
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    private void dismissClings() {
        UiObject2 welcomeScreenGotIt = mDevice.wait(
            Until.findObject(By.res(PACKAGE_NAME, "welcome_tour_got_it")), SHORT_TIMEOUT);
        if (welcomeScreenGotIt != null) {
            welcomeScreenGotIt.clickAndWait(Until.newWindow(), SHORT_TIMEOUT);
        }
        UiObject2 welcomeScreenSkip = mDevice.wait(
            Until.findObject(By.res(PACKAGE_NAME, "welcome_tour_skip")), SHORT_TIMEOUT);
        if (welcomeScreenSkip != null) {
          welcomeScreenSkip.clickAndWait(Until.newWindow(), SHORT_TIMEOUT);
        }
        UiObject2 tutorialDone = mDevice.wait(
                Until.findObject(By.res(PACKAGE_NAME, "action_done")), 2 * SHORT_TIMEOUT);
        if (tutorialDone != null) {
            tutorialDone.clickAndWait(Until.newWindow(), SHORT_TIMEOUT);
        }
        mDevice.wait(Until.findObject(By.text("CONFIDENTIAL")), 2 * SHORT_TIMEOUT);
        UiObject2 splash = mDevice.findObject(By.text("Ok, got it"));
        if (splash != null) {
            splash.clickAndWait(Until.newWindow(), SHORT_TIMEOUT);
        }
    }

    public void waitForEmailSync() {
        // Wait up to 2 seconds for a "waiting" message to appear
        mDevice.wait(Until.hasObject(By.text("Waiting for sync")), 2 * SHORT_TIMEOUT);
        // Wait until any "waiting" messages are gone
        Assert.assertTrue("'Waiting for sync' timed out",
                mDevice.wait(Until.gone(By.text("Waiting for sync")), LONG_TIMEOUT * 6));
        Assert.assertTrue("'Loading' timed out",
                mDevice.wait(Until.gone(By.text("Loading")), LONG_TIMEOUT * 6));
    }

    public UiObject2 openNavigationDrawer() {
        UiObject2 navDrawer = null;
        if (mDevice.getDisplaySizeDp().x < TAB_MIN_WIDTH) {
            navDrawer = mDevice.wait(Until.findObject(
                    By.clazz(ImageButton.class).desc("Navigate up")), SHORT_TIMEOUT);
        } else {
            navDrawer = mDevice.wait(Until.findObject(
                    By.clazz(ImageButton.class).desc("Open navigation drawer")), SHORT_TIMEOUT);
        }
        return navDrawer;
    }

    public UiObject2 getNavigationDrawerContainer() {
        UiObject2 container = null;
        if (mDevice.getDisplaySizeDp().x < TAB_MIN_WIDTH) {
            container = mDevice.wait(
                    Until.findObject(By.res(PACKAGE_NAME, "content_pane")), SHORT_TIMEOUT);
        } else {
            container = mDevice.wait(
                    Until.findObject(By.res(RES_PACKAGE_NAME, "list")), SHORT_TIMEOUT);
        }
        return container;
    }
}