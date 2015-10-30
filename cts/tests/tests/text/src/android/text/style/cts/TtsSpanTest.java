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

package android.text.style.cts;

import android.os.Parcel;
import android.os.PersistableBundle;
import android.text.style.TtsSpan;
import junit.framework.TestCase;

public class TtsSpanTest extends TestCase {

    PersistableBundle bundle;

    protected void setUp() {
        bundle = new PersistableBundle();
        bundle.putString("argument.one", "value.one");
        bundle.putString("argument.two", "value.two");
        bundle.putLong("argument.three", 3);
        bundle.putLong("argument.four", 4);
    }

    public void testGetArgs() {
        TtsSpan t = new TtsSpan("test.type.one", bundle);
        PersistableBundle args = t.getArgs();
        assertEquals(4, args.size());
        assertEquals("value.one", args.getString("argument.one"));
        assertEquals("value.two", args.getString("argument.two"));
        assertEquals(3, args.getLong("argument.three"));
        assertEquals(4, args.getLong("argument.four"));
    }

    public void testGetType() {
        TtsSpan t = new TtsSpan("test.type.two", bundle);
        assertEquals("test.type.two", t.getType());
    }

    public void testDescribeContents() {
        TtsSpan span = new TtsSpan("test.type.three", bundle);
        span.describeContents();
    }

    public void testGetSpanTypeId() {
        TtsSpan span = new TtsSpan("test.type.four", bundle);
        span.getSpanTypeId();
    }

    public void testWriteAndReadParcel() {
        Parcel p = Parcel.obtain();
        try {
            TtsSpan span = new TtsSpan("test.type.five", bundle);
            span.writeToParcel(p, 0);
            p.setDataPosition(0);

            TtsSpan t = new TtsSpan(p);

            assertEquals("test.type.five", t.getType());
            PersistableBundle args = t.getArgs();
            assertEquals(4, args.size());
            assertEquals("value.one", args.getString("argument.one"));
            assertEquals("value.two", args.getString("argument.two"));
            assertEquals(3, args.getLong("argument.three"));
            assertEquals(4, args.getLong("argument.four"));
        } finally {
            p.recycle();
        }
    }
}
