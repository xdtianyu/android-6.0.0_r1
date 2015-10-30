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
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
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

import java.security.InvalidParameterException;
import java.util.ArrayList;

/**
 * Adapter class which creates actions.
 */
class SettingsLayoutAdapter extends RecyclerView.Adapter {
    private static final String TAG = "SettingsLayoutAdapter";
    private static final boolean DEBUG = false;
    private static final DecelerateInterpolator ALPHA_DECEL = new DecelerateInterpolator(2F);
    private final Handler mRefreshViewHandler = new Handler(Looper.getMainLooper());

    /**
     * Object listening for adapter events.
     */
    public interface Listener {

        /**
         * Called when the user clicks on an action.
         */
        void onRowClicked(Layout.LayoutRow item);
    }

    public interface OnFocusListener {

        /**
         * Called when the user focuses on an action.
         */
        void onActionFocused(Layout.LayoutRow item);
    }

    private final ActionOnKeyPressAnimator mActionOnKeyPressAnimator;
    private final ActionOnFocusAnimator mActionOnFocusAnimator;
    private LayoutInflater mInflater;
    private ArrayList<Layout.LayoutRow> mLayoutRows;
    private Listener mListener;
    private boolean mNoAnimateMode = false;
    private boolean mFocusListenerEnabled = true;

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v != null && v.getWindowToken() != null && mListener != null) {
                mListener.onRowClicked(((LayoutRowViewHolder) v.getTag(R.id.action_title)).
                        getLayoutRow());
            }
        }
    };

    public SettingsLayoutAdapter(Listener listener, OnFocusListener onFocusListener) {
        super();
        mListener = listener;
        mActionOnKeyPressAnimator = new ActionOnKeyPressAnimator(listener);
        mActionOnFocusAnimator = new ActionOnFocusAnimator(onFocusListener);
    }

    public void setLayoutRows(ArrayList<Layout.LayoutRow> layoutRows) {
        mLayoutRows = layoutRows;
    }

    public void setNoAnimateMode() {
        mNoAnimateMode = true;
    }

    public void setFocusListenerEnabled(boolean enabled) {
        mFocusListenerEnabled = enabled;
    }

    @Override
    public int getItemViewType(int position) {
        return mLayoutRows.get(position).getViewType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = (LayoutInflater) parent.getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }
        final View v;
        switch (viewType) {
            case Layout.LayoutRow.VIEW_TYPE_ACTION:
                v = mInflater.inflate(R.layout.lb_dialog_action_list_item, parent, false);
                break;
            case Layout.LayoutRow.VIEW_TYPE_STATIC:
                v = mInflater.inflate(R.layout.lb_dialog_static_list_item, parent, false);
                break;
            case Layout.LayoutRow.VIEW_TYPE_WALLOFTEXT:
                v = mInflater.inflate(R.layout.lb_dialog_walloftext_list_item, parent, false);
                break;
            default:
                throw new IllegalStateException("View type not found: " + viewType);
        }
        v.setTag(R.layout.lb_dialog_action_list_item, parent);
        LayoutRowViewHolder viewHolder = new LayoutRowViewHolder(v, mActionOnKeyPressAnimator,
                mActionOnFocusAnimator, mOnClickListener);
        viewHolder.init(viewType);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position) {
        LayoutRowViewHolder holder = (LayoutRowViewHolder) baseHolder;
        if (position < mLayoutRows.size()) {
            holder.bind(mLayoutRows.get(position), position);
        }
    }

    @Override
    public int getItemCount() {
        return mLayoutRows.size();
    }

    public void setListener(Listener listener) {
        mListener = listener;
        mActionOnKeyPressAnimator.setListener(listener);
    }

    public void setOnFocusListener(OnFocusListener onFocusListener) {
        mActionOnFocusAnimator.setOnFocusListener(onFocusListener);
    }

    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
    }

    private class LayoutRowViewHolder extends ViewHolder implements
            Layout.ContentNodeRefreshListener {

        private class RefreshDescription implements Runnable {
            public String mDescriptionText;

            @Override
            public void run() {
                mDescription.setText(mDescriptionText);
            }
        }

        private final ActionOnKeyPressAnimator mActionOnKeyPressAnimator;
        private final ActionOnFocusAnimator mActionOnFocusAnimator;
        private final View.OnClickListener mViewOnClickListener;
        private Layout.LayoutRow mLayoutRow;
        private TextView mDescription = null;
        private TextView mTitle;
        private ImageView mCheckmarkView;
        private ImageView mIndicatorView;
        private View mContent;
        private ImageView mChevronView;
        private View mHairlineView;
        private int mViewType;
        private RefreshDescription mRefreshDescription;

        public LayoutRowViewHolder(View v, ActionOnKeyPressAnimator actionOnKeyPressAnimator,
                ActionOnFocusAnimator actionOnFocusAnimator,
                View.OnClickListener viewOnClickListener) {
            super(v);
            mActionOnKeyPressAnimator = actionOnKeyPressAnimator;
            mActionOnFocusAnimator = actionOnFocusAnimator;
            mViewOnClickListener = viewOnClickListener;
        }

        public Layout.LayoutRow getLayoutRow() {
            return mLayoutRow;
        }

        public void init(int viewType) {
            mViewType = viewType;
            mTitle = (TextView) itemView.findViewById(R.id.action_title);
            if (mViewType == Layout.LayoutRow.VIEW_TYPE_ACTION) {
                mDescription = (TextView) itemView.findViewById(R.id.action_description);
            }
            mCheckmarkView = (ImageView) itemView.findViewById(R.id.action_checkmark);
            mIndicatorView = (ImageView) itemView.findViewById(R.id.action_icon);
            mContent = itemView.findViewById(R.id.action_content);
            mChevronView = (ImageView) itemView.findViewById(R.id.action_next_chevron);
            mHairlineView = itemView.findViewById(R.id.static_hairline);
            itemView.setTag(R.id.action_title, this);
            itemView.setOnKeyListener(mActionOnKeyPressAnimator);
            itemView.setOnClickListener(mViewOnClickListener);
            itemView.setOnFocusChangeListener(mActionOnFocusAnimator);
        }

        //TODO need to create separate xxxViewHolder classes to eliminate tests of "mViewType".
        public void onRefreshView() {
            if (mViewType == Layout.LayoutRow.VIEW_TYPE_ACTION) {
                Layout.StringGetter description = mLayoutRow.getDescription();
                if (description != null) {
                    String text = description.get();
                    if (!TextUtils.equals(mRefreshDescription.mDescriptionText, text)) {
                        mRefreshDescription.mDescriptionText = text;
                        mRefreshViewHandler.removeCallbacks(mRefreshDescription);
                        mRefreshViewHandler.post(mRefreshDescription);
                    }
                }
            }
        }

        public void bind(Layout.LayoutRow layoutRow, int position) {
            mLayoutRow = layoutRow;
            if (mViewType != layoutRow.getViewType()) {
                throw new InvalidParameterException("view type does not match");
            }

            if (mViewType == Layout.LayoutRow.VIEW_TYPE_ACTION) {
                Layout.StringGetter description = layoutRow.getDescription();
                if (description != null) {
                    mRefreshDescription = new RefreshDescription();
                    String text = description.get();
                    mDescription.setText(text);
                    mRefreshDescription.mDescriptionText = text;
                    mDescription.setVisibility(View.VISIBLE);
                    description.setListener(this);
                } else {
                    mDescription.setVisibility(View.GONE);
                }
            } else if (mViewType == Layout.LayoutRow.VIEW_TYPE_STATIC && mHairlineView != null) {
                mHairlineView.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }

            mTitle.setText(layoutRow.getTitle());
            if (mCheckmarkView != null) {
                mCheckmarkView.setVisibility(layoutRow.isChecked() ? View.VISIBLE : View.INVISIBLE);
            }
            layoutRow.getChecked().setListener(this);

            if (mContent != null) {
                ViewGroup.LayoutParams contentLp = mContent.getLayoutParams();
                if (mIndicatorView != null && setIndicator(mIndicatorView, layoutRow)) {
                    contentLp.width = itemView.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.lb_action_text_width);
                } else {
                    contentLp.width = itemView.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.lb_action_text_width_no_icon);
                }
                mContent.setLayoutParams(contentLp);
            }

            if (mChevronView != null) {
                mChevronView.setVisibility(layoutRow.hasNext() ? View.VISIBLE : View.INVISIBLE);
            }

            mActionOnFocusAnimator.unFocus(itemView);
        }

        private boolean setIndicator(final ImageView indicatorView, Layout.LayoutRow action) {
            Drawable indicator = action.getIcon();
            if (indicator != null) {
                indicatorView.setImageDrawable(indicator);
                indicatorView.setVisibility(View.VISIBLE);
            } else {
                Uri iconUri = action.getIconUri();
                if (iconUri != null) {
                    indicatorView.setVisibility(View.INVISIBLE);
                } else {
                    indicatorView.setVisibility(View.GONE);
                    return false;
                }
            }
            return true;
        }

        private void fadeIn(View v) {
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f);
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
            final float verticalPadding =
                res.getDimension(R.dimen.lb_dialog_list_item_vertical_padding);
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

    private class ActionOnFocusAnimator implements View.OnFocusChangeListener {

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
        private OnFocusListener mOnFocusListener;
        private View mSelectedView;

        ActionOnFocusAnimator(OnFocusListener onFocusListener) {
            mOnFocusListener = onFocusListener;
        }

        public void setOnFocusListener(OnFocusListener onFocusListener) {
            mOnFocusListener = onFocusListener;
        }

        public void unFocus(View v) {
            changeFocus((v != null) ? v : mSelectedView, false, false);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                mSelectedView = v;
                if (mNoAnimateMode) {
                    mNoAnimateMode = false;
                    changeFocus(v, true /* hasFocus */, false /* shouldAnimate */);
                } else {
                    changeFocus(v, true /* hasFocus */, true /* shouldAnimate */);
                    if (mOnFocusListener != null && mFocusListenerEnabled) {
                        // We still call onActionFocused so that listeners can clear state if they
                        // want.
                        mOnFocusListener.onActionFocused(
                                ((LayoutRowViewHolder) v.getTag(R.id.action_title)).getLayoutRow());
                    }
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
                        getFloat(res, R.string.lb_dialog_list_item_unselected_text_alpha);

                mSelectedTitleAlpha =
                        getFloat(res, R.string.lb_dialog_list_item_selected_title_text_alpha);
                mDisabledTitleAlpha =
                        getFloat(res, R.string.lb_dialog_list_item_disabled_title_text_alpha);

                mSelectedDescriptionAlpha =
                        getFloat(res, R.string.lb_dialog_list_item_selected_description_text_alpha);
                mUnselectedDescriptionAlpha = getFloat(res,
                        R.string.lb_dialog_list_item_unselected_description_text_alpha);
                mDisabledDescriptionAlpha =
                        getFloat(res, R.string.lb_dialog_list_item_disabled_description_text_alpha);

                mSelectedChevronAlpha = getFloat(res,
                        R.string.lb_dialog_list_item_selected_chevron_background_alpha);
                mDisabledChevronAlpha = getFloat(res,
                        R.string.lb_dialog_list_item_disabled_chevron_background_alpha);
            }

            Layout.LayoutRow layoutRow =
                ((LayoutRowViewHolder) v.getTag(R.id.action_title)).getLayoutRow();

            float titleAlpha;
            if (layoutRow.isEnabled() && !layoutRow.infoOnly()) {
                titleAlpha = hasFocus ? mSelectedTitleAlpha : mUnselectedAlpha;
            } else {
                titleAlpha = mDisabledTitleAlpha;
            }
            float descriptionAlpha;
            if (!hasFocus || layoutRow.infoOnly()) {
                descriptionAlpha = mUnselectedDescriptionAlpha;
            } else {
                descriptionAlpha = layoutRow.isEnabled() ? mSelectedDescriptionAlpha :
                        mDisabledDescriptionAlpha;
            }
            float chevronAlpha;
            if (layoutRow.hasNext() && !layoutRow.infoOnly()) {
                chevronAlpha =
                        layoutRow.isEnabled() ? mSelectedChevronAlpha : mDisabledChevronAlpha;
            } else {
                chevronAlpha = 0;
            }

            final View title = v.findViewById(R.id.action_title);
            if (title != null) {
                setAlpha(title, shouldAnimate, titleAlpha);
            }

            final View description = v.findViewById(R.id.action_description);
            if (description != null) {
                setAlpha(description, shouldAnimate, descriptionAlpha);
            }

            final View checkmark = v.findViewById(R.id.action_checkmark);
            if (checkmark != null) {
                setAlpha(checkmark, shouldAnimate, titleAlpha);
            }

            final View icon = v.findViewById(R.id.action_icon);
            if (icon != null) {
                setAlpha(icon, shouldAnimate, titleAlpha);
            }

            final View chevron = v.findViewById(R.id.action_next_chevron);
            if (chevron != null) {
                setAlpha(chevron, shouldAnimate, chevronAlpha);
            }
        }

        private void setAlpha(View view, boolean shouldAnimate, float alpha) {
            if (shouldAnimate) {
                view.animate().alpha(alpha)
                        .setDuration(mAnimationDuration)
                        .setInterpolator(ALPHA_DECEL)
                        .start();
            } else {
                view.setAlpha(alpha);
            }
        }
    }

    private class ActionOnKeyPressAnimator implements View.OnKeyListener {

        private static final int SELECT_ANIM_DURATION = 100;
        private static final int SELECT_ANIM_DELAY = 0;
        private static final float SELECT_ANIM_SELECTED_ALPHA = 0.2f;
        private static final float SELECT_ANIM_UNSELECTED_ALPHA = 1.0f;
        private static final float CHECKMARK_ANIM_UNSELECTED_ALPHA = 0.0f;
        private static final float CHECKMARK_ANIM_SELECTED_ALPHA = 1.0f;

        private boolean mKeyPressed = false;
        private Listener mListener;

        public ActionOnKeyPressAnimator(Listener listener) {
            mListener = listener;
        }

        public void setListener(Listener listener) {
            mListener = listener;
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
            Layout.LayoutRow layoutRow =
                ((LayoutRowViewHolder) v.getTag(R.id.action_title)).getLayoutRow();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_BUTTON_X:
                case KeyEvent.KEYCODE_BUTTON_Y:
                case KeyEvent.KEYCODE_ENTER:

                    if (!layoutRow.isEnabled() || layoutRow.infoOnly()) {
                        if (v.isSoundEffectsEnabled()
                                && event.getAction() == KeyEvent.ACTION_DOWN) {
                            playSound(v.getContext(), AudioManager.FX_KEYPRESS_INVALID);
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

        private void playSound(Context context, int soundEffect) {
            AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            manager.playSoundEffect(soundEffect);
        }

        private void prepareAndAnimateView(final View v, float initAlpha, float destAlpha,
                int duration,
                int delay, Interpolator interpolator, final boolean pressed) {
            if (v != null && v.getWindowToken() != null) {
                final Layout.LayoutRow layoutRow =
                        ((LayoutRowViewHolder) v.getTag(R.id.action_title)).getLayoutRow();

                if (!pressed) {
                    fadeCheckmarks(v, layoutRow, duration, delay, interpolator);
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
                                mListener.onRowClicked(layoutRow);
                            }
                        }
                    }
                });
                v.animate().start();
            }
        }

        private void fadeCheckmarks(final View v, final Layout.LayoutRow action, int duration,
                int delay, Interpolator interpolator) {
            int actionCheckSetId = action.getCheckSetId();
            if (actionCheckSetId != Layout.LayoutRow.NO_CHECK_SET) {
                ViewGroup parent = (ViewGroup) v.getTag(R.layout.lb_dialog_action_list_item);

                // Find any actions that are checked and are in the same group as the selected
                // action. Fade their checkmarks out.
                for (int i = 0, size = mLayoutRows.size(); i < size; i++) {
                    Layout.LayoutRow a = mLayoutRows.get(i);
                    if (a != action && a.getCheckSetId() == actionCheckSetId && a.isChecked()) {
                        a.setChecked(false);
                        View viewToAnimateOut = parent.getChildAt(i);
                        if (viewToAnimateOut != null) {
                            final View checkView = viewToAnimateOut.findViewById(
                                    R.id.action_checkmark);
                            if (checkView != null) {
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
                }

                // If we we'ren't already checked, fade our checkmark in.
                if (!action.isChecked()) {
                    action.setChecked(true);
                    final View checkView = v.findViewById(R.id.action_checkmark);
                    if (checkView != null) {
                        checkView.setVisibility(View.VISIBLE);
                        checkView.setAlpha(CHECKMARK_ANIM_UNSELECTED_ALPHA);
                        checkView.animate().alpha(CHECKMARK_ANIM_SELECTED_ALPHA)
                                .setDuration(duration)
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

    private static float getFloat(Resources res, int floatResId) {
        TypedValue tv = new TypedValue();
        res.getValue(floatResId, tv, true);
        return tv.getFloat();
    }
}
