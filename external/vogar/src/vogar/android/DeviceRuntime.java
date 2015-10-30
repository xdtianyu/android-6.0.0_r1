/*
 * Copyright (C) 2009 The Android Open Source Project
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
import vogar.Variant;
import vogar.Classpath;
import vogar.Mode;
import vogar.ModeId;
import vogar.Run;
import vogar.commands.VmCommandBuilder;
import vogar.tasks.RunActionTask;
import vogar.tasks.Task;

/**
 * Execute actions on an Android device or emulator using "app_process" or the runtime directly.
 */
public final class DeviceRuntime implements Mode {
    private final Run run;
    private final ModeId modeId;

  public DeviceRuntime(Run run, ModeId modeId, Variant variant) {
        if (!modeId.isDevice() || !modeId.supportsVariant(variant)) {
            throw new IllegalArgumentException("Unsupported mode:" + modeId +
                    " or variant: " + variant);
        }
        this.run = run;
        this.modeId = modeId;
    }

    @Override public Set<Task> installTasks() {
        Set<Task> result = new HashSet<Task>();
        // dex everything on the classpath and push it to the device.
        for (File classpathElement : run.classpath.getElements()) {
            dexAndPush(result, run.basenameOfJar(classpathElement),
                    classpathElement, null);
        }
        return result;
    }

    @Override public Set<Task> installActionTasks(Action action, File jar) {
        Set<Task> result = new HashSet<Task>();
        dexAndPush(result, action.getName(), jar, action);
        return result;
    }

    @Override public Task executeActionTask(Action action, boolean useLargeTimeout) {
        return new RunActionTask(run, action, useLargeTimeout);
    }

    private void dexAndPush(Set<Task> tasks, String name, File jar, Action action) {
        File localDex = run.localDexFile(name);
        File deviceDex = run.targetDexFile(name);
        Task dex = new DexTask(run.androidSdk, run.classpath, run.benchmark, name, jar, action,
                localDex);
        tasks.add(dex);
        tasks.add(run.target.pushTask(localDex, deviceDex).afterSuccess(dex));
    }

    @Override public VmCommandBuilder newVmCommandBuilder(Action action, File workingDirectory) {
        List<String> vmCommand = new ArrayList<String>();
        vmCommand.addAll(run.target.targetProcessPrefix());
        vmCommand.add("cd");
        vmCommand.add(workingDirectory.getAbsolutePath());
        vmCommand.add("&&");
        vmCommand.add(run.getAndroidData());
        Iterables.addAll(vmCommand, run.invokeWith());
        vmCommand.add(run.vmCommand);

        // If you edit this, see also HostRuntime...
        VmCommandBuilder vmCommandBuilder = new VmCommandBuilder(run.log)
                .vmCommand(vmCommand)
                .vmArgs("-Duser.home=" + run.deviceUserHome)
                .maxLength(1024);
        if (modeId == ModeId.APP_PROCESS) {
            return vmCommandBuilder
                .vmArgs(action.getUserDir().getPath())
                .classpathViaProperty(true);
        }

        vmCommandBuilder
                .vmArgs("-Duser.name=" + run.target.getDeviceUserName())
                .vmArgs("-Duser.language=en")
                .vmArgs("-Duser.region=US");

        if (modeId == ModeId.DEVICE_ART_KITKAT) {
            // Required for KitKat to select the ART runtime. Default is Dalvik.
            vmCommandBuilder.vmArgs("-XXlib:libart.so");
        }
        if (!run.benchmark) {
            if (modeId == ModeId.DEVICE_DALVIK) {
              // Historically, vogar has turned off these options for Dalvik.
              vmCommandBuilder.vmArgs("-Xverify:none");
              vmCommandBuilder.vmArgs("-Xdexopt:none");
            }
            vmCommandBuilder.vmArgs("-Xcheck:jni");
        }
        // dalvikvm defaults to no limit, but the framework sets the limit at 2000.
        vmCommandBuilder.vmArgs("-Xjnigreflimit:2000");
        return vmCommandBuilder;
    }

    @Override public Set<Task> cleanupTasks(Action action) {
        return Collections.singleton(run.target.rmTask(action.getUserDir()));
    }

    @Override public Classpath getRuntimeClasspath(Action action) {
        Classpath result = new Classpath();
        result.addAll(run.targetDexFile(action.getName()));
        if (!run.benchmark) {
            for (File classpathElement : run.classpath.getElements()) {
                result.addAll(run.targetDexFile(run.basenameOfJar(classpathElement)));
            }
        }
        // Note we intentionally do not add run.resourceClasspath on
        // the device since it contains host path names.
        return result;
    }
}
