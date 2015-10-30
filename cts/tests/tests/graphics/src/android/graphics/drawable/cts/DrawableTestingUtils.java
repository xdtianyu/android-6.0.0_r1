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

package android.graphics.drawable.cts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;

public class DrawableTestingUtils {
    public static int getPixel(Drawable d, int x, int y) {
        final int w = Math.max(d.getIntrinsicWidth(), x + 1);
        final int h = Math.max(d.getIntrinsicHeight(), y + 1);
        final Bitmap b = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        final Canvas c = new Canvas(b);
        d.setBounds(0, 0, w, h);
        d.draw(c);

        final int pixel = b.getPixel(x, y);
        b.recycle();
        return pixel;
    }
}
