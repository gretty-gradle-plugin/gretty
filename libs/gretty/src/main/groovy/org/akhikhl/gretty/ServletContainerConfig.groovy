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

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class ServletContainerConfig {

  protected static final Logger log = LoggerFactory.getLogger(ServletContainerConfig)

  private static Map<String, Map> configs = createConfigs()

  private static void addRedirectFilter(Project project, String runnerConfig) {
    ProjectUtils.withOverlays(project).find { proj ->
      boolean alteredDependencies = false
      File webXmlFile = new File(ProjectUtils.getWebAppDir(proj), 'WEB-INF/web.xml')
      if(webXmlFile.exists()) {
        def webXml = new groovy.xml.XmlSlurper().parse(webXmlFile)
        if(webXml.filter.find { it.'filter-class'.text() == 'org.akhikhl.gretty.RedirectFilter' }) {
          project.dependencies.add 'runtimeOnly', "org.gretty:gretty-filter:${project.ext.grettyVersion}", {
            exclude group: 'javax.servlet', module: 'servlet-api'
          }

          // #231 If we decide to drop Gradle 6 support / Groovy 2 support, we can drop choosing Groovy versions at runtime again.
          // Fingers crossed that users of gretty-filter update Gradle and Groovy versions in tandem.
          // Likely, building a Groovy 2 project with a build system of Gradle 7 / Groovy 3 would not work
          // because below drags in the wrong versions.
          project.dependencies.add("runtimeOnly", "org.codehaus.groovy:groovy-jmx:${GroovySystem.version}")
          project.dependencies.add("runtimeOnly", "org.codehaus.groovy:groovy-servlet:${GroovySystem.version}")
          alteredDependencies = true
        }
      }
      alteredDependencies
    }
  }

  /**
   * Creates a Jetty 12 servlet container configuration
   * @param name The container name, which also prefixes its version properties (e.g., 'jetty12')
   * @param environment The Jetty environment the runner deploys webapps to (e.g., 'ee10', 'ee11')
   */
  private static Map createJetty12Config(String name, String environment, String runnerGroup, String grettyVersion) {
    String versionProp = "${name}Version"
    String servletApiProp = "${name}ServletApiVersion"
    String websocketApiProp = "${name}WebsocketApiVersion"
    String runnerConfig = "grettyRunner${name.capitalize()}"

    return [
      servletContainerType: 'jetty',
      servletContainerVersion: { project -> project.ext."$versionProp" },
      servletContainerDescription: { project -> "Jetty ${project.ext."$versionProp"} ($environment)" },
      servletContainerRunnerConfig: runnerConfig,
      servletContainerRunnerDependencies: { project ->
        project.dependencies.add servletContainerRunnerConfig, "${runnerGroup}:gretty-runner-${name}:$grettyVersion"
        addRedirectFilter(project, servletContainerRunnerConfig)
        project.configurations[servletContainerRunnerConfig].resolutionStrategy {
          force "jakarta.servlet:jakarta.servlet-api:${project.ext."$servletApiProp"}"
          def jettyVersion = project.ext."$versionProp"
          force "org.eclipse.jetty:jetty-server:$jettyVersion"
          force "org.eclipse.jetty.${environment}:jetty-${environment}-servlet:$jettyVersion"
          force "org.eclipse.jetty.${environment}:jetty-${environment}-webapp:$jettyVersion"
          force "org.eclipse.jetty:jetty-security:$jettyVersion"
          force "org.eclipse.jetty.${environment}:jetty-${environment}-apache-jsp:$jettyVersion"
          force "org.eclipse.jetty.${environment}:jetty-${environment}-annotations:$jettyVersion"
          force "org.eclipse.jetty.${environment}:jetty-${environment}-plus:$jettyVersion"
          force "org.eclipse.jetty.${environment}.websocket:jetty-${environment}-websocket-jakarta-server:$jettyVersion"
          def asm_version = project.ext.asmVersion
          force "org.ow2.asm:asm:$asm_version"
          force "org.ow2.asm:asm-commons:$asm_version"
        }
      },
      servletApiVersion: { project -> project.ext."$servletApiProp" },
      servletApiDependencies: { project ->
        project.dependencies {
          grettyProvidedCompile "jakarta.servlet:jakarta.servlet-api:${project.ext."$servletApiProp"}"
          grettyProvidedCompile "jakarta.websocket:jakarta.websocket-api:${project.ext."$websocketApiProp"}"
        }
      }
    ]
  }

  /**
   * Creates a Tomcat servlet container configuration
   * @param version The Tomcat version number (e.g., '10', '11')
   */
  private static Map createTomcatConfig(String version, String runnerGroup, String grettyVersion) {
    String versionProp = "tomcat${version}Version"
    String servletApiProp = "tomcat${version}ServletApiVersion"
    String websocketApiProp = "tomcat${version}WebsocketApiVersion"
    String runnerConfig = "grettyRunnerTomcat${version}"

    return [
      servletContainerType: 'tomcat',
      servletContainerVersion: { project -> project.ext."$versionProp" },
      servletContainerDescription: { project -> "Tomcat ${project.ext."$versionProp"}" },
      servletContainerRunnerConfig: runnerConfig,
      servletContainerRunnerDependencies: { project ->
        project.dependencies.add servletContainerRunnerConfig, "${runnerGroup}:gretty-runner-tomcat${version}:$grettyVersion"
        addRedirectFilter(project, servletContainerRunnerConfig)
        project.configurations[servletContainerRunnerConfig].resolutionStrategy {
          force "jakarta.servlet:jakarta.servlet-api:${project.ext."$servletApiProp"}"
          def tomcatVersion = project.ext."$versionProp"
          force "org.apache.tomcat.embed:tomcat-embed-core:$tomcatVersion"
          force "org.apache.tomcat.embed:tomcat-embed-el:$tomcatVersion"
          force "org.apache.tomcat.embed:tomcat-embed-jasper:$tomcatVersion"
          force "org.apache.tomcat.embed:tomcat-embed-websocket:$tomcatVersion"
        }
      },
      servletApiVersion: { project -> project.ext."$servletApiProp" },
      servletApiDependencies: { project ->
        project.dependencies {
          grettyProvidedCompile "jakarta.servlet:jakarta.servlet-api:${project.ext."$servletApiProp"}"
          grettyProvidedCompile "jakarta.websocket:jakarta.websocket-api:${project.ext."$websocketApiProp"}"
        }
      }
    ]
  }

  private static createConfigs() {
    String grettyVersion = Externalized.getString('grettyVersion')
    def runnerGroup = "org.gretty"
    def configs = [:]

    configs['jetty11'] = [
      servletContainerType: 'jetty',
      servletContainerVersion: { project -> project.ext.jetty11Version },
      servletContainerDescription: { project -> "Jetty ${project.ext.jetty11Version}" },
      servletContainerRunnerConfig: 'grettyRunnerJetty11',
      servletContainerRunnerDependencies: { project ->
        project.dependencies.add servletContainerRunnerConfig, "${runnerGroup}:gretty-runner-jetty11:$grettyVersion"
        addRedirectFilter(project, servletContainerRunnerConfig)
        project.configurations[servletContainerRunnerConfig].resolutionStrategy {
          force "jakarta.servlet:jakarta.servlet-api:${project.ext.jetty11ServletApiVersion}"
          def jettyVersion = project.ext.jetty11Version
          force "org.eclipse.jetty:jetty-server:$jettyVersion"
          force "org.eclipse.jetty:jetty-servlet:$jettyVersion"
          force "org.eclipse.jetty:jetty-webapp:$jettyVersion"
          force "org.eclipse.jetty:jetty-security:$jettyVersion"
          force "org.eclipse.jetty:apache-jsp:$jettyVersion"
          force "org.eclipse.jetty:jetty-annotations:$jettyVersion"
          force "org.eclipse.jetty:jetty-plus:$jettyVersion"
          force "org.eclipse.jetty.websocket:javax-websocket-server-impl:$jettyVersion"
          def asm_version = project.ext.asmVersion
          force "org.ow2.asm:asm:$asm_version"
          force "org.ow2.asm:asm-commons:$asm_version"
        }
      },
      servletApiVersion: { project -> project.ext.jetty11ServletApiVersion },
      servletApiDependencies: { project ->
        project.dependencies {
          grettyProvidedCompile "jakarta.servlet:jakarta.servlet-api:${project.ext.jetty11ServletApiVersion}"
          grettyProvidedCompile "jakarta.websocket:jakarta.websocket-api:${project.ext.jetty11WebsocketApiVersion}"
        }
      }
    ]

    // Jetty 12 configurations, one per Jakarta EE environment
    configs['jetty12'] = createJetty12Config('jetty12', 'ee10', runnerGroup, grettyVersion)
    configs['jetty12ee11'] = createJetty12Config('jetty12ee11', 'ee11', runnerGroup, grettyVersion)

    // Tomcat configurations (using helper method to reduce duplication)
    configs['tomcat10'] = createTomcatConfig('10', runnerGroup, grettyVersion)
    configs['tomcat11'] = createTomcatConfig('11', runnerGroup, grettyVersion)

    return configs
  }

  static getConfig(servletContainer) {
    servletContainer = servletContainer ?: 'jetty11'
    def result = configs[servletContainer.toString()]
    if(!result)
      throw new Exception("Unsupported servlet container: $servletContainer. Only ${configs.keySet()} are supported")
    result
  }

  static Set getConfigNames() {
    configs.keySet().asImmutable()
  }

  static Map getConfigs() {
    configs.asImmutable()
  }

  static String getJettyCompatibleServletContainer(Project project, String servletContainer) {
    def config = getConfig(servletContainer)
    if(config.servletContainerType == 'jetty')
      return servletContainer
    def compatibleConfigEntry = getConfigs().find { name, c ->
      c.servletContainerType == 'jetty' && c.servletApiVersion(project) == config.servletApiVersion(project)
    }
    if(compatibleConfigEntry)
      return compatibleConfigEntry.key
    String defaultJettyServletContainer = 'jetty11'
    log.warn 'Cannot find jetty container with compatible servlet-api to {}, defaulting to {}', servletContainer, defaultJettyServletContainer
    defaultJettyServletContainer
  }

  static String getTomcatCompatibleServletContainer(Project project, String servletContainer) {
    def config = getConfig(servletContainer)
    if(config.servletContainerType == 'tomcat')
      return servletContainer
    def compatibleConfigEntry = getConfigs().find { name, c ->
      c.servletContainerType == 'tomcat' && c.servletApiVersion(project) == config.servletApiVersion(project)
    }
    if(compatibleConfigEntry)
      return compatibleConfigEntry.key
    String defaultJettyServletContainer = 'tomcat10'
    log.warn 'Cannot find tomcat container with compatible servlet-api to {}, defaulting to {}', servletContainer, defaultJettyServletContainer
    defaultJettyServletContainer
  }
}
