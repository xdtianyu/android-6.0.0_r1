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

package vogar.android;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vogar.Run;
import vogar.Target;
import vogar.commands.Command;

public final class AdbTarget extends Target {
    private final Run run;

    public AdbTarget(Run run) {
        this.run = run;
    }

    @Override public File defaultDeviceDir() {
        return new File("/data/local/tmp/vogar");
    }

    @Override public List<String> targetProcessPrefix() {
        return ImmutableList.of("adb", "shell");
    }

    // TODO: pull the methods from androidsdk into here

    @Override public void await(File directory) {
        run.androidSdk.waitForDevice();
        run.androidSdk.ensureDirectory(directory);
        run.androidSdk.remount();
    }

    @Override public List<File> ls(File directory) throws FileNotFoundException {
        return run.androidSdk.deviceFilesystem.ls(directory);
    }

    @Override public String getDeviceUserName() {
        // TODO: move this to device set up
        // The default environment doesn't include $USER, so dalvikvm doesn't set "user.name".
        // DeviceDalvikVm uses this to set "user.name" manually with -D.
        String line = new Command(run.log, "adb", "shell", "id").execute().get(0);
        // TODO: use 'id -un' when we don't need to support anything older than M
        Matcher m = Pattern.compile("^uid=\\d+\\((\\S+)\\) gid=\\d+\\(\\S+\\).*").matcher(line);
        return m.matches() ? m.group(1) : "root";
    }

    @Override public void rm(File file) {
        run.androidSdk.rm(file);
    }

    @Override public void mkdirs(File file) {
        run.androidSdk.deviceFilesystem.mkdirs(file);
    }

    @Override public void forwardTcp(int port) {
        run.androidSdk.forwardTcp(port);
    }

    @Override public void push(File local, File remote) {
        run.androidSdk.push(local, remote);
    }

    @Override public void pull(File remote, File local) {
        run.androidSdk.pull(remote, local);
    }
}
