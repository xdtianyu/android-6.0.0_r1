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

package android.media.tv.cts;

import android.media.tv.TvContentRating;

import java.util.List;

import junit.framework.TestCase;

/**
 * Test for {@link android.media.tv.TvContentRating}.
 */
public class TvContentRatingTest extends TestCase {

    public void testCreateRating() throws Exception {
        final String DOMAIN = "android.media.tv";
        final String RATING_SYSTEM = "US_TVPG";
        final String MAIN_RATING = "US_TVPG_TV_MA";
        final String SUB_RATING_1 = "US_TVPG_TV_S";
        final String SUB_RATING_2 = "US_TVPG_TV_V";

        TvContentRating rating = TvContentRating.createRating(DOMAIN, RATING_SYSTEM, MAIN_RATING,
                SUB_RATING_1, SUB_RATING_2);
        assertEquals(DOMAIN, rating.getDomain());
        assertEquals(RATING_SYSTEM, rating.getRatingSystem());
        assertEquals(MAIN_RATING, rating.getMainRating());
        List<String> subRatings = rating.getSubRatings();
        assertEquals(2, subRatings.size());
        assertTrue("Sub-ratings does not contain " + SUB_RATING_1,
                subRatings.contains(SUB_RATING_1));
        assertTrue("Sub-ratings does not contain " + SUB_RATING_2,
                subRatings.contains(SUB_RATING_2));
    }

    public void testFlattenAndUnflatten() throws Exception {
        final String DOMAIN = "android.media.tv";
        final String RATING_SYSTEM = "US_TVPG";
        final String MAIN_RATING = "US_TVPG_TV_MA";
        final String SUB_RATING_1 = "US_TVPG_TV_S";
        final String SUB_RATING_2 = "US_TVPG_TV_V";

        String flattened = TvContentRating.createRating(DOMAIN, RATING_SYSTEM, MAIN_RATING,
                SUB_RATING_1, SUB_RATING_2).flattenToString();
        TvContentRating rating = TvContentRating.unflattenFromString(flattened);

        assertEquals(DOMAIN, rating.getDomain());
        assertEquals(RATING_SYSTEM, rating.getRatingSystem());
        assertEquals(MAIN_RATING, rating.getMainRating());
        List<String> subRatings = rating.getSubRatings();
        assertEquals(2, subRatings.size());
        assertTrue("Sub-ratings does not contain " + SUB_RATING_1,
                subRatings.contains(SUB_RATING_1));
        assertTrue("Sub-ratings does not contain " + SUB_RATING_2,
                subRatings.contains(SUB_RATING_2));
    }
}
