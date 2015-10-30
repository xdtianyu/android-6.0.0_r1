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

package com.android.tv.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.tv.settings.R;

public class RefcountImageView extends ImageView {

    private boolean mAutoUnrefOnDetach;
    private boolean mHasClipRect;
    private final RectF mClipRect = new RectF();

    public RefcountImageView(Context context) {
        this(context, null);
    }

    public RefcountImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefcountImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefcountImageView);
        mAutoUnrefOnDetach = a.getBoolean(R.styleable.RefcountImageView_autoUnrefOnDetach, true);
    }

    public void setAutoUnrefOnDetach(boolean autoUnref) {
        mAutoUnrefOnDetach = autoUnref;
    }

    public boolean getAutoUnrefOnDetach() {
        return mAutoUnrefOnDetach;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAutoUnrefOnDetach) {
            setImageDrawable(null);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        Drawable previousDrawable = getDrawable();
        super.setImageDrawable(drawable);
        releaseRef(previousDrawable);
    }

    private static void releaseRef(Drawable drawable) {
        if (drawable instanceof RefcountBitmapDrawable) {
            ((RefcountBitmapDrawable) drawable).getRefcountObject().releaseRef();
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            for (int i = 0, z = layerDrawable.getNumberOfLayers(); i < z; i++) {
                releaseRef(layerDrawable.getDrawable(i));
            }
        }
    }

    public void setClipRect(float l, float t, float r, float b) {
        mClipRect.set(l, t, r, b);
        mHasClipRect = true;
    }

    public void clearClipRect() {
        mHasClipRect = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mHasClipRect) {
            int saveCount = canvas.save();
            canvas.clipRect(mClipRect);
            super.onDraw(canvas);
            canvas.restoreToCount(saveCount);
        } else {
            super.onDraw(canvas);
        }
    }
}
