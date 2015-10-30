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

package android.databinding;

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.maven.MavenPom
import org.gradle.api.file.FileCollection
import org.gradle.api.publication.maven.internal.ant.DefaultGroovyMavenDeployer
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import groovy.json.JsonSlurper;

public class UploadToBintrayTask extends DefaultTask {
    static final String API_URL = "https://api.bintray.com";
    static final String PACKAGE_HEADER = "X-Bintray-Package"
    static final String VERSION_HEADER = "X-Bintray-Version"
    static final String PUBLISH_HEADER = "X-Bintray-Publish"
    static final String OVERRIDE_HEADER = "X-Bintray-Override"
    static final String EXPLODE_HEADER = "X-Bintray-Explode"
    static final String CONTENT_PREFIX = "content/android/android-tools"
    String username;
    String apiKey;
    String pkg;
    String version;
    FileCollection localFiles;
    String mavenRepoAbsPath;
    String targetPath;
    public void configureFrom(DefaultGroovyMavenDeployer deployer) {
        String repoUrl = deployer.repository.url
        if (repoUrl == null) {
            throw new RuntimeException("Cannot find repo url for $deployer")
        }

        def pom = deployer.pom
        pkg = pom.groupId + "." + pom.artifactId
        version = pom.version
        mavenRepoAbsPath = repoUrl
        targetPath = "${pkg.replaceAll("\\.", "/")}/${version}"
        localFiles = project.fileTree(mavenRepoAbsPath + "/" + targetPath)
    }
    @TaskAction
    public void upload() {
        if (username == null || apiKey == null) {
            throw new IllegalArgumentException("You should pass your bintray user and " +
                    "api key as params e.g. ./gradlew ${BintrayPlugin.DEFAULT_TASK_NAME}" +
                    " -P${BintrayPlugin.USER_PROP}=<my username>" +
                    " -P${BintrayPlugin.API_KEY_PROP}=<my api key>")
        }
        println(log())
        for (File localFile : localFiles) {
            def p = ['curl', '-u', "$username:$apiKey", "-H",
                     "$PACKAGE_HEADER: $pkg", "-H", "$VERSION_HEADER: $version",
                     "-X", "PUT", "--data-binary", "@${localFile.getAbsolutePath()}",
                     "$API_URL/$CONTENT_PREFIX/$targetPath/${localFile.name}"]
            println("executing $p")
            def execute = p.execute()
            execute.waitFor()
            if (execute.exitValue() != 0) {
                throw new RuntimeException("failed to upload artifact. error: ${execute.err.text}")
            }
            def responseText = execute.text
            def json = new JsonSlurper().parseText(responseText)
            if (json.getAt("message") != "success") {
                throw new RuntimeException("Cannot upload artifact. Error response: " +
                        "${json.getAt("message")}")
            }
            println("uploaded $localFile")
        }
    }

    public String log() {
        return "UploadToBintrayTask{" +
                "username='" + username + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", pkg='" + pkg + '\'' +
                ", version='" + version + '\'' +
                ", localFile=" + localFiles +
                ", mavenRepo=" + mavenRepoAbsPath +
                '}';
    }
}
