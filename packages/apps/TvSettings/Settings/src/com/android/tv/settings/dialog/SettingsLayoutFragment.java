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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;

/**
 * Displays content on the left and actions on the right.
 */
public class SettingsLayoutFragment extends Fragment implements Layout.LayoutNodeRefreshListener {

    public static final String TAG_LEAN_BACK_DIALOG_FRAGMENT = "leanBackSettingsLayoutFragment";
    private static final String EXTRA_CONTENT_TITLE = "title";
    private static final String EXTRA_CONTENT_BREADCRUMB = "breadcrumb";
    private static final String EXTRA_CONTENT_DESCRIPTION = "description";
    private static final String EXTRA_CONTENT_ICON = "icon";
    private static final String EXTRA_CONTENT_ICON_URI = "iconUri";
    private static final String EXTRA_CONTENT_ICON_BITMAP = "iconBitmap";
    private static final String EXTRA_CONTENT_ICON_BACKGROUND = "iconBackground";
    private static final String EXTRA_ACTION_NAME = "name";
    private static final String EXTRA_ACTION_SELECTED_INDEX = "selectedIndex";
    private static final String EXTRA_ENTRY_TRANSITION_PERFORMED = "entryTransitionPerformed";
    private static final int ANIMATION_FRAGMENT_ENTER = 1;
    private static final int ANIMATION_FRAGMENT_EXIT = 2;
    private static final int ANIMATION_FRAGMENT_ENTER_POP = 3;
    private static final int ANIMATION_FRAGMENT_EXIT_POP = 4;
    private static final float WINDOW_ALIGNMENT_OFFSET_PERCENT = 50f;
    private static final float FADE_IN_ALPHA_START = 0f;
    private static final float FADE_IN_ALPHA_FINISH = 1f;
    private static final float SLIDE_OUT_ANIMATOR_LEFT = 0f;
    private static final float SLIDE_OUT_ANIMATOR_RIGHT = 200f;
    private static final float SLIDE_OUT_ANIMATOR_START_ALPHA = 0f;
    private static final float SLIDE_OUT_ANIMATOR_END_ALPHA = 1f;

    public interface Listener {
        void onActionClicked(Layout.Action action);
    }

    /**
     * Builds a SettingsLayoutFragment object.
     */
    public static class Builder {

        private String mContentTitle;
        private String mContentBreadcrumb;
        private String mContentDescription;
        private Drawable mIcon;
        private Uri mIconUri;
        private Bitmap mIconBitmap;
        private int mIconBackgroundColor = Color.TRANSPARENT;
        private String mName;

        public SettingsLayoutFragment build() {
            SettingsLayoutFragment fragment = new SettingsLayoutFragment();
            Bundle args = new Bundle();
            args.putString(EXTRA_CONTENT_TITLE, mContentTitle);
            args.putString(EXTRA_CONTENT_BREADCRUMB, mContentBreadcrumb);
            args.putString(EXTRA_CONTENT_DESCRIPTION, mContentDescription);
            //args.putParcelable(EXTRA_CONTENT_ICON, mIcon);
            fragment.mIcon = mIcon;
            args.putParcelable(EXTRA_CONTENT_ICON_URI, mIconUri);
            args.putParcelable(EXTRA_CONTENT_ICON_BITMAP, mIconBitmap);
            args.putInt(EXTRA_CONTENT_ICON_BACKGROUND, mIconBackgroundColor);
            args.putString(EXTRA_ACTION_NAME, mName);
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

        public Builder icon(Drawable icon) {
            mIcon = icon;
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

        public Builder name(String name) {
            mName = name;
            return this;
        }
    }

    public static void add(FragmentManager fm, SettingsLayoutFragment f) {
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

    private SettingsLayoutAdapter mAdapter;
    private VerticalGridView mListView;
    private String mTitle;
    private String mBreadcrumb;
    private String mDescription;
    private Drawable mIcon;
    private Uri mIconUri;
    private Bitmap mIconBitmap;
    private int mIconBackgroundColor = Color.TRANSPARENT;
    private Layout mLayout;
    private String mName;
    private int mSelectedIndex = -1;
    private boolean mEntryTransitionPerformed;
    private boolean mIntroAnimationInProgress;
    private int mAnimateInDuration;
    private int mAnimateDelay;
    private int mSecondaryAnimateDelay;
    private int mSlideInStagger;
    private int mSlideInDistance;
    private final Handler refreshViewHandler = new Handler();

    private final Runnable mRefreshViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (isResumed()) {
                mLayout.setSelectedIndex(mListView.getSelectedPosition());
                mLayout.reloadLayoutRows();
                mAdapter.setLayoutRows(mLayout.getLayoutRows());
                mAdapter.setNoAnimateMode();
                mAdapter.notifyDataSetChanged();
                mListView.setSelectedPositionSmooth(mLayout.getSelectedIndex());
            }
        }
    };

