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
import org.apache.catalina.core.StandardContext
import org.apache.catalina.startup.Tomcat
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class TomcatServerManager implements ServerManager {

  private static final Log log = LogFactory.getLog(TomcatServerManager)

  private TomcatConfigurer configurer
  protected Map params
	protected Tomcat tomcat

  TomcatServerManager(TomcatConfigurer configurer) {
    this.configurer = configurer
  }

  private TomcatServerConfigurer createServerConfigurer() {
    return new TomcatServerConfigurer(configurer, params);
  }

  @Override
  void setParams(Map params) {
    this.params = params
  }

  @Override
  ServerStartEvent startServer() {
    assert tomcat == null

    log.debug "${params.servletContainerDescription} starting."

    TomcatServerConfigurer serverConfigurer = createServerConfigurer()
    tomcat = serverConfigurer.createAndConfigureServer()

    boolean result = false
    try {
      /*
       * First we start tomcat without any webapp and then add webapps one by one
       * to make sure that web server is ready as soon as possible, so
       * later loaded apps can send web requests to early loaded ones.
       */
      tomcat.start()

      for(Map webapp in params.webApps) {
        StandardContext context = serverConfigurer.createContext(webapp, tomcat)
        tomcat.host.addChild(context)
      }

      result = true
    } catch(Throwable x) {
      log.error 'Error starting server', x
      Map startInfo = new TomcatServerStartInfo().getInfo(tomcat, null, params)
      startInfo.status = 'error starting server'
      startInfo.error = true
      startInfo.errorMessage = x.getMessage() ?: x.getClass().getName()
      StringWriter sw = new StringWriter()
      x.printStackTrace(new PrintWriter(sw))
      startInfo.stackTrace = sw.toString()
      return new ServerStartEvent(startInfo)
    }

    if(result) {
      Map startInfo = new TomcatServerStartInfo().getInfo(tomcat, null, params)
      log.debug "${params.servletContainerDescription} started."
      return new ServerStartEvent(startInfo)
    }

    throw new IllegalStateException()
  }

  @Override
  void stopServer() {
    if(tomcat != null) {
      log.debug "${params.servletContainerDescription} stopping."
      tomcat.stop()
      tomcat.getServer().await()
      tomcat.destroy()
      tomcat = null
      log.debug "${params.servletContainerDescription} stopped."
    }
  }

  @Override
  void redeploy(List<String> webapps) {
    if(tomcat != null) {
      log.debug "redeploying ${webapps.join(", ")}."
      def containers = webapps.collect { TomcatServerConfigurer.getEffectiveContextPath(it) }.collect { tomcat.host.findChild(it) }
      //
      containers.each { tomcat.host.removeChild(it) }
      webapps.collect { contextPath -> params.webApps.find { it.contextPath == contextPath}}.each {
        def context = createServerConfigurer().createContext(it, tomcat)
        tomcat.host.addChild(context)
      }
    }
  }
}
