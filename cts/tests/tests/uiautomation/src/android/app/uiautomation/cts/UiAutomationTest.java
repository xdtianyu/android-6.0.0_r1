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

package android.app.uiautomation.cts;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.UiAutomation;
import android.content.Intent;
import android.view.FrameStats;
import android.view.WindowAnimationFrameStats;
import android.view.WindowContentFrameStats;
import android.view.accessibility.AccessibilityWindowInfo;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.UiAutomatorTestCase;

import java.util.List;

/**
 * Tests for the UiAutomation APIs.
 */
public class UiAutomationTest extends UiAutomatorTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        AccessibilityServiceInfo info = getInstrumentation().getUiAutomation().getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        getInstrumentation().getUiAutomation().setServiceInfo(info);
    }

    public void testWindowContentFrameStats() throws Exception {
        Activity activity = null;
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

            // Start an activity.
            Intent intent = new Intent(getInstrumentation().getContext(),
                    UiAutomationTestFirstActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = getInstrumentation().startActivitySync(intent);

            // Wait for things to settle.
            getUiDevice().waitForIdle();

            // Find the application window.
            final int windowId = findAppWindowId(uiAutomation.getWindows());
            assertTrue(windowId >= 0);

            // Clear stats to be with a clean slate.
            assertTrue(uiAutomation.clearWindowContentFrameStats(windowId));

            // Find the list to scroll around.
            UiScrollable listView = new UiScrollable(new UiSelector().resourceId(
                    "android.app.cts.uiautomation:id/list_view"));

            // Scoll a bit.
            listView.scrollToEnd(Integer.MAX_VALUE);
            listView.scrollToBeginning(Integer.MAX_VALUE);

            // Get the frame stats.
            WindowContentFrameStats stats = uiAutomation.getWindowContentFrameStats(windowId);

            // Check the frame stats...

            // We should have somethong.
            assertNotNull(stats);

            // The refresh presiod is always positive.
            assertTrue(stats.getRefreshPeriodNano() > 0);

            // There is some frame data.
            final int frameCount = stats.getFrameCount();
            assertTrue(frameCount > 0);

            // The frames are ordered in ascending order.
            assertWindowContentTimestampsInAscendingOrder(stats);

            // The start and end times are based on first and last frame.
            assertEquals(stats.getStartTimeNano(), stats.getFramePresentedTimeNano(0));
            assertEquals(stats.getEndTimeNano(), stats.getFramePresentedTimeNano(frameCount - 1));
        } finally {
            // Clean up.
            if (activity != null) {
                activity.finish();
            }
        }
    }

    public void testWindowContentFrameStatsNoAnimation() throws Exception {
        Activity activity = null;
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

            // Start an activity.
            Intent intent = new Intent(getInstrumentation().getContext(),
                    UiAutomationTestFirstActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = getInstrumentation().startActivitySync(intent);

            // Wait for things to settle.
            getUiDevice().waitForIdle();

            // Wait for Activity draw finish
            getInstrumentation().waitForIdleSync();

            // Find the application window.
            final int windowId = findAppWindowId(uiAutomation.getWindows());
            assertTrue(windowId >= 0);

            // Clear stats to be with a clean slate.
            assertTrue(uiAutomation.clearWindowContentFrameStats(windowId));

            // Get the frame stats.
            WindowContentFrameStats stats = uiAutomation.getWindowContentFrameStats(windowId);

            // Check the frame stats...

            // We should have somethong.
            assertNotNull(stats);

            // The refresh presiod is always positive.
            assertTrue(stats.getRefreshPeriodNano() > 0);

            // There is no data.
            assertTrue(stats.getFrameCount() == 0);

            // The start and end times are undefibed as we have no data.
            assertEquals(stats.getStartTimeNano(), FrameStats.UNDEFINED_TIME_NANO);
            assertEquals(stats.getEndTimeNano(), FrameStats.UNDEFINED_TIME_NANO);
        } finally {
            // Clean up.
            if (activity != null) {
                activity.finish();
            }
        }
    }

    public void testWindowAnimationFrameStats() throws Exception {
        Activity firstActivity = null;
        Activity secondActivity = null;
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

            // Start the frist activity.
            Intent firstIntent = new Intent(getInstrumentation().getContext(),
                    UiAutomationTestFirstActivity.class);
            firstIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            firstActivity = getInstrumentation().startActivitySync(firstIntent);

            // Wait for things to settle.
            getUiDevice().waitForIdle();

            // Clear the window animation stats to be with a clean slate.
            uiAutomation.clearWindowAnimationFrameStats();

            // Start the second activity
            Intent secondIntent = new Intent(getInstrumentation().getContext(),
                    UiAutomationTestSecondActivity.class);
            secondIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            secondActivity = getInstrumentation().startActivitySync(secondIntent);

            // Wait for things to settle.
            getUiDevice().waitForIdle();

            // Get the frame stats.
            WindowAnimationFrameStats stats = uiAutomation.getWindowAnimationFrameStats();

            // Check the frame stats...

            // We should have somethong.
            assertNotNull(stats);

            // The refresh presiod is always positive.
            assertTrue(stats.getRefreshPeriodNano() > 0);

            // There is some frame data.
            final int frameCount = stats.getFrameCount();
            assertTrue(frameCount > 0);

            // The frames are ordered in ascending order.
            assertWindowAnimationTimestampsInAscendingOrder(stats);

            // The start and end times are based on first and last frame.
            assertEquals(stats.getStartTimeNano(), stats.getFramePresentedTimeNano(0));
            assertEquals(stats.getEndTimeNano(), stats.getFramePresentedTimeNano(frameCount - 1));
        } finally {
            // Clean up.
            if (firstActivity != null) {
                firstActivity.finish();
            }
            if (secondActivity != null) {
                secondActivity.finish();
            }
        }
    }

    public void testWindowAnimationFrameStatsNoAnimation() throws Exception {
        UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

        // Wait for things to settle.
        getUiDevice().waitForIdle();

        // Clear the window animation stats to be with a clean slate.
        uiAutomation.clearWindowAnimationFrameStats();

        // Get the frame stats.
        WindowAnimationFrameStats stats = uiAutomation.getWindowAnimationFrameStats();

        // Check the frame stats...

        // We should have somethong.
        assertNotNull(stats);

        // The refresh presiod is always positive.
        assertTrue(stats.getRefreshPeriodNano() > 0);

        // There is no data.
        assertTrue(stats.getFrameCount() == 0);

        // The start and end times are undefibed as we have no data.
        assertEquals(stats.getStartTimeNano(), FrameStats.UNDEFINED_TIME_NANO);
        assertEquals(stats.getEndTimeNano(), FrameStats.UNDEFINED_TIME_NANO);
    }

    private void assertWindowContentTimestampsInAscendingOrder(WindowContentFrameStats stats) {
        long lastExpectedTimeNano = 0;
        long lastPresentedTimeNano = 0;
        long lastPreparedTimeNano = 0;

        final int frameCount = stats.getFrameCount();
        for (int i = 0; i < frameCount; i++) {
            final long expectedTimeNano = stats.getFramePostedTimeNano(i);
            assertTrue(expectedTimeNano > lastExpectedTimeNano);
            lastExpectedTimeNano = expectedTimeNano;

            final long presentedTimeNano = stats.getFramePresentedTimeNano(i);
            if (lastPresentedTimeNano == FrameStats.UNDEFINED_TIME_NANO) {
                assertTrue(presentedTimeNano == FrameStats.UNDEFINED_TIME_NANO);
            } else if (presentedTimeNano != FrameStats.UNDEFINED_TIME_NANO) {
                assertTrue(presentedTimeNano > lastPresentedTimeNano);
            }
            lastPresentedTimeNano = presentedTimeNano;

            final long preparedTimeNano = stats.getFrameReadyTimeNano(i);
            if (lastPreparedTimeNano == FrameStats.UNDEFINED_TIME_NANO) {
                assertTrue(preparedTimeNano == FrameStats.UNDEFINED_TIME_NANO);
            } else if (preparedTimeNano != FrameStats.UNDEFINED_TIME_NANO) {
                assertTrue(preparedTimeNano > lastPreparedTimeNano);
            }
            lastPreparedTimeNano = preparedTimeNano;
        }
    }

    private void assertWindowAnimationTimestampsInAscendingOrder(WindowAnimationFrameStats stats) {
        long lastPresentedTimeNano = 0;

        final int frameCount = stats.getFrameCount();
        for (int i = 0; i < frameCount; i++) {
            final long presentedTimeNano = stats.getFramePresentedTimeNano(i);
            if (lastPresentedTimeNano == FrameStats.UNDEFINED_TIME_NANO) {
                assertTrue(presentedTimeNano == FrameStats.UNDEFINED_TIME_NANO);
            } else if (presentedTimeNano != FrameStats.UNDEFINED_TIME_NANO) {
                assertTrue(presentedTimeNano >= lastPresentedTimeNano);
            }
            lastPresentedTimeNano = presentedTimeNano;
        }
    }

    private int findAppWindowId(List<AccessibilityWindowInfo> windows) {
        final int windowCount = windows.size();
        for (int i = 0; i < windowCount; i++) {
            AccessibilityWindowInfo window = windows.get(i);
            if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                return window.getId();
            }
        }
        return -1;
    }
}