    private final SettingsLayoutAdapter.Listener mLayoutViewRowClicked =
        new SettingsLayoutAdapter.Listener() {
            @Override
            public void onRowClicked(Layout.LayoutRow layoutRow) {
                onRowViewClicked(layoutRow);
            }
        };

    private final SettingsLayoutAdapter.OnFocusListener mLayoutViewOnFocus =
        new SettingsLayoutAdapter.OnFocusListener() {
            @Override
            public void onActionFocused(Layout.LayoutRow action) {
                if (getActivity() instanceof SettingsLayoutAdapter.OnFocusListener) {
                    SettingsLayoutAdapter.OnFocusListener listener =
                            (SettingsLayoutAdapter.OnFocusListener) getActivity();
                    listener.onActionFocused(action);
                }
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        mTitle = state.getString(EXTRA_CONTENT_TITLE);
        mBreadcrumb = state.getString(EXTRA_CONTENT_BREADCRUMB);
        mDescription = state.getString(EXTRA_CONTENT_DESCRIPTION);
        //mIcon = state.getParcelable(EXTRA_CONTENT_ICON_RESOURCE_ID, 0);
        mIconUri = state.getParcelable(EXTRA_CONTENT_ICON_URI);
        mIconBitmap = state.getParcelable(EXTRA_CONTENT_ICON_BITMAP);
        mIconBackgroundColor = state.getInt(EXTRA_CONTENT_ICON_BACKGROUND, Color.TRANSPARENT);
        mName = state.getString(EXTRA_ACTION_NAME);
        mSelectedIndex = state.getInt(EXTRA_ACTION_SELECTED_INDEX, -1);
        mEntryTransitionPerformed = state.getBoolean(EXTRA_ENTRY_TRANSITION_PERFORMED, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.lb_dialog_fragment, container, false);

        View contentContainer = v.findViewById(R.id.content_fragment);
        View content = inflater.inflate(R.layout.lb_dialog_content, container, false);
        ((ViewGroup) contentContainer).addView(content);
        initializeContentView(content);
        v.setTag(R.id.content_fragment, content);

        View actionContainer = v.findViewById(R.id.action_fragment);
        View action = inflater.inflate(R.layout.lb_dialog_action_list, container, false);
        ((ViewGroup) actionContainer).addView(action);
        setActionView(action);
        v.setTag(R.id.action_fragment, action);

        Resources res = getActivity().getResources();
        mAnimateInDuration = res.getInteger(R.integer.animate_in_duration);
        mAnimateDelay = res.getInteger(R.integer.animate_delay);
        mSecondaryAnimateDelay = res.getInteger(R.integer.secondary_animate_delay);
        mSlideInStagger = res.getInteger(R.integer.slide_in_stagger);
        mSlideInDistance = res.getInteger(R.integer.slide_in_distance);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CONTENT_TITLE, mTitle);
        outState.putString(EXTRA_CONTENT_BREADCRUMB, mBreadcrumb);
        outState.putString(EXTRA_CONTENT_DESCRIPTION, mDescription);
        //outState.putInt(EXTRA_CONTENT_ICON_RESOURCE_ID, mIconResourceId);
        outState.putParcelable(EXTRA_CONTENT_ICON_URI, mIconUri);
        outState.putParcelable(EXTRA_CONTENT_ICON_BITMAP, mIconBitmap);
        outState.putInt(EXTRA_CONTENT_ICON_BACKGROUND, mIconBackgroundColor);
        outState.putInt(EXTRA_ACTION_SELECTED_INDEX,
                (mListView != null) ? mListView.getSelectedPosition() : -1);
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
            final View dialogView = getView();
            final View contentView = (View) dialogView.getTag(R.id.content_fragment);

            int bgColor = contentView.getContext().getColor(R.color.lb_dialog_activity_background);
            final ColorDrawable bgDrawable = new ColorDrawable();
            bgDrawable.setColor(bgColor);
            dialogView.setBackground(bgDrawable);
        }
    }

