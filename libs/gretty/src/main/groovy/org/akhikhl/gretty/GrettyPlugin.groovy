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
import org.apache.commons.io.FilenameUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Paths

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class GrettyPlugin implements Plugin<Project> {

  protected static final Logger log = LoggerFactory.getLogger(GrettyPlugin)

  private void addConfigurations(Project project) {
    project.configurations {
      implementation {
        exclude module: 'spring-boot-starter-tomcat'
        exclude module: 'spring-boot-starter-jetty'
      }
      gretty
      grettyStarter
      springBoot {
        exclude module: 'spring-boot-starter-tomcat'
        exclude module: 'spring-boot-starter-jetty'
        exclude group: 'org.eclipse.jetty'
        exclude group: 'org.eclipse.jetty.websocket'
      }
      grettyNoSpringBoot {
        extendsFrom project.configurations.gretty
        exclude group: 'org.springframework.boot'
      }
      grettySpringLoaded {
        transitive = false
      }
      grettyProductRuntime
      grettyProvidedCompile
      project.configurations.findByName('implementation')?.extendsFrom grettyProvidedCompile
    }

    ServletContainerConfig.getConfigs().each { configName, config ->
      project.configurations.create config.servletContainerRunnerConfig
    }
  }

  private void addConfigurationsAfterEvaluate(Project project) {
    def runtimeConfig = project.configurations.findByName('runtimeClasspath')
    project.configurations {
      springBoot {
        if (runtimeConfig)
          extendsFrom runtimeConfig
      }
      grettyProductRuntime {
        if (runtimeConfig)
          extendsFrom runtimeConfig
      }
    }
    // need to configure providedCompile, so that war excludes grettyProvidedCompile artifacts
    def providedCompile = project.configurations.findByName('providedCompile')
    if(providedCompile)
      providedCompile.extendsFrom project.configurations.grettyProvidedCompile
  }

  private void addDependencies(Project project) {

    String grettyVersion = Externalized.getString('grettyVersion')
    String springBootVersion = project.gretty.springBootVersion ?: (project.hasProperty('springBootVersion') ? project.springBootVersion : Externalized.getString('springBootVersion'))
    String springLoadedVersion = project.gretty.springLoadedVersion ?: (project.hasProperty('springLoadedVersion') ? project.springLoadedVersion : Externalized.getString('springLoadedVersion'))
    String springVersion = project.gretty.springVersion ?: (project.hasProperty('springVersion') ? project.springVersion : Externalized.getString('springVersion'))

    project.dependencies {
      grettyStarter "org.gretty:gretty-starter:$grettyVersion"
      grettyStarter ProjectUtils.getCurrentGroovy(project).dependencies
      grettySpringLoaded "org.springframework:springloaded:$springLoadedVersion"
    }

    ServletContainerConfig.getConfig(project.gretty.servletContainer).with { config ->
      def closure = config.servletApiDependencies
      closure = closure.rehydrate(config, closure.owner, closure.thisObject)
      closure.resolveStrategy = Closure.DELEGATE_FIRST
      closure(project)
    }

    ServletContainerConfig.getConfigs().each { configName, config ->
      def closure = config.servletContainerRunnerDependencies
      closure = closure.rehydrate(config, closure.owner, closure.thisObject)
      closure.resolveStrategy = Closure.DELEGATE_FIRST
      closure(project)
    }

    if(project.gretty.springBoot) {
      String configName = project.configurations.findByName('implementation') ? 'implementation' : 'springBoot'
      project.dependencies.add configName, "org.springframework.boot:spring-boot-starter-web:$springBootVersion", {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
      }
      project.dependencies.add configName, "org.springframework.boot:spring-boot-starter-websocket:${springBootVersion}", {
        exclude group: 'org.apache.tomcat.embed'
      }
      project.dependencies.add configName, "org.springframework:spring-messaging:$springVersion"
      project.dependencies.add configName, "org.springframework:spring-websocket:$springVersion"
      configName = project.configurations.findByName('runtimeOnly') ? 'runtimeOnly' : 'springBoot'
      project.dependencies.add configName, "org.gretty:gretty-springboot:$grettyVersion"
    }

    for(String overlay in project.gretty.overlays)
      project.dependencies.add 'grettyProvidedCompile', project.project(overlay)

    def runtimeConfig = project.configurations.findByName('runtimeClasspath')
    if(runtimeConfig) {
      if(runtimeConfig.allDependencies.find { it.name == 'slf4j-api' } && !runtimeConfig.allDependencies.find { it.name in ['slf4j-nop', 'slf4j-simple', 'slf4j-log4j12', 'slf4j-jdk14', 'logback-classic', 'log4j-slf4j-impl'] }) {
        log.warn("Found slf4j-api dependency but no providers were found.  Did you mean to add slf4j-simple? See https://www.slf4j.org/codes.html#noProviders .")
      }
    }

    project.farms.farmsMap.each { fname, farm ->
      farm.webAppRefs.each { wref, options ->
        def typeAndResult = FarmConfigurerUtil.resolveWebAppType(project, options.suppressMavenToProjectResolution, wref)
        def type = typeAndResult[0] as FarmWebappType
        def result = typeAndResult[1]

        def configurationName = ProjectUtils.getFarmConfigurationName(fname)
        if(FarmWebappType.WAR_DEPENDENCY == type) {
          project.configurations.maybeCreate(configurationName)
          project.dependencies.add configurationName, result
        }

        if(type in [FarmWebappType.WAR_DEPENDENCY, FarmWebappType.WAR_FILE]) {
          project.configurations.maybeCreate(configurationName)
          options.dependencies?.each {
            project.dependencies.add configurationName, it
          }
        }
      }
    }
  }

  private void addExtensions(Project project) {

    project.extensions.create('gretty', GrettyExtension)

    project.extensions.create('farm', FarmExtension, project)

    project.extensions.create('farms', FarmsExtension, project)
    project.farms.farmsMap_[''] = project.farm

    project.extensions.create('product', ProductExtension)

    project.extensions.create('products', ProductsExtension)
    project.products.productsMap[''] = project.product
  }

  private void addRepositories(Project project) {
    project.repositories {
      mavenLocal()
      mavenCentral()
      maven { url 'https://repo.spring.io/release' }
      maven { url 'https://repo.spring.io/milestone' }
      maven { url 'https://repo.spring.io/snapshot' }
    }
  }

  private void addTaskDependencies(Project project) {

    project.tasks.configureEach { task ->
      if (GradleUtils.instanceOf(task, 'org.akhikhl.gretty.AppStartTask'))
        task.dependsOn {
          // We don't need any task for hard inplace mode.
          task.effectiveInplace ? project.tasks.prepareInplaceWebApp : project.tasks.prepareArchiveWebApp
        }
      else if (GradleUtils.instanceOf(task, 'org.akhikhl.gretty.FarmStartTask')) {
        task.dependsOn {
          task.getWebAppConfigsForProjects().findResults {
            def proj = project.project(it.projectPath)
            boolean inplace = it.inplace == null ? task.inplace : it.inplace
            String prepareTaskName = inplace ? 'prepareInplaceWebApp' : 'prepareArchiveWebApp'
            def projTask = proj.tasks.findByName(prepareTaskName)
            if(!projTask)
              proj.tasks.configureEach { t ->
                if(t.name == prepareTaskName)
                  task.dependsOn t
              }
            projTask
          }
        }

        def farmName = task.farmName
        project.farms.farmsMap[farmName].webAppRefs.each { wref, options ->
          def typeAndResult = FarmConfigurerUtil.resolveWebAppType(project, options.suppressMavenToProjectResolution, wref)
          def type = typeAndResult[0]
          if(type in [FarmWebappType.WAR_FILE, FarmWebappType.WAR_DEPENDENCY]) {
            if (options.overlays) {
              def warFile
              if (type == FarmWebappType.WAR_FILE) {
                warFile = typeAndResult[1]
              } else if (type == FarmWebappType.WAR_DEPENDENCY) {
                warFile = ProjectUtils.getFileFromConfiguration(project, ProjectUtils.getFarmConfigurationName(farmName), typeAndResult[1])
              }

              def warFileName = warFile.name
              def farmOverlayArchive = project.tasks.findByName("farmOverlayArchive${farmName}${warFileName}")
              task.dependsOn farmOverlayArchive
            }
          }
        }
      }
    }
  }

  private void addTasks(Project project) {

    if(project.tasks.findByName('classes')) { // JVM project?

      project.tasks.register('prepareInplaceWebAppFolder', Copy) {
        group = 'gretty'
        description = 'Copies webAppDir of this web-app and all overlays (if any) to ${buildDir}/inplaceWebapp'

        def getInplaceMode = {
          project.tasks.findByName('appRun').effectiveInplaceMode
        }

        // We should track changes in inplaceMode value or plugin would show UP-TO-DATE for this task
        // even if inplaceMode was changed
        inputs.property('inplaceMode', getInplaceMode)

        onlyIf { getInplaceMode() != 'hard' }

        // Attention: call order is important here! Overlay files must be copied prior to this web-app files.

        for (String overlay in project.gretty.overlays) {
          Project overlayProject = project.project(overlay)
          dependsOn { overlayProject.tasks.findByName('prepareInplaceWebAppFolder') }
          from "${overlayProject.buildDir}/inplaceWebapp"
        }

        from ProjectUtils.getWebAppDir(project)

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        def closure = project.gretty.webappCopy
        closure = closure.rehydrate(it, closure.owner, closure.thisObject)
        closure()

        into "${project.buildDir}/inplaceWebapp"
      }

      project.tasks.register('createInplaceWebAppFolder') {
        group = 'gretty'
        description = 'Creates directory ${buildDir}/inplaceWebapp'

        dependsOn project.tasks.prepareInplaceWebAppFolder

        File inplaceWebappDir = new File("${project.buildDir}/inplaceWebapp")

        outputs.upToDateWhen {
          inplaceWebappDir.exists()
        }

        doFirst {
          inplaceWebappDir.mkdirs()
        }
      }

      project.tasks.register('prepareInplaceWebAppClasses') {
        group = 'gretty'
        description = 'Compiles classes of this web-app and all overlays (if any)'
        dependsOn project.tasks.classes
        for(String overlay in project.gretty.overlays)
          dependsOn "$overlay:prepareInplaceWebAppClasses"
      }

      project.tasks.register('prepareInplaceWebApp') {
        group = 'gretty'
        description = 'Prepares inplace web-app'
        dependsOn project.tasks.createInplaceWebAppFolder
        dependsOn project.tasks.prepareInplaceWebAppClasses
      }

      def archiveTask = project.tasks.findByName('war') ?: project.tasks.jar

      archiveTask.configure {
        def closure = project.gretty.webappCopy
        closure = closure.rehydrate(it, closure.owner, closure.thisObject)
        closure()
      }

      if(project.gretty.overlays) {

        project.ext.finalArchivePath = archiveTask.archivePath

        archiveTask.archiveFileName = 'partial.' + (project.tasks.findByName('war') ? 'war' : 'jar')

        // 'explodeWebApps' task is only activated by 'overlayArchive' task
        project.tasks.register('explodeWebApps') {
          group = 'gretty'
          description = 'Explodes this web-app and all overlays (if any) to ${buildDir}/explodedWebapp'
          for(String overlay in project.gretty.overlays)
            dependsOn "$overlay:assemble" as String
          dependsOn archiveTask
          for(String overlay in project.gretty.overlays)
            inputs.file { ProjectUtils.getFinalArchivePath(project.project(overlay)) }
          inputs.file archiveTask.archivePath
          outputs.dir "${project.buildDir}/explodedWebapp"
          doLast {
            ProjectUtils.prepareExplodedWebAppFolder(project)
          }
        }

        project.tasks.register('overlayArchive') {
          group = 'gretty'
          description = 'Creates archive from exploded web-app in ${buildDir}/explodedWebapp'
          dependsOn project.tasks.explodeWebApps
          inputs.dir "${project.buildDir}/explodedWebapp"
          outputs.file project.ext.finalArchivePath
          doLast {
            ant.zip destfile: project.ext.finalArchivePath, basedir: "${project.buildDir}/explodedWebapp"
          }
        }

        project.tasks.named('assemble').configure {
          dependsOn project.tasks.overlayArchive
        }
      } // overlays

      project.tasks.register('prepareArchiveWebApp') {
        group = 'gretty'
        description = 'Prepares war web-app'
        if(project.gretty.overlays)
          dependsOn project.tasks.overlayArchive
        else
          dependsOn archiveTask
      }

      project.tasks.register('appRun', AppStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in interactive mode.'
      }

      project.tasks.register('appRunDebug', AppStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in debug and interactive mode.'
        debug = true
      }

      project.tasks.register('appStart', AppStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace (stopped by \'appStop\').'
        interactive = false
      }

      project.tasks.register('appStartDebug', AppStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in debug mode (stopped by \'appStop\').'
        interactive = false
        debug = true
      }

      if(project.plugins.findPlugin(org.gradle.api.plugins.WarPlugin)) {

        project.tasks.register('appRunWar', AppStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in interactive mode.'
          inplace = false
        }

        project.tasks.register('appRunWarDebug', AppStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in debug and interactive mode.'
          inplace = false
          debug = true
        }

        project.tasks.register('appStartWar', AppStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file (stopped by \'appStop\').'
          inplace = false
          interactive = false
        }

        project.tasks.register('appStartWarDebug', AppStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in debug mode (stopped by \'appStop\').'
          inplace = false
          interactive = false
          debug = true
        }
      }

      project.tasks.register('appStop', AppStopTask) {
        group = 'gretty'
        description = 'Sends \'stop\' command to a running server.'
      }

      project.tasks.register('appRestart', AppRestartTask) {
        group = 'gretty'
        description = 'Sends \'restart\' command to a running server.'
      }

      project.tasks.register('appBeforeIntegrationTest', AppBeforeIntegrationTestTask) {
        group = 'gretty'
        description = 'Starts server before integration test.'
      }

      project.tasks.register('appAfterIntegrationTest', AppAfterIntegrationTestTask) {
        group = 'gretty'
        description = 'Stops server after integration test.'
      }

      project.tasks.register('jettyRun', JettyStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in interactive mode.'
      }

      project.tasks.register('jettyRunDebug', JettyStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in debug and interactive mode.'
        debug = true
      }

      project.tasks.register('jettyStart', JettyStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace (stopped by \'jettyStop\').'
        interactive = false
      }

      project.tasks.register('jettyStartDebug', JettyStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in debug mode (stopped by \'jettyStop\').'
        interactive = false
        debug = true
      }

      if(project.plugins.findPlugin(org.gradle.api.plugins.WarPlugin)) {

        project.tasks.register('jettyRunWar', JettyStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in interactive mode.'
          inplace = false
        }

        project.tasks.register('jettyRunWarDebug', JettyStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in debug and interactive mode.'
          inplace = false
          debug = true
        }

        project.tasks.register('jettyStartWar', JettyStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file (stopped by \'jettyStop\').'
          inplace = false
          interactive = false
        }

        project.tasks.register('jettyStartWarDebug', JettyStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in debug mode (stopped by \'jettyStop\').'
          inplace = false
          interactive = false
          debug = true
        }
      }

      project.tasks.register('jettyStop', AppStopTask) {
        group = 'gretty'
        description = 'Sends \'stop\' command to a running server.'
      }

      project.tasks.register('jettyRestart', AppRestartTask) {
        group = 'gretty'
        description = 'Sends \'restart\' command to a running server.'
      }

      project.tasks.register('tomcatRun', TomcatStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in interactive mode.'
      }

      project.tasks.register('tomcatRunDebug', TomcatStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in debug and interactive mode.'
        debug = true
      }

      project.tasks.register('tomcatStart', TomcatStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace (stopped by \'tomcatStop\').'
        interactive = false
      }

      project.tasks.register('tomcatStartDebug', TomcatStartTask) {
        group = 'gretty'
        description = 'Starts web-app inplace, in debug mode (stopped by \'tomcatStop\').'
        interactive = false
        debug = true
      }

      if(project.plugins.findPlugin(org.gradle.api.plugins.WarPlugin)) {

        project.tasks.register('tomcatRunWar', TomcatStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in interactive mode.'
          inplace = false
        }

        project.tasks.register('tomcatRunWarDebug', TomcatStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in debug and interactive mode.'
          inplace = false
          debug = true
        }

        project.tasks.register('tomcatStartWar', TomcatStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file (stopped by \'tomcatStop\').'
          inplace = false
          interactive = false
        }

        project.tasks.register('tomcatStartWarDebug', TomcatStartTask) {
          group = 'gretty'
          description = 'Starts web-app on WAR-file, in debug mode (stopped by \'tomcatStop\').'
          inplace = false
          interactive = false
          debug = true
        }
      }

      project.tasks.register('tomcatStop', AppStopTask) {
        group = 'gretty'
        description = 'Sends \'stop\' command to a running server.'
      }

      project.tasks.register('tomcatRestart', AppRestartTask) {
        group = 'gretty'
        description = 'Sends \'restart\' command to a running server.'
      }

      project.ext.jettyBeforeIntegrationTest = project.tasks.ext.jettyBeforeIntegrationTest = project.tasks.appBeforeIntegrationTest

      project.ext.jettyAfterIntegrationTest = project.tasks.ext.jettyAfterIntegrationTest = project.tasks.appAfterIntegrationTest

      project.tasks.register('showClassPath') {
        group = 'gretty'
        description = 'Shows classpath information'
        doLast {
          println "Runner classpath:"
          project.tasks.appRun.runnerClassPath.each { URL url ->
            println " $url"
          }
          project.tasks.appRun.getWebappClassPaths().each { contextPath, cp ->
            println "$contextPath classpath:"
            cp.each { URL url ->
              println " $url"
            }
          }
        }
      }

    } // JVM project

    project.farms.farmsMap.each { fname, farm ->
      def overlayTasks = []
      farm.webAppRefs.each { wref, options ->
        def typeAndResult = FarmConfigurerUtil.resolveWebAppType(project, options.suppressMavenToProjectResolution, wref)
        def type = typeAndResult[0] as FarmWebappType
        switch (type) {
          case FarmWebappType.WAR_FILE:
          case FarmWebappType.WAR_DEPENDENCY:
            def overlays = options.overlays
            if (overlays) {
              log.info("Farm {} contains webapp {} with overlays: {}", fname, wref, options.overlays)

              def warFile
              if(type == FarmWebappType.WAR_FILE) {
                warFile = typeAndResult[1]
              } else if (type == FarmWebappType.WAR_DEPENDENCY) {
                warFile = ProjectUtils.getFileFromConfiguration(project, ProjectUtils.getFarmConfigurationName(fname), typeAndResult[1])
              }

              def outputFolder = Paths.get(project.buildDir.absolutePath, 'farms', fname, 'explodedWebapps', FilenameUtils.removeExtension(warFile.name)).toFile()
              def outputFolderPath = outputFolder.absolutePath

              // FIXME does not use task configuration avoidance #41
              def explodeWebappTask = project.task("farmExplodeWebapp$fname${warFile.name}", group: 'gretty') {
                description = 'Explode webapp and all overlays into ${buildDir}/farms/${fname}/explodedWebapps/${wref}'
                for(String overlay in overlays) {
                  dependsOn "$overlay:assemble" as  String
                }
                for(String overlay in overlays)
                  inputs.file { ProjectUtils.getFinalArchivePath(project.project(overlay)) }
                inputs.file warFile
                outputs.dir outputFolder
                doLast {
                  ProjectUtils.prepareExplodedFarmWebAppFolder(project, warFile, overlays, outputFolderPath)
                }
              }

              // TODO: gradle way of doing this?
              def repackagedArchive = Paths.get(project.buildDir.absolutePath, 'farms', fname, 'explodedWebapps', warFile.name).toFile().absolutePath
              // FIXME does not use task configuration avoidance #41
              def archiveWebappTask = project.task("farmOverlayArchive$fname${warFile.name}", group: 'gretty') {
                description = 'Creates archive from exploded web-app in ${buildDir}/farms/${fname}/explodedWebapps/${wref}'
                dependsOn explodeWebappTask
                inputs.dir outputFolder
                outputs.file repackagedArchive
                doLast {
                  ant.zip destfile: repackagedArchive, basedir: outputFolderPath
                }
              }

              overlayTasks.addAll([explodeWebappTask, archiveWebappTask])

              options.finalArchivePath = repackagedArchive
            }
            break
        }
      }

      String farmDescr = fname ? "farm '${fname}'" : 'default farm'

      project.tasks.register("farmRun$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} inplace, in interactive mode."
        farmName = fname
        if(!fname)
          doFirst {
            GradleUtils.disableTaskOnOtherProjects(project, 'run')
            GradleUtils.disableTaskOnOtherProjects(project, 'jettyRun')
            GradleUtils.disableTaskOnOtherProjects(project, 'farmRun')
          }
      }

      project.tasks.register("farmRunDebug$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} inplace, in debug and in interactive mode."
        farmName = fname
        debug = true
        if(!fname)
          doFirst {
            GradleUtils.disableTaskOnOtherProjects(project, 'debug')
            GradleUtils.disableTaskOnOtherProjects(project, 'jettyRunDebug')
            GradleUtils.disableTaskOnOtherProjects(project, 'farmRunDebug')
          }
      }

      project.tasks.register("farmStart$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} inplace (stopped by 'farmStop${fname}')."
        farmName = fname
        interactive = false
      }

      project.tasks.register("farmStartDebug$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} inplace, in debug mode (stopped by 'farmStop${fname}')."
        farmName = fname
        interactive = false
        debug = true
      }

      project.tasks.register("farmRunWar$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} on WAR-files, in interactive mode."
        farmName = fname
        inplace = false
      }

      project.tasks.register("farmRunWarDebug$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} on WAR-files, in debug and in interactive mode."
        farmName = fname
        debug = true
        inplace = false
      }

      project.tasks.register("farmStartWar$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} on WAR-files (stopped by 'farmStop${fname}')."
        farmName = fname
        interactive = false
        inplace = false
      }

      project.tasks.register("farmStartWarDebug$fname", FarmStartTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} on WAR-files, in debug (stopped by 'farmStop${fname}')."
        farmName = fname
        interactive = false
        debug = true
        inplace = false
      }

      project.tasks.register("farmStop$fname", FarmStopTask) {
        group = 'gretty'
        description = "Sends \'stop\' command to a running ${farmDescr}."
        farmName = fname
      }

      project.tasks.register("farmRestart$fname", FarmRestartTask) {
        group = 'gretty'
        description = "Sends \'restart\' command to a running ${farmDescr}."
        farmName = fname
      }

      // FIXME does not use task configuration avoidance #41
      project.task("farmBeforeIntegrationTest$fname", type: FarmBeforeIntegrationTestTask) {
        group = 'gretty'
        description = "Starts ${farmDescr} before integration test."
        farmName = fname
      }

      // FIXME does not use task configuration avoidance #41
      project.task("farmIntegrationTest$fname", type: FarmIntegrationTestTask) {
        group = 'gretty'
        description = "Runs integration tests on ${farmDescr} web-apps."
        farmName = fname
        dependsOn 'farmBeforeIntegrationTest' + fname
        finalizedBy 'farmAfterIntegrationTest' + fname
      }

      // FIXME does not use task configuration avoidance #41
      project.task("farmAfterIntegrationTest$fname", type: FarmAfterIntegrationTestTask) {
        group = 'gretty'
        description = "Stops ${farmDescr} after integration test."
        farmName = fname
      }
    } // farmsMap
  } // addTasks

  private void afterProjectEvaluate(Project project) {

    if(project.extensions.findByName('gretty')) {

      addConfigurationsAfterEvaluate(project)
      addTaskDependencies(project)
      new ProductsConfigurer(project).configureProducts()

      if(project.gretty.autoConfigureRepositories)
        addRepositories(project)

      addDependencies(project)
      addTasks(project)

      for(Closure afterEvaluateClosure in project.gretty.afterEvaluate) {
        afterEvaluateClosure.delegate = project.gretty
        afterEvaluateClosure.resolveStrategy = Closure.DELEGATE_FIRST
        afterEvaluateClosure()
      }

      // FIXME does not use task configuration avoidance #41
      project.tasks.findByName('appBeforeIntegrationTest')?.with {
        if(!integrationTestTaskAssigned)
          integrationTestTask null // default binding
      }

      // FIXME does not use task configuration avoidance #41
      project.tasks.findByName('appAfterIntegrationTest')?.with {
        if(!integrationTestTaskAssigned)
          integrationTestTask null // default binding
      }

      // FIXME does not use task configuration avoidance #41
      if(!project.tasks.findByName('run') && project.hasProperty('gretty_runTask') && Boolean.valueOf(project.gretty_runTask))
        project.tasks.register('run') {
          group = 'gretty'
          description = 'Starts web-app inplace, in interactive mode. Same as appRun task.'
          dependsOn 'appRun'
        }

      // FIXME does not use task configuration avoidance #41
      if(!project.tasks.findByName('debug') && project.hasProperty('gretty_debugTask') && Boolean.valueOf(project.gretty_debugTask))
        project.tasks.register('debug') {
          group = 'gretty'
          description = 'Starts web-app inplace, in debug and interactive mode. Same as appRunDebug task.'
          dependsOn 'appRunDebug'
        }
    }
  }

  private void afterAllProjectsEvaluate(Project rootProject) {

    rootProject.allprojects { project ->

      if(project.extensions.findByName('farms'))
        project.farms.farmsMap.each { fname, farm ->

          for(Closure afterEvaluateClosure in farm.afterEvaluate) {
            afterEvaluateClosure.delegate = farm
            afterEvaluateClosure.resolveStrategy = Closure.DELEGATE_FIRST
            afterEvaluateClosure()
          }

          // FIXME does not use task configuration avoidance #41
          if(!project.tasks."farmBeforeIntegrationTest$fname".integrationTestTaskAssigned)
            project.tasks."farmBeforeIntegrationTest$fname".integrationTestTask null // default binding

          // FIXME does not use task configuration avoidance #41
          if(!project.tasks."farmIntegrationTest$fname".integrationTestTaskAssigned)
            project.tasks."farmIntegrationTest$fname".integrationTestTask null // default binding

          // FIXME does not use task configuration avoidance #41
          if(!project.tasks."farmAfterIntegrationTest$fname".integrationTestTaskAssigned)
            project.tasks."farmAfterIntegrationTest$fname".integrationTestTask null // default binding
        }
    }
  }

  void apply(final Project project) {

    if(project.gradle.gradleVersion.startsWith('1.')) {
      String releaseNumberStr = project.gradle.gradleVersion.split('\\.')[1]
      if(releaseNumberStr.contains('-'))
        releaseNumberStr = releaseNumberStr.split('-')[0]
      int releaseNumber = releaseNumberStr as int
      if(releaseNumber < 10)
        throw new GradleException("Gretty supports only Gradle 1.10 or newer. You have Gradle ${project.gradle.gradleVersion}.")
    }

    project.ext {
      grettyVersion = Externalized.getString('grettyVersion')
      if(!has('jetty7Version'))
        jetty7Version = Externalized.getString('jetty7Version')
      if(!has('jetty7ServletApiVersion'))
        jetty7ServletApiVersion = Externalized.getString('jetty7ServletApiVersion')
      if(!has('jetty8Version'))
        jetty8Version = Externalized.getString('jetty8Version')
      if(!has('jetty8ServletApiVersion'))
        jetty8ServletApiVersion = Externalized.getString('jetty8ServletApiVersion')
      if(!has('jetty9Version'))
        jetty9Version = Externalized.getString('jetty9Version')
      if(!has('jetty93Version'))
        jetty93Version = Externalized.getString('jetty93Version')
      if(!has('jetty94Version'))
        jetty94Version = Externalized.getString('jetty94Version')
      if(!has('jetty9ServletApiVersion'))
        jetty9ServletApiVersion = Externalized.getString('jetty9ServletApiVersion')
      if(!has('jetty10Version'))
        jetty10Version = Externalized.getString('jetty10Version')
      if(!has('jetty10ServletApiVersion'))
        jetty10ServletApiVersion = Externalized.getString('jetty10ServletApiVersion')
      if(!has('tomcat85Version'))
        tomcat85Version = Externalized.getString('tomcat85Version')
      if(!has('tomcat85ServletApiVersion'))
        tomcat85ServletApiVersion = Externalized.getString('tomcat85ServletApiVersion')
      if(!has('tomcat9Version'))
        tomcat9Version = Externalized.getString('tomcat9Version')
      if(!has('tomcat9ServletApiVersion'))
        tomcat9ServletApiVersion = Externalized.getString('tomcat9ServletApiVersion')
      if(!has('asmVersion'))
        asmVersion = Externalized.getString('asmVersion')
    }

    addExtensions(project)
    addConfigurations(project)
    project.afterEvaluate {
      afterProjectEvaluate(project)
    }

    //there can be multiple submodules applying this plugin and we want to register callback only once
    if(!project.rootProject.hasProperty('gretty_')) {
      Project rootProject = project.rootProject
      rootProject.ext.gretty_ = [:]
      rootProject.getGradle().projectsEvaluated {
        afterAllProjectsEvaluate(rootProject)
      }
    }
  } // apply
}
