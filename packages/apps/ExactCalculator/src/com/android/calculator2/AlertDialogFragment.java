/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.TextView;

public class AlertDialogFragment extends DialogFragment {

    private static final String NAME = AlertDialogFragment.class.getName();
    private static final String KEY_MESSAGE = NAME + "_message";
    private static final String KEY_BUTTON_NEGATIVE = NAME + "_button_negative";

    public static void showMessageDialog(Activity activity, CharSequence message) {
        final Bundle args = new Bundle();
        args.putCharSequence(KEY_MESSAGE, message);
        args.putCharSequence(KEY_BUTTON_NEGATIVE, activity.getString(R.string.dismiss));

        final AlertDialogFragment dialogFragment = new AlertDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.show(activity.getFragmentManager(), null /* tag */);
    }

    public AlertDialogFragment() {
        setStyle(STYLE_NO_TITLE, android.R.attr.alertDialogTheme);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        final Context context = getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final TextView textView = (TextView) inflater.inflate(R.layout.dialog_message,
                null /* root */);
        textView.setText(args.getCharSequence(KEY_MESSAGE));
        return new AlertDialog.Builder(context)
                .setView(textView)
                .setNegativeButton(args.getCharSequence(KEY_BUTTON_NEGATIVE), null /* listener */)
                .create();
    }
}