    public void setLayout(Layout layout) {
        mLayout = layout;
        mLayout.setRefreshViewListener(this);
    }

    // TODO refactor to get this call as the result of a callback from the Layout.
    private void updateViews() {
        View dialogView = getView();
        View contentView = (View) dialogView.getTag(R.id.content_fragment);

        mBreadcrumb = mLayout.getBreadcrumb();
        TextView breadcrumbView = (TextView) contentView.getTag(R.id.breadcrumb);
        breadcrumbView.setText(mBreadcrumb);

        mTitle = mLayout.getTitle();
        TextView titleView = (TextView) contentView.getTag(R.id.title);
        titleView.setText(mTitle);

        mDescription = mLayout.getDescription();
        TextView descriptionView = (TextView) contentView.getTag(R.id.description);
        descriptionView.setText(mDescription);

        mAdapter.setLayoutRows(mLayout.getLayoutRows());
        mAdapter.notifyDataSetChanged();
        mAdapter.setFocusListenerEnabled(false);
        mListView.setSelectedPosition(mLayout.getSelectedIndex());
        mAdapter.setFocusListenerEnabled(true);
    }

    public void setIcon(int resId) {
        View dialogView = getView();
        View contentView = (View) dialogView.getTag(R.id.content_fragment);
        ImageView iconView = (ImageView) contentView.findViewById(R.id.icon);
        if (iconView != null) {
            iconView.setImageResource(resId);
        }
    }

    /**
     * Notification that a part of the model antecedent to the visible view has changed.
     */
    @Override
    public void onRefreshView() {
        refreshViewHandler.removeCallbacks(mRefreshViewRunnable);
        refreshViewHandler.post(mRefreshViewRunnable);
    }

    /**
     * Return the currently selected node. The return value may be null, if this is called before
     * the layout has been rendered for the first time. Clients should check the return value
     * before using.
     */
    @Override
    public Layout.Node getSelectedNode() {
        int index = mListView.getSelectedPosition();
        ArrayList<Layout.LayoutRow> layoutRows = mLayout.getLayoutRows();
        if (index < layoutRows.size()) {
            return layoutRows.get(index).getNode();
        } else {
            return null;
        }
    }

    /**
     * Process forward key press.
     */
    void onRowViewClicked(Layout.LayoutRow layoutRow) {
        if (layoutRow.isGoBack()) {
            onBackPressed();
        } else if (layoutRow.isRadio()) {
            if (layoutRow.setRadioSelectedIndex()) {
                // SelectionGroup selection has changed, notify client.
                Listener actionListener = (Listener) getActivity();
                if (actionListener != null) {
                    // Create a temporary Action to return the id.
                    actionListener.onActionClicked(new Layout.Action(layoutRow.getRadioId()));
                }
            }
            onBackPressed();
        } else {
            Layout.Action action = layoutRow.getUserAction();
            if (action != null) {
                Listener actionListener = (Listener) getActivity();
                if (actionListener != null) {
                    actionListener.onActionClicked(action);
                }
            } else if (mLayout.onClickNavigate(layoutRow)) {
                mLayout.setParentSelectedIndex(mListView.getSelectedPosition());
                updateViews();
            }
        }
    }

