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
import org.eclipse.jetty.ee10.annotations.AnnotationConfiguration
import org.eclipse.jetty.ee10.plus.webapp.EnvConfiguration
import org.eclipse.jetty.ee10.plus.webapp.PlusConfiguration
import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.eclipse.jetty.ee10.webapp.Configuration
import org.eclipse.jetty.ee10.webapp.FragmentConfiguration
import org.eclipse.jetty.ee10.webapp.JettyWebXmlConfiguration
import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration
import org.eclipse.jetty.ee10.webapp.WebXmlConfiguration
import org.eclipse.jetty.logging.JettyLevel
import org.eclipse.jetty.logging.JettyLogger

import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.util.ClassMatcher
import org.eclipse.jetty.util.resource.PathResource
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceFactory
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.xml.XmlConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class JettyConfigurerImpl implements JettyConfigurer {

  private static final Logger log = LoggerFactory.getLogger(JettyConfigurerImpl)

  private SSOAuthenticatorFactory ssoAuthenticatorFactory

  @Override
  def beforeStart(boolean isDebug) {
    setLevel( 'org.akhikhl.gretty', isDebug ? JettyLevel.DEBUG : JettyLevel.INFO)
    setLevel('org.eclipse.jetty', JettyLevel.WARN)
    setLevel('org.eclipse.jetty.annotations.AnnotationConfiguration', JettyLevel.ERROR)
    setLevel('org.eclipse.jetty.annotations.AnnotationParser', JettyLevel.ERROR)
    setLevel('org.eclipse.jetty.util.component.AbstractLifeCycle', JettyLevel.ERROR)
  }

  private static setLevel(String loggerName, JettyLevel level) {
    def logger = LoggerFactory.getLogger(loggerName)
    if (logger instanceof JettyLogger) {
      ((JettyLogger) logger).setLevel(level)
    }
  }

  @Override
  def addLifeCycleListener(lifecycle, listener) {
    lifecycle.addLifeCycleListener(listener)
    listener
  }

  @Override
  void applyContextConfigFile(webAppContext, URL contextConfigFile) {
    if(contextConfigFile) {
      log.info 'Configuring {} with {}', webAppContext.contextPath, contextConfigFile
      XmlConfiguration xmlConfiguration = new XmlConfiguration(new PathResource(contextConfigFile))
      xmlConfiguration.configure(webAppContext)
    }
  }

  @Override
  void applyJettyXml(server, String jettyXml) {
    if(jettyXml != null) {
      log.info 'Configuring server with {}', jettyXml
      XmlConfiguration xmlConfiguration = new XmlConfiguration(new PathResource(new File(jettyXml)))
      xmlConfiguration.configure(server)
    }
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
        if(params.sslKeyStorePath) {
          if(params.sslKeyStorePath.startsWith('classpath:')) {
            String resString = params.sslKeyStorePath - 'classpath:'
            URL url = getClass().getResource(resString)
            if(url == null)
              throw new Exception("Could not resource referenced in sslKeyStorePath: '${resString}'")
            sslContextFactory.setKeyStoreResource(new PathResource(url))
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
            sslContextFactory.setTrustStoreResource(new PathResource(url))
          }
          else
            sslContextFactory.setTrustStorePath(params.sslTrustStorePath)
        }
        if(params.sslTrustStorePassword)
          sslContextFactory.setTrustStorePassword(params.sslTrustStorePassword)
        if(params.sslNeedClientAuth)
          sslContextFactory.setNeedClientAuth(params.sslNeedClientAuth)
      }

      if(params.httpsIdleTimeout)
        httpsConn.idleTimeout = params.httpsIdleTimeout

      if(newHttpsConnector)
        server.addConnector(httpsConn)
    }
  }

  @Override
  void configureSecurity(context, String realm, String realmConfigFile, boolean singleSignOn) {
    context.securityHandler.loginService = new HashLoginService(realm, realmConfigFile)
    if(singleSignOn) {
      if(ssoAuthenticatorFactory == null)
        ssoAuthenticatorFactory = new SSOAuthenticatorFactory()
      context.securityHandler.authenticatorFactory = ssoAuthenticatorFactory
    }
  }

  @Override
  void configureSessionManager(server, context, Map serverParams, Map webappParams) {
    SessionHandler sessionHandler
    if(serverParams.singleSignOn) {
      sessionHandler = new SingleSignOnSessionHandler()
      sessionHandler.setMaxInactiveInterval(60 * 30) // 30 minutes
      sessionHandler.getSessionCookieConfig().setPath('/')
    } else {
      sessionHandler = new SessionHandler()
      sessionHandler.setMaxInactiveInterval(60 * 30) // 30 minutes
    }
    context.setSessionHandler(sessionHandler)
  }

  @Override
  def createResourceCollection(List paths) {
    return paths.collect { path -> ResourceFactory.root().newResource(path) }
  }

  @Override
  def createServer() {
    // fix for issue https://github.com/akhikhl/gretty/issues/24
    org.eclipse.jetty.util.resource.Resource.defaultUseCaches = false
    return new Server()
  }

  @Override
  def createWebAppContext(Map serverParams, Map webappParams) {
    List<String> webappClassPath = webappParams.webappClassPath
    JettyWebAppContext context = new JettyWebAppContext()
    context.setThrowUnavailableOnStartupException(true)
    context.setWebInfLib(webappClassPath.findAll { it.endsWith('.jar') }.collect { new File(it) })
    context.setExtraClasspath(webappClassPath.collect { it.endsWith('.jar') ? it : (it.endsWith('/') ? it : it + '/') }.join(';'))
    context.setInitParameter('org.eclipse.jetty.servlet.Default.useFileMappedBuffer', serverParams.productMode ? 'true' : 'false')
    context.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN,
        '.*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$');

    context.addSystemClassMatcher(new ClassMatcher().tap {
      // I do not know if the servlet classes are system classes in the truest sense.
      // However, those must be loaded by the app class loader. Otherwise the check in
      // org.eclipse.jetty.servlet.ServletHolder#checkServletType does not succeed, because
      // class HttpServlet (loaded from the web app class loader) is not assignable to class
      // HttpServlet (loaded from the app class loader). Hence the error message 'YourServlet
      // is not a Jakarta servlet'.
      include 'jakarta.'
    })

    return context
  }

  @Override
  def findHttpConnector(server) {
    server.connectors.find { it.connectionFactories.find { it.protocol.startsWith('HTTP') } && !it.connectionFactories.find { it.protocol.startsWith('SSL') } }
  }

  @Override
  def findHttpsConnector(server) {
    server.connectors.find { it.connectionFactories.find { it.protocol.startsWith('HTTP') } && it.connectionFactories.find { it.protocol.startsWith('SSL') } }
  }

  @Override
  URL findResourceURL(baseResource, String path) {
    Resource res = baseResource.addPath(path)
    if(res.exists())
      return res.getURI().toURL()
    null
  }

  @Override
  List getConfigurations(Map webappParams) {
    [
            new WebInfConfigurationEx(),
            new WebXmlConfiguration(),
            new MetaInfConfiguration(),
            new FragmentConfiguration(),
            new EnvConfiguration(),
            new PlusConfiguration(),
            new AnnotationConfiguration(),
            new JettyWebXmlConfiguration()
    ]
  }

  @Override
  void removeLifeCycleListener(lifecycle, listener) {
    lifecycle.removeLifeCycleListener(listener)
  }

  @Override
  void setConfigurationsToWebAppContext(webAppContext, List configurations) {
    webAppContext.setConfigurations(configurations as Configuration[])
  }

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
