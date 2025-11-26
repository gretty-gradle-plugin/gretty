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
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Base class for Jetty configurers, containing common logic shared across Jetty versions.
 * Subclasses implement version-specific behavior.
 */
@CompileStatic(TypeCheckingMode.SKIP)
abstract class JettyConfigurerBase implements JettyConfigurer {

  protected static final Logger log = LoggerFactory.getLogger(JettyConfigurerBase)

  // ========== Abstract Methods - Version Specific ==========

  /**
   * Create and configure a WebAppContext for the given parameters.
   * This is version-specific due to API changes between Jetty versions.
   */
  abstract def createWebAppContext(Map serverParams, Map webappParams)

  /**
   * Create a resource collection from the given paths.
   * ResourceCollection API changed between Jetty versions.
   */
  abstract def createResourceCollection(List paths)

  /**
   * Configure security for the webapp context.
   * SSO support varies between versions.
   */
  abstract void configureSecurity(context, String realm, String realmConfigFile, boolean singleSignOn)

  /**
   * Configure session manager for the webapp.
   * Session handling differs between versions, especially for SSO.
   */
  abstract void configureSessionManager(server, context, Map serverParams, Map webappParams)

  /**
   * Get the list of configurations for the webapp.
   * Configuration classes are in different packages per version.
   */
  abstract List getConfigurations(Map webappParams)

  /**
   * Apply context configuration from file.
   * PathResource API may differ between versions.
   */
  abstract void applyContextConfigFile(webAppContext, URL contextConfigFile)

  /**
   * Apply Jetty XML configuration to server.
   * XmlConfiguration API may differ between versions.
   */
  abstract void applyJettyXml(server, String jettyXml)

  /**
   * Find a resource URL within the base resource.
   * Resource API differs between versions.
   */
  abstract URL findResourceURL(baseResource, String path)

  /**
   * Setup logging levels for Jetty.
   * Logging API differs between versions.
   */
  abstract def beforeStart(boolean isDebug)

  /**
   * Set the resource base or WAR file for the web app context.
   * API changed between Jetty versions (setResourceBase vs setBaseResource).
   */
  abstract void setWebAppResourceBaseOrWar(context, String resourceBase, boolean isDirectory)

  // ========== Common Implementation ==========

  @Override
  def addLifeCycleListener(lifecycle, listener) {
    lifecycle.addLifeCycleListener(listener)
    listener
  }

  @Override
  void configureConnectors(server, Map params) {

    HttpConfiguration http_config = new HttpConfiguration()
    if(params.httpsPort) {
      http_config.setSecureScheme('https')
      http_config.setSecurePort(params.httpsPort)
    }

    Connector httpConn = findHttpConnector(server)

    boolean newHttpConnector = false
    if(params.httpEnabled && !httpConn) {
      newHttpConnector = true
      httpConn = new ServerConnector(server, new HttpConnectionFactory(http_config))
    }

    if(httpConn) {
      if(!httpConn.host)
        httpConn.host = params.host ?: ServerDefaults.defaultHost

      if(!httpConn.port)
        httpConn.port = params.httpPort ?: ServerDefaults.defaultHttpPort

      if(httpConn.port == ServerDefaults.RANDOM_FREE_PORT)
        httpConn.port = 0

      if(params.httpIdleTimeout)
        httpConn.idleTimeout = params.httpIdleTimeout

      if(newHttpConnector)
        server.addConnector(httpConn)
    }

    Connector httpsConn = findHttpsConnector(server)

    boolean newHttpsConnector = false
    if(params.httpsEnabled && !httpsConn) {
      newHttpsConnector = true
      HttpConfiguration https_config = new HttpConfiguration(http_config)
      https_config.addCustomizer(new SecureRequestCustomizer())
      httpsConn = new ServerConnector(server,
        new SslConnectionFactory(new SslContextFactory.Server(), 'http/1.1'),
        new HttpConnectionFactory(https_config))
    }

    if(httpsConn) {
      if(!httpsConn.host)
        httpsConn.host = params.host ?: ServerDefaults.defaultHost

      if(!httpsConn.port)
        httpsConn.port = params.httpsPort ?: ServerDefaults.defaultHttpsPort

      if(httpsConn.port == ServerDefaults.RANDOM_FREE_PORT)
        httpsConn.port = 0

      def sslContextFactory = httpsConn.getConnectionFactories().find { it instanceof SslConnectionFactory }?.getSslContextFactory()
      if(sslContextFactory) {
        configureSslContextFactory(sslContextFactory, params)
      }

      if(params.httpsIdleTimeout)
        httpsConn.idleTimeout = params.httpsIdleTimeout

      if(newHttpsConnector)
        server.addConnector(httpsConn)
    }
  }

