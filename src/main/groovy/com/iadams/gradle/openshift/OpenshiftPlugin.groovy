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

import com.iadams.gradle.openshift.extensions.AuthenticationExtension
import com.iadams.gradle.openshift.extensions.OpenshiftExtension
import com.iadams.gradle.openshift.tasks.AbstractOpenshiftTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class OpenshiftPlugin implements Plugin<Project> {

  static final OPENSHIFT_EXTENSION = 'openshift'
  static final OPENSHIFT_AUTH_EXTENSION = 'auth'

  @Override
  void apply(Project project) {
    setupExtensions(project)
  }

  static void setupExtensions(Project project) {
    OpenshiftExtension extension = project.extensions.create(OPENSHIFT_EXTENSION, OpenshiftExtension)
    extension.extensions.create(OPENSHIFT_AUTH_EXTENSION, AuthenticationExtension)

    project.tasks.withType(AbstractOpenshiftTask) {
      conventionMapping.baseUrl = {extension.baseUrl}
      conventionMapping.namespace = {extension.namespace}
      conventionMapping.trustCerts = {extension.trustCerts}
      conventionMapping.token = {extension.auth.token}
      conventionMapping.username = {extension.auth.username}
      conventionMapping.password = {extension.auth.password}
    }
  }
}
