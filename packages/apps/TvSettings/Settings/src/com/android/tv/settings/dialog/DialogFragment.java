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
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tv.settings.R;
import com.android.tv.settings.util.AccessibilityHelper;
import com.android.tv.settings.widget.BitmapWorkerOptions;
import com.android.tv.settings.widget.DrawableDownloader;
import com.android.tv.settings.widget.DrawableDownloader.BitmapCallback;

import java.util.ArrayList;

/**
 * Displays content on the left and actions on the right.
 */
public class DialogFragment extends Fragment {

    private static final String TAG_LEAN_BACK_DIALOG_FRAGMENT = "leanBackDialogFragment";
    private static final String EXTRA_CONTENT_TITLE = "title";
    private static final String EXTRA_CONTENT_BREADCRUMB = "breadcrumb";
    private static final String EXTRA_CONTENT_DESCRIPTION = "description";
    private static final String EXTRA_CONTENT_ICON_RESOURCE_ID = "iconResourceId";
    private static final String EXTRA_CONTENT_ICON_URI = "iconUri";
    private static final String EXTRA_CONTENT_ICON_BITMAP = "iconBitmap";
    private static final String EXTRA_CONTENT_ICON_BACKGROUND = "iconBackground";
    private static final String EXTRA_ACTION_NAME = "name";
    private static final String EXTRA_ACTION_ACTIONS = "actions";
    private static final String EXTRA_ACTION_SELECTED_INDEX = "selectedIndex";
    private static final String EXTRA_ENTRY_TRANSITION_PERFORMED = "entryTransitionPerformed";
    private static final int ANIMATE_IN_DURATION = 250;
    private static final int ANIMATE_DELAY = 550;
    private static final int SECONDARY_ANIMATE_DELAY = 120;
    private static final int SLIDE_IN_DISTANCE = 120;
    private static final int ANIMATION_FRAGMENT_ENTER = 1;
    private static final int ANIMATION_FRAGMENT_EXIT = 2;
    private static final int ANIMATION_FRAGMENT_ENTER_POP = 3;
    private static final int ANIMATION_FRAGMENT_EXIT_POP = 4;

    /**
     * Builds a LeanBackDialogFragment object.
     */
    public static class Builder {

        private String mContentTitle;
        private String mContentBreadcrumb;
        private String mContentDescription;
        private int mIconResourceId;
        private Uri mIconUri;
        private Bitmap mIconBitmap;
        private int mIconBackgroundColor = Color.TRANSPARENT;
        private ArrayList<Action> mActions;
        private String mName;
        private int mSelectedIndex;

        public DialogFragment build() {
            DialogFragment fragment = new DialogFragment();
            Bundle args = new Bundle();
            args.putString(EXTRA_CONTENT_TITLE, mContentTitle);
            args.putString(EXTRA_CONTENT_BREADCRUMB, mContentBreadcrumb);
            args.putString(EXTRA_CONTENT_DESCRIPTION, mContentDescription);
            args.putInt(EXTRA_CONTENT_ICON_RESOURCE_ID, mIconResourceId);
            args.putParcelable(EXTRA_CONTENT_ICON_URI, mIconUri);
            args.putParcelable(EXTRA_CONTENT_ICON_BITMAP, mIconBitmap);
            args.putInt(EXTRA_CONTENT_ICON_BACKGROUND, mIconBackgroundColor);
            args.putParcelableArrayList(EXTRA_ACTION_ACTIONS, mActions);
            args.putString(EXTRA_ACTION_NAME, mName);
            args.putInt(EXTRA_ACTION_SELECTED_INDEX, mSelectedIndex);
            fragment.setArguments(args);
            return fragment;
        }

        public Builder title(String title) {
            mContentTitle = title;
            return this;
        }

        public Builder breadcrumb(String breadcrumb) {
            mContentBreadcrumb = breadcrumb;
            return this;
        }

        public Builder description(String description) {
            mContentDescription = description;
            return this;
        }

        public Builder iconResourceId(int iconResourceId) {
            mIconResourceId = iconResourceId;
            return this;
        }

        public Builder iconUri(Uri iconUri) {
            mIconUri = iconUri;
            return this;
        }

        public Builder iconBitmap(Bitmap iconBitmap) {
            mIconBitmap = iconBitmap;
            return this;
        }

        public Builder iconBackgroundColor(int iconBackgroundColor) {
            mIconBackgroundColor = iconBackgroundColor;
            return this;
        }

        public Builder actions(ArrayList<Action> actions) {
            mActions = actions;
            return this;
        }

        public Builder name(String name) {
            mName = name;
            return this;
        }

        public Builder selectedIndex(int selectedIndex) {
            mSelectedIndex = selectedIndex;
            return this;
        }
    }

