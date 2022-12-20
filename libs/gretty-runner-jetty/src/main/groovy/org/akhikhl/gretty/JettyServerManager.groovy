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
final class JettyServerManager implements ServerManager {

  private JettyConfigurer configurer
  protected Map params
	protected server

  JettyServerManager(JettyConfigurer configurer) {
    this.configurer = configurer
  }

  @Override
  void setParams(Map params) {
    this.params = params
  }

  @Override
  ServerStartEvent startServer() {
    assert server == null
    configurer.beforeStart(params.getOrDefault('debug', true))
    configurer.debug '{} starting.', params.servletContainerDescription

    JettyServerConfigurer serverConfigurer = createServerConfigurer()
    server = serverConfigurer.createAndConfigureServer()

    boolean result = false
    try {
      /*
       * First we start jetty without any webapp and then add webapps one by one
       * to make sure that web server is ready as soon as possible, so
       * later loaded apps can send web requests to early loaded ones.
       */
      server.start()

      File baseDir = new File(params.baseDir)
      for(Map webapp in params.webApps) {
          def context = serverConfigurer.createContext(webapp, baseDir, server)
          configurer.addHandlerToServer(server, context)
      }

      result = true
    } catch(Throwable x) {
      configurer.error 'Error starting server', x
      if(x.getClass().getName() == 'org.eclipse.jetty.util.MultiException') {
        for(Throwable xx in x.getThrowables())
          log.error 'Error', xx
      }

      Map startInfo = new JettyServerStartInfo().getInfo(server, configurer, params)
      startInfo.status = 'error starting server'
      startInfo.error = true
      startInfo.errorMessage = x.getMessage() ?: x.getClass().getName()
      StringWriter sw = new StringWriter()
      x.printStackTrace(new PrintWriter(sw))
      startInfo.stackTrace = sw.toString()
      return new ServerStartEvent(startInfo)
    }

    if(result) {
      Map startInfo = new JettyServerStartInfo().getInfo(server, configurer, params)
      configurer.debug '{} started.', params.servletContainerDescription
      return new ServerStartEvent(startInfo)
    }

    throw new IllegalStateException()
  }

  private JettyServerConfigurer createServerConfigurer() {
    new JettyServerConfigurer(configurer, params)
  }

  @Override
  void stopServer() {
    if(server != null) {
      configurer.debug '{} stopping.', params.servletContainerDescription
      server.stop()
      server = null
      configurer.debug '{} stopped.', params.servletContainerDescription
    }
  }

  @Override
  void redeploy(List<String> contextPaths) {
    configurer.debug('redeploying {}.', contextPaths.join(' '))
    def handlers = configurer.getHandlersByContextPaths(server, contextPaths)
    handlers.each {
      configurer.error("removing handlers: ${it}")
      configurer.removeHandlerFromServer(server, it)
    }
    //
    contextPaths.collect { contextPath ->
      params.webApps.find { it.contextPath == contextPath }
    }.each {
      def context = createServerConfigurer().createContext(it, new File(params.baseDir), server)
      configurer.addHandlerToServer(server, context)
      context
    }
  }
}
