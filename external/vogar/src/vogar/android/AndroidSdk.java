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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import vogar.Classpath;
import vogar.HostFileCache;
import vogar.Log;
import vogar.Md5Cache;
import vogar.ModeId;
import vogar.commands.Command;
import vogar.commands.Mkdir;
import vogar.util.Strings;

/**
 * Android SDK commands such as adb, aapt and dx.
 */
public class AndroidSdk {

    // $BOOTCLASSPATH defined by system/core/rootdir/init.rc
    public static final String[] BOOTCLASSPATH = new String[] { "core-libart",
                                                                "conscrypt",
                                                                "okhttp",
                                                                "core-junit",
                                                                "bouncycastle",
                                                                "ext",
                                                                "framework",
                                                                "telephony-common",
                                                                "mms-common",
                                                                "framework",
                                                                "android.policy",
                                                                "services",
                                                                "apache-xml"};


    public static final String[] HOST_BOOTCLASSPATH = new String[] {
            "core-libart-hostdex",
            "conscrypt-hostdex",
            "okhttp-hostdex",
            "bouncycastle-hostdex",
            "apache-xml-hostdex",
    };

    private final Log log;
    private final Mkdir mkdir;
    private final File[] compilationClasspath;
    public final DeviceFilesystem deviceFilesystem;

    private Md5Cache dexCache;
    private Md5Cache pushCache;

    public static Collection<File> defaultExpectations() {
        File[] files = new File("libcore/expectations").listFiles(new FilenameFilter() {
            // ignore obviously temporary files
            public boolean accept(File dir, String name) {
                return !name.endsWith("~") && !name.startsWith(".");
            }
        });
        return (files != null) ? Arrays.asList(files) : Collections.<File>emptyList();
    }

    public AndroidSdk(Log log, Mkdir mkdir, ModeId modeId) {
        this.log = log;
        this.mkdir = mkdir;
        this.deviceFilesystem = new DeviceFilesystem(log, "adb", "shell");

        List<String> path = new Command(log, "which", "adb").execute();
        if (path.isEmpty()) {
            throw new RuntimeException("adb not found");
        }
        File adb = new File(path.get(0)).getAbsoluteFile();
        String parentFileName = adb.getParentFile().getName();

        /*
         * We probably get aapt/adb/dx from either a copy of the Android SDK or a copy
         * of the Android source code. In either case, all three tools are in the same
         * directory as each other.
         *
         * Android SDK >= v9 (gingerbread):
         *  <sdk>/platform-tools/aapt
         *  <sdk>/platform-tools/adb
         *  <sdk>/platform-tools/dx
         *  <sdk>/platforms/android-?/android.jar
         *
         * Android build tree (target):
         *  <source>/out/host/linux-x86/bin/aapt
         *  <source>/out/host/linux-x86/bin/adb
         *  <source>/out/host/linux-x86/bin/dx
         *  <source>/out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes.jar
         */

        if ("platform-tools".equals(parentFileName)) {
            File sdkRoot = adb.getParentFile().getParentFile();
            File newestPlatform = getNewestPlatform(sdkRoot);
            log.verbose("using android platform: " + newestPlatform);
            compilationClasspath = new File[] { new File(newestPlatform, "android.jar") };
            log.verbose("using android sdk: " + sdkRoot);
        } else if ("bin".equals(parentFileName)) {
            File sourceRoot = adb.getParentFile().getParentFile()
                    .getParentFile().getParentFile().getParentFile();
            log.verbose("using android build tree: " + sourceRoot);

            String pattern = "out/target/common/obj/JAVA_LIBRARIES/%s_intermediates/classes.jar";
            if (modeId.isHost()) {
                pattern = "out/host/common/obj/JAVA_LIBRARIES/%s_intermediates/classes.jar";
            }

            String[] jarNames = modeId.getJarNames();
            compilationClasspath = new File[jarNames.length];
            for (int i = 0; i < jarNames.length; i++) {
                String jar = jarNames[i];
                compilationClasspath[i] = new File(sourceRoot, String.format(pattern, jar));
            }
        } else {
            throw new RuntimeException("Couldn't derive Android home from " + adb);
        }
    }

    /**
     * Returns the platform directory that has the highest API version. API
     * platform directories are named like "android-9" or "android-11".
     */
    private File getNewestPlatform(File sdkRoot) {
        File newestPlatform = null;
        int newestPlatformVersion = 0;
        for (File platform : new File(sdkRoot, "platforms").listFiles()) {
            try {
                int version = Integer.parseInt(platform.getName().substring("android-".length()));
                if (version > newestPlatformVersion) {
                    newestPlatform = platform;
                    newestPlatformVersion = version;
                }
            } catch (NumberFormatException ignore) {
                // Ignore non-numeric preview versions like android-Honeycomb
            }
        }
        return newestPlatform;
    }

    public static Collection<File> defaultSourcePath() {
        return filterNonExistentPathsFrom("libcore/support/src/test/java",
                                          "external/mockwebserver/src/main/java/");
    }

    private static Collection<File> filterNonExistentPathsFrom(String... paths) {
        ArrayList<File> result = new ArrayList<File>();
        String buildRoot = System.getenv("ANDROID_BUILD_TOP");
        for (String path : paths) {
            File file = new File(buildRoot, path);
            if (file.exists()) {
                result.add(file);
            }
        }
        return result;
    }

    public File[] getCompilationClasspath() {
        return compilationClasspath;
    }

