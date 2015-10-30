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
package android.transition.cts;

import com.android.cts.transition.R;

import android.transition.ChangeScroll;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;

public class ChangeScrollTest extends BaseTransitionTest {

    public ChangeScrollTest() {
    }

    public void testChangeScroll() throws Throwable {
        enterScene(R.layout.scene5);
        final Transition transition = new ChangeScroll();
        transition.setDuration(100);
        SimpleTransitionListener listener = new SimpleTransitionListener();
        transition.addListener(listener);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = mActivity.findViewById(R.id.text);
                final int scrollX = view.getScrollX();
                final int scrollY = view.getScrollY();
                assertEquals(0, scrollX);
                assertEquals(0, scrollY);
                TransitionManager.beginDelayedTransition(mSceneRoot, transition);
                view.scrollTo(150, 300);
            }
        });
        waitForStart(listener);
        waitForAnimationFrame();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = mActivity.findViewById(R.id.text);
                final int scrollX = view.getScrollX();
                final int scrollY = view.getScrollY();
                assertTrue(scrollX > 0);
                assertTrue(scrollX < 150);
                assertTrue(scrollY > 0);
                assertTrue(scrollY < 300);
            }
        });
        waitForEnd(listener, 100);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = mActivity.findViewById(R.id.text);
                final int scrollX = view.getScrollX();
                final int scrollY = view.getScrollY();
                assertEquals(150, scrollX);
                assertEquals(300, scrollY);
            }
        });
    }
}