    /**
     * Process back key press.
     */
    public boolean onBackPressed() {
        if (mLayout.goBack()) {
            updateViews();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Client has requested header with {@param title} be selected. If there is no such header
     * return to the first row.
     */
    protected void goBackToTitle(String title) {
        mLayout.goToTitle(title);
        updateViews();
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

    /**
     * Called when intro animation is finished.
     * <p>
     * If a subclass is going to alter the view, should wait until this is
     * called.
     */
    public void onIntroAnimationFinished() {
        mIntroAnimationInProgress = false;

        // Display the selector view.
        View focusedChild = mListView.getFocusedChild();
        if (focusedChild != null) {
            View actionView = (View) getView().getTag(R.id.action_fragment);
            int height = focusedChild.getHeight ();
            View selectorView = actionView.findViewById(R.id.selector);
            LayoutParams lp = selectorView.getLayoutParams();
            lp.height = height;
            selectorView.setLayoutParams(lp);
            selectorView.setAlpha (1f);
        }
    }

    public boolean isIntroAnimationInProgress() {
        return mIntroAnimationInProgress;
    }

    private void initializeContentView(View content) {
        TextView titleView = (TextView) content.findViewById(R.id.title);
        TextView breadcrumbView = (TextView) content.findViewById(R.id.breadcrumb);
        TextView descriptionView = (TextView) content.findViewById(R.id.description);
        titleView.setText(mTitle);
        breadcrumbView.setText(mBreadcrumb);
        descriptionView.setText(mDescription);
        final ImageView iconImageView = (ImageView) content.findViewById(R.id.icon);
        iconImageView.setBackgroundColor(mIconBackgroundColor);

        // Force text fields to be focusable when accessibility is enabled.
        if (AccessibilityHelper.forceFocusableViews(getActivity())) {
            titleView.setFocusable(true);
            titleView.setFocusableInTouchMode(true);
            descriptionView.setFocusable(true);
            descriptionView.setFocusableInTouchMode(true);
            breadcrumbView.setFocusable(true);
            breadcrumbView.setFocusableInTouchMode(true);
        }

        if (mIcon != null) {
            iconImageView.setImageDrawable(mIcon);
            updateViewSize(iconImageView);
        } else if (mIconBitmap != null) {
            iconImageView.setImageBitmap(mIconBitmap);
            updateViewSize(iconImageView);
        } else if (mIconUri != null) {
            iconImageView.setVisibility(View.INVISIBLE);
            /*

            BitmapDownloader bitmapDownloader = BitmapDownloader.getInstance(
                    content.getContext());
            mBitmapCallBack = new BitmapCallback() {
                @Override
                public void onBitmapRetrieved(Bitmap bitmap) {
                    if (bitmap != null) {
                        mIconBitmap = bitmap;
                        iconImageView.setVisibility(View.VISIBLE);
                        iconImageView.setImageBitmap(bitmap);
                        updateViewSize(iconImageView);
                    }
                }
            };

            bitmapDownloader.getBitmap(new BitmapWorkerOptions.Builder(
                    content.getContext()).resource(mIconUri)
                    .width(iconImageView.getLayoutParams().width).build(),
                    mBitmapCallBack);
            */
        } else {
            iconImageView.setVisibility(View.GONE);
        }

        content.setTag(R.id.title, titleView);
        content.setTag(R.id.breadcrumb, breadcrumbView);
        content.setTag(R.id.description, descriptionView);
        content.setTag(R.id.icon, iconImageView);
    }

    private void setActionView(View action) {
        mAdapter = new SettingsLayoutAdapter(mLayoutViewRowClicked, mLayoutViewOnFocus);
        mAdapter.setLayoutRows(mLayout.getLayoutRows());
        if (action instanceof VerticalGridView) {
            mListView = (VerticalGridView) action;
        } else {
            mListView = (VerticalGridView) action.findViewById(R.id.list);
            if (mListView == null) {
                throw new IllegalArgumentException("No ListView exists.");
            }
            mListView.setWindowAlignmentOffset(0);
            mListView.setWindowAlignmentOffsetPercent(WINDOW_ALIGNMENT_OFFSET_PERCENT);
            mListView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
            View selectorView = action.findViewById(R.id.selector);
            if (selectorView != null) {
                mListView.setOnScrollListener(new SelectorAnimator(selectorView, mListView));
            }
        }

        mListView.requestFocusFromTouch();
        mListView.setAdapter(mAdapter);
        int initialSelectedIndex;
        if (mSelectedIndex >= 0 && mSelectedIndex < mLayout.getLayoutRows().size()) {
            // "mSelectedIndex" is a valid index and so must have been initialized from a Bundle in
            // the "onCreate" member and the only way it could be a valid index is if it was saved
            // by "onSaveInstanceState" since it is initialized to "-1" (an invalid value) in the
            // constructor.
            initialSelectedIndex = mSelectedIndex;
        } else {
            // First time this fragment is being instantiated, i.e. did not reach here via the
            // "onSaveInstanceState" route. Initialize the index from the starting index defined
            // in the "Layout".
            initialSelectedIndex = mLayout.getSelectedIndex();
        }
        mListView.setSelectedPositionSmooth(initialSelectedIndex);
        action.setTag(R.id.list, mListView);
        action.setTag(R.id.selector, action.findViewById(R.id.selector));
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
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(v, "alpha", FADE_IN_ALPHA_START,
                FADE_IN_ALPHA_FINISH);
        alphaAnimator.setDuration(v.getContext().getResources().getInteger(
                android.R.integer.config_mediumAnimTime));
        alphaAnimator.start();
    }

    private void performEntryTransition() {
        final View dialogView = getView();
        final View contentView = (View) dialogView.getTag(R.id.content_fragment);
        final View actionContainerView = dialogView.findViewById(R.id.action_fragment);

        mIntroAnimationInProgress = true;

        // Fade out the old activity.
        getActivity().overridePendingTransition(0, R.anim.lb_dialog_fade_out);

        int bgColor = contentView.getContext().getColor(R.color.lb_dialog_activity_background);
        final ColorDrawable bgDrawable = new ColorDrawable();
        bgDrawable.setColor(bgColor);
        bgDrawable.setAlpha(0);
        dialogView.setBackground(bgDrawable);
        dialogView.setVisibility(View.INVISIBLE);

        // We need to defer the remainder of the animation preparation until the first layout has
        // occurred, as we don't yet know the final location of the icon.
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
                        contentView.postOnAnimationDelayed(mEntryAnimationRunnable, mAnimateDelay);
                    }

                    final Runnable mEntryAnimationRunnable = new Runnable() {
                            @Override
                        public void run() {
                            if (!isAdded()) {
                                // We have been detached before this could run, so just bail.
                                return;
                            }

                            dialogView.setVisibility(View.VISIBLE);

                            // Fade in the activity background protection
                            ObjectAnimator oa = ObjectAnimator.ofInt(bgDrawable, "alpha", 255);
                            oa.setDuration(mAnimateInDuration);
                            oa.setStartDelay(mSecondaryAnimateDelay);
                            oa.setInterpolator(new DecelerateInterpolator(1.0f));
                            oa.start();

                            boolean isRtl = ViewCompat.getLayoutDirection(contentView) ==
                                    ViewCompat.LAYOUT_DIRECTION_RTL;
                            int startDist = isRtl ? mSlideInDistance : -mSlideInDistance;
                            int endDist = isRtl ? -actionContainerView.getMeasuredWidth() :
                                    actionContainerView.getMeasuredWidth();

                            // Fade in and slide in the ContentFragment TextViews from the start.
                            prepareAndAnimateView((View) contentView.getTag(R.id.title),
                                    startDist, false);
                            prepareAndAnimateView((View) contentView.getTag(R.id.breadcrumb),
                                    startDist, false);
                            prepareAndAnimateView((View) contentView.getTag(R.id.description),
                                    startDist, false);

                            // Fade in and slide in the ActionFragment from the end.
                            prepareAndAnimateView(actionContainerView,
                                    endDist, false);
                            prepareAndAnimateView((View) contentView.getTag(R.id.icon),
                                    startDist, true);
                        }
                    };
                });
    }

