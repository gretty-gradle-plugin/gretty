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
/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class JettyServerConfigurer {

  protected final JettyConfigurer configurer
  protected final Map params

  JettyServerConfigurer(JettyConfigurer configurer, Map params) {
    this.configurer = configurer
    this.params = params
  }

  protected void configureWithBaseResource(Map webapp, context) {

    URL contextConfigFile = getContextConfigFile(context.baseResource, params.servletContainerId)
    if (!contextConfigFile && webapp.contextConfigFile)
      contextConfigFile = new File(webapp.contextConfigFile).toURI().toURL()
    configurer.applyContextConfigFile(context, contextConfigFile)

    String realm = webapp.realm ?: params.realm
    File realmConfigFile
    URL realmConfigFileUrl = getRealmFile(context.baseResource, params.servletContainerId)
    if (realmConfigFileUrl) {
      realmConfigFile = new File(realmConfigFileUrl.toURI()).canonicalFile
    } else {
      if (webapp.realmConfigFile) {
        realmConfigFile = new File(webapp.realmConfigFile).canonicalFile
      }
      else if (params.realmConfigFile) {
        realmConfigFile = new File(params.realmConfigFile).canonicalFile
      }
    }
    if (realm && realmConfigFile) {
      if (context.securityHandler.loginService == null) {
        configurer.info 'Configuring {} with realm \'{}\', {}', context.contextPath, realm, realmConfigFile
        configurer.configureSecurity(context, realm, realmConfigFile.toString(), params.singleSignOn ?: false)
      } else
        configurer.warn 'loginService is already configured, ignoring realm \'{}\', {}', realm, realmConfigFile
    }
  }

  def createAndConfigureServer(Closure configureContext = null) {

    def server = configurer.createServer()

    new File(params.baseDir, 'webapps').mkdirs()

    configurer.applyJettyXml(server, params.serverConfigFile)
    configurer.configureConnectors(server, params)
    configurer.setHandlersToServer(server, [])
    return server
  }

  def createContext(Map webapp, File baseDir, server, Closure configureContext = null) {
    def context = configurer.createWebAppContext(params, webapp)
    context.displayName = webapp.contextPath
    context.contextPath = webapp.contextPath

    if (!params.supressSetConfigurations) {
      List configurations = configurer.getConfigurations(webapp)
      BaseResourceConfiguration baseRes = configurations.find { it instanceof BaseResourceConfiguration }
      if (baseRes) {
        baseRes.setExtraResourceBases(webapp.extraResourceBases)
        baseRes.addBaseResourceListener this.&configureWithBaseResource.curry(webapp)
      }
      configurer.setConfigurationsToWebAppContext(context, configurations)
    }

    File tempDir = new File(baseDir, 'webapps-exploded' + context.contextPath)
    tempDir.mkdirs()
    configurer.debug 'jetty context temp directory: {}', tempDir
    context.setTempDirectory(tempDir)
    if (context.respondsTo('setPersistTempDirectory')) // not supported on older jetty versions
      context.setPersistTempDirectory(true)

    webapp.initParameters?.each { key, value ->
      context.setInitParameter(key, value)
    }

    File resourceFile = new File(webapp.resourceBase)

    if (resourceFile.isDirectory()) {
      def aliases = ["/": webapp.resourceBase]
      context.setResourceAliases(aliases)
    }
    else
      context.setWar(webapp.resourceBase)

    configurer.configureSessionManager(server, context, params, webapp)

    if (configureContext) {
      configureContext.delegate = this
      configureContext(webapp, context)
    }

    if (webapp.springBoot) {
      context.setInitParameter("GRETTY_SPRING_BOOT_MAIN_CLASS", webapp.springBootMainClass)
    }
    context
  }

  URL getContextConfigFile(baseResource, String servletContainer) {
    for(def possibleFileName in [ servletContainer + '-env.xml', 'jetty-env.xml' ]) {
      URL url = configurer.findResourceURL(baseResource, 'META-INF/' + possibleFileName)
      if(url) {
        configurer.info 'resolved {} to {}', possibleFileName, url
        return url
      }
    }
    null
  }

  URL getRealmFile(baseResource, String servletContainer) {
    for(def possibleFileName in [ servletContainer + '-realm.properties', 'jetty-realm.properties' ]) {
      URL url = configurer.findResourceURL(baseResource, 'META-INF/' + possibleFileName)
      if(url)
        return url
    }
    null
  }
}
