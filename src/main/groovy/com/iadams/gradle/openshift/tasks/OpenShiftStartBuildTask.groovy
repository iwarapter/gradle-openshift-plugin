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

import com.iadams.gradle.openshift.utils.Builds
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.LogWatch
import io.fabric8.openshift.api.model.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

class OpenShiftStartBuildTask extends AbstractOpenshiftTask {

  @Input
  String buildName

  @InputFile
  File dockerTar

  @Input
  @Optional
  boolean watch = false

  @Input
  @Optional
  boolean binary = false

  @Internal
  String lastBuildStatus

    OpenShiftStartBuildTask() {
    super('Starts a build for a given deploymentConfigName')
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
      waitForOpenShiftBuildToComplete(b)
    }
  }

  private void waitForOpenShiftBuildToComplete(Build build) throws GradleException {
    final CountDownLatch latch = new CountDownLatch(1)
    final CountDownLatch logTerminateLatch = new CountDownLatch(1)
    final AtomicReference<Build> buildHolder = new AtomicReference<>()
    String buildName = getName(build)
    Watcher<Build> buildWatcher = new Watcher<Build>() {
      @Override
      void eventReceived(Watcher.Action action, Build resource) {
        buildHolder.set(resource)
        if (isBuildCompleted(resource)) {
          latch.countDown()
        }
      }

      @Override
      void onClose(KubernetesClientException cause) {
      }
    }
    logger.info("Waiting for build " + buildName + " to complete...")
    LogWatch logWatch = client.pods().withName(buildName + "-build").watchLog()
    watchLogInThread(logWatch, "Failed to tail build log", logTerminateLatch)

    Watch watcher = client.builds().withName(buildName).watch(buildWatcher)
    while (latch.getCount() > 0L) {
      try {
        latch.await()
      } catch (InterruptedException e) {
        // ignore
      }
    }
    //log watch thread to get the last logs.
    sleep(500)
    logTerminateLatch.countDown()
    build = buildHolder.get()
    String status = getBuildStatusPhase(build)
    if (Builds.isFailed(status) || Builds.isCancelled(status)) {
      throw new GradleException("OpenShift Build " + buildName + " " + getBuildStatusReason(build))
    }
    logger.info("Build " + buildName + " " + status)
  }

  void watchLogInThread(LogWatch logWatcher, final String failureMessage, final CountDownLatch terminateLatch) {
    final InputStream input = logWatcher.getOutput()
    Thread thread = new Thread() {
      @Override
      void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input))
        try {
          while (true) {
            String line = reader.readLine()
            if (line == null) {
              return
            }
            if (terminateLatch.getCount() <= 0L) {
              return
            }
            logger.info(line)
          }
        } catch (IOException e) {
          // Check again the latch which could be already count down to zero in between
          // so that an IO exception occurs on read
          if (terminateLatch.getCount() > 0L) {
            logger.error("%s : %s", failureMessage, e)
          }
        }
      }
    }
    thread.start()
  }

  private boolean isBuildCompleted(Build build) {
    String status = getBuildStatusPhase(build)
    if (isNotBlank(status)) {
      if (!Objects.equals(status, lastBuildStatus)) {
        lastBuildStatus = status
        logger.debug("Build %s status: %s", getName(build), status)
      }
      return Builds.isFinished(status)
    }
    return false
  }

  static String getBuildStatusPhase(Build build) {
    String status = null
    BuildStatus buildStatus = build.getStatus()
    if (buildStatus != null) {
      status = buildStatus.getPhase()
    }
    return status
  }

  static String getBuildStatusReason(Build build) {
    BuildStatus buildStatus = build.getStatus()
    if (buildStatus != null) {
      String reason = buildStatus.getReason()
      String phase = buildStatus.getPhase()
      if (isNotBlank(phase)) {
        if (isNotBlank(reason)) {
          return phase + ": " + reason
        } else {
          return phase
        }
      } else {
        return defaultIfEmpty(reason, "")
      }
    }
    return ""
  }

  private checkOrCreateBuildConfig() {

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

  static String getName(HasMetadata entity) {
    if (entity != null) {
      return getName(entity.getMetadata())
    } else {
      return null
    }
  }

  static String getName(ObjectMeta entity) {
    if (entity != null) {
      return firstNonBlank(entity.getName(),
        getAdditionalPropertyText(entity.getAdditionalProperties(), "id"),
        entity.getUid())
    } else {
      return null
    }
  }

  protected static String getAdditionalPropertyText(Map<String, Object> additionalProperties, String name) {
    if (additionalProperties != null) {
      Object value = additionalProperties.get(name)
      if (value != null) {
        return value.toString()
      }
    }
    return null
  }

  static String firstNonBlank(String... values) {
    for (String value : values) {
      if (notEmpty(value)) {
        return value
      }
    }
    return null
  }

  static boolean notEmpty(String text) {
    return text != null && text.length() > 0
  }

  static boolean isNullOrBlank(String value) {
    return value == null || value.length() == 0 || value.trim().length() == 0
  }

  static boolean isNotBlank(String text) {
    return !isNullOrBlank(text)
  }

  static String defaultIfEmpty(String value, String defaultValue) {
    return notEmpty(value) ? value : defaultValue
  }
}
