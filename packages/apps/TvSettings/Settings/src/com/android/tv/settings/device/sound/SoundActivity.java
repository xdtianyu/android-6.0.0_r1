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

package com.android.tv.settings.device.sound;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.DialogFragment;
import com.android.tv.settings.dialog.DialogFragment.Action;

import java.util.ArrayList;

/**
 * Activity that allows the enabling and disabling of sound effects.
 */
public class SoundActivity extends Activity implements Action.Listener {

    private static final String PREFERENCE_KEY = "sound_effects";
    private static final String ACTION_SOUND_ON = "sound_on";
    private static final String ACTION_SOUND_OFF = "sound_off";
    private AudioManager mAudioManager;
    private DialogFragment mDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mDialogFragment = new DialogFragment.Builder()
                .title(getString(R.string.device_sound_effects))
                .breadcrumb(getString(R.string.header_category_device))
                .iconResourceId(getIconResource(getContentResolver()))
                .iconBackgroundColor(getResources().getColor(R.color.icon_background))
                .actions(getActions()).build();
        DialogFragment.add(getFragmentManager(), mDialogFragment);
    }

    private ArrayList<Action> getActions() {
        boolean soundEffectsAreOn = getSoundEffectsEnabled(getContentResolver());
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action.Builder()
                .key(ACTION_SOUND_ON)
                .title(getString(R.string.settings_on))
                .checked(soundEffectsAreOn)
                .checkSetId(1)
                .build());
        actions.add(new Action.Builder()
                .key(ACTION_SOUND_OFF)
                .title(getString(R.string.settings_off))
                .checked(!soundEffectsAreOn)
                .checkSetId(1)
                .build());
        return actions;
    }

    @Override
    public void onActionClicked(Action action) {
        if (ACTION_SOUND_ON.equals(action.getKey())) {
            mAudioManager.loadSoundEffects();
            setSoundEffectsEnabled(1);
        } else if (ACTION_SOUND_OFF.equals(action.getKey())) {
            mAudioManager.unloadSoundEffects();
            setSoundEffectsEnabled(0);
        }
        mDialogFragment.setIcon(getIconResource(getContentResolver()));
    }

    public static String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    public static int getIconResource(ContentResolver contentResolver) {
        return getSoundEffectsEnabled(contentResolver) ? R.drawable.settings_sound_on_icon
                : R.drawable.settings_sound_off_icon;
    }

    private void setSoundEffectsEnabled(int value) {
        Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, value);
    }

    private static boolean getSoundEffectsEnabled(ContentResolver contentResolver) {
        return Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1)
                != 0;
    }
}
