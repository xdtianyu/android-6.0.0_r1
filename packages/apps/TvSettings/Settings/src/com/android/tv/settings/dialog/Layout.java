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

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A data class which represents a settings layout within an
 * {@link SettingsLayoutFragment}. Represents a list of choices the
 * user can make, a radio-button list of configuration options, or just a
 * list of information.
 */
public class Layout implements Parcelable {

    public interface LayoutNodeRefreshListener {
        void onRefreshView();
        Node getSelectedNode();
    }

    public interface ContentNodeRefreshListener {
        void onRefreshView();
    }

    public interface Node {
        String getTitle();
    }

    private abstract static class LayoutTreeNode implements Node {
        LayoutTreeBranch mParent;

        /* package */ boolean isEnabled() {
            return false;
        }

        /* package */ abstract Appearance getAppearance();
        /* package */ abstract void Log(int level);
    }

    private abstract static class LayoutTreeBranch extends LayoutTreeNode {
        final ArrayList<LayoutTreeNode> mChildren;
        LayoutTreeBranch() {
            mChildren = new ArrayList<>();
        }
    }

    public static class LayoutRow {
        public static final int NO_CHECK_SET = 0;
        public static final int VIEW_TYPE_ACTION = 0;
        public static final int VIEW_TYPE_STATIC = 1;
        public static final int VIEW_TYPE_WALLOFTEXT = 2;

        private String mTitle;
        private StringGetter mDescription;
        private final LayoutTreeNode mNode;
        private final boolean mEnabled;
        private final int mViewType;
        private BooleanGetter mChecked = FALSE_GETTER;
        private Drawable mIcon = null;
        private int mSelectionIndex;

        public Node getNode() {
            return mNode;
        }

        public Uri getIconUri() {
            return null;
        }

        public Drawable getIcon() {
            return mIcon;
        }

        public int getCheckSetId() {
            return 0;
        }

        public boolean isChecked() {
            return mChecked.get();
        }

        public BooleanGetter getChecked() {
            return mChecked;
        }

        public void setChecked(boolean v) {
            mChecked = v ? TRUE_GETTER : FALSE_GETTER;
        }

        public boolean infoOnly() {
            return false;
        }

        public boolean isEnabled() {
            return mEnabled;
        }

        public boolean hasNext() {
            return false;
        }

        public String getTitle() {
            return mTitle;
        }

        public StringGetter getDescription() {
            return mDescription;
        }

        public int getViewType() {
            return mViewType;
        }

        public boolean isRadio() {
            return mNode instanceof SelectionGroup;
        }

        public int getRadioId() {
            return ((SelectionGroup) mNode).getId(mSelectionIndex);
        }

        public boolean setRadioSelectedIndex() {
            return ((SelectionGroup) mNode).setSelectedIndex(mSelectionIndex);
        }

        public boolean isGoBack() {
            if (mNode instanceof Action) {
                Action a = (Action) mNode;
                if (a.mActionId == Action.ACTION_BACK) {
                    return true;
                }
            }
            return false;
        }

        public Action getUserAction() {
            if (mNode instanceof Action) {
                Action a = (Action) mNode;
                if (a.mActionId != Action.ACTION_NONE) {
                    return a;
                }
            }
            return null;
        }

        public int getContentIconRes() {
            if (mNode instanceof Header) {
                return ((Header) mNode).mContentIconRes;
            }
            return 0;
        }

        public LayoutRow(LayoutTreeNode node) {
            mNode = node;
            if (node instanceof Static) {
                mViewType = VIEW_TYPE_STATIC;
                mTitle = ((Static) node).mTitle;
            } else if (node instanceof WallOfText) {
                mViewType = VIEW_TYPE_WALLOFTEXT;
                mTitle = ((WallOfText) node).mTitle;
            } else {
                mViewType = VIEW_TYPE_ACTION;
            }
            mEnabled = node.isEnabled();
            final Appearance a = node.getAppearance();
            if (a != null) {
                mTitle = a.getTitle();
                mDescription = a.mDescriptionGetter;
                mIcon = a.getIcon();
                mChecked = a.getChecked();
            }
        }

