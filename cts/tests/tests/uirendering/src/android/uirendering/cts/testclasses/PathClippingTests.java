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

package android.uirendering.cts.testclasses;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.bitmapcomparers.MSSIMComparer;
import android.uirendering.cts.bitmapverifiers.SamplePointVerifier;
import android.uirendering.cts.testinfrastructure.ActivityTestBase;
import android.uirendering.cts.testinfrastructure.CanvasClient;
import android.uirendering.cts.testinfrastructure.ViewInitializer;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import com.android.cts.uirendering.R;

public class PathClippingTests extends ActivityTestBase {
    // draw circle with hole in it, with stroked circle
    static final CanvasClient sTorusDrawCanvasClient = new CanvasClient() {
        @Override
        public String getDebugString() {
            return "TorusDraw";
        }

        @Override
        public void draw(Canvas canvas, int width, int height) {
            Paint paint = new Paint();
            paint.setAntiAlias(false);
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(20);
            canvas.drawCircle(30, 30, 40, paint);
        }
    };

    // draw circle with hole in it, by path operations + path clipping
    static final CanvasClient sTorusClipCanvasClient = new CanvasClient() {
        @Override
        public String getDebugString() {
            return "TorusClipDraw";
        }

        @Override
        public void draw(Canvas canvas, int width, int height) {
            canvas.save();

            Path path = new Path();
            path.addCircle(30, 30, 50, Path.Direction.CW);
            path.addCircle(30, 30, 30, Path.Direction.CCW);

            canvas.clipPath(path);
            canvas.drawColor(Color.BLUE);

            canvas.restore();
        }
    };

    @SmallTest
    public void testCircleWithCircle() {
        createTest()
                .addCanvasClient(sTorusDrawCanvasClient, false)
                .addCanvasClient(sTorusClipCanvasClient)
                .runWithComparer(new MSSIMComparer(0.90));
    }

    @SmallTest
    public void testCircleWithPoints() {
        createTest()
                .addCanvasClient(sTorusClipCanvasClient)
                .runWithVerifier(new SamplePointVerifier(
                        new Point[] {
                                // inside of circle
                                new Point(30, 50),
                                // on circle
                                new Point(30 + 32, 30 + 32),
                                // outside of circle
                                new Point(30 + 38, 30 + 38),
                                new Point(80, 80)
                        },
                        new int[] {
                                Color.WHITE,
                                Color.BLUE,
                                Color.WHITE,
                                Color.WHITE,
                        }));
    }

    @SmallTest
    public void testViewRotate() {
        createTest()
                .addLayout(R.layout.blue_padded_layout, new ViewInitializer() {
                    @Override
                    public void initializeView(View view) {
                        ViewGroup rootView = (ViewGroup) view;
                        rootView.setClipChildren(true);
                        View childView = rootView.getChildAt(0);
                        childView.setPivotX(40);
                        childView.setPivotY(40);
                        childView.setRotation(45f);

                    }
                })
                .runWithVerifier(new SamplePointVerifier(
                        new Point[] {
                                // inside of rotated rect
                                new Point(40, 40),
                                new Point(40 + 25, 40 + 25),
                                // outside of rotated rect
                                new Point(40 + 31, 40 + 31),
                                new Point(80, 80)
                        },
                        new int[] {
                                Color.BLUE,
                                Color.BLUE,
                                Color.WHITE,
                                Color.WHITE,
                        }));
    }

    @SmallTest
    public void testTextClip() {
        createTest()
                .addCanvasClient(new CanvasClient() {
                    @Override
                    public void draw(Canvas canvas, int width, int height) {
                        canvas.save();

                        Path path = new Path();
                        path.addCircle(0, 45, 45, Path.Direction.CW);
                        path.addCircle(90, 45, 45, Path.Direction.CW);
                        canvas.clipPath(path);

                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        paint.setTextSize(90);
                        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                        canvas.drawText("STRING", 0, 90, paint);

                        canvas.restore();
                    }
                })
                .runWithComparer(new MSSIMComparer(0.90));
    }

    @SmallTest
    public void testWebViewClipWithCircle() {
        createTest()
                // golden client - draw a simple non-AA circle
                .addCanvasClient(new CanvasClient() {
                    @Override
                    public void draw(Canvas canvas, int width, int height) {
                        Paint paint = new Paint();
                        paint.setAntiAlias(false);
                        paint.setColor(Color.BLUE);
                        canvas.drawOval(0, 0, width, height, paint);
                    }
                }, false)
                // verify against solid color webview, clipped to its parent oval
                .addLayout(R.layout.circle_clipped_webview, new ViewInitializer() {
                    @Override
                    public void initializeView(View view) {
                        WebView webview = (WebView)view.findViewById(R.id.webview);
                        assertNotNull(webview);
                        webview.loadData("<body style=\"background-color:blue\">", null, null);
                    }
                })
                .runWithComparer(new MSSIMComparer(0.95));
    }
}
