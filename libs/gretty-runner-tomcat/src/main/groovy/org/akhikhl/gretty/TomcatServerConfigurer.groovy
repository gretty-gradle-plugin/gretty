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
import org.apache.catalina.Host
import org.apache.catalina.Lifecycle
import org.apache.catalina.LifecycleEvent
import org.apache.catalina.LifecycleListener
import org.apache.catalina.authenticator.SingleSignOn
import org.apache.catalina.connector.Connector
import org.apache.catalina.core.StandardContext
import org.apache.catalina.realm.MemoryRealm
import org.apache.catalina.startup.Catalina
import org.apache.catalina.startup.Tomcat
import org.apache.catalina.startup.Tomcat.DefaultWebXmlListener
import org.apache.catalina.startup.Tomcat.FixContextListener
import org.apache.tomcat.util.net.SSLHostConfig
import org.xml.sax.InputSource

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class TomcatServerConfigurer {

  protected final TomcatConfigurer configurer
  protected final Map params

  TomcatServerConfigurer(TomcatConfigurer configurer, Map params) {
    this.configurer = configurer
    this.params = params
  }

  Tomcat createAndConfigureServer(Closure configureContext = null) {

    Tomcat tomcat = new Tomcat()

    if(params.enableNaming)
      tomcat.enableNaming()

    File baseDir = new File(params.baseDir)
    new File(baseDir, 'webapps').mkdirs()

    File tempDir = new File(baseDir, 'temp')

    def service
    def connectors

    if(params.serverConfigFile) {
      def catalina = new Catalina()
      def digester = catalina.createStartDigester()
      new File(params.serverConfigFile).withInputStream {
        def inputSource = new InputSource(params.serverConfigFile)
        inputSource.setByteStream(it)
        digester.push(catalina)
        digester.parse(inputSource)
      }
      def server = tomcat.server = catalina.getServer()
      def services = server.findServices()
      assert services.length == 1
      service = services[0]
      connectors = service.findConnectors()
      tomcat.host = service.getContainer().findChildren().find { it instanceof Host }
      tomcat.port = connectors[0].port
      tomcat.hostname = tomcat.host.name
      server.setCatalina(catalina)
      configurer.setBaseDir(tomcat, baseDir)
    } else {
      configurer.setBaseDir(tomcat, baseDir)
      tomcat.engine.backgroundProcessorDelay = -1
      tomcat.host.autoDeploy = true
      service = tomcat.service
      connectors = service.findConnectors()
    }

    if(!tomcat.hostname)
      tomcat.hostname = params.host ?: ServerDefaults.defaultHost

    Connector httpConn = connectors.find { it.scheme == 'http' }

    boolean newHttpConnector = false
    if(params.httpEnabled && !httpConn) {
      newHttpConnector = true
      httpConn = new Connector('HTTP/1.1')
      httpConn.scheme = 'http'
    }

    if(httpConn) {
      if(!httpConn.port || httpConn.port < 0)
        httpConn.port = params.httpPort ?: ServerDefaults.defaultHttpPort

      if(httpConn.port == ServerDefaults.RANDOM_FREE_PORT)
        httpConn.port = 0

      if(params.httpIdleTimeout)
        assert httpConn.setProperty('keepAliveTimeout', params.httpIdleTimeout.toString())

      httpConn.maxPostSize = -1 // unlimited post size
      httpConn.maxPartCount = -1

      if(newHttpConnector) {
        service.addConnector(httpConn)
        connectors = service.findConnectors()
      }
    }

    Connector httpsConn = connectors.find { it.scheme == 'https' }

    boolean newHttpsConnector = false
    if(params.httpsEnabled && !httpsConn) {
        newHttpsConnector = true
        httpsConn = new Connector('HTTP/1.1')
        httpsConn.scheme = 'https'
        httpsConn.secure = true
        assert httpsConn.setProperty('SSLEnabled', 'true')
    }

    if(httpsConn) {
      if(!httpsConn.port || httpsConn.port < 0)
        httpsConn.port = params.httpsPort ?: ServerDefaults.defaultHttpsPort

      if(httpsConn.port == ServerDefaults.RANDOM_FREE_PORT)
        httpsConn.port = 0

      def sslConfig = new SSLHostConfig()
      httpsConn.addSslHostConfig(sslConfig)
      def cert = sslConfig.getCertificates(true).first()

      if(params.sslKeyManagerPassword)
        cert.certificateKeyPassword = params.sslKeyManagerPassword
      if(params.sslKeyStorePath) {
        if(params.sslKeyStorePath.startsWith('classpath:')) {
          String resString = params.sslKeyStorePath - 'classpath:'
          File keystoreFile = new File(tempDir, 'keystore')
          keystoreFile.parentFile.mkdirs()
          if(keystoreFile.exists())
            keystoreFile.delete()
          def stm = getClass().getResourceAsStream(resString)
          if(stm == null)
            throw new Exception("Could not resource referenced in sslKeyStorePath: '${resString}'")
          stm.withStream {
            keystoreFile.withOutputStream { outs ->
              outs << stm
            }
          }
          cert.certificateKeystoreFile = keystoreFile.absolutePath
        }
        else
          cert.certificateKeystoreFile = params.sslKeyStorePath
      }
      if(params.sslKeyStorePassword)
        cert.certificateKeystorePassword = params.sslKeyStorePassword
      if(params.sslTrustStorePath) {
        if(params.sslTrustStorePath.startsWith('classpath:')) {
          String resString = params.sslTrustStorePath - 'classpath:'
          File truststoreFile = new File(tempDir, 'truststore')
          truststoreFile.parentFile.mkdirs()
          if(truststoreFile.exists())
            truststoreFile.delete()
          def stm = getClass().getResourceAsStream(resString)
          if(stm == null)
            throw new Exception("Could not resource referenced in sslTrustStorePath: '${resString}'")
          stm.withStream {
            truststoreFile.withOutputStream { outs ->
              outs << stm
            }
          }
          sslConfig.truststoreFile = truststoreFile.absolutePath
        }
        else
          sslConfig.truststoreFile = params.sslTrustStorePath
      }
      if(params.sslTrustStorePassword)
        sslConfig.truststorePassword = params.sslTrustStorePassword

      if(params.httpsIdleTimeout)
        assert httpsConn.setProperty('keepAliveTimeout', params.httpsIdleTimeout.toString())

      httpsConn.maxPostSize = -1  // unlimited
      httpsConn.maxPartCount = -1

      if(newHttpsConnector) {
        service.addConnector(httpsConn)
        connectors = service.findConnectors()
      }
    }

    if(httpConn && httpsConn)
      httpConn.redirectPort = httpsConn.port

    if(httpConn)
      tomcat.setConnector(httpConn)
    else if(httpsConn)
      tomcat.setConnector(httpsConn)
    else if(connectors.length != 0)
      tomcat.setConnector(connectors[0])

    if(params.singleSignOn && !tomcat.host.pipeline.valves.find { it instanceof SingleSignOn })
      tomcat.host.addValve(new SingleSignOn())

    tomcat
  }

  public StandardContext createContext(Map webapp, Tomcat tomcat, Closure configureContext = null) {
    StandardContext context = params.contextClass ? params.contextClass.newInstance() : new StandardContext()
    String effectiveContextPath = getEffectiveContextPath(webapp.contextPath);
    context.name = effectiveContextPath
    context.path = effectiveContextPath
    configurer.setResourceBase(context, webapp)
    // context.setLogEffectiveWebXml(true) // enable for debugging webxml merge
    URL[] classpathUrls = (webapp.webappClassPath ?: []).collect { new URL(it) } as URL[]
    URLClassLoader classLoader = new URLClassLoader(classpathUrls, params.parentClassLoader ?: this.getClass().getClassLoader())
    if (webapp.springBoot) {
      context.addParameter('GRETTY_SPRING_BOOT_MAIN_CLASS', webapp.springBootMainClass)
    }
    context.addLifecycleListener(new SpringloadedCleanup())
    context.setParentClassLoader(classLoader)
    context.setJarScanner(configurer.createJarScanner(context.getJarScanner(), new JarSkipPatterns()))
    context.setParentClassLoader(classLoader)

    webapp.initParameters?.each { key, value ->
      context.addParameter(key, value)
    }

    def realmConfigFile = webapp.realmConfigFile ?: params.realmConfigFile
    if (realmConfigFile && new File(realmConfigFile).exists()) {
      context.logger.info "${webapp.contextPath} -> realm config ${realmConfigFile}"
      def realm = new MemoryRealm()
      realm.setPathname(realmConfigFile)
      context.setRealm(realm)
    } else
      context.addLifecycleListener(new FixContextListener())

    context.configFile = tomcat.getWebappConfigFile(webapp.resourceBase, webapp.contextPath)
    if (!context.configFile && webapp.contextConfigFile)
      context.configFile = new File(webapp.contextConfigFile).toURI().toURL()
    if (context.configFile)
      context.logger.info "Configuring ${webapp.contextPath} with ${context.configFile}"

    context.addLifecycleListener(configurer.createContextConfig(classpathUrls))

    if (configureContext) {
      configureContext.delegate = this
      configureContext(webapp, context)
    }

    if (!context.findChild('default'))
      context.addLifecycleListener(new DefaultWebXmlListener())

    if (context.logger.isDebugEnabled())
      context.addLifecycleListener(new LifecycleListener() {
        @Override
        public void lifecycleEvent(LifecycleEvent event) {
          if (event.type == Lifecycle.CONFIGURE_START_EVENT) {
            def pipeline = context.getPipeline()
            context.logger.debug "START: context=${context.path}, pipeline: $pipeline #${System.identityHashCode(pipeline)}"
            context.logger.debug '  valves:'
            for (def v in pipeline.getValves())
              context.logger.debug "    $v #${System.identityHashCode(v)}"
          }
        }
      })
    context
  }

  public static String getEffectiveContextPath(String contextPath) {
    return contextPath == '/' ? "" : contextPath
  }
}
