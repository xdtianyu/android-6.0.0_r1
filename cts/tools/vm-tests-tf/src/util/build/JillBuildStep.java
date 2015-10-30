/*
 * Copyright (C) 2013 The Android Open Source Project
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

package util.build;

import com.android.jill.Jill;
import com.android.jill.Main;
import com.android.jill.Options;

import java.io.File;

public class JillBuildStep extends BuildStep {

    JillBuildStep(BuildFile inputFile, BuildFile outputFile) {
        super(inputFile, outputFile);
    }

    @Override
    boolean build() {
        if (super.build()) {

            File outDir = outputFile.fileName.getParentFile();
            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("failed to create output dir: "
                        + outDir.getAbsolutePath());
                return false;
            }

            int args = 3;
            String[] commandLine = new String[args];
            commandLine[0] = "--output";
            commandLine[1] = outputFile.fileName.getAbsolutePath();
            commandLine[2] = inputFile.fileName.getAbsolutePath();

            try {
                Options options = Main.getOptions(commandLine);
                Jill.process(options);
            } catch (Throwable ex) {
                ex.printStackTrace();
                return false;
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            JillBuildStep other = (JillBuildStep) obj;

            return inputFile.equals(other.inputFile) && outputFile.equals(other.outputFile);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return inputFile.hashCode() ^ outputFile.hashCode();
    }
}
