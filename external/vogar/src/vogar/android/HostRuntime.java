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

import com.google.common.collect.Iterables;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vogar.Action;
import vogar.Classpath;
import vogar.Mode;
import vogar.ModeId;
import vogar.Run;
import vogar.Variant;
import vogar.commands.VmCommandBuilder;
import vogar.tasks.MkdirTask;
import vogar.tasks.RunActionTask;
import vogar.tasks.Task;

/**
 * Executes actions on a Dalvik or ART runtime on a Linux desktop.
 */
public final class HostRuntime implements Mode {
    private final Run run;
    private final ModeId modeId;
    private final Variant variant;

    public HostRuntime(Run run, ModeId modeId, Variant variant) {
        if (!modeId.isHost() || !modeId.supportsVariant(variant)) {
            throw new IllegalArgumentException("Unsupported mode:" + modeId +
                    " or variant: " + variant);
        }
        this.run = run;
        this.modeId = modeId;
        this.variant = variant;
    }

    @Override public Task executeActionTask(Action action, boolean useLargeTimeout) {
        return new RunActionTask(run, action, useLargeTimeout);
    }

    private File dalvikCache() {
        return run.localFile("android-data", run.dalvikCache);
    }

    @Override public Set<Task> installTasks() {
        Set<Task> result = new HashSet<Task>();
        for (File classpathElement : run.classpath.getElements()) {
            String name = run.basenameOfJar(classpathElement);
            result.add(new DexTask(run.androidSdk, run.classpath, run.benchmark, name,
                    classpathElement, null, run.localDexFile(name)));
        }
        result.add(new MkdirTask(run.mkdir, dalvikCache()));
        return result;
    }

    @Override public Set<Task> cleanupTasks(Action action) {
        return Collections.emptySet();
    }

    @Override public Set<Task> installActionTasks(Action action, File jar) {
        return Collections.<Task>singleton(new DexTask(run.androidSdk, Classpath.of(jar),
                run.benchmark, action.getName(), jar, action, run.localDexFile(action.getName())));
    }

    @Override public VmCommandBuilder newVmCommandBuilder(Action action, File workingDirectory) {
        String buildRoot = System.getenv("ANDROID_BUILD_TOP");

        List<File> jars = new ArrayList<File>();
        for (String jar : modeId.getJarNames()) {
            jars.add(new File(buildRoot, "out/host/linux-x86/framework/" + jar + ".jar"));
        }
        Classpath bootClasspath = Classpath.of(jars);

        String libDir = buildRoot + "/out/host/linux-x86";
        if (variant == Variant.X32) {
            libDir += "/lib";
        } else if (variant == Variant.X64) {
            libDir += "/lib64";
        } else {
            throw new AssertionError("Unsupported variant:" + variant);
        }

        List<String> vmCommand = new ArrayList<String>();
        vmCommand.addAll(run.target.targetProcessPrefix());
        vmCommand.add("ANDROID_PRINTF_LOG=tag");
        vmCommand.add("ANDROID_LOG_TAGS=*:i");
        vmCommand.add("ANDROID_DATA=" + dalvikCache().getParent());
        vmCommand.add("ANDROID_ROOT=" + buildRoot + "/out/host/linux-x86");
        vmCommand.add("LD_LIBRARY_PATH=" + libDir);
        vmCommand.add("DYLD_LIBRARY_PATH=" + libDir);
        Iterables.addAll(vmCommand, run.invokeWith());
        vmCommand.add(buildRoot + "/out/host/linux-x86/bin/" + run.vmCommand);

        VmCommandBuilder builder = new VmCommandBuilder(run.log);

        // If you edit this, see also DeviceRuntime...
        builder.vmCommand(vmCommand)
                .vmArgs("-Xbootclasspath:" + bootClasspath.toString())
                .vmArgs("-Duser.language=en")
                .vmArgs("-Duser.region=US");
        if (!run.benchmark) {
            if (modeId == ModeId.HOST_DALVIK) {
              // Historically, vogar has turned off these options for Dalvik.
              builder.vmArgs("-Xverify:none");
              builder.vmArgs("-Xdexopt:none");
            }
            builder.vmArgs("-Xcheck:jni");
        }
        if (modeId == ModeId.HOST_ART_KITKAT) {
            // Required for KitKat to select the ART runtime. Default is Dalvik.
            builder.vmArgs("-XXlib:libart.so");
        }
        // dalvikvm defaults to no limit, but the framework sets the limit at 2000.
        builder.vmArgs("-Xjnigreflimit:2000");
        return builder;
    }

    @Override public Classpath getRuntimeClasspath(Action action) {
        Classpath result = new Classpath();
        result.addAll(run.localDexFile(action.getName()));
        for (File classpathElement : run.classpath.getElements()) {
            result.addAll(run.localDexFile(run.basenameOfJar(classpathElement)));
        }
        result.addAll(run.resourceClasspath);
        return result;
    }
}