    private void prepareAndAnimateView(final View v, float initTransX,
            final boolean notifyAnimationFinished) {
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        v.buildLayer();
        v.setAlpha(0);
        v.setTranslationX(initTransX);
        v.animate().alpha(1f).translationX(0).setDuration(mAnimateInDuration)
                .setStartDelay(mSecondaryAnimateDelay);
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
        float toX = isRtl ? SLIDE_OUT_ANIMATOR_RIGHT : -SLIDE_OUT_ANIMATOR_RIGHT;
        return createTranslateAlphaAnimator(v, SLIDE_OUT_ANIMATOR_LEFT, toX,
                SLIDE_OUT_ANIMATOR_END_ALPHA, SLIDE_OUT_ANIMATOR_START_ALPHA);
    }

    private Animator createSlideInFromEndAnimator(View v) {
        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
        float fromX = isRtl ? -SLIDE_OUT_ANIMATOR_RIGHT : SLIDE_OUT_ANIMATOR_RIGHT;
        return createTranslateAlphaAnimator(v, fromX, SLIDE_OUT_ANIMATOR_LEFT,
                SLIDE_OUT_ANIMATOR_START_ALPHA, SLIDE_OUT_ANIMATOR_END_ALPHA);
    }

    private Animator createSlideInFromStartAnimator(View v) {
        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
        float fromX = isRtl ? SLIDE_OUT_ANIMATOR_RIGHT : -SLIDE_OUT_ANIMATOR_RIGHT;
        return createTranslateAlphaAnimator(v, fromX, SLIDE_OUT_ANIMATOR_LEFT,
                SLIDE_OUT_ANIMATOR_START_ALPHA, SLIDE_OUT_ANIMATOR_END_ALPHA);
    }

