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

package android.graphics.drawable.cts;

import com.android.cts.graphics.R;

import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Color;
import android.test.AndroidTestCase;

public class RippleDrawableTest extends AndroidTestCase {
    public void testConstructor() {
        new RippleDrawable(ColorStateList.valueOf(Color.RED), null, null);
    }

    public void testAccessRadius() {
        RippleDrawable drawable =
            new RippleDrawable(ColorStateList.valueOf(Color.RED), null, null);
        assertEquals(RippleDrawable.RADIUS_AUTO, drawable.getRadius());
        drawable.setRadius(10);
        assertEquals(10, drawable.getRadius());
    }

    public void testRadiusAttr() {
        RippleDrawable drawable =
                (RippleDrawable) getContext().getDrawable(R.drawable.rippledrawable_radius);
        assertEquals(10, drawable.getRadius());
    }
}
