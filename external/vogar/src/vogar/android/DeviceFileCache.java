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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;
import vogar.FileCache;
import vogar.Log;

public class DeviceFileCache implements FileCache {
    private final Log log;
    private final File cacheRoot;
    private AndroidSdk androidSdk;

    /** filled lazily */
    private Set<File> cachedFiles;

    public DeviceFileCache(Log log, File deviceDir, AndroidSdk androidSdk) {
        this.log = log;
        this.cacheRoot = new File(deviceDir, "md5-cache");
        this.androidSdk = androidSdk;
    }

    public boolean existsInCache(String key) {
        if (cachedFiles == null) {
            try {
                cachedFiles = new HashSet<File>();
                cachedFiles.addAll(androidSdk.deviceFilesystem.ls(cacheRoot));
                log.verbose("indexed on-device cache: " + cachedFiles.size() + " entries.");
            } catch (FileNotFoundException e) {
                // cacheRoot probably just hasn't been created yet.
                cachedFiles = new HashSet<File>();
            }
        }
        File cachedFile = new File(cacheRoot, key);
        return cachedFiles.contains(cachedFile);
    }

    public void copyFromCache(String key, File destination) {
        File cachedFile = new File(cacheRoot, key);
        androidSdk.cp(cachedFile, destination);
    }

    public void copyToCache(File source, String key) {
        File cachedFile = new File(cacheRoot, key);
        androidSdk.deviceFilesystem.mkdirs(cacheRoot);
        // Copy it onto the same file system first, then atomically move it into place.
        // That way, if we fail, we don't leave anything dangerous lying around.
        File temporary = new File(cachedFile + ".tmp");
        androidSdk.cp(source, temporary);
        androidSdk.mv(temporary, cachedFile);
    }
}