        public LayoutRow(final SelectionGroup selectionGroup, int selectedIndex) {
            mNode = selectionGroup;
            mViewType = VIEW_TYPE_ACTION;
            mSelectionIndex = selectedIndex;
            mTitle = selectionGroup.getTitle(selectedIndex);
            mChecked = selectionGroup.getChecked(selectedIndex) ? TRUE_GETTER : FALSE_GETTER;
            mDescription = selectionGroup.getDescription(selectedIndex);
            mEnabled = true;
        }
    }

    /**
     * Getter object for boolean values, mostly used for checked state on headers/actions/etc.
     * This is a slightly simpler alternative to using a LayoutGetter to make sure the checked state
     * is always up to date.
     */
    public abstract static class BooleanGetter {
        private ContentNodeRefreshListener mListener;

        public void setListener(ContentNodeRefreshListener listener) {
            mListener = listener;
        }

        public abstract boolean get();

        /**
         * Notification from client that antecedent data has changed and the string should be
         * redisplayed.
         */
        public void refreshView() {
            if (mListener != null) {
                mListener.onRefreshView();
            }
        }
    }

    /**
     * Basic mutable implementation of BooleanGetter. Call {@link MutableBooleanGetter#set(boolean)}
     * to modify the state and automatically refresh the view.
     */
    public final static class MutableBooleanGetter extends BooleanGetter {
        private boolean mState;

        public MutableBooleanGetter() {
            mState = false;
        }

        public MutableBooleanGetter(boolean state) {
            mState = state;
        }

        @Override
        public boolean get() {
            return mState;
        }

        /**
         * Set the boolean value for this object. Automatically calls {@link #refreshView()}
         * @param newState State to set
         */
        public void set(boolean newState) {
            mState = newState;
            refreshView();
        }
    }

    private final static class LiteralBooleanGetter extends BooleanGetter {
        private final boolean mState;

        public LiteralBooleanGetter(boolean state) {
            mState = state;
        }

        @Override
        public boolean get() {
            return mState;
        }

        @Override
        public void setListener(ContentNodeRefreshListener listener) {}

        @Override
        public void refreshView() {}
    }

    private static final BooleanGetter TRUE_GETTER = new LiteralBooleanGetter(true);
    private static final BooleanGetter FALSE_GETTER = new LiteralBooleanGetter(false);

    public abstract static class DrawableGetter {
        public abstract Drawable get();

        /**
         * Notification from client that antecedent data has changed and the drawable should be
         * redisplayed.
         */
        public void refreshView() {
            //TODO - When implementing, ensure that multiple updates from the same event do not
            // cause multiple view updates.
        }
    }

    public abstract static class StringGetter {
        private ContentNodeRefreshListener mListener;

        public void setListener(ContentNodeRefreshListener listener) {
            mListener = listener;
        }

        public abstract String get();

        /**
         * Notification from client that antecedent data has changed and the string should be
         * redisplayed.
         */
        public void refreshView() {
            if (mListener != null) {
                mListener.onRefreshView();
            }
        }
    }

    /**
     * Implementation of "StringGetter" that stores and returns a literal string.
     */
    private static class LiteralStringGetter extends StringGetter {
        private final String mValue;
        public String get() {
            return mValue;
        }
        LiteralStringGetter(String value) {
            mValue = value;
        }
    }

    /**
     * Implementation of "StringGetter" that stores a string resource id and returns a string.
     */
    private static class ResourceStringGetter extends StringGetter {
        private final int mStringResourceId;
        private final Resources mRes;
        public String get() {
            return mRes.getString(mStringResourceId);
        }
        ResourceStringGetter(Resources res, int stringResourceId) {
            mRes = res;
            mStringResourceId = stringResourceId;
        }
    }

    public abstract static class LayoutGetter extends LayoutTreeNode {
        // Layout manages this listener; removing it when this node is not visible and setting it
        // when it is.  Users are expected to set the listener with Layout.setRefreshViewListener.
        private LayoutNodeRefreshListener mListener;

        public void setListener(LayoutNodeRefreshListener listener) {
            mListener = listener;
        }

        public void notVisible() {
            mListener = null;
        }

        public abstract Layout get();

        public Node getSelectedNode() {
            if (mListener != null) {
                return mListener.getSelectedNode();
            } else {
                return null;
            }
        }

        /**
         * Notification from client that antecedent data has changed and the list containing the
         * contents of this getter should be updated.
         */
        public void refreshView() {
            if (mListener != null) {
                mListener.onRefreshView();
            }
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        /* package */ Appearance getAppearance() {
            return null;
        }

        @Override
        /* package */ void Log(int level) {
            Log.d("Layout", indent(level) + "LayoutGetter");
            Layout l = get();
            l.Log(level + 1);
        }
    }