    private Animator createSlideOutToEndAnimator(View v) {
        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
        float toX = isRtl ? -SLIDE_OUT_ANIMATOR_RIGHT : SLIDE_OUT_ANIMATOR_RIGHT;
        return createTranslateAlphaAnimator(v, SLIDE_OUT_ANIMATOR_LEFT, toX,
                SLIDE_OUT_ANIMATOR_END_ALPHA, SLIDE_OUT_ANIMATOR_START_ALPHA);
    }

    private Animator createFadeOutAnimator(View v) {
        return createAlphaAnimator(v, SLIDE_OUT_ANIMATOR_END_ALPHA, SLIDE_OUT_ANIMATOR_START_ALPHA);
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

        /**
         * We want to fade in the selector if we've stopped scrolling on it. If we're scrolling, we
         * want to ensure to dim the selector if we haven't already. We dim the last highlighted
         * view so that while a user is scrolling, nothing is highlighted.
         */
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                // The selector starts with a height of 0. In order to scale up from 0 we first
                // need the set the height to 1 and scale form there.
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
                            .setDuration(mAnimationDuration)
                            .setInterpolator(new DecelerateInterpolator(2f));
                    if (mFadedOut) {
                        // Selector is completely faded out, so we can just scale before fading in.
                        mSelectorView.setScaleY(scaleY);
                    } else {
                        // Selector is not faded out, so we must animate the scale as we fade in.
                        animation.scaleY(scaleY);
                    }
                    animation.start();
                }
            } else {
                mSelectorView.animate()
                        .alpha(0f)
                        .setDuration(mAnimationDuration)
                        .setInterpolator(new DecelerateInterpolator(2f))
                        .start();
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

}
