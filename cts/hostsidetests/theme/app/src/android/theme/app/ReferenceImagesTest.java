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

package android.theme.app;

import android.test.ActivityInstrumentationTestCase2;

import java.io.File;

/**
 * Activity test case used to instrument generation of reference images.
 */
public class ReferenceImagesTest extends ActivityInstrumentationTestCase2<GenerateImagesActivity> {

    public ReferenceImagesTest() {
        super(GenerateImagesActivity.class);
    }

    public void testGenerateReferenceImages() throws Exception {
        setActivityInitialTouchMode(true);

        final GenerateImagesActivity activity = getActivity();
        activity.waitForCompletion();

        assertTrue(activity.getFinishReason(), activity.isFinishSuccess());

        final File outputDir = activity.getOutputDir();
        final File outputZip = new File(outputDir.getParentFile(), outputDir.getName() + ".zip");
        if (outputZip.exists()) {
            // Remove any old test results.
            outputZip.delete();
        }

        ThemeTestUtils.compressDirectory(outputDir, outputZip);
        ThemeTestUtils.deleteDirectory(outputDir);

        assertTrue("Generated reference image ZIP", outputZip.exists());
    }
}
