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
import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.logging.JettyLevel
import org.eclipse.jetty.logging.JettyLogger
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.util.resource.PathResource
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.*
import org.eclipse.jetty.xml.XmlConfiguration
import org.slf4j.LoggerFactory

/**
 * Jetty 11 specific implementation of JettyConfigurer.
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class JettyConfigurerImpl extends JettyConfigurerBase {

  private SSOAuthenticatorFactory ssoAuthenticatorFactory

  @Override
  def beforeStart(boolean isDebug) {
    setLevel('org.akhikhl.gretty', isDebug ? JettyLevel.DEBUG : JettyLevel.INFO)
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
  protected void setSslKeyStoreResource(sslContextFactory, URL url) {
    sslContextFactory.setKeyStoreResource(new PathResource(url))
  }

  @Override
  protected void setSslTrustStoreResource(sslContextFactory, URL url) {
    sslContextFactory.setTrustStoreResource(new PathResource(url))
  }

  @Override
  def createServer() {
    // fix for issue https://github.com/akhikhl/gretty/issues/24
    org.eclipse.jetty.util.resource.Resource.defaultUseCaches = false
    return new org.eclipse.jetty.server.Server()
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
    new ResourceCollection(paths as String[])
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
  protected void setConfigurationsToWebAppContextImpl(webAppContext, List configurations) {
    webAppContext.setConfigurations(configurations as Configuration[])
  }

  @Override
  void setWebAppResourceBaseOrWar(context, String resourceBase, boolean isDirectory) {
    if (isDirectory)
      context.setResourceBase(resourceBase)
    else
      context.setWar(resourceBase)
  }
}
