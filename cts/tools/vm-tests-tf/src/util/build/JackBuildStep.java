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

import com.android.jack.Jack;
import com.android.jack.Main;
import com.android.jack.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JackBuildStep extends SourceBuildStep {

    private final String destPath;
    private final String classPath;
    private final Set<String> sourceFiles = new HashSet<String>();

    public JackBuildStep(String destPath, String classPath) {
        this.destPath = destPath;
        this.classPath = classPath;
    }

    @Override
    public void addSourceFile(String sourceFile) {
        sourceFiles.add(sourceFile);
    }

    @Override
    boolean build() {
        if (super.build()) {
            if (sourceFiles.isEmpty()) {
                return true;
            }

            File outDir = new File(destPath).getParentFile();
            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("failed to create output dir: "
                        + outDir.getAbsolutePath());
                return false;
            }
            List<String> commandLine = new ArrayList(4 + sourceFiles.size());
            commandLine.add("--verbose");
            commandLine.add("error");
            commandLine.add("--classpath");
            commandLine.add(classPath);
            commandLine.add("--output-jack");
            commandLine.add(destPath);
            commandLine.addAll(sourceFiles);

            try {
                Options options = Main.parseCommandLine(commandLine);
                Jack.checkAndRun(options);
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
            JackBuildStep other = (JackBuildStep) obj;
            return destPath.equals(other.destPath) && classPath.equals(other.classPath)
                    && sourceFiles.equals(other.sourceFiles);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return destPath.hashCode() ^ classPath.hashCode() ^ sourceFiles.hashCode();
    }
}
