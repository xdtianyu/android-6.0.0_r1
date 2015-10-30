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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import vogar.tasks.Task;

/**
 * A target runtime environment such as a remote device or the local host
 */
public abstract class Target {
    public abstract List<String> targetProcessPrefix();
    public abstract File defaultDeviceDir();
    public abstract String getDeviceUserName();

    public abstract List<File> ls(File directory) throws FileNotFoundException;
    public abstract void await(File nonEmptyDirectory);
    public abstract void rm(File file);
    public abstract void mkdirs(File file);
    public abstract void forwardTcp(int port);
    public abstract void push(File local, File remote);
    public abstract void pull(File remote, File local);

    public final Task pushTask(final File local, final File remote) {
        return new Task("push " + remote) {
            @Override protected Result execute() throws Exception {
                push(local, remote);
                return Result.SUCCESS;
            }
        };
    }

    public final Task rmTask(final File remote) {
        return new Task("rm " + remote) {
            @Override protected Result execute() throws Exception {
                rm(remote);
                return Result.SUCCESS;
            }
        };
    }
}
