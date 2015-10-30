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
package android.databinding

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publication.maven.internal.ant.DefaultGroovyMavenDeployer
import org.gradle.api.tasks.Upload

class BintrayPlugin implements Plugin<Project> {
    public static final USER_PROP = "bintrayUser"
    public static final API_KEY_PROP = "bintrayApiKey"
    public static final DEFAULT_TASK_NAME = "uploadToBintray"

    @Override
    void apply(Project target) {
        def taskName = DEFAULT_TASK_NAME
        String user = target.getProperties().get(USER_PROP)
        String apiKey = target.getProperties().get(API_KEY_PROP)
        def uploadArchivesTask = target.tasks.findByName("uploadArchives")
        if (uploadArchivesTask == null) {
            throw new RuntimeException("Cannot find uploadArchives task in $target")
        }
        Upload uploadTask = uploadArchivesTask
        def bintrayTask = target.tasks.create(taskName, UploadToBintrayTask, {
            it.dependsOn uploadTask
            it.apiKey = apiKey
            it.username = user
        })
        target.afterEvaluate({
            def mavenDeployerRepo = uploadTask.repositories.findByName("mavenDeployer")
            if (mavenDeployerRepo == null) {
                throw new RuntimeException("Cannot find maven deployer repository")
            }
            DefaultGroovyMavenDeployer mavenDeployer = mavenDeployerRepo
            mavenDeployer.pom.whenConfigured({
                bintrayTask.configureFrom(mavenDeployer)
            })
        })


    }
}
