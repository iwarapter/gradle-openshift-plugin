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
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Ignore
import spock.lang.Stepwise

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Stepwise
class OpenshiftPluginIntegSpec extends OpenShiftBaseIntegSpec {

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
    result.output.contains('Step 1/4 : FROM nginx')
    result.output.contains('Step 2/4 : COPY index.html /')
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

  def "we can deploy our image"(){
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

  def basicBuildScript(){
    """ plugins {
          id 'com.iadams.openshift'
        }

        group = 'com.example'
        version = '0.1'

        openshift {
          baseUrl = 'https://127.0.0.1:8443'
          auth {
            token = 'GrB-ybUVCtDe0BVDm04WhdjvvRKvXfsl2pVEp_KL-SY'
            username = 'developer'
            password = 'developer'
          }
        }"""
  }
}
