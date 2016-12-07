/*
 * Gradle Openshift Plugin
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Iain Adams
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.iadams.gradle.openshift.tasks

import io.fabric8.kubernetes.api.model.KubernetesListBuilder
import io.fabric8.openshift.api.model.Build
import io.fabric8.openshift.api.model.BuildConfig
import io.fabric8.openshift.api.model.BuildOutput
import io.fabric8.openshift.api.model.BuildOutputBuilder
import io.fabric8.openshift.api.model.BuildStrategy
import io.fabric8.openshift.api.model.BuildStrategyBuilder
import io.fabric8.openshift.api.model.ImageStream
import io.fabric8.openshift.api.model.ImageStreamBuilder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

class StartBuildTask extends AbstractOpenshiftTask {

  @Input
  String buildName

  @InputFile
  File dockerTar

  @Input
  @Optional
  boolean watch = false

  StartBuildTask() {
    super('Starts a build for a given buildName')
  }

  @Override
  void executeAction() {

    checkOrCreateImageStream()
    checkOrCreateBuildConfig()

    Build b = client.buildConfigs()
      .withName(getBuildName())
      .instantiateBinary()
      .fromFile(getDockerTar())

    if (watch) {
      //TODO wait for build to finish
    }
  }

  private checkOrCreateBuildConfig(){

    BuildStrategy buildStrategy = new BuildStrategyBuilder().withType("Docker").build()
    BuildOutput buildOutput = new BuildOutputBuilder().withNewTo()
        .withKind("ImageStreamTag")
        .withName("${getBuildName()}:${project.version}")
      .endTo().build()

    BuildConfig buildConfig = client.buildConfigs().withName(getBuildName()).get()
    if (buildConfig == null) {
      createBuildConfig(buildName, buildStrategy, buildOutput)
    }
  }

  private void checkOrCreateImageStream() {
    if (client.imageStreams().withName(getBuildName()).get() == null) {
      logger.lifecycle("Creating ImageStream ${getBuildName()}")
      client.imageStreams().createNew()
        .withNewMetadata()
         .withName(getBuildName())
        .endMetadata()
      .done()
    }
  }

  private void createBuildConfig(String buildName, BuildStrategy buildStrategy, BuildOutput buildOutput) {
    logger.lifecycle("Creating BuildConfig ${buildName}")
    client.buildConfigs().createNew()
      .withNewMetadata()
        .withName(buildName)
      .endMetadata()
      .withNewSpec()
        .withNewSource()
          .withType("Binary")
        .endSource()
        .withStrategy(buildStrategy)
        .withOutput(buildOutput)
      .endSpec()
    .done()
  }
}
