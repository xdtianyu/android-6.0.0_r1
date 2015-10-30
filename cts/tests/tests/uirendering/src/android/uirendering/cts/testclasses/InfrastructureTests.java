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
package android.uirendering.cts.testclasses;

import android.graphics.Point;
import com.android.cts.uirendering.R;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.bitmapcomparers.BitmapComparer;
import android.uirendering.cts.bitmapcomparers.MSSIMComparer;
import android.uirendering.cts.bitmapverifiers.RectVerifier;
import android.uirendering.cts.testinfrastructure.ActivityTestBase;
import android.uirendering.cts.testinfrastructure.CanvasClient;
import android.uirendering.cts.testinfrastructure.ViewInitializer;
import android.view.View;

public class InfrastructureTests extends ActivityTestBase {

    @SmallTest
    public void testScreenshot() {
        for (int i = 0 ; i < 500 ; i ++) {
            takeScreenshot(new Point());
            System.gc();
        }
    }

    /**
     * Ensure that both render paths are producing independent output. We do this
     * by verifying that two paths that should render differently *do* render
     * differently.
     */
    @SmallTest
    public void testRenderSpecIsolation() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                canvas.drawColor(canvas.isHardwareAccelerated() ? Color.BLACK : Color.WHITE);
            }
        };
        BitmapComparer inverseComparer = new BitmapComparer() {
            @Override
            public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
                    int height) {

                // Return true if the images aren't even 10% similar. They should be completely
                // different, since they should both be completely different colors.
                final float threshold = 0.1f;
                return !(new MSSIMComparer(threshold)).verifySame(ideal, given, offset, stride,
                        width, height);
            }
        };
        createTest()
                .addCanvasClient(canvasClient)
                .runWithComparer(inverseComparer);
    }

    @SmallTest
    public void testViewInitializer() {
        final Rect clipRect = new Rect(0, 0, 50, 50);
        ViewInitializer viewInitializer = new ViewInitializer() {
            @Override
            public void initializeView(View view) {
                view.setClipBounds(clipRect);
            }
        };
        createTest()
                .addLayout(R.layout.simple_red_layout, viewInitializer)
                .runWithVerifier(new RectVerifier(Color.WHITE, Color.RED, clipRect));
    }
}
