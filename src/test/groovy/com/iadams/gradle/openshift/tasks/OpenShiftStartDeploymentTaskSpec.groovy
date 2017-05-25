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

import io.fabric8.openshift.api.model.DeploymentConfigBuilder
import io.fabric8.openshift.client.server.mock.OpenShiftServer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class OpenShiftStartDeploymentTaskSpec extends Specification {
  static final String PLUGIN_ID = 'com.iadams.openshift'
  Project project

  @Rule
  TemporaryFolder projectDir

  def setup() {
      project = ProjectBuilder.builder().build()
      project.pluginManager.apply PLUGIN_ID
  }

  @Rule
  public OpenShiftServer server = new OpenShiftServer()

  def "we can trigger the latest deployment"(){
    given:
    server.expect().get().withPath("/oapi/v1/namespaces/test/deploymentconfigs/test-app").andReturn(200, new DeploymentConfigBuilder()
        .withNewMetadata().withName('test-app').endMetadata().withNewStatus().withLatestVersion(0L).endStatus().build()).once()
    server.expect().get().withPath("/oapi/v1/namespaces/test/deploymentconfigs/test-app").andReturn(200, new DeploymentConfigBuilder()
        .withNewMetadata().withName('test-app').endMetadata().withNewSpec().withReplicas(1).endSpec().withNewStatus().withLatestVersion(0L).endStatus().build()).times(2)
    server.expect().patch().withPath("/oapi/v1/namespaces/test/deploymentconfigs/test-app").andReturn(200, new DeploymentConfigBuilder()
        .withNewMetadata().withName('test-app').endMetadata().withNewSpec().withReplicas(1).endSpec().withNewStatus().withLatestVersion(1L).endStatus().build()).once()
    server.expect().get().withPath("/oapi/v1/namespaces/test/deploymentconfigs/test-app").andReturn(200, new DeploymentConfigBuilder()
        .withNewMetadata().withName('test-app').withGeneration(0L).endMetadata().withNewSpec().withReplicas(1).endSpec().withNewStatus().withReplicas(1).withObservedGeneration(1L).withLatestVersion(1L).endStatus().build()).times(2)

    when:
    OpenShiftStartDeploymentTask t = project.tasks.create('example', OpenShiftStartDeploymentTask.class)
    t.client = server.getOpenshiftClient()
    t.namespace = 'test'
    t.deployment = 'test-app'

    t.executeAction()

    then:
    server.mockServer.requestCount == 6
  }
}
