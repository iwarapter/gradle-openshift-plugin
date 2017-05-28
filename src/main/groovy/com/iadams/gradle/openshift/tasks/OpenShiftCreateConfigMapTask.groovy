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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input

class OpenShiftCreateConfigMapTask extends AbstractOpenshiftTask {

  @Input
  String configMapName

  @Input
  def configMap

  OpenShiftCreateConfigMapTask() {
    super('Tag existing images into image streams')
  }

  @Override
  void executeAction() {
    switch (getConfigMap()) {
      case File:
        File f = (File) getConfigMap()
        if(f.isDirectory()){
          client.configMaps().inNamespace(getNamespace()).withName(getConfigMapName()).createOrReplaceWithNew()
              .withNewMetadata()
                .withName(getConfigMapName())
              .endMetadata()
                .withData(f.listFiles().collectEntries{[(it.name): it.text]})
              .done()
        }
        else {
          client.configMaps().inNamespace(getNamespace()).withName(getConfigMapName()).createOrReplaceWithNew()
              .withNewMetadata()
                .withName(getConfigMapName())
              .endMetadata()
               .withData([(f.name): f.text])
              .done()
        }
        break
      case FileCollection:
        FileCollection f = (FileCollection) getConfigMap()
        client.configMaps().inNamespace(getNamespace()).withName(getConfigMapName()).createOrReplaceWithNew()
            .withNewMetadata()
            .withName(getConfigMapName())
            .endMetadata()
            .withData(f.collectEntries {[(it.name): it.text]})
            .done()
        break
      default:
        throw new InvalidUserDataException("Unable to create configMap with object ${getConfigMap().class}")
    }
  }
}
