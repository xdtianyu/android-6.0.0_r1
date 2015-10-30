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

package android.widget.cts;

import com.android.cts.widget.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.cts.util.WidgetTestUtils;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.test.AndroidTestCase;
import android.util.Xml;
import android.widget.Switch;

import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Test {@link Switch}.
 */
public class SwitchTest extends AndroidTestCase {
    public void testConstructor() throws XmlPullParserException, IOException {
        new Switch(mContext);

        XmlResourceParser parser = mContext.getResources().getLayout(R.layout.switch_layout);
        WidgetTestUtils.beginDocument(parser, "Switch");

        new Switch(mContext, parser);

        new Switch(mContext, parser, 0);

        new Switch(mContext, parser, 0, 0);
    }

    public void testAccessThumbTint() throws XmlPullParserException, IOException {
        XmlResourceParser parser = mContext.getResources().getLayout(R.layout.switch_layout);
        WidgetTestUtils.beginDocument(parser, "Switch");
        Switch aSwitch = new Switch(mContext, parser);
        assertEquals(Color.WHITE, aSwitch.getThumbTintList().getDefaultColor());
        assertEquals(Mode.SRC_OVER, aSwitch.getThumbTintMode());

        ColorStateList colors = ColorStateList.valueOf(Color.RED);
        aSwitch.setThumbTintList(colors);
        aSwitch.setThumbTintMode(Mode.XOR);

        assertSame(colors, aSwitch.getThumbTintList());
        assertEquals(Mode.XOR, aSwitch.getThumbTintMode());
    }

    public void testAccessTrackTint() throws XmlPullParserException, IOException {
        XmlResourceParser parser = mContext.getResources().getLayout(R.layout.switch_layout);
        WidgetTestUtils.beginDocument(parser, "Switch");
        Switch aSwitch = new Switch(mContext, parser);
        assertEquals(Color.BLACK, aSwitch.getTrackTintList().getDefaultColor());
        assertEquals(Mode.SRC_ATOP, aSwitch.getTrackTintMode());

        ColorStateList colors = ColorStateList.valueOf(Color.RED);
        aSwitch.setTrackTintList(colors);
        aSwitch.setTrackTintMode(Mode.XOR);

        assertSame(colors, aSwitch.getTrackTintList());
        assertEquals(Mode.XOR, aSwitch.getTrackTintMode());
    }
}
