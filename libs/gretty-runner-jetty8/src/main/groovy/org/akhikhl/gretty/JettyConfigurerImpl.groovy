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
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.bio.SocketConnector
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.session.HashSessionManager
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.server.ssl.SslSocketConnector
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.log.Logger
import org.eclipse.jetty.util.log.StdErrLog
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.webapp.*
import org.eclipse.jetty.xml.XmlConfiguration

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class JettyConfigurerImpl implements JettyConfigurer {

  private static final Logger log = Log.getLogger(JettyConfigurerImpl)

  private SSOAuthenticatorFactory ssoAuthenticatorFactory
  private HashSessionManager sharedSessionManager

  @Override
  def beforeStart(boolean isDebug) {
    setLevel( 'org.akhikhl.gretty', isDebug ? StdErrLog.LEVEL_DEBUG : StdErrLog.LEVEL_INFO)
    setLevel('org.eclipse.jetty', StdErrLog.LEVEL_WARN)
    setLevel('org.eclipse.jetty.annotations.AnnotationConfiguration', StdErrLog.LEVEL_WARN + 1)
    setLevel('org.eclipse.jetty.annotations.AnnotationParser', StdErrLog.LEVEL_WARN + 1)
    setLevel('org.eclipse.jetty.util.component.AbstractLifeCycle', StdErrLog.LEVEL_WARN + 1)
  }

  private static setLevel(String loggerName, int level) {
    def logger = Log.getLogger(loggerName)
    if (logger instanceof StdErrLog) {
      ((StdErrLog) logger).setLevel(level)
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
      XmlConfiguration xmlConfiguration = new XmlConfiguration(contextConfigFile)
      xmlConfiguration.configure(webAppContext)
    }
  }

  @Override
  void applyJettyXml(server, String jettyXml) {
    if(jettyXml != null) {
      log.info 'Configuring server with {}', jettyXml
      XmlConfiguration xmlConfiguration = new XmlConfiguration(new File(jettyXml).toURI().toURL())
      xmlConfiguration.configure(server)
    }
  }

  @Override
  void configureConnectors(server, Map params) {

    Connector httpConn = findHttpConnector(server)

    boolean newConnector = false
    if(params.httpEnabled && !httpConn) {
      newConnector = true
      httpConn = new SocketConnector()
      httpConn.soLingerTime = -1
    }

    if(httpConn) {
      if(!httpConn.host)
        httpConn.host = params.host ?: ServerDefaults.defaultHost

      if(!httpConn.port)
        httpConn.port = params.httpPort ?: ServerDefaults.defaultHttpPort

      if(httpConn.port == ServerDefaults.RANDOM_FREE_PORT)
        httpConn.port = 0

      if(params.httpIdleTimeout)
        httpConn.maxIdleTime = params.httpIdleTimeout

      if(newConnector)
        server.addConnector(httpConn)
    }

    Connector httpsConn = findHttpsConnector(server)

    boolean newHttpsConnector = false
    if(params.httpsEnabled && !httpsConn) {
      newHttpsConnector = true
      httpsConn = new SslSocketConnector(new SslContextFactory())
      httpsConn.soLingerTime = -1
    }

    if(httpsConn) {
      if(!httpsConn.host)
        httpsConn.host = params.host ?: ServerDefaults.defaultHost

      if(!httpsConn.port)
        httpsConn.port = params.httpsPort ?: ServerDefaults.defaultHttpsPort

      if(httpsConn.port == ServerDefaults.RANDOM_FREE_PORT)
        httpsConn.port = 0

      def sslContextFactory = httpsConn.getSslContextFactory()
      if(params.sslKeyStorePath) {
        if(params.sslKeyStorePath.startsWith('classpath:')) {
          String resString = params.sslKeyStorePath - 'classpath:'
          URL url = getClass().getResource(resString)
          if(url == null)
            throw new Exception("Could not resource referenced in sslKeyStorePath: '${resString}'")
          sslContextFactory.setKeyStoreResource(Resource.newResource(url))
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
          sslContextFactory.setTrustStoreResource(Resource.newResource(url))
        }
        else
          sslContextFactory.setTrustStorePath(params.sslTrustStorePath)
      }
      if(params.sslTrustStorePassword)
        sslContextFactory.setTrustStorePassword(params.sslTrustStorePassword)
      if(params.sslNeedClientAuth)
        sslContextFactory.setNeedClientAuth(params.sslNeedClientAuth)

      if(params.httpsIdleTimeout)
        httpsConn.maxIdleTime = params.httpsIdleTimeout

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
    HashSessionManager sessionManager
    if(serverParams.singleSignOn) {
      sessionManager = sharedSessionManager
      if(sessionManager == null) {
        sessionManager = sharedSessionManager = new HashSessionManager() {

        }
        sessionManager.setMaxInactiveInterval(60 * 30) // 30 minutes
        sessionManager.getSessionCookieConfig().setPath('/')
      }
    } else {
      sessionManager = new HashSessionManager()
      sessionManager.setMaxInactiveInterval(60 * 30) // 30 minutes
    }
    def sessionHandler = new SessionHandler(sessionManager)
    // By setting server we fix bug with older jetty versions: sessionHandler produces NullPointerException, when session manager is reassigned.
    sessionHandler.setServer(server)
    context.setSessionHandler(sessionHandler)
  }

  @Override
  def createResourceCollection(List paths) {
    new ResourceCollection(paths as String[])
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
    if (webappParams.webXml != null) context.setDescriptor(webappParams.webXml);
    context.setAttribute('org.eclipse.jetty.server.webapp.ContainerExcludeJarPattern', getAnnotationExcludeJarPattern())
    return context
  }

  @Override
  def findHttpConnector(server) {
    server.connectors.find { (it instanceof SocketConnector) && !(it instanceof SslSocketConnector) }
  }

  @Override
  def findHttpsConnector(server) {
    server.connectors.find { it instanceof SslSocketConnector }
  }

  @Override
  URL findResourceURL(baseResource, String path) {
    Resource res
    if(baseResource instanceof ResourceCollection)
      res = baseResource.findResource(path)
    else
      res = baseResource.addPath(path)
    if(res.exists())
      return res.getURL()
    null
  }

  private String getAnnotationExcludeJarPattern() {
    String result
    URLConnection resConn = this.getClass().getResource('AnnotationExcludeJarPattern.txt').openConnection()
    // this fixes exceptions when reloading classes in running application
    resConn.setUseCaches(false)
    resConn.getInputStream().withStream {
      result = it.readLines('UTF-8').join('|')
    }
    result
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
      new AnnotationConfigurationEx(webappParams.webappClassPath),
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
    return null
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
    collection.manage(handler)
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
    // There is no "error" in Jetty's log facade.
    log.warn(message, args)
  }
}
