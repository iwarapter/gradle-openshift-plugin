# Gradle OpenShift Plugin

Gradle plugin to integrate with OpenShift, the project currently provides a base plugin with no default (opinionated) tasks.

[![Build Status](https://travis-ci.org/iwarapter/gradle-openshift-plugin.svg?branch=master)](https://travis-ci.org/iwarapter/gradle-openshift-plugin)

Latest Version
--------------
All versions can be found [here].

Usage
-----------

Build script snippet for use in all Gradle versions:
```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.iadams.gradle:gradle-openshift-plugin:0.1-RC1"
  }
}

apply plugin: "com.iadams.openshift-base"
```
Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```
plugins {
  id "com.iadams.openshift-base" version "0.1-RC1"
}
```

Tasks
-----------

No default tasks are provided however there are several task types you can use:

#### OpenShiftStartBuildTask

This will trigger a binary build with the given tar.gz containing the `Dockerfile` and relevant artifacts.

```groovy
task startBuild(type: com.iadams.gradle.openshift.tasks.OpenShiftStartBuildTask) {
  namespace = 'myproject'
  watch = true
  buildName = 'test-app'
  dockerTar = file('build/build.tar.gz')
}
```

#### OpenShiftTagTask

This will create a tag with a given name from the `latest` imageStream tag.

```groovy
task tagBuild(type: com.iadams.gradle.openshift.tasks.OpenShiftTagTask) {
  namespace = 'myproject'
  imageName = 'test-app'
  tag = version
}
```

#### OpenShiftStartDeploymentTask

Triggers a new deployment.

```groovy
task deploy(type: com.iadams.gradle.openshift.tasks.OpenShiftStartDeploymentTask) {
  namespace = 'myproject'
  deployment = 'test-app'
}
```

#### OpenShiftCreateConfigMapTask

Creates (or replaces) a `configMap` with a given `File` or `FileCollection` the file can be either a file or a directory.

```groovy
task createConfig(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateConfigMapTask) {
  namespace = 'myproject'
  configMapName = 'test-app-config1'
  configMap = file('app.properties')
}
```

#### OpenShiftCreateSecretTask

Creates (or replaces) a `secret` with a given `File` or `FileCollection` the file can be either a file or a directory.

```groovy
task createSecret(type: com.iadams.gradle.openshift.tasks.OpenShiftCreateSecretTask) {
  namespace = 'myproject'
  secretName = 'test-app-config1'
  secret = file('app.properties')
}
```

## Configuration

The only configuration currently required is the baseUrl and authentication config (basic auth or token).

#### build.gradle
```groovy
openshift {
  baseUrl = 'https://127.0.0.1:8443'
  auth {
    username = 'user'
    password = 'pass'
    //either basic auth or token
    token = 'token'
  }
}
```

[here]:https://plugins.gradle.org/plugin/com.iadams.openshift-base