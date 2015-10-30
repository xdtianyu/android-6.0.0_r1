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

import android.transition.Transition;
import android.transition.Transition.TransitionListener;

/**
 * Listener captures whether each of the methods is called.
 */
class SimpleTransitionListener implements TransitionListener {
    public Transition transition;

    public boolean started;

    public boolean ended;

    public boolean canceled;

    public boolean paused;

    public boolean resumed;

    @Override
    public synchronized void onTransitionStart(Transition transition) {
        started = true;
        this.transition = transition;
        notifyAll();
    }

    @Override
    public synchronized void onTransitionEnd(Transition transition) {
        ended = true;
        notifyAll();
    }

    @Override
    public synchronized void onTransitionCancel(Transition transition) {
        canceled = true;
        notifyAll();
    }

    @Override
    public synchronized void onTransitionPause(Transition transition) {
        paused = true;
        notifyAll();
    }

    @Override
    public synchronized void onTransitionResume(Transition transition) {
        resumed = true;
        notifyAll();
    }
}
