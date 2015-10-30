/*
 * Copyright (C) 2012 The Android Open Source Project
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
package android.animation.cts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.test.ActivityInstrumentationTestCase2;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class AnimatorSetTest extends
        ActivityInstrumentationTestCase2<AnimationActivity> {
    private AnimationActivity mActivity;
    private AnimatorSet mAnimatorSet;
    private long mDuration = 1000;
    private Object object;
    private ObjectAnimator yAnimator;
    private ObjectAnimator xAnimator;
    Set<Integer> identityHashes = new HashSet<Integer>();

    public AnimatorSetTest() {
        super(AnimationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
        object = mActivity.view.newBall;
        yAnimator = getYAnimator(object);
        xAnimator = getXAnimator(object);
    }

     public void testPlaySequentially() throws Throwable {
         Animator[] animatorArray = {xAnimator, yAnimator};

         mAnimatorSet = new AnimatorSet();
         mAnimatorSet.playSequentially(animatorArray);

         assertFalse(mAnimatorSet.isRunning());
         startAnimation(mAnimatorSet);
         Thread.sleep(100);
         assertTrue(mAnimatorSet.isRunning());
    }

    public void testPlayTogether() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = {xAnimator, yAnimator};

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);

        assertFalse(mAnimatorSet.isRunning());
        startAnimation(mAnimatorSet);
        Thread.sleep(100);
        assertTrue(mAnimatorSet.isRunning());
   }

    public void testDuration() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = { xAnimator, yAnimator };

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setDuration(1000);

        startAnimation(mAnimatorSet);
        Thread.sleep(100);
        assertEquals(mAnimatorSet.getDuration(), 1000);
    }

    public void testStartDelay() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = { xAnimator, yAnimator };

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setStartDelay(10);

        startAnimation(mAnimatorSet);
        Thread.sleep(100);
        assertEquals(mAnimatorSet.getStartDelay(), 10);
    }

    public void testgetChildAnimations() throws Throwable {
        Animator[] animatorArray = { xAnimator, yAnimator };

        mAnimatorSet = new AnimatorSet();
        ArrayList<Animator> childAnimations = mAnimatorSet.getChildAnimations();
        assertEquals(0, mAnimatorSet.getChildAnimations().size());
        mAnimatorSet.playSequentially(animatorArray);
        assertEquals(2, mAnimatorSet.getChildAnimations().size());
    }

    public void testSetInterpolator() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = {xAnimator, yAnimator};
        TimeInterpolator interpolator = new AccelerateDecelerateInterpolator();
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setInterpolator(interpolator);

        assertFalse(mAnimatorSet.isRunning());
        startAnimation(mAnimatorSet);
        Thread.sleep(100);

        ArrayList<Animator> animatorList = mAnimatorSet.getChildAnimations();
        assertEquals(interpolator, ((ObjectAnimator)animatorList.get(0)).getInterpolator());
        assertEquals(interpolator, ((ObjectAnimator)animatorList.get(1)).getInterpolator());
    }

    public ObjectAnimator getXAnimator(Object object) {
        String propertyX = "x";
        float startX = mActivity.mStartX;
        float endX = mActivity.mStartX + mActivity.mDeltaX;
        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(object, propertyX, startX, endX);
        xAnimator.setDuration(mDuration);
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        xAnimator.setInterpolator(new AccelerateInterpolator());
        xAnimator.setRepeatMode(ValueAnimator.REVERSE);
        return xAnimator;
    }

    public ObjectAnimator getYAnimator(Object object) {
         String property = "y";
         float startY = mActivity.mStartY;
         float endY = mActivity.mStartY + mActivity.mDeltaY;
         ObjectAnimator yAnimator = ObjectAnimator.ofFloat(object, property, startY, endY);
         yAnimator.setDuration(mDuration);
         yAnimator.setRepeatCount(2);
         yAnimator.setInterpolator(new AccelerateInterpolator());
         yAnimator.setRepeatMode(ValueAnimator.REVERSE);
        return yAnimator;
    }

    private void startAnimation(final AnimatorSet animatorSet) throws Throwable {
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.startAnimatorSet(animatorSet);
            }
        });
    }

    private void assertUnique(Object object) {
        assertUnique(object, "");
    }

    private void assertUnique(Object object, String msg) {
        final int code = System.identityHashCode(object);
        assertTrue("object should be unique " + msg + ", obj:" + object, identityHashes.add(code));

    }

    public void testClone() throws Throwable {
        final AnimatorSet set1 = new AnimatorSet();
        final AnimatorListenerAdapter setListener = new AnimatorListenerAdapter() {};
        set1.addListener(setListener);
        ObjectAnimator animator1 = new ObjectAnimator();
        animator1.setDuration(100);
        animator1.setPropertyName("x");
        animator1.setIntValues(5);
        animator1.setInterpolator(new LinearInterpolator());
        AnimatorListenerAdapter listener1 = new AnimatorListenerAdapter(){};
        AnimatorListenerAdapter listener2 = new AnimatorListenerAdapter(){};
        animator1.addListener(listener1);

        ObjectAnimator animator2 = new ObjectAnimator();
        animator2.setDuration(100);
        animator2.setInterpolator(new LinearInterpolator());
        animator2.addListener(listener2);
        animator2.setPropertyName("y");
        animator2.setIntValues(10);

        set1.playTogether(animator1, animator2);

        AnimateObject target = new AnimateObject();
        set1.setTarget(target);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                set1.start();
            }
        });
        assertTrue(set1.isStarted());

        animator1.getListeners();
        AnimatorSet set2 = set1.clone();
        assertFalse(set2.isStarted());

        assertUnique(set1);
        assertUnique(animator1);
        assertUnique(animator2);

        assertUnique(set2);
        assertEquals(2, set2.getChildAnimations().size());

        Animator clone1 = set2.getChildAnimations().get(0);
        Animator clone2 = set2.getChildAnimations().get(1);

        for (Animator animator : set2.getChildAnimations()) {
            assertUnique(animator);
        }

        assertTrue(clone1.getListeners().contains(listener1));
        assertTrue(clone2.getListeners().contains(listener2));

        assertTrue(set2.getListeners().contains(setListener));

        for (Animator.AnimatorListener listener : set1.getListeners()) {
            assertTrue(set2.getListeners().contains(listener));
        }

        assertEquals(animator1.getDuration(), clone1.getDuration());
        assertEquals(animator2.getDuration(), clone2.getDuration());
        assertSame(animator1.getInterpolator(), clone1.getInterpolator());
        assertSame(animator2.getInterpolator(), clone2.getInterpolator());
    }

    class AnimateObject {
        int x = 1;
        int y = 2;
    }
}
