package org.akhikhl.gretty

import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskInputs
import org.gradle.api.tasks.TaskOutputs
import org.gradle.api.tasks.TaskState
import org.gradle.process.JavaForkOptions
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

interface JacocoHelper extends Task, JavaForkOptions, ExtensionAware {
  @Internal
  JacocoTaskExtension getJacoco()

  @Internal
  @Override
  Map<String, Object> getEnvironment()

  @Internal
  @Override
  String getExecutable()

  @Internal
  @Override
  TaskInputs getInputs()

  @Internal
  @Override
  TaskOutputs getOutputs()

  @Internal
  @Override
  TaskState getState()

  @Internal
  @Override
  File getWorkingDir()
}
