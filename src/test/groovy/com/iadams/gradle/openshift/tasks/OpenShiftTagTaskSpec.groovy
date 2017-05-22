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

import io.fabric8.openshift.api.model.ImageStreamTagBuilder
import io.fabric8.openshift.client.mock.OpenShiftServer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class OpenShiftTagTaskSpec extends Specification {

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

  def "we can trigger a build"() {
    given:
    server.expect().post().withPath("/oapi/v1/namespaces/test/imagestreamtags").andReturn(200, new ImageStreamTagBuilder()
      .build()).once()

    when:
    OpenShiftTagTask t = project.tasks.create('example', OpenShiftTagTask.class)
    t.client = server.getOpenshiftClient()
    t.namespace = 'test'
    t.imageName = 'test-app'
    t.tag = '0.1'

    t.executeAction()

    then:
    server.getMockServer().takeRequest()
  }
}
