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

package com.android.tv.settings.device.display.daydream;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.dreams.DreamService;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.tv.settings.dialog.old.Action;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Comparator;

public class DreamInfoAction extends Action {

    private static final int CHECK_SET_ID = 1;

    static class DreamInfoActionComparator implements Comparator<DreamInfoAction> {
        private final ComponentName mDefaultDream;

        public DreamInfoActionComparator(ComponentName defaultDream) {
            mDefaultDream = defaultDream;
        }

        @Override
        public int compare(DreamInfoAction lhs, DreamInfoAction rhs) {
            return sortKey(lhs).compareTo(sortKey(rhs));
        }

        private String sortKey(DreamInfoAction di) {
            StringBuilder sb = new StringBuilder();
            sb.append(di.mDreamComponentName.equals(mDefaultDream) ? '0' : '1');
            sb.append(di.getTitle());
            return sb.toString();
        }
    }

    private static final String TAG = "DreamInfoAction";

    private final ResolveInfo mResolveInfo;
    private final ComponentName mDreamComponentName;
    private final ComponentName mSettingsComponentName;

    DreamInfoAction(ResolveInfo resolveInfo, ComponentName activeDream, PackageManager pm) {
        this(resolveInfo, activeDream, (String) resolveInfo.loadLabel(pm), getDreamComponentName(
                resolveInfo), getSettingsComponentName(resolveInfo, pm));
    }

    private DreamInfoAction(ResolveInfo resolveInfo, ComponentName activeDream, String title,
            ComponentName dreamComponentName, ComponentName settingsComponentName) {
        this(resolveInfo, dreamComponentName.equals(activeDream), title, dreamComponentName,
                settingsComponentName);
    }

    protected DreamInfoAction(ResolveInfo resolveInfo, boolean checked, String title,
            ComponentName dreamComponentName, ComponentName settingsComponentName) {
        super(null, title, null, null, 0, null, checked, false, settingsComponentName != null,
                false, null, CHECK_SET_ID, true);
        mResolveInfo = resolveInfo;
        mDreamComponentName = dreamComponentName;
        mSettingsComponentName = settingsComponentName;
    }

    public Drawable getIndicator(Context context) {
        return mResolveInfo.loadIcon(context.getPackageManager());
    }

    public static Parcelable.Creator<DreamInfoAction> CREATOR =
            new Parcelable.Creator<DreamInfoAction>() {

                @Override
                public DreamInfoAction createFromParcel(Parcel source) {
                    return new DreamInfoAction((ResolveInfo) source.readParcelable(null),
                            source.readInt() == 1, source.readString(),
                            (ComponentName) source.readParcelable(null),
                            (ComponentName) source.readParcelable(null));
                }

                @Override
                public DreamInfoAction[] newArray(int size) {
                    return new DreamInfoAction[size];
                }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mResolveInfo, flags);
        dest.writeInt(isChecked() ? 1 : 0);
        dest.writeString(getTitle());
        dest.writeParcelable(mDreamComponentName, flags);
        dest.writeParcelable(mSettingsComponentName, flags);
    }

    public Intent getSettingsIntent() {
        if (mSettingsComponentName != null) {
            return new Intent().setComponent(mSettingsComponentName);
        } else {
            return null;
        }
    }

    public void setDream(DreamBackend dreamBackend) {
        if (!dreamBackend.isEnabled()) {
            dreamBackend.setEnabled(true);
        }
        dreamBackend.setActiveDream(mDreamComponentName);
        dreamBackend.setActiveDreamInfoAction(this);
    }

    private static ComponentName getDreamComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private static ComponentName getSettingsComponentName(ResolveInfo resolveInfo,
            PackageManager pm) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null
                || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        XmlResourceParser parser = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, DreamService.DREAM_META_DATA);
            if (parser == null) {
                Log.w(TAG, "No " + DreamService.DREAM_META_DATA + " meta-data");
                return null;
            }
            Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            String nodeName = parser.getName();
            if (!"dream".equals(nodeName)) {
                Log.w(TAG, "Meta-data does not start with dream tag");
                return null;
            }
            TypedArray sa = res.obtainAttributes(attrs, com.android.internal.R.styleable.Dream);
            cn = sa.getString(com.android.internal.R.styleable.Dream_settingsActivity);
            sa.recycle();
        } catch (NameNotFoundException e) {
            caughtException = e;
        } catch (IOException e) {
            caughtException = e;
        } catch (XmlPullParserException e) {
            caughtException = e;
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        if (caughtException != null) {
            Log.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName, caughtException);
            return null;
        }
        return cn == null ? null : ComponentName.unflattenFromString(cn);
    }
}