    public static void add(FragmentManager fm, DialogFragment f) {
        boolean hasDialog = fm.findFragmentByTag(TAG_LEAN_BACK_DIALOG_FRAGMENT) != null;
        FragmentTransaction ft = fm.beginTransaction();

        if (hasDialog) {
            ft.setCustomAnimations(ANIMATION_FRAGMENT_ENTER,
                    ANIMATION_FRAGMENT_EXIT, ANIMATION_FRAGMENT_ENTER_POP,
                    ANIMATION_FRAGMENT_EXIT_POP);
            ft.addToBackStack(null);
        }
        ft.replace(android.R.id.content, f, TAG_LEAN_BACK_DIALOG_FRAGMENT).commit();
    }

    private DialogActionAdapter mAdapter;
    private SelectorAnimator mSelectorAnimator;
    private VerticalGridView mListView;
    private Action.Listener mListener;
    private String mTitle;
    private String mBreadcrumb;
    private String mDescription;
    private int mIconResourceId;
    private Uri mIconUri;
    private Bitmap mIconBitmap;
    private int mIconBackgroundColor = Color.TRANSPARENT;
    private ArrayList<Action> mActions;
    private String mName;
    private int mSelectedIndex = -1;
    private boolean mEntryTransitionPerformed;
    private boolean mIntroAnimationInProgress;
    private BitmapCallback mBitmapCallBack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        android.util.Log.v("DialogFragment", "onCreate");
        super.onCreate(savedInstanceState);
        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        if (mTitle == null) {
            mTitle = state.getString(EXTRA_CONTENT_TITLE);
        }
        if (mBreadcrumb == null) {
            mBreadcrumb = state.getString(EXTRA_CONTENT_BREADCRUMB);
        }
        if (mDescription == null) {
            mDescription = state.getString(EXTRA_CONTENT_DESCRIPTION);
        }
        if (mIconResourceId == 0) {
            mIconResourceId = state.getInt(EXTRA_CONTENT_ICON_RESOURCE_ID, 0);
        }
        if (mIconUri == null) {
            mIconUri = state.getParcelable(EXTRA_CONTENT_ICON_URI);
        }
        if (mIconBitmap == null) {
            mIconBitmap = state.getParcelable(EXTRA_CONTENT_ICON_BITMAP);
        }
        if (mIconBackgroundColor == Color.TRANSPARENT) {
            mIconBackgroundColor = state.getInt(EXTRA_CONTENT_ICON_BACKGROUND, Color.TRANSPARENT);
        }
        if (mActions == null) {
            mActions = state.getParcelableArrayList(EXTRA_ACTION_ACTIONS);
        }
        if (mName == null) {
            mName = state.getString(EXTRA_ACTION_NAME);
        }
        if (mSelectedIndex == -1) {
            mSelectedIndex = state.getInt(EXTRA_ACTION_SELECTED_INDEX, -1);
        }
        mEntryTransitionPerformed = state.getBoolean(EXTRA_ENTRY_TRANSITION_PERFORMED, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.lb_dialog_fragment, container, false);

        View contentContainer = v.findViewById(R.id.content_fragment);
        View content = inflater.inflate(R.layout.lb_dialog_content, container, false);
        ((ViewGroup) contentContainer).addView(content);
        setContentView(content);
        v.setTag(R.id.content_fragment, content);

