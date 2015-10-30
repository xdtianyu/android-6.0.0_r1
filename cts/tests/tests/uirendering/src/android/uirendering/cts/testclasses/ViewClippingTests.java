package android.uirendering.cts.testclasses;

import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.uirendering.cts.bitmapverifiers.BitmapVerifier;
import android.uirendering.cts.bitmapverifiers.RectVerifier;
import android.uirendering.cts.testclasses.view.UnclippedBlueView;
import android.uirendering.cts.testinfrastructure.ActivityTestBase;
import android.uirendering.cts.testinfrastructure.ViewInitializer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import com.android.cts.uirendering.R;

/**
 * This tests view clipping by modifying properties of blue_padded_layout, and validating
 * the resulting rect of content.
 *
 * Since the layout is blue on a white background, this is always done with a RectVerifier.
 */
public class ViewClippingTests extends ActivityTestBase {
    final Rect FULL_RECT = new Rect(0, 0, 90, 90);
    final Rect BOUNDS_RECT = new Rect(0, 0, 80, 80);
    final Rect PADDED_RECT = new Rect(15, 16, 63, 62);
    final Rect OUTLINE_RECT = new Rect(1, 2, 78, 79);
    final Rect CLIP_BOUNDS_RECT = new Rect(10, 20, 50, 60);

    final ViewInitializer BOUNDS_CLIP_INIT = new ViewInitializer() {
        @Override
        public void initializeView(View rootView) {
            ((ViewGroup)rootView).setClipChildren(true);
        }
    };
    final ViewInitializer PADDING_CLIP_INIT = new ViewInitializer() {
        @Override
        public void initializeView(View rootView) {
            ViewGroup child = (ViewGroup) rootView.findViewById(R.id.child);
            child.setClipToPadding(true);
            child.setWillNotDraw(true);
            child.addView(new UnclippedBlueView(rootView.getContext()));
        }
    };
    final ViewInitializer OUTLINE_CLIP_INIT = new ViewInitializer() {
        @Override
        public void initializeView(View rootView) {
            View child = rootView.findViewById(R.id.child);
            child.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRect(OUTLINE_RECT);
                }
            });
            child.setClipToOutline(true);
        }
    };
    final ViewInitializer CLIP_BOUNDS_CLIP_INIT = new ViewInitializer() {
        @Override
        public void initializeView(View view) {
            view.setClipBounds(CLIP_BOUNDS_RECT);
        }
    };

    static BitmapVerifier makeClipVerifier(Rect blueBoundsRect) {
        // very high error tolerance, since all these tests care about is clip alignment
        return new RectVerifier(Color.WHITE, Color.BLUE, blueBoundsRect, 75);
    }

    public void testSimpleUnclipped() {
        createTest()
                .addLayout(R.layout.blue_padded_layout, null)
                .runWithVerifier(makeClipVerifier(FULL_RECT));
    }

    public void testSimpleBoundsClip() {
        createTest()
                .addLayout(R.layout.blue_padded_layout, BOUNDS_CLIP_INIT)
                .runWithVerifier(makeClipVerifier(BOUNDS_RECT));
    }

    public void testSimpleClipBoundsClip() {
        createTest()
                .addLayout(R.layout.blue_padded_layout, CLIP_BOUNDS_CLIP_INIT)
                .runWithVerifier(makeClipVerifier(CLIP_BOUNDS_RECT));
    }

    public void testSimplePaddingClip() {
        createTest()
                .addLayout(R.layout.blue_padded_layout, PADDING_CLIP_INIT)
                .runWithVerifier(makeClipVerifier(PADDED_RECT));
    }

    public void testSimpleOutlineClip() {
        // NOTE: Only HW is supported
        createTest()
                .addLayout(R.layout.blue_padded_layout, OUTLINE_CLIP_INIT, true)
                .runWithVerifier(makeClipVerifier(OUTLINE_RECT));

        // SW ignores the outline clip
        createTest()
                .addLayout(R.layout.blue_padded_layout, OUTLINE_CLIP_INIT, false)
                .runWithVerifier(makeClipVerifier(FULL_RECT));
    }

    // TODO: add tests with clip + scroll, and with interesting combinations of the above
}