    public void setCaches(HostFileCache hostFileCache, DeviceFileCache deviceCache) {
        this.dexCache = new Md5Cache(log, "dex", hostFileCache);
        this.pushCache = new Md5Cache(log, "pushed", deviceCache);
    }

    /**
     * Converts all the .class files on 'classpath' into a dex file written to 'output'.
     */
    public void dex(File output, Classpath classpath) {
        mkdir.mkdirs(output.getParentFile());

        String key = dexCache.makeKey(classpath);
        if (key != null) {
            boolean cacheHit = dexCache.getFromCache(output, key);
            if (cacheHit) {
                log.verbose("dex cache hit for " + classpath);
                return;
            }
        }

        /*
         * We pass --core-library so that we can write tests in the
         * same package they're testing, even when that's a core
         * library package. If you're actually just using this tool to
         * execute arbitrary code, this has the unfortunate
         * side-effect of preventing "dx" from protecting you from
         * yourself.
         *
         * Memory options pulled from build/core/definitions.mk to
         * handle large dx input when building dex for APK.
         */
        new Command.Builder(log)
                .args("dx")
                .args("-JXms16M")
                .args("-JXmx1536M")
                .args("--dex")
                .args("--output=" + output)
                .args("--core-library")
                .args((Object[]) Strings.objectsToStrings(classpath.getElements())).execute();
        dexCache.insert(key, output);
    }

    public void packageApk(File apk, File manifest) {
        List<String> aapt = new ArrayList<String>(Arrays.asList("aapt",
                                                                "package",
                                                                "-F", apk.getPath(),
                                                                "-M", manifest.getPath(),
                                                                "-I", "prebuilts/sdk/current/android.jar"));
        new Command(log, aapt).execute();
    }

    public void addToApk(File apk, File dex) {
        new Command(log, "aapt", "add", "-k", apk.getPath(), dex.getPath()).execute();
    }

    public void mv(File source, File destination) {
        new Command(log, "adb", "shell", "mv", source.getPath(), destination.getPath()).execute();
    }

    public void rm(File name) {
        new Command(log, "adb", "shell", "rm", "-r", name.getPath()).execute();
    }

    public void cp(File source, File destination) {
        // adb doesn't support "cp" command directly
        new Command(log, "adb", "shell", "cat", source.getPath(), ">", destination.getPath())
                .execute();
    }

    public void pull(File remote, File local) {
        new Command(log, "adb", "pull", remote.getPath(), local.getPath()).execute();
    }

    public void push(File local, File remote) {
        Command fallback = new Command(log, "adb", "push", local.getPath(), remote.getPath());
        deviceFilesystem.mkdirs(remote.getParentFile());
        // don't yet cache directories (only used by jtreg tests)
        if (pushCache != null && local.isFile()) {
            String key = pushCache.makeKey(local);
            boolean cacheHit = pushCache.getFromCache(remote, key);
            if (cacheHit) {
                log.verbose("device cache hit for " + local);
                return;
            }
            fallback.execute();
            pushCache.insert(key, remote);
        } else {
            fallback.execute();
        }
    }

    public void install(File apk) {
        new Command(log, "adb", "install", "-r", apk.getPath()).execute();
    }

    public void uninstall(String packageName) {
        new Command(log, "adb", "uninstall", packageName).execute();
    }

    public void forwardTcp(int port) {
        new Command(log, "adb", "forward", "tcp:" + port, "tcp:" + port).execute();
    }

    public void remount() {
        new Command(log, "adb", "remount").execute();
    }

    public void waitForDevice() {
        new Command(log, "adb", "wait-for-device").execute();
    }

    /**
     * Make sure the directory exists.
     */
    public void ensureDirectory(File path) {
        String pathArgument = path.getPath() + "/";
        if (pathArgument.equals("/sdcard/")) {
            // /sdcard is a mount point. If it exists but is empty we do
            // not want to use it. So we wait until it is not empty.
            waitForNonEmptyDirectory(pathArgument, 5 * 60);
        } else {
            Command command = new Command(log, "adb", "shell", "ls", pathArgument);
            List<String> output = command.execute();
            // TODO: We should avoid checking for the error message, and instead have
            // the Command class understand a non-zero exit code from an adb shell command.
            if (!output.isEmpty()
                && output.get(0).equals(pathArgument + ": No such file or directory")) {
                throw new RuntimeException("'" + pathArgument + "' does not exist on device");
            }
            // Otherwise the directory exists.
        }
    }

    private void waitForNonEmptyDirectory(String pathArgument, int timeoutSeconds) {
        final int millisPerSecond = 1000;
        final long start = System.currentTimeMillis();
        final long deadline = start + (millisPerSecond * timeoutSeconds);

        while (true) {
            final int remainingSeconds =
                    (int) ((deadline - System.currentTimeMillis()) / millisPerSecond);
            Command command = new Command(log, "adb", "shell", "ls", pathArgument);
            List<String> output;
            try {
                output = command.executeWithTimeout(remainingSeconds);
            } catch (TimeoutException e) {
                throw new RuntimeException("Timed out after " + timeoutSeconds
                                           + " seconds waiting for " + pathArgument, e);
            }
            try {
                Thread.sleep(millisPerSecond);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // We just want any output.
            if (!output.isEmpty()) {
                return;
            }

            log.warn("Waiting on " + pathArgument + " to be mounted ");
        }
    }
}
