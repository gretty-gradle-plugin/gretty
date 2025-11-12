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
import org.eclipse.jetty.logging.JettyLevel
import org.eclipse.jetty.logging.JettyLogger
import org.eclipse.jetty.ee10.plus.webapp.EnvConfiguration
import org.eclipse.jetty.ee10.plus.webapp.PlusConfiguration
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.eclipse.jetty.util.resource.PathResource
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceFactory
import org.eclipse.jetty.ee10.webapp.*
import org.eclipse.jetty.xml.XmlConfiguration
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.net.URI

/**
 * Jetty 12 specific implementation of JettyConfigurer.
 *
 * Supports major Jetty features including:
 * - Multiple resource bases via ResourceFactory.combine()
 * - Custom WebAppContext configuration
 * - SSL/TLS with configurable keystores
 *
 * Note: Single Sign-On (SSO) is not currently supported in Jetty 12 due to
 * significant API changes in the Jetty 12 Core security architecture.
 * SSO support may be added in a future release once Jetty provides appropriate hooks.
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class JettyConfigurerImpl extends JettyConfigurerBase {

  @Override
  def beforeStart(boolean isDebug) {
    setLevel('org.akhikhl.gretty', isDebug ? JettyLevel.DEBUG : JettyLevel.INFO)
    setLevel('org.eclipse.jetty', JettyLevel.WARN)
    setLevel('org.eclipse.jetty.ee10.annotations.AnnotationConfiguration', JettyLevel.ERROR)
    setLevel('org.eclipse.jetty.ee10.annotations.AnnotationParser', JettyLevel.ERROR)
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
    // Note: Resource.defaultUseCaches was removed in Jetty 12
    return new org.eclipse.jetty.server.Server()
  }

  @Override
  void configureSecurity(context, String realm, String realmConfigFile, boolean singleSignOn) {
    def loginService = new HashLoginService(realm, new PathResource(Path.of(realmConfigFile)))
    context.securityHandler.loginService = loginService
    if(singleSignOn) {
      log.warn 'Single Sign-On is not currently supported in Jetty 12 due to API changes. ' +
               'SSO configuration will be ignored. Use Jetty 11 if SSO is required.'
    }
  }

  @Override
  void configureSessionManager(server, context, Map serverParams, Map webappParams) {
    SessionHandler sessionHandler = context.getSessionHandler()
    if (sessionHandler == null) {
      sessionHandler = new SessionHandler()
      sessionHandler.setServer(server)
      context.setSessionHandler(sessionHandler)
    } else {
      sessionHandler.setServer(server)
    }
    sessionHandler.setMaxInactiveInterval(60 * 30) // 30 minutes
    if(serverParams.singleSignOn) {
      log.warn 'Single Sign-On session handling is not currently supported in Jetty 12. ' +
               'Standard session handling will be used instead.'
    }
  }

  @Override
  def createResourceCollection(List paths) {
    // In Jetty 12, ResourceCollection is now CombinedResource created via ResourceFactory.combine()
    if(!paths || paths.isEmpty()) {
      return null
    }

    // Create ResourceFactory once, not per-path
    ResourceFactory factory = ResourceFactory.of(this)

    // Convert paths to Resources
    List<Resource> resources = paths.collect { path ->
      factory.newResource(Path.of(path.toString()))
    }.findAll { it != null && it.exists() }

    if(resources.isEmpty()) {
      return null
    }

    // Use ResourceFactory.combine() to create a CombinedResource
    return ResourceFactory.combine(resources)
  }

  @Override
  def createWebAppContext(Map serverParams, Map webappParams) {
    List<String> webappClassPath = webappParams.webappClassPath
    JettyWebAppContext context = new JettyWebAppContext()
    context.setThrowUnavailableOnStartupException(true)
    context.setWebInfLib(webappClassPath.findAll { it.endsWith('.jar') }.collect { new File(it) })

    // In Jetty 12, setExtraClasspath requires directories to end with "/"
    if(webappClassPath && !webappClassPath.isEmpty()) {
      // Convert file:// URLs to paths first, then filter and format
      List<String> validPaths = webappClassPath.collect { pathStr ->
        // Convert file:// URLs to absolute paths once
        pathStr.startsWith('file:') ? new File(new URI(pathStr)).absolutePath : pathStr
      }.findAll { path ->
        // Filter out non-existent paths
        new File(path).exists()
      }.collect { path ->
        // Add trailing slash to directories (JARs don't need it)
        path.endsWith('.jar') ? path : (path.endsWith('/') ? path : path + '/')
      }

      if (!validPaths.isEmpty()) {
        String extraClasspath = validPaths.join(',')
        context.setExtraClasspath(extraClasspath)
        log.info "Set extra classpath for Jetty 12: ${extraClasspath}"
      }
    }

    context.setInitParameter('org.eclipse.jetty.servlet.Default.useFileMappedBuffer', serverParams.productMode ? 'true' : 'false')
    context.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN,
        '.*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$');

    context.addServerClassMatcher(new ClassMatcher().tap {
      // Server classes must be available from the server classloader
      // This includes Jetty servlet classes needed by DefaultServlet in webdefault.xml
      exclude 'org.eclipse.jetty.ee10.servlet.'
      exclude 'org.eclipse.jetty.ee10.servlet.listener.'
      // JSP classes must also be available from server classloader for Jetty to find JettyJspServlet
      exclude 'org.eclipse.jetty.ee10.jsp.'
      exclude 'org.eclipse.jetty.ee10.apache.jsp.'
      // Apache Jasper and Juli classes for JSP compilation
      exclude 'org.apache.jasper.'
      exclude 'org.apache.juli.'
    })

    context.addSystemClassMatcher(new ClassMatcher().tap {
      // System classes must be loaded by the app class loader to avoid type mismatches
      include 'jakarta.'
    })

    return context
  }

  @Override
  URL findResourceURL(baseResource, String path) {
    // In Jetty 12, addPath() was removed, use resolve() instead
    Resource res = baseResource.resolve(path)
    if(res != null && res.exists())
      return res.getURI().toURL()
    null
  }

  @Override
  List getConfigurations(Map webappParams) {
    def webInfConfig = new WebInfConfigurationEx()
    // Pass webapp classpath to the configuration
    if (webappParams.webappClassPath) {
      webInfConfig.setWebappClassPath(webappParams.webappClassPath as List<String>)
    }
    [
      webInfConfig,
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
    // In Jetty 12, Resource API changed - use ResourceFactory to create resources
    if (isDirectory) {
      // Use the context's ResourceFactory to create a Resource from the path
      def resource = context.getResourceFactory().newResource(Path.of(resourceBase))
      context.setBaseResource(resource)
    }
    else
      context.setWar(resourceBase)
  }
}