  protected void configureSslContextFactory(sslContextFactory, Map params) {
    if(params.sslKeyStorePath) {
      if(params.sslKeyStorePath.startsWith('classpath:')) {
        String resString = params.sslKeyStorePath - 'classpath:'
        URL url = getClass().getResource(resString)
        if(url == null)
          throw new Exception("Could not resource referenced in sslKeyStorePath: '${resString}'")
        setSslKeyStoreResource(sslContextFactory, url)
      }
      else
        sslContextFactory.setKeyStorePath(params.sslKeyStorePath)
    }
    if(params.sslKeyStorePassword)
      sslContextFactory.setKeyStorePassword(params.sslKeyStorePassword)
    if(params.sslKeyManagerPassword)
      sslContextFactory.setKeyManagerPassword(params.sslKeyManagerPassword)
    if(params.sslTrustStorePath) {
      if(params.sslTrustStorePath.startsWith('classpath:')) {
        String resString = params.sslTrustStorePath - 'classpath:'
        URL url = getClass().getResource(resString)
        if(url == null)
          throw new Exception("Could not resource referenced in sslTrustStorePath: '${resString}'")
        setSslTrustStoreResource(sslContextFactory, url)
      }
      else
        sslContextFactory.setTrustStorePath(params.sslTrustStorePath)
    }
    if(params.sslTrustStorePassword)
      sslContextFactory.setTrustStorePassword(params.sslTrustStorePassword)
    if(params.sslNeedClientAuth)
      sslContextFactory.setNeedClientAuth(params.sslNeedClientAuth)
  }

  // Template method for version-specific SSL resource setting
  protected abstract void setSslKeyStoreResource(sslContextFactory, URL url)
  protected abstract void setSslTrustStoreResource(sslContextFactory, URL url)

  /**
   * Create a new Jetty server instance.
   * Version-specific because Resource.defaultUseCaches was removed in Jetty 12.
   */
  abstract def createServer()

  @Override
  def findHttpConnector(server) {
    server.connectors.find { it.connectionFactories.find { it.protocol.startsWith('HTTP') } && !it.connectionFactories.find { it.protocol.startsWith('SSL') } }
  }

  @Override
  def findHttpsConnector(server) {
    server.connectors.find { it.connectionFactories.find { it.protocol.startsWith('HTTP') } && it.connectionFactories.find { it.protocol.startsWith('SSL') } }
  }

  @Override
  void removeLifeCycleListener(lifecycle, listener) {
    lifecycle.removeLifeCycleListener(listener)
  }

  @Override
  void setConfigurationsToWebAppContext(webAppContext, List configurations) {
    setConfigurationsToWebAppContextImpl(webAppContext, configurations)
  }

  // Template method for version-specific configuration setting
  protected abstract void setConfigurationsToWebAppContextImpl(webAppContext, List configurations)

  private ContextHandlerCollection findContextHandlerCollection(Handler handler) {
    if(handler instanceof ContextHandlerCollection)
      return handler
    if(handler.respondsTo('getHandlers'))
      return handler.getHandlers().findResult { findContextHandlerCollection(it) }
    null
  }

  @Override
  void setHandlersToServer(server, List handlers) {
    ContextHandlerCollection contexts = findContextHandlerCollection(server.handler)
    if(!contexts)
      contexts = new ContextHandlerCollection()

    contexts.setHandlers(handlers as Handler[])
    if(server.handler == null)
      server.handler = contexts
  }

  @Override
  List getHandlersByContextPaths(server, List contextPaths) {
    ContextHandlerCollection context = findContextHandlerCollection(((Server)server).handler)
    return context.getHandlers().findAll {
      if(it.respondsTo("getContextPath")) {
        contextPaths.contains(it.getContextPath())
      }
    }
  }

  @Override
  void removeHandlerFromServer(server, handler) {
    def collection = findContextHandlerCollection(server.handler)
    collection.removeHandler(handler)
  }

  @Override
  void addHandlerToServer(server, handler) {
    def collection = findContextHandlerCollection(server.handler)
    collection.addHandler(handler)

    // we need to make new handler managed by the HandlerCollection
    // so it is stopped automatically when the whole server is stopped
    // or when handler is removed from collection.
    collection.manage(handler)

    // addHandler and manage don't start the context, so we need to do start it manually
    handler.start()
  }

  @Override
  def debug(String message, Object... args) {
    log.debug(message, args)
  }

  @Override
  def info(String message, Object... args) {
    log.info(message, args)
  }

  @Override
  def warn(String message, Object... args) {
    log.warn(message, args)
  }

  @Override
  def error(String message, Object... args) {
    log.error(message, args)
  }
}
