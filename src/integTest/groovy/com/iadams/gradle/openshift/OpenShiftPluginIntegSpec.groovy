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
package com.iadams.gradle.openshift

import com.iadams.gradle.openshift.utils.OpenShiftBaseIntegSpec
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.openshift.client.DefaultOpenShiftClient
import io.fabric8.openshift.client.OpenShiftClient
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Stepwise

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Stepwise
class OpenShiftPluginIntegSpec extends OpenShiftBaseIntegSpec {

  String registryUrl = ''
  String token = ''

  def setup(){
    Config config = new ConfigBuilder()
        .withMasterUrl('https://127.0.0.1:8443')
        .withUsername('developer').withPassword('developer')
        .withNamespace('default')
        .build()

    OpenShiftClient client = new DefaultOpenShiftClient(config)
    Service svc = client.services().inNamespace('default').withName('docker-registry').get()
    registryUrl = "${svc.spec.clusterIP}:${svc.spec.ports[0].targetPort.intVal}"
    token = client.configuration.getOauthToken()
    client.close()
  }

  def "we can start a build"() {
    setup:
    buildFile << """
            ${basicBuildScript()}

            task startBuild(type: com.iadams.gradle.openshift.tasks.OpenShiftStartBuildTask) {
              namespace = 'myproject'
              buildName = 'test-app'
              dockerTar = file('build/build.tar.gz')
            }
            """
    copyResources('docker-build/build.tar.gz', 'build/build.tar.gz')

    when:
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments('startBuild', '--i', '--s')
      .withPluginClasspath(pluginClasspath)
      .withDebug(true)
      .build()

    then:
    println result.output
    result.task(":startBuild").outcome == SUCCESS
  }

  def "we can start a build and watch the logs"() {
    setup:
    buildFile << """
            ${basicBuildScript()}

            task startBuild(type: com.iadams.gradle.openshift.tasks.OpenShiftStartBuildTask) {
              namespace = 'myproject'
              watch = true
              buildName = 'test-app'
              dockerTar = file('build/build.tar.gz')
            }
            """
    copyResources('docker-build/build.tar.gz', 'build/build.tar.gz')

    when:
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments('startBuild', '--i', '--s')
      .withPluginClasspath(pluginClasspath)
      .withDebug(true)
      .build()

    then:
    println result.output
    result.task(":startBuild").outcome == SUCCESS
    result.output.contains('Receiving source from STDIN as archive ...')
    result.output.contains('Step 1/5 : FROM node:6.10.3-alpine')
    result.output.contains('Step 2/5 : EXPOSE 8080')
    result.output.contains('Step 3/5 : COPY server.js')
  }

  def "we can tag a built image"() {
    setup:
    buildFile << """
            ${basicBuildScript()}

            task tagBuild(type: com.iadams.gradle.openshift.tasks.OpenShiftTagTask) {
              namespace = 'myproject'
              imageName = 'test-app'
              tag = version
            }
            """

    when:
    def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('tagBuild', '--i', '--s')
            .withPluginClasspath(pluginClasspath)
            .withDebug(true)
            .build()

    then:
    println result.output
    result.task(":tagBuild").outcome == SUCCESS
  }

  def "we can create a new deployment"(){
    setup:
    buildFile << """
            ${basicBuildScript()}

            import io.fabric8.openshift.api.model.DeploymentConfigBuilder
            import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder

            task createDeployment(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateDeploymentTask) {
              namespace = 'myproject'
              deployment = new DeploymentConfigBuilder()
                            .withNewMetadata()
                              .withName('test-app')
                            .endMetadata()
                            .withNewSpec()
                              .withReplicas(1)
                              .withTriggers(new DeploymentTriggerPolicyBuilder().withType('ConfigChange').build())
                              .addToSelector('app','test-app')
                              .withNewTemplate()
                                .withNewMetadata()
                                  .addToLabels('app', 'test-app')
                                .endMetadata()
                                .withNewSpec()
                                  .addNewContainer()
                                    .withName('test-app')
                                    .withImage('${registryUrl}/myproject/test-app:0.1')
                                  .endContainer()
                                .endSpec()
                              .endTemplate()
                            .endSpec()
                            .withNewStatus()
                              .withLatestVersion(0L)
                            .endStatus()
                            .build()
            }
            """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('createDeployment', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":createDeployment").outcome == SUCCESS
  }

  def "we can trigger a new deployment of our image"(){
    setup:
    buildFile << """
            ${basicBuildScript()}

            task deploy(type: com.iadams.gradle.openshift.tasks.OpenShiftStartDeploymentTask) {
              namespace = 'myproject'
              deployment = 'test-app'
            }
            """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('deploy', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":deploy").outcome == SUCCESS
  }

  def "we can create a configMap with a single file"(){
    setup:

    buildFile << """
            ${basicBuildScript()}

            task createConfig(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateConfigMapTask) {
              namespace = 'myproject'
              configMapName = 'test-app-config1'
              configMap = file('app.properties')
            }
            """
    copyResources('app.properties', 'app.properties')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('createConfig', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":createConfig").outcome == SUCCESS
  }

  def "we can create a configMap with a series of files"(){
    setup:

    buildFile << """
            ${basicBuildScript()}

            task createConfig(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateConfigMapTask) {
              namespace = 'myproject'
              configMapName = 'test-app-config2'
              configMap = files('app.properties', 'other.properties')
            }
            """
    copyResources('app.properties', 'app.properties')
    copyResources('other.properties', 'other.properties')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('createConfig', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":createConfig").outcome == SUCCESS
  }

  def "we can create a configMap with a directory"(){
    setup:

    buildFile << """
            ${basicBuildScript()}

            task createConfig(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateConfigMapTask) {
              namespace = 'myproject'
              configMapName = 'test-app-config3'
              configMap = file('dir/')
            }
            """
    copyResources('app.properties', 'dir/app.properties')
    copyResources('other.properties', 'dir/other.properties')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('createConfig', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":createConfig").outcome == SUCCESS
  }


  def "we can create a secret with a single file"(){
    setup:

    buildFile << """
            ${basicBuildScript()}

            task createSecret(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateSecretTask) {
              namespace = 'myproject'
              secretName = 'test-app-config1'
              secret = file('app.properties')
            }
            """
    copyResources('app.properties', 'app.properties')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('createSecret', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":createSecret").outcome == SUCCESS
  }

  def "we can create a secret with a series of files"(){
    setup:

    buildFile << """
            ${basicBuildScript()}

            task createSecret(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateSecretTask) {
              namespace = 'myproject'
              secretName = 'test-app-config2'
              secret = files('app.properties', 'other.properties')
            }
            """
    copyResources('app.properties', 'app.properties')
    copyResources('other.properties', 'other.properties')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('createSecret', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":createSecret").outcome == SUCCESS
  }

  def "we can create a secret with a directory"(){
    setup:

    buildFile << """
            ${basicBuildScript()}

            task createSecret(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateSecretTask) {
              namespace = 'myproject'
              secretName = 'test-app-config3'
              secret = file('dir/')
            }
            """
    copyResources('app.properties', 'dir/app.properties')
    copyResources('other.properties', 'dir/other.properties')

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('createSecret', '--i', '--s')
        .withPluginClasspath(pluginClasspath)
        .withDebug(true)
        .build()

    then:
    println result.output
    result.task(":createSecret").outcome == SUCCESS
  }

  def basicBuildScript(){
    """ plugins {
          id 'com.iadams.openshift'
        }

        group = 'com.example'
        version = '0.1'

        openshift {
          baseUrl = 'https://127.0.0.1:8443'
          auth {
            token = '$token'
          }
        }"""
  }
}
