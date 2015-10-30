package android.uirendering.cts.util;

import android.graphics.Color;

public class CompareUtils {
    /**
     * @return True if close enough
     */
    public static boolean verifyPixelWithThreshold(int color, int expectedColor, int threshold) {
        int diff = Math.abs(Color.red(color) - Color.red(expectedColor))
                + Math.abs(Color.green(color) - Color.green(expectedColor))
                + Math.abs(Color.blue(color) - Color.blue(expectedColor));
        return diff <= threshold;
    }
}