    /**
     * A list of "select one of" radio style buttons.
     */
    public static class SelectionGroup extends LayoutTreeNode {
        final String mTitle[];
        final StringGetter mDescription[];
        final int mId[];
        int mSelectedIndex;

        public static final class Builder {
            private static class Item {
                public final String title;
                public final StringGetter description;
                public final int id;

                public Item(String title, String description, int id) {
                    this.title = title;
                    if (TextUtils.isEmpty(description)) {
                        this.description = null;
                    } else {
                        this.description = new LiteralStringGetter(description);
                    }
                    this.id = id;
                }
            }

            private final List<Item> mItems;
            private boolean mSetInitialId = false;
            private int mInitialId;

            public Builder() {
                mItems = new ArrayList<>();
            }

            public Builder(int count) {
                mItems = new ArrayList<>(count);
            }

            public Builder add(String title, String description, int id) {
                mItems.add(new Item(title, description, id));
                return this;
            }

            public Builder select(int id) {
                mSetInitialId = true;
                mInitialId = id;
                return this;
            }

            public SelectionGroup build() {
                final int size = mItems.size();
                final String[] titles = new String[size];
                final StringGetter[] descriptions = new StringGetter[size];
                final int[] ids = new int[size];
                int i = 0;
                for (final Item item : mItems) {
                    titles[i] = item.title;
                    descriptions[i] = item.description;
                    ids[i] = item.id;
                    i++;
                }
                final SelectionGroup selectionGroup = new SelectionGroup(titles, descriptions, ids);
                if (mSetInitialId) {
                    selectionGroup.setSelected(mInitialId);
                }
                return selectionGroup;
            }
        }

        public SelectionGroup(Resources res, int param[][]) {
            mSelectedIndex = -1;
            mTitle = new String[param.length];
            mDescription = new StringGetter[param.length];
            mId = new int[param.length];
            for (int i = 0; i < param.length; ++i) {
                mTitle[i] = res.getString(param[i][0]);
                mId[i] = param[i][1];
            }
        }

        public SelectionGroup(String[] titles, StringGetter[] descriptions, int[] ids) {
            mSelectedIndex = -1;
            mTitle = titles;
            mDescription = descriptions;
            mId = ids;
        }

        @Override
        public String getTitle() {
            if (mSelectedIndex >= 0 && mSelectedIndex < mTitle.length) {
                return mTitle[mSelectedIndex];
            } else {
                return "";
            }
        }

        @Override
        /* package */ Appearance getAppearance() {
            return null;
        }

        @Override
        /* package */ void Log(int level) {
            Log.d("Layout", indent(level) + "SelectionGroup  '" + getTitle() + "'");
        }

        public String getTitle(int index) {
            return mTitle[index];
        }

        public StringGetter getDescription(int index) {
            return mDescription[index];
        }

        public boolean getChecked(int index) {
            return mSelectedIndex == index;
        }

        public int size() {
            return mTitle.length;
        }

        public void setSelected(int id) {
            mSelectedIndex = -1;
            for (int index = 0, dim = mId.length; index < dim; ++index) {
                if (mId[index] == id) {
                    mSelectedIndex = index;
                    break;
                }
            }
        }

        public boolean setSelectedIndex(int selectedIndex) {
            if (mSelectedIndex != selectedIndex && selectedIndex < mId.length) {
                mSelectedIndex = selectedIndex;
                return true;
            } else {
                return false;
            }
        }

        public int getId(int index) {
            return mId[index];
        }

        public int getId() {
            return mId[mSelectedIndex];
        }
    }

    /**
     * Implementation of "StringGetter" that returns a string describing the currently selected
     * item in a SelectionGroup.
     */
    public static class SelectionGroupStringGetter extends StringGetter {

        private final SelectionGroup mSelectionGroup;

        @Override
        public String get() {
            return mSelectionGroup.getTitle();
        }

        public SelectionGroupStringGetter(SelectionGroup selectionGroup) {
            mSelectionGroup = selectionGroup;
        }

    }

