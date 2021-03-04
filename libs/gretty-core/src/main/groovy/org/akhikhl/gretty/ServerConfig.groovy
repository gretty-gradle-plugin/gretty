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
import groovy.transform.ToString
import groovy.transform.TypeCheckingMode
import org.gradle.api.model.ReplacedBy
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
@ToString
class ServerConfig {

  static final int RANDOM_FREE_PORT = PortUtils.RANDOM_FREE_PORT

  List<String> jvmArgs

  @Optional @Input
  List<String> getJvmArgs() {
    return jvmArgs
  }

  Map<String, String> systemProperties

  @Optional @Input
  Map<String, String> getSystemProperties() {
    return systemProperties
  }

  String servletContainer

  @Optional @Input
  String getServletContainer() {
    return servletContainer
  }

  Boolean managedClassReload

  @Optional @Input
  Boolean getManagedClassReload() {
    return managedClassReload
  }

  String host

  @Optional @Input
  String getHost() {
    return host
  }


  Boolean httpEnabled

  @Optional @Input
  Boolean getHttpEnabled() {
    return httpEnabled
  }

  Integer httpPort

  @Optional @Input
  Integer getHttpPort() {
    return httpPort
  }

  Integer httpIdleTimeout

  @Optional @Input
  Integer getHttpIdleTimeout() {
    return httpIdleTimeout
  }

  Boolean httpsEnabled

  @Optional @Input
  Boolean getHttpsEnabled() {
    return httpsEnabled
  }

  Integer httpsPort

  @Optional @Input
  Integer getHttpsPort() {
    return httpsPort
  }

  Integer httpsIdleTimeout

  @Optional @Input
  Integer getHttpsIdleTimeout() {
    return httpsIdleTimeout
  }

  String sslHost

  @Optional @Input
  String getSslHost() {
    return sslHost
  }

  def sslKeyStorePath

  @Optional
  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  def getSslKeyStorePath() {
    return sslKeyStorePath
  }
  
  String sslKeyStorePassword

  @Internal
  String getSslKeyStorePassword() {
    return sslKeyStorePassword
  }

  String sslKeyManagerPassword

  @Internal
  String getSslKeyManagerPassword() {
    return sslKeyManagerPassword
  }

  def sslTrustStorePath

  @Optional
  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  def getSslTrustStorePath() {
    return sslTrustStorePath
  }

  String sslTrustStorePassword

  @Internal
  String getSslTrustStorePassword() {
    return sslTrustStorePassword
  }

  boolean sslNeedClientAuth

  @Input
  boolean getSslNeedClientAuth() {
    return sslNeedClientAuth
  }

  def realm
  def realmConfigFile
  def serverConfigFile

  String interactiveMode

  @Optional @Input
  String getInteractiveMode() {
    return interactiveMode
  }

  Integer scanInterval

  @Optional @Input
  Integer getScanInterval() {
    return scanInterval
  }

  def logbackConfigFile

  @Optional
  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  def getLogbackConfigFile() {
    return logbackConfigFile
  }

  String loggingLevel

  @Console
  String getLoggingLevel() {
    return loggingLevel
  }
  Boolean consoleLogEnabled

  @Console
  Boolean getConsoleLogEnabled() {
    return consoleLogEnabled
  }

  Boolean fileLogEnabled

  @Console
  Boolean getFileLogEnabled() {
    return fileLogEnabled
  }

  def logFileName

  @Internal
  def getLogFileName() {
    return logFileName
  }

  def logDir

  @Internal
  def getLogDir() {
    return logDir
  }

  List<Closure> onStart

  @Internal
  List<Closure> getOnStart() {
    return onStart
  }

  List<Closure> onStop

  @Internal
  List<Closure> getOnStop() {
    return onStop
  }

  List<Closure> onScan

  @Internal
  List<Closure> getOnScan() {
    return onScan
  }

  List<Closure> onScanFilesChanged

