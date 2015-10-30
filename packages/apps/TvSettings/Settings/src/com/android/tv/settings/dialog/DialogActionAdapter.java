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

package com.android.tv.settings.dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.DialogFragment.Action;
import com.android.tv.settings.widget.BitmapWorkerOptions;
import com.android.tv.settings.widget.DrawableDownloader;
import com.android.tv.settings.widget.DrawableDownloader.BitmapCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class which creates actions.
 *
 * @hide
 */
class DialogActionAdapter extends RecyclerView.Adapter {
    private static final String TAG = "ActionAdapter";
    private static final boolean DEBUG = false;

    private final ActionOnKeyPressAnimator mActionOnKeyPressAnimator;
    private final ActionOnFocusAnimator mActionOnFocusAnimator;
    private LayoutInflater mInflater;
    private final List<Action> mActions;
    private Action.Listener mListener;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v != null && v.getWindowToken() != null && mListener != null) {
                mListener.onActionClicked(((ActionViewHolder) v.getTag(R.id.action_title)).getAction());
            }
        }
    };

    public DialogActionAdapter(Action.Listener listener, Action.OnFocusListener onFocusListener,
            List<Action> actions) {
        super();
        mListener = listener;
        mActions = new ArrayList<Action>(actions);
        mActionOnKeyPressAnimator = new ActionOnKeyPressAnimator(listener, mActions);
        mActionOnFocusAnimator = new ActionOnFocusAnimator(onFocusListener);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = (LayoutInflater) parent.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }
        View v = mInflater.inflate(R.layout.lb_dialog_action_list_item, parent, false);
        v.setTag(R.layout.lb_dialog_action_list_item, parent);
        return new ActionViewHolder(v, mActionOnKeyPressAnimator, mActionOnFocusAnimator, mOnClickListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position) {
        ActionViewHolder holder = (ActionViewHolder) baseHolder;

        if (position >= mActions.size()) {
            return;
        }

        holder.init(mActions.get(position));
    }

    @Override
    public int getItemCount() {
        return mActions.size();
    }

    public int getCount() {
        return mActions.size();
    }

    public Action getItem(int position) {
        return mActions.get(position);
    }

    public void setListener(Action.Listener listener) {
        mListener = listener;
        mActionOnKeyPressAnimator.setListener(listener);
    }

    public void setOnFocusListener(Action.OnFocusListener onFocusListener) {
        mActionOnFocusAnimator.setOnFocusListener(onFocusListener);
    }

    /**
     * Used for serialization only.
     */
    public ArrayList<Action> getActions() {
        return new ArrayList<Action>(mActions);
    }

    public void setActions(ArrayList<Action> actions) {
        mActionOnFocusAnimator.unFocus(null);
        mActions.clear();
        mActions.addAll(actions);
        notifyDataSetChanged();
    }

    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
    }

    private static class ActionViewHolder extends ViewHolder {

        private final ActionOnKeyPressAnimator mActionOnKeyPressAnimator;
        private final ActionOnFocusAnimator mActionOnFocusAnimator;
        private final View.OnClickListener mViewOnClickListener;
        private Action mAction;

        private BitmapCallback mPendingBitmapCallback;

        public ActionViewHolder(View v, ActionOnKeyPressAnimator actionOnKeyPressAnimator,
                ActionOnFocusAnimator actionOnFocusAnimator,
                View.OnClickListener viewOnClickListener) {
            super(v);
            mActionOnKeyPressAnimator = actionOnKeyPressAnimator;
            mActionOnFocusAnimator = actionOnFocusAnimator;
            mViewOnClickListener = viewOnClickListener;
        }

        public Action getAction() {
            return mAction;
        }

        public void init(Action action) {
            mAction = action;

            if (mPendingBitmapCallback != null) {
                DrawableDownloader.getInstance(
                        itemView.getContext()).cancelDownload(mPendingBitmapCallback);
                mPendingBitmapCallback = null;
            }
            TextView title = (TextView) itemView.findViewById(R.id.action_title);
            TextView description = (TextView) itemView.findViewById(R.id.action_description);
            description.setText(action.getDescription());
            description.setVisibility(
                    TextUtils.isEmpty(action.getDescription()) ? View.GONE : View.VISIBLE);
            title.setText(action.getTitle());
            ImageView checkmarkView = (ImageView) itemView.findViewById(R.id.action_checkmark);
            checkmarkView.setVisibility(action.isChecked() ? View.VISIBLE : View.INVISIBLE);

            ImageView indicatorView = (ImageView) itemView.findViewById(R.id.action_icon);
            View content = itemView.findViewById(R.id.action_content);
            ViewGroup.LayoutParams contentLp = content.getLayoutParams();
            if (setIndicator(indicatorView, action)) {
                contentLp.width = itemView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.lb_action_text_width);
            } else {
                contentLp.width = itemView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.lb_action_text_width_no_icon);
            }
            content.setLayoutParams(contentLp);

            ImageView chevronView = (ImageView) itemView.findViewById(R.id.action_next_chevron);
            chevronView.setVisibility(action.hasNext() ? View.VISIBLE : View.INVISIBLE);

            final Resources res = itemView.getContext().getResources();
            if (action.hasMultilineDescription()) {
                title.setMaxLines(res.getInteger(R.integer.lb_dialog_action_title_max_lines));
                description.setMaxHeight(
                        getDescriptionMaxHeight(itemView.getContext(), title));
            } else {
                title.setMaxLines(res.getInteger(R.integer.lb_dialog_action_title_min_lines));
                description.setMaxLines(
                        res.getInteger(R.integer.lb_dialog_action_description_min_lines));
            }

            itemView.setTag(R.id.action_title, this);
            itemView.setOnKeyListener(mActionOnKeyPressAnimator);
            itemView.setOnClickListener(mViewOnClickListener);
            itemView.setOnFocusChangeListener(mActionOnFocusAnimator);
            mActionOnFocusAnimator.unFocus(itemView);
        }

        private boolean setIndicator(final ImageView indicatorView, Action action) {

            Context context = indicatorView.getContext();
            Drawable indicator = action.getIndicator(context);
            if (indicator != null) {
                indicatorView.setImageDrawable(indicator);
                indicatorView.setVisibility(View.VISIBLE);
            } else {
                Uri iconUri = action.getIconUri();
                if (iconUri != null) {
                    indicatorView.setVisibility(View.INVISIBLE);

                    mPendingBitmapCallback = new BitmapCallback() {
                        @Override
                        public void onBitmapRetrieved(Drawable bitmap) {
                            if (bitmap != null) {
                                indicatorView.setVisibility(View.VISIBLE);
                                indicatorView.setImageDrawable(bitmap);
                                fadeIn(indicatorView);
                            }
                            mPendingBitmapCallback = null;
                        }
                    };

                    DrawableDownloader.getInstance(context).getBitmap(
                            new BitmapWorkerOptions.Builder(
                                    context).resource(iconUri)
                                    .width(indicatorView.getLayoutParams().width).build(),
                            mPendingBitmapCallback);

                } else {
                    indicatorView.setVisibility(View.GONE);
                    return false;
                }
            }
            return true;
        }

        private void fadeIn(View v) {
            v.setAlpha(0f);
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(v,
                    "alpha", 1f);
            alphaAnimator.setDuration(
                    v.getContext().getResources().getInteger(
                            android.R.integer.config_mediumAnimTime));
            alphaAnimator.start();
        }

        /**
         * @return the max height in pixels the description can be such that the
         *         action nicely takes up the entire screen.
         */
        private int getDescriptionMaxHeight(Context context, TextView title) {
            final Resources res = context.getResources();
            final float verticalPadding = res.getDimension(R.dimen.lb_dialog_list_item_vertical_padding);
            final int titleMaxLines = res.getInteger(R.integer.lb_dialog_action_title_max_lines);
            final int displayHeight = ((WindowManager) context.getSystemService(
                    Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();

            // The 2 multiplier on the title height calculation is a
            // conservative estimate for font padding which can not be
            // calculated at this stage since the view hasn't been rendered yet.
            return (int) (displayHeight -
                    2 * verticalPadding - 2 * titleMaxLines * title.getLineHeight());
        }

    }

    private static class ActionOnFocusAnimator implements View.OnFocusChangeListener {

        private boolean mResourcesSet;
        private float mUnselectedAlpha;
        private float mSelectedTitleAlpha;
        private float mDisabledTitleAlpha;
        private float mSelectedDescriptionAlpha;
        private float mDisabledDescriptionAlpha;
        private float mUnselectedDescriptionAlpha;
        private float mSelectedChevronAlpha;
        private float mDisabledChevronAlpha;
        private int mAnimationDuration;
        private Action.OnFocusListener mOnFocusListener;
        private View mSelectedView;

        ActionOnFocusAnimator(Action.OnFocusListener onFocusListener) {
            mOnFocusListener = onFocusListener;
        }

        public void setOnFocusListener(Action.OnFocusListener onFocusListener) {
            mOnFocusListener = onFocusListener;
        }

        public void unFocus(View v) {
            changeFocus((v != null) ? v : mSelectedView, false, false);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                mSelectedView = v;
                changeFocus(v, true /* hasFocus */, true /* shouldAnimate */);
                if (mOnFocusListener != null) {
                    // We still call onActionFocused so that listeners can clear
                    // state if they want.
                    mOnFocusListener.onActionFocused(
                            ((ActionViewHolder) v.getTag(R.id.action_title)).getAction());
                }
            } else {
                if (mSelectedView == v) {
                    mSelectedView = null;
                }
                changeFocus(v, false /* hasFocus */, true /* shouldAnimate */);
            }
        }

        private void changeFocus(View v, boolean hasFocus, boolean shouldAnimate) {
            if (v == null) {
                return;
            }

            if (!mResourcesSet) {
                mResourcesSet = true;
                final Resources res = v.getContext().getResources();

                mAnimationDuration = res.getInteger(R.integer.lb_dialog_animation_duration);
                mUnselectedAlpha =
                        Float.valueOf(res.getString(R.string.lb_dialog_list_item_unselected_text_alpha));

                mSelectedTitleAlpha =
                        Float.valueOf(res.getString(R.string.lb_dialog_list_item_selected_title_text_alpha));
                mDisabledTitleAlpha =
                        Float.valueOf(res.getString(R.string.lb_dialog_list_item_disabled_title_text_alpha));

                mSelectedDescriptionAlpha =
                        Float.valueOf(
                                res.getString(R.string.lb_dialog_list_item_selected_description_text_alpha));
                mUnselectedDescriptionAlpha =
                        Float.valueOf(
                                res.getString(R.string.lb_dialog_list_item_unselected_description_text_alpha));
                mDisabledDescriptionAlpha =
                        Float.valueOf(
                                res.getString(R.string.lb_dialog_list_item_disabled_description_text_alpha));

                mSelectedChevronAlpha =
                        Float.valueOf(
                                res.getString(R.string.lb_dialog_list_item_selected_chevron_background_alpha));
                mDisabledChevronAlpha =
                        Float.valueOf(
                                res.getString(R.string.lb_dialog_list_item_disabled_chevron_background_alpha));
            }

            Action action = ((ActionViewHolder) v.getTag(R.id.action_title)).getAction();

            float titleAlpha = action.isEnabled() && !action.infoOnly()
                    ? (hasFocus ? mSelectedTitleAlpha : mUnselectedAlpha) : mDisabledTitleAlpha;
            float descriptionAlpha = (!hasFocus || action.infoOnly()) ? mUnselectedDescriptionAlpha
                    : (action.isEnabled() ? mSelectedDescriptionAlpha : mDisabledDescriptionAlpha);
            float chevronAlpha = action.hasNext() && !action.infoOnly()
                    ? (action.isEnabled() ? mSelectedChevronAlpha : mDisabledChevronAlpha) : 0;

            TextView title = (TextView) v.findViewById(R.id.action_title);
            setAlpha(title, shouldAnimate, titleAlpha);

            TextView description = (TextView) v.findViewById(R.id.action_description);
            setAlpha(description, shouldAnimate, descriptionAlpha);

            ImageView checkmark = (ImageView) v.findViewById(R.id.action_checkmark);
            setAlpha(checkmark, shouldAnimate, titleAlpha);

            ImageView icon = (ImageView) v.findViewById(R.id.action_icon);
            setAlpha(icon, shouldAnimate, titleAlpha);

            ImageView chevron = (ImageView) v.findViewById(R.id.action_next_chevron);
            setAlpha(chevron, shouldAnimate, chevronAlpha);
        }

        private void setAlpha(View view, boolean shouldAnimate, float alpha) {
            if (shouldAnimate) {
                view.animate().alpha(alpha)
                        .setDuration(mAnimationDuration)
                        .setInterpolator(new DecelerateInterpolator(2F))
                        .start();
            } else {
                view.setAlpha(alpha);
            }
        }
    }

    private static class ActionOnKeyPressAnimator implements View.OnKeyListener {

        private static final int SELECT_ANIM_DURATION = 100;
        private static final int SELECT_ANIM_DELAY = 0;
        private static final float SELECT_ANIM_SELECTED_ALPHA = 0.2f;
        private static final float SELECT_ANIM_UNSELECTED_ALPHA = 1.0f;
        private static final float CHECKMARK_ANIM_UNSELECTED_ALPHA = 0.0f;
        private static final float CHECKMARK_ANIM_SELECTED_ALPHA = 1.0f;

        private final List<Action> mActions;
        private boolean mKeyPressed = false;
        private Action.Listener mListener;

        public ActionOnKeyPressAnimator(Action.Listener listener,
                List<Action> actions) {
            mListener = listener;
            mActions = actions;
        }

        public void setListener(Action.Listener listener) {
            mListener = listener;
        }

        private void playSound(Context context, int soundEffect) {
            AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            manager.playSoundEffect(soundEffect);
        }

        /**
         * Now only handles KEYCODE_ENTER and KEYCODE_NUMPAD_ENTER key event.
         */
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (v == null) {
                return false;
            }
            boolean handled = false;
            Action action = ((ActionViewHolder) v.getTag(R.id.action_title)).getAction();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_BUTTON_X:
                case KeyEvent.KEYCODE_BUTTON_Y:
                case KeyEvent.KEYCODE_ENTER:

                    if (!action.isEnabled() || action.infoOnly()) {
                        if (v.isSoundEffectsEnabled()
                                && event.getAction() == KeyEvent.ACTION_DOWN) {
                            // TODO: requires API 19
                            //playSound(v.getContext(), AudioManager.FX_KEYPRESS_INVALID);
                        }
                        return true;
                    }

                    switch (event.getAction()) {
                        case KeyEvent.ACTION_DOWN:
                            if (!mKeyPressed) {
                                mKeyPressed = true;

                                if (v.isSoundEffectsEnabled()) {
                                    playSound(v.getContext(), AudioManager.FX_KEY_CLICK);
                                }

                                if (DEBUG) {
                                    Log.d(TAG, "Enter Key down");
                                }

                                prepareAndAnimateView(v, SELECT_ANIM_UNSELECTED_ALPHA,
                                        SELECT_ANIM_SELECTED_ALPHA, SELECT_ANIM_DURATION,
                                        SELECT_ANIM_DELAY, null, mKeyPressed);
                                handled = true;
                            }
                            break;
                        case KeyEvent.ACTION_UP:
                            if (mKeyPressed) {
                                mKeyPressed = false;

                                if (DEBUG) {
                                    Log.d(TAG, "Enter Key up");
                                }

                                prepareAndAnimateView(v, SELECT_ANIM_SELECTED_ALPHA,
                                        SELECT_ANIM_UNSELECTED_ALPHA, SELECT_ANIM_DURATION,
                                        SELECT_ANIM_DELAY, null, mKeyPressed);
                                handled = true;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
            return handled;
        }

        private void prepareAndAnimateView(final View v, float initAlpha, float destAlpha,
                int duration,
                int delay, Interpolator interpolator, final boolean pressed) {
            if (v != null && v.getWindowToken() != null) {
                final Action action = ((ActionViewHolder) v.getTag(R.id.action_title)).getAction();

                if (!pressed) {
                    fadeCheckmarks(v, action, duration, delay, interpolator);
                }

                v.setAlpha(initAlpha);
                v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                v.buildLayer();
                v.animate().alpha(destAlpha).setDuration(duration).setStartDelay(delay);
                if (interpolator != null) {
                    v.animate().setInterpolator(interpolator);
                }
                v.animate().setListener(new AnimatorListenerAdapter() {
                        @Override
                    public void onAnimationEnd(Animator animation) {

                        v.setLayerType(View.LAYER_TYPE_NONE, null);
                        if (!pressed) {
                            if (mListener != null) {
                                mListener.onActionClicked(action);
                            }
                        }
                    }
                });
                v.animate().start();
            }
        }

        private void fadeCheckmarks(final View v, final Action action, int duration, int delay,
                Interpolator interpolator) {
            int actionCheckSetId = action.getCheckSetId();
            if (actionCheckSetId != Action.NO_CHECK_SET) {
                ViewGroup parent = (ViewGroup) v.getTag(R.layout.lb_dialog_action_list_item);
                // Find any actions that are checked and are in the same group
                // as the selected action. Fade their checkmarks out.
                for (int i = 0, size = mActions.size(); i < size; i++) {
                    Action a = mActions.get(i);
                    if (a != action && a.getCheckSetId() == actionCheckSetId && a.isChecked()) {
                        a.setChecked(false);
                        View viewToAnimateOut = parent.getChildAt(i);
                        if (viewToAnimateOut != null) {
                            final View checkView = viewToAnimateOut.findViewById(
                                    R.id.action_checkmark);
                            checkView.animate().alpha(CHECKMARK_ANIM_UNSELECTED_ALPHA)
                                    .setDuration(duration).setStartDelay(delay);
                            if (interpolator != null) {
                                checkView.animate().setInterpolator(interpolator);
                            }
                            checkView.animate().setListener(new AnimatorListenerAdapter() {
                                    @Override
                                public void onAnimationEnd(Animator animation) {
                                    checkView.setVisibility(View.INVISIBLE);
                                }
                            });
                        }
                    }
                }

                // If we we'ren't already checked, fade our checkmark in.
                if (!action.isChecked()) {
                    action.setChecked(true);
                    final View checkView = v.findViewById(R.id.action_checkmark);
                    checkView.setVisibility(View.VISIBLE);
                    checkView.setAlpha(CHECKMARK_ANIM_UNSELECTED_ALPHA);
                    checkView.animate().alpha(CHECKMARK_ANIM_SELECTED_ALPHA).setDuration(duration)
                            .setStartDelay(delay);
                    if (interpolator != null) {
                        checkView.animate().setInterpolator(interpolator);
                    }
                    checkView.animate().setListener(null);
                }
            }
        }
    }
}