    private static class Appearance {
        private Drawable mIcon;
        private DrawableGetter mIconGetter;
        private String mTitle;
        private StringGetter mDescriptionGetter;
        private BooleanGetter mChecked = FALSE_GETTER;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder()
                .append("'")
                .append(mTitle)
                .append("'");
            if (mDescriptionGetter != null) {
                stringBuilder
                    .append(" : '")
                    .append(mDescriptionGetter.get())
                    .append("'");
            }
            stringBuilder
                .append(" : '")
                .append(mChecked.get())
                .append("'");
            return stringBuilder.toString();
        }

        public String getTitle() {
            return mTitle;
        }

        public Drawable getIcon() {
            if (mIconGetter != null) {
                return mIconGetter.get();
            } else {
                return mIcon;
            }
        }

        public BooleanGetter getChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked ? TRUE_GETTER : FALSE_GETTER;
        }

        public void setChecked(BooleanGetter getter) {
            mChecked = getter;
        }
    }

    /**
     * Header is a container for a sub-menu of "LayoutTreeNode" items.
     */
    public static class Header extends LayoutTreeBranch {
        private final Appearance mAppearance = new Appearance();
        private int mSelectedIndex = 0;
        private String mDetailedDescription;
        private int mContentIconRes = 0;
        private boolean mEnabled = true;

        public static class Builder {
            private final Resources mRes;
            private final Header mHeader = new Header();

            public Builder(Resources res) {
                mRes = res;
            }

            public Builder icon(int resId) {
                mHeader.mAppearance.mIcon = mRes.getDrawable(resId);
                return this;
            }

            public Builder icon(DrawableGetter drawableGetter) {
                mHeader.mAppearance.mIconGetter = drawableGetter;
                return this;
            }

            public Builder contentIconRes(int resId) {
                mHeader.mContentIconRes = resId;
                return this;
            }

            public Builder title(int resId) {
                mHeader.mAppearance.mTitle = mRes.getString(resId);
                return this;
            }

            public Builder description(int resId) {
                mHeader.mAppearance.mDescriptionGetter = new ResourceStringGetter(mRes, resId);
                return this;
            }

            public Builder description(SelectionGroup selectionGroup) {
                mHeader.mAppearance.mDescriptionGetter = new SelectionGroupStringGetter(
                        selectionGroup);
                return this;
            }

            public Builder title(String title) {
                mHeader.mAppearance.mTitle = title;
                return this;
            }

            public Builder description(String description) {
                mHeader.mAppearance.mDescriptionGetter = description != null ?
                        new LiteralStringGetter(description) : null;
                return this;
            }

            public Builder description(StringGetter description) {
                mHeader.mAppearance.mDescriptionGetter = description;
                return this;
            }

            public Builder detailedDescription(int resId) {
                mHeader.mDetailedDescription = mRes.getString(resId);
                return this;
            }

            public Builder detailedDescription(String detailedDescription) {
                mHeader.mDetailedDescription = detailedDescription;
                return this;
            }

            public Builder enabled(boolean enabled) {
                mHeader.mEnabled = enabled;
                return this;
            }

            public Header build() {
                return mHeader;
            }
        }

        @Override
        public String getTitle() {
            return mAppearance.getTitle();
        }

        public Header add(LayoutTreeNode node) {
            node.mParent = this;
            mChildren.add(node);
            return this;
        }

        public Header setSelectionGroup(SelectionGroup selectionGroup) {
            selectionGroup.mParent = this;
            mChildren.add(selectionGroup);
            mAppearance.mDescriptionGetter = new SelectionGroupStringGetter(
                    selectionGroup);
            return this;
        }

        String getDetailedDescription() {
            return mDetailedDescription;
        }

        @Override
        /* package */ boolean isEnabled() {
            return mEnabled;
        }

        @Override
        /* package */ Appearance getAppearance() {
            return mAppearance;
        }

        @Override
        /* package */ void Log(int level) {
            Log.d("Layout", indent(level) + "Header  " + mAppearance);
            for (LayoutTreeNode i : mChildren)
                i.Log(level + 1);
        }
    }

    public static class Action extends LayoutTreeNode {
        public static final int ACTION_NONE = -1;
        public static final int ACTION_INTENT = -2;
        public static final int ACTION_BACK = -3;
        private final int mActionId;
        private final Intent mIntent;
        private final Appearance mAppearance = new Appearance();
        private Bundle mActionData;
        private boolean mDefaultSelection = false;
        private boolean mEnabled = true;

        public Action(int id) {
            mActionId = id;
            mIntent = null;
        }

        private Action(Intent intent) {
            mActionId = ACTION_INTENT;
            mIntent = intent;
        }

        public static class Builder {
            private final Resources mRes;
            private final Action mAction;

            public Builder(Resources res, int id) {
                mRes = res;
                mAction = new Action(id);
            }

            public Builder(Resources res, Intent intent) {
                mRes = res;
                mAction = new Action(intent);
            }

            public Builder title(int resId) {
                mAction.mAppearance.mTitle = mRes.getString(resId);
                return this;
            }

            public Builder description(int resId) {
                mAction.mAppearance.mDescriptionGetter = new LiteralStringGetter(mRes.getString(
                        resId));
                return this;
            }

            public Builder title(String title) {
                mAction.mAppearance.mTitle = title;
                return this;
            }

            public Builder icon(int resId) {
                mAction.mAppearance.mIcon = mRes.getDrawable(resId);
                return this;
            }

            public Builder icon(Drawable drawable) {
                mAction.mAppearance.mIcon = drawable;
                return this;
            }

            public Builder description(String description) {
                mAction.mAppearance.mDescriptionGetter = new LiteralStringGetter(description);
                return this;
            }

            public Builder description(StringGetter description) {
                mAction.mAppearance.mDescriptionGetter = description;
                return this;
            }

            public Builder checked(boolean checked) {
                mAction.mAppearance.setChecked(checked);
                return this;
            }

            public Builder checked(BooleanGetter getter) {
                mAction.mAppearance.setChecked(getter);
                return this;
            }

            public Builder data(Bundle data) {
                mAction.mActionData = data;
                return this;
            }

            /*
             * Makes this action default initial selection when the list is displayed.
             */
            public Builder defaultSelection() {
                mAction.mDefaultSelection = true;
                return this;
            }

            public Builder enabled(boolean enabled) {
                mAction.mEnabled = enabled;
                return this;
            }

            public Action build() {
                return mAction;
            }
        }

        @Override
        /* package */ boolean isEnabled() {
            return mEnabled;
        }

        @Override
        /* package */ Appearance getAppearance() {
            return mAppearance;
        }

        @Override
        /* package */ void Log(int level) {
            Log.d("Layout", indent(level) + "Action  #" + mActionId + "  " + mAppearance);
        }

        public int getId() {
            return mActionId;
        }

        public Intent getIntent() {
            return mIntent;
        }

        @Override
        public String getTitle() {
            return mAppearance.getTitle();
        }

        public Bundle getData() {
            return mActionData;
        }
    }

    public static class Status extends LayoutTreeNode {
        private final Appearance mAppearance = new Appearance();
        private boolean mEnabled = true;

        public static class Builder {
            private final Resources mRes;
            private final Status mStatus = new Status();

            public Builder(Resources res) {
                mRes = res;
            }

            public Builder icon(int resId) {
                mStatus.mAppearance.mIcon = mRes.getDrawable(resId);
                return this;
            }

            public Builder title(int resId) {
                mStatus.mAppearance.mTitle = mRes.getString(resId);
                return this;
            }

            public Builder description(int resId) {
                mStatus.mAppearance.mDescriptionGetter = new LiteralStringGetter(mRes.getString(
                        resId));
                return this;
            }

            public Builder title(String title) {
                mStatus.mAppearance.mTitle = title;
                return this;
            }

            public Builder description(String description) {
                mStatus.mAppearance.mDescriptionGetter = new LiteralStringGetter(description);
                return this;
            }

            public Builder description(StringGetter description) {
                mStatus.mAppearance.mDescriptionGetter = description;
                return this;
            }

            public Builder enabled(boolean enabled) {
                mStatus.mEnabled = enabled;
                return this;
            }

            public Status build() {
                return mStatus;
            }
        }

        @Override
        public String getTitle() {
            return mAppearance.getTitle();
        }

        @Override
        /* package */ boolean isEnabled() {
            return mEnabled;
        }

        @Override
        /* package */ Appearance getAppearance() {
            return mAppearance;
        }

        @Override
        /* package */ void Log(int level) {
            Log.d("Layout", indent(level) + "Status  " + mAppearance);
        }
    }

    public static class Static extends LayoutTreeNode {
        private String mTitle;

        public static class Builder {
            private final Resources mRes;
            private final Static mStatic = new Static();

            public Builder(Resources res) {
                mRes = res;
            }

            public Builder title(int resId) {
                mStatic.mTitle = mRes.getString(resId);
                return this;
            }

            public Builder title(String title) {
                mStatic.mTitle = title;
                return this;
            }

            public Static build() {
                return mStatic;
            }
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        /* package */ Appearance getAppearance() {
            return null;
        }

        @Override
        /* package */ void Log(int level) {
            Log.d("Layout", indent(level) + "Static  '" + mTitle + "'");
        }
    }

    public static class WallOfText extends LayoutTreeNode {
        private String mTitle;

        public static class Builder {
            private final Resources mRes;
            private final WallOfText mWallOfText = new WallOfText();

            public Builder(Resources res) {
                mRes = res;
            }

            public Builder title(int resId) {
                mWallOfText.mTitle = mRes.getString(resId);
                return this;
            }

            public Builder title(String title) {
                mWallOfText.mTitle = title;
                return this;
            }

            public WallOfText build() {
                return mWallOfText;
            }
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        /* package */ Appearance getAppearance() {
            return null;
        }

        @Override
        /* package */ void Log(int level) {
            Log.d("Layout", indent(level) + "Static  '" + mTitle + "'");
        }
    }

    /**
     * Pointer to currently visible item.
     */
    private Header mNavigationCursor;

    /**
     * Index of selected item when items are displayed. This is used by LayoutGetter to implemented
     * selection stability, where a LayoutGetter can arrange for a list that is refreshed regularly
     * to carry forward a selection.
     */
    private int mInitialItemIndex = -1;
    private final ArrayList<LayoutRow> mLayoutRows = new ArrayList<>();
    private final ArrayList<LayoutGetter> mVisibleLayoutGetters = new ArrayList<>();
    private final ArrayList<LayoutTreeNode> mChildren = new ArrayList<>();
    private String mTopLevelBreadcrumb = "";
    private LayoutNodeRefreshListener mListener;

    public ArrayList<LayoutRow> getLayoutRows() {
        return mLayoutRows;
    }

    public void setRefreshViewListener(LayoutNodeRefreshListener listener) {
        mListener = listener;
        for (final LayoutGetter getter : mVisibleLayoutGetters) {
            getter.setListener(listener);
        }
    }

    /**
     * Return the breadcrumb the user should see in the content pane.
     */
    public String getBreadcrumb() {
      if (mNavigationCursor.mParent == null) {
          // At the top level of the layout.
          return mTopLevelBreadcrumb;
      } else {
          // Showing a header down the hierarchy, breadcrumb is title of item above.
          return ((Header) (mNavigationCursor.mParent)).mAppearance.mTitle;
      }
    }

    /**
     * Navigate up one level, return true if a parent node is now visible. Return false if the
     * already at the top level node. The controlling fragment interprets a false return value as
     * "stop activity".
     */
    public boolean goBack() {
        if (mNavigationCursor.mParent != null) {
            mNavigationCursor = (Header) mNavigationCursor.mParent;
            updateLayoutRows();
            return true;
        }
        return false;
    }

    /**
     * Parcelable implementation.
     */
    public Layout(Parcel in) {
    }

    public Layout() {
        mNavigationCursor = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Layout createFromParcel(Parcel in) {
            return new Layout(in);
        }

        public Layout[] newArray(int size) {
            return new Layout[size];
        }
    };

    String getTitle() {
        return mNavigationCursor.mAppearance.mTitle;
    }

    Drawable getIcon() {
        return mNavigationCursor.mAppearance.getIcon();
    }

    String getDescription() {
        return mNavigationCursor.getDetailedDescription();
    }

    public void goToTitle(String title) {
        while (mNavigationCursor.mParent != null) {
            mNavigationCursor = (Header) (mNavigationCursor.mParent);
            if (TextUtils.equals(mNavigationCursor.mAppearance.mTitle, title)) {
                break;
            }
        }
        updateLayoutRows();
    }

    /*
     * Respond to a user click on "layoutRow" and return "true" if the state of the display has
     * changed. A controlling fragment will respond to a "true" return by updating the view.
     */
    public boolean onClickNavigate(LayoutRow layoutRow) {
        LayoutTreeNode node = layoutRow.mNode;
        if (node instanceof Header) {
            mNavigationCursor.mSelectedIndex = mLayoutRows.indexOf(layoutRow);
            mNavigationCursor = (Header) node;
            updateLayoutRows();
            return true;
        }
        return false;
    }

    public void reloadLayoutRows() {
        updateLayoutRows();
    }

    public Layout add(Header header) {
        header.mParent = null;
        mChildren.add(header);
        return this;
    }

    public Layout add(LayoutTreeNode leaf) {
        leaf.mParent = null;
        mChildren.add(leaf);
        return this;
    }

    public Layout breadcrumb(String topLevelBreadcrumb) {
        mTopLevelBreadcrumb = topLevelBreadcrumb;
        return this;
    }

    /**
     * Sets the selected node to the first top level node with its title member equal to "title". If
     * "title" is null, empty, or there are no top level nodes with a title member equal to "title",
     * set the first node in the list as the selected.
     */
    public Layout setSelectedByTitle(String title) {
        for (int i = 0; i < mChildren.size(); ++i) {
            if (TextUtils.equals(mChildren.get(i).getTitle(), title)) {
                mInitialItemIndex = i;
                break;
            }
        }
        return this;
    }

    public void Log(int level) {
        for (LayoutTreeNode i : mChildren) {
            i.Log(level + 1);
        }
    }

    public void Log() {
        Log.d("Layout", "----- Layout");
        Log(0);
    }

    public void navigateToRoot() {
        if (mChildren.size() > 0) {
            mNavigationCursor = (Header) mChildren.get(0);
        } else {
            mNavigationCursor = null;
        }
        updateLayoutRows();
    }

    public int getSelectedIndex() {
        return mNavigationCursor.mSelectedIndex;
    }

    public void setSelectedIndex(int index) {
        mNavigationCursor.mSelectedIndex = index;
    }

    public void setParentSelectedIndex(int index) {
        if (mNavigationCursor.mParent != null) {
            Header u = (Header) mNavigationCursor.mParent;
            u.mSelectedIndex = index;
        }
    }

    private void addNodeListToLayoutRows(ArrayList<LayoutTreeNode> list) {
        for (LayoutTreeNode node : list) {
            if (node instanceof LayoutGetter) {
                // Add subitems of "node" recursively.
                LayoutGetter layoutGetter = (LayoutGetter) node;
                layoutGetter.setListener(mListener);
                mVisibleLayoutGetters.add(layoutGetter);
                Layout layout = layoutGetter.get();
                for (LayoutTreeNode child : layout.mChildren) {
                    child.mParent = mNavigationCursor;
                }
                int initialIndex = layout.mInitialItemIndex;
                if (initialIndex != -1) {
                    mNavigationCursor.mSelectedIndex = mLayoutRows.size() + initialIndex;
                }
                addNodeListToLayoutRows(layout.mChildren);
            } else if (node instanceof SelectionGroup) {
                SelectionGroup sg = (SelectionGroup) node;
                for (int i = 0; i < sg.size(); ++i) {
                    mLayoutRows.add(new LayoutRow(sg, i));
                }
            } else {
                if (node instanceof Action && ((Action) node).mDefaultSelection) {
                    mNavigationCursor.mSelectedIndex = mLayoutRows.size();
                }
                mLayoutRows.add(new LayoutRow(node));
            }
        }
    }

    private void updateLayoutRows() {
        mLayoutRows.clear();
        for (LayoutGetter layoutGetter : mVisibleLayoutGetters) {
            layoutGetter.notVisible();
        }
        mVisibleLayoutGetters.clear();
        addNodeListToLayoutRows(mNavigationCursor.mChildren);

        // Skip past any unselectable items
        final int rowCount = mLayoutRows.size();
        while (mNavigationCursor.mSelectedIndex < rowCount - 1 &&
                mNavigationCursor.mSelectedIndex >= 0 &&
                ((mLayoutRows.get(mNavigationCursor.mSelectedIndex).mViewType
                        == LayoutRow.VIEW_TYPE_STATIC) ||
                (mLayoutRows.get(mNavigationCursor.mSelectedIndex).mViewType
                        == LayoutRow.VIEW_TYPE_WALLOFTEXT))) {
            mNavigationCursor.mSelectedIndex++;
        }
    }

    private static String indent(int level) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            s.append("  ");
        }
        return s.toString();
    }
}