  @Internal
  List<Closure> getOnScanFilesChanged() {
    return onScanFilesChanged
  }

  Boolean secureRandom

  @Optional @Input
  Boolean getSecureRandom() {
    return secureRandom
  }
  
  String springBootVersion

  @Optional @Input
  String getSpringBootVersion() {
    return springBootVersion
  }

  String springLoadedVersion

  @Optional @Input
  String getSpringLoadedVersion() {
    return springLoadedVersion
  }

  String springVersion

  @Optional @Input
  String getSpringVersion() {
    return springVersion
  }

  String logbackVersion

  @Optional @Input
  String getLogbackVersion() {
    return logbackVersion
  }

  Boolean singleSignOn

  @Optional @Input
  Boolean getSingleSignOn() {
    return singleSignOn
  }

  /**
   * Tomcat-specific: Enables JNDI naming which is disabled by default.
   */
  Boolean enableNaming

  @Optional @Input
  Boolean getEnableNaming() {
    return enableNaming
  }

  String redeployMode

  @Optional @Input
  String getRedeployMode() {
    return redeployMode
  }

  String scanner

  @Optional @Input
  String getScanner() {
    return scanner
  }

  @Internal
  String portPropertiesFileName

  @Optional @Input
  Boolean liveReloadEnabled

  static ServerConfig getDefaultServerConfig(String serverName) {
    ServerConfig result = new ServerConfig()
    result.jvmArgs = []
    result.servletContainer = 'jetty11'
    result.managedClassReload = false
    result.httpEnabled = true
    result.httpsEnabled = false
    result.interactiveMode = 'stopOnKeyPress'
    result.scanInterval = 1
    result.loggingLevel = 'INFO'
    result.consoleLogEnabled = true
    result.fileLogEnabled = true
    result.logFileName = serverName
    result.redeployMode = 'restart'
    result.logDir = "${System.getProperty('user.home')}/logs" as String
    result.scanner = 'jetty'
    result.portPropertiesFileName = 'gretty_ports.properties'
    result.liveReloadEnabled = false
    return result
  }

  // use serverConfigFile instead
  @Deprecated
  @ReplacedBy("serverConfigFile")
  def getJettyXmlFile() {
    serverConfigFile
  }

  // use httpPort instead
  @Deprecated
  @ReplacedBy("httpPort")
  Integer getPort() {
    httpPort
  }

  @Internal
  int getRandomFreePort() {
    RANDOM_FREE_PORT
  }

  void jvmArg(Object a) {
    if(a) {
      if(jvmArgs == null)
        jvmArgs = []
      jvmArgs.add(a)
    }
  }

  void jvmArgs(Object... args) {
    if(args) {
      if(jvmArgs == null)
        jvmArgs = []
      jvmArgs.addAll(args)
    }
  }

  void onScan(Closure newValue) {
    if(onScan == null)
      onScan = []
    onScan.add newValue
  }

  void onScanFilesChanged(Closure newValue) {
    if(onScanFilesChanged == null)
      onScanFilesChanged = []
    onScanFilesChanged.add newValue
  }

  void onStart(Closure newValue) {
    if(onStart == null)
      onStart = []
    onStart.add newValue
  }

  void onStop(Closure newValue) {
    if(onStop == null)
      onStop = []
    onStop.add newValue
  }

  // use serverConfigFile instead
  @Deprecated
  void setJettyXmlFile(newValue) {
    serverConfigFile = newValue
  }

  // use httpPort instead
  @Deprecated
  void setPort(Integer newValue) {
    httpPort = newValue
  }

  void systemProperty(String name, Object value) {
    if(systemProperties == null)
      systemProperties = [:]
    systemProperties[name] = value
  }

  void systemProperties(Map<String, Object> m) {
    if(m) {
      if(systemProperties == null)
        systemProperties = [:]
      systemProperties << m
    }
  }
}