        View actionContainer = v.findViewById(R.id.action_fragment);
        View action = inflater.inflate(R.layout.lb_dialog_action_list, container, false);
        ((ViewGroup) actionContainer).addView(action);
        setActionView(action);
        v.setTag(R.id.action_fragment, action);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CONTENT_TITLE, mTitle);
        outState.putString(EXTRA_CONTENT_BREADCRUMB, mBreadcrumb);
        outState.putString(EXTRA_CONTENT_DESCRIPTION, mDescription);
        outState.putInt(EXTRA_CONTENT_ICON_RESOURCE_ID, mIconResourceId);
        outState.putParcelable(EXTRA_CONTENT_ICON_URI, mIconUri);
        outState.putParcelable(EXTRA_CONTENT_ICON_BITMAP, mIconBitmap);
        outState.putInt(EXTRA_CONTENT_ICON_BACKGROUND, mIconBackgroundColor);
        outState.putParcelableArrayList(EXTRA_ACTION_ACTIONS, mActions);
        outState.putInt(EXTRA_ACTION_SELECTED_INDEX,
                (mListView != null) ? getSelectedItemPosition() : mSelectedIndex);
        outState.putString(EXTRA_ACTION_NAME, mName);
        outState.putBoolean(EXTRA_ENTRY_TRANSITION_PERFORMED, mEntryTransitionPerformed);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mEntryTransitionPerformed) {
            mEntryTransitionPerformed = true;
            performEntryTransition();
        } else {
            performSelectorTransition();
        }
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        View dialogView = getView();
        View contentView = (View) dialogView.getTag(R.id.content_fragment);
        View actionView = (View) dialogView.getTag(R.id.action_fragment);
        View actionContainerView = dialogView.findViewById(R.id.action_fragment);
        View titleView = (View) contentView.getTag(R.id.title);
        View breadcrumbView = (View) contentView.getTag(R.id.breadcrumb);
        View descriptionView = (View) contentView.getTag(R.id.description);
        View iconView = (View) contentView.getTag(R.id.icon);
        View listView = (View) actionView.getTag(R.id.list);
        View selectorView = (View) actionView.getTag(R.id.selector);

        ArrayList<Animator> animators = new ArrayList<>();

        switch (nextAnim) {
            case ANIMATION_FRAGMENT_ENTER:
                animators.add(createSlideInFromEndAnimator(titleView));
                animators.add(createSlideInFromEndAnimator(breadcrumbView));
                animators.add(createSlideInFromEndAnimator(descriptionView));
                animators.add(createSlideInFromEndAnimator(iconView));
                animators.add(createSlideInFromEndAnimator(listView));
                animators.add(createSlideInFromEndAnimator(selectorView));
                break;
            case ANIMATION_FRAGMENT_EXIT:
                animators.add(createSlideOutToStartAnimator(titleView));
                animators.add(createSlideOutToStartAnimator(breadcrumbView));
                animators.add(createSlideOutToStartAnimator(descriptionView));
                animators.add(createSlideOutToStartAnimator(iconView));
                animators.add(createSlideOutToStartAnimator(listView));
                animators.add(createSlideOutToStartAnimator(selectorView));
                animators.add(createFadeOutAnimator(actionContainerView));
                break;
            case ANIMATION_FRAGMENT_ENTER_POP:
                animators.add(createSlideInFromStartAnimator(titleView));
                animators.add(createSlideInFromStartAnimator(breadcrumbView));
                animators.add(createSlideInFromStartAnimator(descriptionView));
                animators.add(createSlideInFromStartAnimator(iconView));
                animators.add(createSlideInFromStartAnimator(listView));
                animators.add(createSlideInFromStartAnimator(selectorView));
                break;
            case ANIMATION_FRAGMENT_EXIT_POP:
                animators.add(createSlideOutToEndAnimator(titleView));
                animators.add(createSlideOutToEndAnimator(breadcrumbView));
                animators.add(createSlideOutToEndAnimator(descriptionView));
                animators.add(createSlideOutToEndAnimator(iconView));
                animators.add(createSlideOutToEndAnimator(listView));
                animators.add(createSlideOutToEndAnimator(selectorView));
                animators.add(createFadeOutAnimator(actionContainerView));
                break;
            default:
                return super.onCreateAnimator(transit, enter, nextAnim);
        }

        mEntryTransitionPerformed = true;
        return createDummyAnimator(dialogView, animators);
    }

    public void setIcon(int iconResourceId) {
        mIconResourceId = iconResourceId;
        View v = getView();
        if (v != null) {
            final ImageView iconImageView = (ImageView) v.findViewById(R.id.icon);
            if (iconImageView != null) {
                if (iconResourceId != 0) {
                    iconImageView.setImageResource(iconResourceId);
                    iconImageView.setVisibility(View.VISIBLE);
                    updateViewSize(iconImageView);
                }
            }
        }
    }

    /**
     * Fragments need to call this method in its {@link #onResume()} to set the
     * custom listener. <br/>
     * Activities do not need to call this method
     *
     * @param listener
     */
    public void setListener(Action.Listener listener) {
        mListener = listener;
    }

    public boolean hasListener() {
        return mListener != null;
    }

    public ArrayList<Action> getActions() {
        return mActions;
    }

    public void setActions(ArrayList<Action> actions) {
        mActions = actions;
        if (mAdapter != null) {
            mAdapter.setActions(mActions);
        }
    }

    public View getItemView(int position) {
        return mListView.getChildAt(position);
    }

    public void setSelectedPosition(int position) {
        mListView.setSelectedPosition(position);
    }

    public int getSelectedItemPosition() {
        return mListView.indexOfChild(mListView.getFocusedChild());
    }

    /**
     * Called when intro animation is finished.
     * <p>
     * If a subclass is going to alter the view, should wait until this is
     * called.
     */
    public void onIntroAnimationFinished() {
        mIntroAnimationInProgress = false;
    }

    public boolean isIntroAnimationInProgress() {
        return mIntroAnimationInProgress;
    }

    private void setContentView(View content) {
        TextView titleView = (TextView) content.findViewById(R.id.title);
        TextView breadcrumbView = (TextView) content.findViewById(R.id.breadcrumb);
        TextView descriptionView = (TextView) content.findViewById(R.id.description);
        titleView.setText(mTitle);
        breadcrumbView.setText(mBreadcrumb);
        descriptionView.setText(mDescription);
        final ImageView iconImageView = (ImageView) content.findViewById(R.id.icon);
        if (mIconBackgroundColor != Color.TRANSPARENT) {
            iconImageView.setBackgroundColor(mIconBackgroundColor);
        }

        if (AccessibilityHelper.forceFocusableViews(getActivity())) {
            titleView.setFocusable(true);
            titleView.setFocusableInTouchMode(true);
            descriptionView.setFocusable(true);
            descriptionView.setFocusableInTouchMode(true);
            breadcrumbView.setFocusable(true);
            breadcrumbView.setFocusableInTouchMode(true);
        }

        if (mIconResourceId != 0) {
            iconImageView.setImageResource(mIconResourceId);
            updateViewSize(iconImageView);
        } else {
            if (mIconBitmap != null) {
                iconImageView.setImageBitmap(mIconBitmap);
                updateViewSize(iconImageView);
            } else {
                if (mIconUri != null) {
                    iconImageView.setVisibility(View.INVISIBLE);

                    DrawableDownloader bitmapDownloader = DrawableDownloader.getInstance(
                            content.getContext());
                    mBitmapCallBack = new BitmapCallback() {
                        @Override
                        public void onBitmapRetrieved(Drawable bitmap) {
                            if (bitmap != null) {
                                mIconBitmap = (bitmap instanceof BitmapDrawable) ? ((BitmapDrawable) bitmap)
                                        .getBitmap()
                                        : null;
                                iconImageView.setVisibility(View.VISIBLE);
                                iconImageView.setImageDrawable(bitmap);
                                updateViewSize(iconImageView);
                            }
                        }
                    };

                    bitmapDownloader.getBitmap(new BitmapWorkerOptions.Builder(
                            content.getContext()).resource(mIconUri)
                            .width(iconImageView.getLayoutParams().width).build(),
                            mBitmapCallBack);
                } else {
                    iconImageView.setVisibility(View.GONE);
                }
            }
        }

        content.setTag(R.id.title, titleView);
        content.setTag(R.id.breadcrumb, breadcrumbView);
        content.setTag(R.id.description, descriptionView);
        content.setTag(R.id.icon, iconImageView);
    }

    private void setActionView(View action) {
        mAdapter = new DialogActionAdapter(new Action.Listener() {
            @Override
            public void onActionClicked(Action action) {
                // eat events if action is disabled or only displays info
                if (!action.isEnabled() || action.infoOnly()) {
                    return;
                }

                /**
                 * If the custom lister has been set using
                 * {@link #setListener(DialogActionAdapter.Listener)}, use it.
                 * If not, use the activity's default listener.
                 */
                if (mListener != null) {
                    mListener.onActionClicked(action);
                } else if (getActivity() instanceof Action.Listener) {
                    Action.Listener listener = (Action.Listener) getActivity();
                    listener.onActionClicked(action);
                }
            }
        }, new Action.OnFocusListener() {
            @Override
            public void onActionFocused(Action action) {
                if (getActivity() instanceof Action.OnFocusListener) {
                    Action.OnFocusListener listener = (Action.OnFocusListener) getActivity();
                    listener.onActionFocused(action);
                }
            }
        }, mActions);

        if (action instanceof VerticalGridView) {
            mListView = (VerticalGridView) action;
        } else {
            mListView = (VerticalGridView) action.findViewById(R.id.list);
            if (mListView == null) {
                throw new IllegalStateException("No ListView exists.");
            }
//            mListView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
//            mListView.setWindowAlignmentOffsetPercent(0.5f);
//            mListView.setItemAlignmentOffset(0);
//            mListView.setItemAlignmentOffsetPercent(VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
            mListView.setWindowAlignmentOffset(0);
            mListView.setWindowAlignmentOffsetPercent(50f);
            mListView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
            View selectorView = action.findViewById(R.id.selector);
            if (selectorView != null) {
                mSelectorAnimator = new SelectorAnimator(selectorView, mListView);
                mListView.setOnScrollListener(mSelectorAnimator);
            }
        }

        mListView.requestFocusFromTouch();
        mListView.setAdapter(mAdapter);
        mListView.setSelectedPosition(
                (mSelectedIndex >= 0 && mSelectedIndex < mActions.size()) ? mSelectedIndex
                : getFirstCheckedAction());

        action.setTag(R.id.list, mListView);
        action.setTag(R.id.selector, action.findViewById(R.id.selector));
    }

    private int getFirstCheckedAction() {
        for (int i = 0, size = mActions.size(); i < size; i++) {
            if (mActions.get(i).isChecked()) {
                return i;
            }
        }
        return 0;
    }

    private void updateViewSize(ImageView iconView) {
        int intrinsicWidth = iconView.getDrawable().getIntrinsicWidth();
        LayoutParams lp = iconView.getLayoutParams();
        if (intrinsicWidth > 0) {
            lp.height = lp.width * iconView.getDrawable().getIntrinsicHeight()
                    / intrinsicWidth;
        } else {
            // If no intrinsic width, then just mke this a square.
            lp.height = lp.width;
        }
    }

    private void fadeIn(View v) {
        v.setAlpha(0f);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(v, "alpha", 1f);
        alphaAnimator.setDuration(v.getContext().getResources().getInteger(
                android.R.integer.config_mediumAnimTime));
        alphaAnimator.start();
    }

    private void runDelayedAnim(final Runnable runnable) {
        final View dialogView = getView();
        final View contentView = (View) dialogView.getTag(R.id.content_fragment);

        contentView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        contentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        // if we buildLayer() at this time, the texture is
                        // actually not created delay a little so we can make
                        // sure all hardware layer is created before animation,
                        // in that way we can avoid the jittering of start
                        // animation
                        contentView.postOnAnimationDelayed(runnable, ANIMATE_DELAY);
                    }
                });

    }

    private void performSelectorTransition() {
        runDelayedAnim(new Runnable() {
            @Override
            public void run() {
                // Fade in the selector.
                if (mSelectorAnimator != null) {
                    mSelectorAnimator.fadeIn();
                }
            }
        });
    }

    private void performEntryTransition() {
        final View dialogView = getView();
        final View contentView = (View) dialogView.getTag(R.id.content_fragment);
        final View actionContainerView = dialogView.findViewById(R.id.action_fragment);

        mIntroAnimationInProgress = true;

        // Fade out the old activity.
        getActivity().overridePendingTransition(0, R.anim.lb_dialog_fade_out);

        int bgColor = contentView.getContext().getResources()
                .getColor(R.color.lb_dialog_activity_background);
        final ColorDrawable bgDrawable = new ColorDrawable();
        bgDrawable.setColor(bgColor);
        bgDrawable.setAlpha(0);
        dialogView.setBackground(bgDrawable);
        dialogView.setVisibility(View.INVISIBLE);

        runDelayedAnim(new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) {
                    // We have been detached before this could run,
                    // so just bail
                    return;
                }

                dialogView.setVisibility(View.VISIBLE);

                // Fade in the activity background protection
                ObjectAnimator oa = ObjectAnimator.ofInt(bgDrawable, "alpha", 255);
                oa.setDuration(ANIMATE_IN_DURATION);
                oa.setStartDelay(SECONDARY_ANIMATE_DELAY);
                oa.setInterpolator(new DecelerateInterpolator(1.0f));
                oa.start();

                boolean isRtl = ViewCompat.getLayoutDirection(contentView) ==
                        ViewCompat.LAYOUT_DIRECTION_RTL;
                int startDist = isRtl ? SLIDE_IN_DISTANCE : -SLIDE_IN_DISTANCE;
                int endDist = isRtl ? -actionContainerView.getMeasuredWidth() :
                        actionContainerView.getMeasuredWidth();

                // Fade in and slide in the ContentFragment
                // TextViews from the start.
                prepareAndAnimateView((View) contentView.getTag(R.id.title),
                        startDist, false);
                prepareAndAnimateView((View) contentView.getTag(R.id.breadcrumb),
                        startDist, false);
                prepareAndAnimateView((View) contentView.getTag(R.id.description),
                        startDist, false);

                // Fade in and slide in the ActionFragment from the
                // end.
                prepareAndAnimateView(actionContainerView,
                        endDist, false);
                prepareAndAnimateView((View) contentView.getTag(R.id.icon),
                        startDist, true);

                // Fade in the selector.
                if (mSelectorAnimator != null) {
                    mSelectorAnimator.fadeIn();
                }
            }
        });
    }

    private void prepareAndAnimateView(final View v, float initTransX,
            final boolean notifyAnimationFinished) {
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        v.buildLayer();
        v.setAlpha(0);
        v.setTranslationX(initTransX);
        v.animate().alpha(1f).translationX(0).setDuration(ANIMATE_IN_DURATION)
                .setStartDelay(SECONDARY_ANIMATE_DELAY);
        v.animate().setInterpolator(new DecelerateInterpolator(1.0f));
        v.animate().setListener(new AnimatorListenerAdapter() {
                @Override
            public void onAnimationEnd(Animator animation) {
                v.setLayerType(View.LAYER_TYPE_NONE, null);
                if (notifyAnimationFinished) {
                    onIntroAnimationFinished();
                }
            }
        });
        v.animate().start();
    }

    private Animator createDummyAnimator(final View v, ArrayList<Animator> animators) {
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        return new UntargetableAnimatorSet(animatorSet);
    }

    private Animator createAnimator(View v, int resourceId) {
        Animator animator = AnimatorInflater.loadAnimator(v.getContext(), resourceId);
        animator.setTarget(v);
        return animator;
    }

    private Animator createSlideOutToStartAnimator(View v) {
        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
        return createTranslateAlphaAnimator(v, 0, isRtl ? 200f : -200f, 1f, 0);
    }

    private Animator createSlideInFromEndAnimator(View v) {
        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
        return createTranslateAlphaAnimator(v, isRtl ? -200f : 200f, 0, 0, 1f);
    }

    private Animator createSlideInFromStartAnimator(View v) {
        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
        return createTranslateAlphaAnimator(v, isRtl ? 200f : -200f, 0, 0, 1f);
    }

    private Animator createSlideOutToEndAnimator(View v) {
        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
        return createTranslateAlphaAnimator(v, 0, isRtl ? -200f : 200f, 1f, 0);
    }

    private Animator createFadeOutAnimator(View v) {
        return createAlphaAnimator(v, 1f, 0);
    }

    private Animator createTranslateAlphaAnimator(View v, float fromTranslateX, float toTranslateX,
            float fromAlpha, float toAlpha) {
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(v, "translationX", fromTranslateX,
                toTranslateX);
        translateAnimator.setDuration(
                getResources().getInteger(android.R.integer.config_longAnimTime));
        Animator alphaAnimator = createAlphaAnimator(v, fromAlpha, toAlpha);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(translateAnimator).with(alphaAnimator);
        return animatorSet;
    }

    private Animator createAlphaAnimator(View v, float fromAlpha, float toAlpha) {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(v, "alpha", fromAlpha, toAlpha);
        alphaAnimator.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        return alphaAnimator;
    }

    private static class SelectorAnimator extends RecyclerView.OnScrollListener {

        private final View mSelectorView;
        private final ViewGroup mParentView;
        private final int mAnimationDuration;
        private volatile boolean mFadedOut = true;

        SelectorAnimator(View selectorView, ViewGroup parentView) {
            mSelectorView = selectorView;
            mParentView = parentView;
            mAnimationDuration = selectorView.getContext()
                    .getResources().getInteger(R.integer.lb_dialog_animation_duration);
        }

        // We want to fade in the selector if we've stopped scrolling on it. If
        // we're scrolling, we want to ensure to dim the selector if we haven't
        // already. We dim the last highlighted view so that while a user is
        // scrolling, nothing is highlighted.
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                fadeIn();
            } else {
                fadeOut();
            }
        }

        public void fadeIn() {
            // The selector starts with a height of 0. In order to scale up
            // from
            // 0 we first need the set the height to 1 and scale form there.
            int selectorHeight = mSelectorView.getHeight();
            if (selectorHeight == 0) {
                LayoutParams lp = mSelectorView.getLayoutParams();
                lp.height = selectorHeight = mSelectorView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.lb_action_fragment_selector_min_height);
                mSelectorView.setLayoutParams(lp);
            }
            View focusedChild = mParentView.getFocusedChild();
            if (focusedChild != null) {
                float scaleY = (float) focusedChild.getHeight() / selectorHeight;
                ViewPropertyAnimator animation = mSelectorView.animate()
                        .alpha(1f)
                        .setListener(new Listener(false))
                        .setDuration(mAnimationDuration)
                        .setInterpolator(new DecelerateInterpolator(2f));
                if (mFadedOut) {
                    // selector is completely faded out, so we can just
                    // scale
                    // before fading in.
                    mSelectorView.setScaleY(scaleY);
                } else {
                    // selector is not faded out, so we must animate the
                    // scale
                    // as we fade in.
                    animation.scaleY(scaleY);
                }
                animation.start();
            }
        }

        public void fadeOut() {
            mSelectorView.animate()
            .alpha(0f)
            .setDuration(mAnimationDuration)
            .setInterpolator(new DecelerateInterpolator(2f))
            .setListener(new Listener(true))
            .start();
        }

        /**
         * Sets {@link BaseScrollAdapterFragment#mFadedOut}
         * {@link BaseScrollAdapterFragment#mFadedOut} is true, iff
         * {@link BaseScrollAdapterFragment#mSelectorView} has an alpha of 0
         * (faded out). If false the view either has an alpha of 1 (visible) or
         * is in the process of animating.
         */
        private class Listener implements Animator.AnimatorListener {
            private final boolean mFadingOut;
            private boolean mCanceled;

            public Listener(boolean fadingOut) {
                mFadingOut = fadingOut;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (!mFadingOut) {
                    mFadedOut = false;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled && mFadingOut) {
                    mFadedOut = true;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        }
    }

    private static class UntargetableAnimatorSet extends Animator {

        private final AnimatorSet mAnimatorSet;

        UntargetableAnimatorSet(AnimatorSet animatorSet) {
            mAnimatorSet = animatorSet;
        }

        @Override
        public void addListener(Animator.AnimatorListener listener) {
            mAnimatorSet.addListener(listener);
        }

        @Override
        public void cancel() {
            mAnimatorSet.cancel();
        }

        @Override
        public Animator clone() {
            return mAnimatorSet.clone();
        }

        @Override
        public void end() {
            mAnimatorSet.end();
        }

        @Override
        public long getDuration() {
            return mAnimatorSet.getDuration();
        }

        @Override
        public ArrayList<Animator.AnimatorListener> getListeners() {
            return mAnimatorSet.getListeners();
        }

        @Override
        public long getStartDelay() {
            return mAnimatorSet.getStartDelay();
        }

        @Override
        public boolean isRunning() {
            return mAnimatorSet.isRunning();
        }

        @Override
        public boolean isStarted() {
            return mAnimatorSet.isStarted();
        }

        @Override
        public void removeAllListeners() {
            mAnimatorSet.removeAllListeners();
        }

        @Override
        public void removeListener(Animator.AnimatorListener listener) {
            mAnimatorSet.removeListener(listener);
        }

        @Override
        public Animator setDuration(long duration) {
            return mAnimatorSet.setDuration(duration);
        }

        @Override
        public void setInterpolator(TimeInterpolator value) {
            mAnimatorSet.setInterpolator(value);
        }

        @Override
        public void setStartDelay(long startDelay) {
            mAnimatorSet.setStartDelay(startDelay);
        }

        @Override
        public void setTarget(Object target) {
            // ignore
        }

        @Override
        public void setupEndValues() {
            mAnimatorSet.setupEndValues();
        }

        @Override
        public void setupStartValues() {
            mAnimatorSet.setupStartValues();
        }

        @Override
        public void start() {
            mAnimatorSet.start();
        }
    }

    /**
     * An data class which represents an action within an
     * {@link DialogFragment}. A list of Actions represent a list of choices the
     * user can make, a radio-button list of configuration options, or just a
     * list of information.
     */
    public static class Action implements Parcelable {

        private static final String TAG = "Action";

        public static final int NO_DRAWABLE = 0;
        public static final int NO_CHECK_SET = 0;
        public static final int DEFAULT_CHECK_SET_ID = 1;

        /**
         * Object listening for adapter events.
         */
        public interface Listener {

            /**
             * Called when the user clicks on an action.
             */
            public void onActionClicked(Action action);
        }

        public interface OnFocusListener {

            /**
             * Called when the user focuses on an action.
             */
            public void onActionFocused(Action action);
        }

        /**
         * Builds a Action object.
         */
        public static class Builder {
            private String mKey;
            private String mTitle;
            private String mDescription;
            private Intent mIntent;
            private String mResourcePackageName;
            private int mDrawableResource = NO_DRAWABLE;
            private Uri mIconUri;
            private boolean mChecked;
            private boolean mMultilineDescription;
            private boolean mHasNext;
            private boolean mInfoOnly;
            private int mCheckSetId = NO_CHECK_SET;
            private boolean mEnabled = true;

            public Action build() {
                Action action = new Action();
                action.mKey = mKey;
                action.mTitle = mTitle;
                action.mDescription = mDescription;
                action.mIntent = mIntent;
                action.mResourcePackageName = mResourcePackageName;
                action.mDrawableResource = mDrawableResource;
                action.mIconUri = mIconUri;
                action.mChecked = mChecked;
                action.mMultilineDescription = mMultilineDescription;
                action.mHasNext = mHasNext;
                action.mInfoOnly = mInfoOnly;
                action.mCheckSetId = mCheckSetId;
                action.mEnabled = mEnabled;
                return action;
            }

            public Builder key(String key) {
                mKey = key;
                return this;
            }

            public Builder title(String title) {
                mTitle = title;
                return this;
            }

            public Builder description(String description) {
                mDescription = description;
                return this;
            }

            public Builder intent(Intent intent) {
                mIntent = intent;
                return this;
            }

            public Builder resourcePackageName(String resourcePackageName) {
                mResourcePackageName = resourcePackageName;
                return this;
            }

            public Builder drawableResource(int drawableResource) {
                mDrawableResource = drawableResource;
                return this;
            }

            public Builder iconUri(Uri iconUri) {
                mIconUri = iconUri;
                return this;
            }

            public Builder checked(boolean checked) {
                mChecked = checked;
                return this;
            }

            public Builder multilineDescription(boolean multilineDescription) {
                mMultilineDescription = multilineDescription;
                return this;
            }

            public Builder hasNext(boolean hasNext) {
                mHasNext = hasNext;
                return this;
            }

            public Builder infoOnly(boolean infoOnly) {
                mInfoOnly = infoOnly;
                return this;
            }

            public Builder checkSetId(int checkSetId) {
                mCheckSetId = checkSetId;
                return this;
            }

            public Builder enabled(boolean enabled) {
                mEnabled = enabled;
                return this;
            }
        }

        private String mKey;
        private String mTitle;
        private String mDescription;
        private Intent mIntent;

        /**
         * If not {@code null}, the package name to use to retrieve
         * {@link #mDrawableResource}.
         */
        private String mResourcePackageName;

        private int mDrawableResource;
        private Uri mIconUri;
        private boolean mChecked;
        private boolean mMultilineDescription;
        private boolean mHasNext;
        private boolean mInfoOnly;
        private int mCheckSetId;
        private boolean mEnabled;

        private Action() {
        }

        public String getKey() {
            return mKey;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getDescription() {
            return mDescription;
        }

        public void setDescription(String description) {
            mDescription = description;
        }

        public Intent getIntent() {
            return mIntent;
        }

        public boolean isChecked() {
            return mChecked;
        }

        public int getDrawableResource() {
            return mDrawableResource;
        }

        public Uri getIconUri() {
            return mIconUri;
        }

        public String getResourcePackageName() {
            return mResourcePackageName;
        }

        /**
         * Returns the check set id this action is a part of. All actions in the
         * same list with the same check set id are considered linked. When one
         * of the actions within that set is selected that action becomes
         * checked while all the other actions become unchecked.
         *
         * @return an integer representing the check set this action is a part
         *         of or {@link NO_CHECK_SET} if this action isn't a
         *         part of a check set.
         */
        public int getCheckSetId() {
            return mCheckSetId;
        }

        public boolean hasMultilineDescription() {
            return mMultilineDescription;
        }

        public boolean isEnabled() {
            return mEnabled;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
        }

        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        /**
         * @return true if the action will request further user input when
         *         selected (such as showing another dialog or launching a new
         *         activity). False, otherwise.
         */
        public boolean hasNext() {
            return mHasNext;
        }

        /**
         * @return true if the action will only display information and is thus
         *         unactionable. If both this and {@link #hasNext()} are true,
         *         infoOnly takes precedence. (default is false) e.g. the amount
         *         of storage a document uses or cost of an app.
         */
        public boolean infoOnly() {
            return mInfoOnly;
        }

        /**
         * Returns an indicator to be drawn. If null is returned, no space for
         * the indicator will be made.
         *
         * @param context the context of the Activity this Action belongs to
         * @return an indicator to draw or null if no indicator space should
         *         exist.
         */
        public Drawable getIndicator(Context context) {
            if (mDrawableResource == NO_DRAWABLE) {
                return null;
            }
            if (mResourcePackageName == null) {
                return context.getResources().getDrawable(mDrawableResource);
            }
            // If we get to here, need to load the resources.
            Drawable icon = null;
            try {
                Context packageContext = context.createPackageContext(mResourcePackageName, 0);
                icon = packageContext.getResources().getDrawable(mDrawableResource);
            } catch (PackageManager.NameNotFoundException e) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "No icon for this action.");
                }
            } catch (Resources.NotFoundException e) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "No icon for this action.");
                }
            }
            return icon;
        }

        public static Parcelable.Creator<Action> CREATOR = new Parcelable.Creator<Action>() {

                @Override
            public Action createFromParcel(Parcel source) {

                return new Action.Builder()
                        .key(source.readString())
                        .title(source.readString())
                        .description(source.readString())
                        .intent((Intent) source.readParcelable(Intent.class.getClassLoader()))
                        .resourcePackageName(source.readString())
                        .drawableResource(source.readInt())
                        .iconUri((Uri) source.readParcelable(Uri.class.getClassLoader()))
                        .checked(source.readInt() != 0)
                        .multilineDescription(source.readInt() != 0)
                        .checkSetId(source.readInt())
                        .build();
            }

                @Override
            public Action[] newArray(int size) {
                return new Action[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mKey);
            dest.writeString(mTitle);
            dest.writeString(mDescription);
            dest.writeParcelable(mIntent, flags);
            dest.writeString(mResourcePackageName);
            dest.writeInt(mDrawableResource);
            dest.writeParcelable(mIconUri, flags);
            dest.writeInt(mChecked ? 1 : 0);
            dest.writeInt(mMultilineDescription ? 1 : 0);
            dest.writeInt(mCheckSetId);
        }
    }
}
