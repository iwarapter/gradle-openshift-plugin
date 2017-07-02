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

import groovy.json.JsonSlurper
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.openshift.client.server.mock.OpenShiftServer
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class OpenShiftConfigMapTaskSpec extends Specification {

  static final String PLUGIN_ID = 'com.iadams.openshift-base'
  Project project

  @Rule
  TemporaryFolder projectDir

  def setup() {
    project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
    project.pluginManager.apply PLUGIN_ID
  }

  @Rule
  public OpenShiftServer server = new OpenShiftServer()

  def "we can create a config map from a file"(){
    given:
    projectDir.newFile('app.properties') << 'prop=example'
    server.expect().post().withPath("/api/v1/namespaces/test/configmaps").andReturn(200, new ConfigMapBuilder()
        .build()).once()

    when:
    OpenShiftCreateConfigMapTask t = project.tasks.create('example', OpenShiftCreateConfigMapTask.class)
    t.client = server.getOpenshiftClient()
    t.namespace = 'test'
    t.configMapName = 'my-conf'
    t.configMap = project.file('app.properties')

    t.executeAction()

    then:
    server.getMockServer().requestCount == 2
    server.mockServer.takeRequest()
    Object result = new JsonSlurper().parseText(server.mockServer.takeRequest().getBody().readUtf8())

    result.kind == 'ConfigMap'
    result.data.'app.properties' == 'prop=example'
  }

  def "we can create a config map from a directory"(){
    given:
    projectDir.newFolder('folder')
    projectDir.newFile('folder/app.properties') << 'prop=example'
    server.expect().post().withPath("/api/v1/namespaces/test/configmaps").andReturn(200, new ConfigMapBuilder()
        .build()).once()

    when:
    OpenShiftCreateConfigMapTask t = project.tasks.create('example', OpenShiftCreateConfigMapTask.class)
    t.client = server.getOpenshiftClient()
    t.namespace = 'test'
    t.configMapName = 'my-conf'
    t.configMap = project.file('folder/')

    t.executeAction()

    then:
    server.getMockServer().requestCount == 2
    server.mockServer.takeRequest()
    Object result = new JsonSlurper().parseText(server.mockServer.takeRequest().getBody().readUtf8())

    result.kind == 'ConfigMap'
    result.data.'app.properties' == 'prop=example'
  }

  def "we can create a config map from a file tree"(){
    given:
    projectDir.newFolder('folder')
    projectDir.newFile('folder/app.properties') << 'prop=example'
    server.expect().post().withPath("/api/v1/namespaces/test/configmaps").andReturn(200, new ConfigMapBuilder()
        .build()).once()

    when:
    OpenShiftCreateConfigMapTask t = project.tasks.create('example', OpenShiftCreateConfigMapTask.class)
    t.client = server.getOpenshiftClient()
    t.namespace = 'test'
    t.configMapName = 'my-conf'
    t.configMap = project.fileTree(dir: 'folder')

    t.executeAction()

    then:
    server.getMockServer().requestCount == 2
    server.mockServer.takeRequest()
    Object result = new JsonSlurper().parseText(server.mockServer.takeRequest().getBody().readUtf8())

    result.kind == 'ConfigMap'
    result.data.'app.properties' == 'prop=example'
  }

  def "we can create a config map from a file collection"(){
    given:
    projectDir.newFile('app.properties') << 'prop=example'
    server.expect().post().withPath("/api/v1/namespaces/test/configmaps").andReturn(200, new ConfigMapBuilder()
        .build()).once()

    when:
    OpenShiftCreateConfigMapTask t = project.tasks.create('example', OpenShiftCreateConfigMapTask.class)
    t.client = server.getOpenshiftClient()
    t.namespace = 'test'
    t.configMapName = 'my-conf'
    t.configMap = project.files('app.properties')

    t.executeAction()

    then:
    server.getMockServer().requestCount == 2
    server.mockServer.takeRequest()
    Object result = new JsonSlurper().parseText(server.mockServer.takeRequest().getBody().readUtf8())

    result.kind == 'ConfigMap'
    result.data.'app.properties' == 'prop=example'
  }


  def "misconfigured tasks error"(){
    when:
    OpenShiftCreateConfigMapTask t = project.tasks.create('example', OpenShiftCreateConfigMapTask.class)
    t.client = server.getOpenshiftClient()
    t.namespace = 'test'
    t.configMapName = 'my-conf'
    t.configMap = 'app.properties'

    t.executeAction()

    then:
    server.getMockServer().requestCount == 0
    thrown(InvalidUserDataException)
  }
}
