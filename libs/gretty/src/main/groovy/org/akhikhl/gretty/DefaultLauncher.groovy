/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

import java.util.concurrent.Future

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class DefaultLauncher extends LauncherBase {

  static File getPortPropertiesFile(Project project, ServerConfig serverConfig) {
    project.file("${project.buildDir}/${serverConfig.portPropertiesFileName}")
  }

  static Collection<URL> getRunnerClassPath(Project project) {
    def files = ProjectUtils.getRunnerFileCollection(project)
    (files.collect { it.toURI().toURL() }) as LinkedHashSet
  }

  private final ExecOperations execOperations
  private Collection<URL> runnerClasspath
  protected Project project

  ScannerManager scannerManager

  DefaultLauncher(Project project, LauncherConfig config, ExecOperations execOperations) {
    super(config)
    this.project = project
    this.execOperations = execOperations
  }

  protected void afterJavaExec() {
    super.afterJavaExec()
    scannerManager?.stopScanner()
  }

  @Override
  void beforeLaunch() {
    runnerClasspath = getRunnerClassPath(project)
    super.beforeLaunch()
  }

  protected void beforeJavaExec() {
    super.beforeJavaExec()
    scannerManager?.startScanner()
    project.file("${project.buildDir}/gretty_ports")
  }

  protected File getPortPropertiesFile() {
    getPortPropertiesFile(project, sconfig)
  }

  @Override
  protected String getServletContainerId() {
    sconfig.servletContainer
  }

  @Override
  protected String getServletContainerDescription() {
    ServletContainerConfig.getConfig(sconfig.servletContainer).servletContainerDescription(project)
  }

  @Override
  protected List<String> getServletContainerClasspath() {
    return ProjectUtils.getServletContainerClasspath(project, sconfig.servletContainer).toList().collect { it.toString()}
  }

  @Override
  protected void javaExec(JavaExecParams params) {
    def capturedRunnerClasspath = runnerClasspath
    execOperations.javaexec { JavaExecSpec spec ->
      if(log.isDebugEnabled())
        for(def path in capturedRunnerClasspath)
          log.debug 'Runner classpath: {}', path

      spec.classpath = project.files(capturedRunnerClasspath)
      def jvmArgs = params.jvmArgs

      if(params.debug) {
        // Checking if we are in pre 5.6 Gradle API
        if(spec.metaClass.getMetaProperty('debugOptions')==null) {
          if(log.isDebugEnabled()) {
            log.debug("Before Gradle 5.6 setting DebugOptions manually")
          }
          jvmArgs.add '-Xdebug'
          String debugArg = "-Xrunjdwp:transport=dt_socket,server=y,suspend=${params.debugSuspend ? 'y' : 'n'},address=${params.debugPort}"
          jvmArgs.add debugArg
        } else {
          if(log.isDebugEnabled()) {
            log.debug("Gradle 5.6 or upper, using DebugOptions on javaExec")
          }
          spec.debug = true
          spec.debugOptions {jd ->
            jd.enabled = true
            jd.port = params.debugPort
            jd.server = true
            jd.suspend = params.debugSuspend
          }
        }
        log.info 'DEBUG MODE, port={}, suspend={}', params.debugPort, params.debugSuspend
      }

      if (params.jvmExecutable) {
        spec.executable params.jvmExecutable
      }

      spec.jvmArgs jvmArgs
      spec.systemProperties params.systemProperties
      spec.mainClass.set(params.main)
      spec.args = params.args
    }
  }

  @Override
  protected void prepareToRun(WebAppConfig wconfig) {
    ProjectUtils.prepareToRun(project, wconfig)
  }

  @Override
  protected void rebuildWebapps() {
    webAppConfigs.each { WebAppConfig wconfig ->
      if(wconfig.projectPath) {
        def proj = project.project(wconfig.projectPath)
        ProjectConnection connection = GradleConnector.newConnector().useInstallation(proj.gradle.gradleHomeDir).forProjectDirectory(proj.projectDir).connect()
        try {
          connection.newBuild().forTasks(wconfig.inplace ? 'prepareInplaceWebApp' : 'prepareArchiveWebApp').run()
        } finally {
          connection.close()
        }
      }
    }
  }
}
