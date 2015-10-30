/*
 * Copyright (C) 2010 The Android Open Source Project
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

package vogar;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vogar.commands.Command;

/**
 * Run tests on the host machine.
 */
public final class LocalTarget extends Target {
    private final Run run;

    public LocalTarget(Run run) {
        this.run = run;
    }

    @Override public File defaultDeviceDir() {
        return new File("/tmp/vogar");
    }

    @Override public List<String> targetProcessPrefix() {
        return ImmutableList.of("sh", "-c");
    }

    @Override public String getDeviceUserName() {
        throw new UnsupportedOperationException();
    }

    @Override public void await(File nonEmptyDirectory) {
    }

    @Override public void rm(File file) {
        run.rm.file(file);
    }

    @Override public List<File> ls(File directory) {
        return Arrays.asList(directory.listFiles());
    }

    @Override public void mkdirs(File file) {
        run.mkdir.mkdirs(file);
    }

    @Override public void forwardTcp(int port) {
        // do nothing
    }

    @Override public void push(File local, File remote) {
        if (remote.equals(local)) {
            return;
        }
        // if the user dir exists, cp would copy the files to the wrong place
        if (remote.exists()) {
            throw new IllegalStateException();
        }
        new Command(run.log, "cp", "-r", local.toString(), remote.toString()).execute();
    }

    @Override public void pull(File remote, File local) {
        new Command(run.log, "cp", remote.getPath(), local.getPath()).execute();
    }
}
