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

/**
 *
 * @author akhikhl
 */
@CompileStatic
@ToString
class ServerConfig {

  static final int RANDOM_FREE_PORT = -1
  String jvmExecutable
  List<String> jvmArgs
  Map<String, String> systemProperties
  String servletContainer
  Boolean managedClassReload
  String host
  Boolean httpEnabled
  Integer httpPort
  Integer httpIdleTimeout
  Boolean httpsEnabled
  Integer httpsPort
  Integer httpsIdleTimeout
  String sslHost
  def sslKeyStorePath
  String sslKeyStorePassword
  String sslKeyManagerPassword
  def sslTrustStorePath
  String sslTrustStorePassword
  boolean sslNeedClientAuth
  def realm
  def realmConfigFile
  def serverConfigFile
  String interactiveMode
  Integer scanInterval
  List<Closure> onStart = []
  List<Closure> onStop = []
  List<Closure> onScan = []
  List<Closure> onScanFilesChanged = []

  Boolean secureRandom
  String springBootVersion
  String springLoadedVersion
  String springVersion
  Boolean singleSignOn
  /**
   * Tomcat-specific: Enables JNDI naming which is disabled by default.
   */
  Boolean enableNaming

  String redeployMode
  String scanner

  String portPropertiesFileName

  static ServerConfig getDefaultServerConfig(String serverName) {
    ServerConfig result = new ServerConfig()
    result.jvmArgs = []
    result.servletContainer = 'jetty12'
    result.managedClassReload = false
    result.httpEnabled = true
    result.httpsEnabled = false
    result.interactiveMode = 'stopOnKeyPress'
    result.scanInterval = 1
    result.redeployMode = 'restart'
    result.scanner = 'jetty'
    result.portPropertiesFileName = 'gretty_ports.properties'
    return result
  }

  int getRandomFreePort() {
    RANDOM_FREE_PORT
  }

  void jvmArg(String a) {
    if(a) {
      if(jvmArgs == null)
        jvmArgs = []
      jvmArgs.add(a)
    }
  }

  void jvmArgs(String... args) {
    if(args) {
      if(jvmArgs == null)
        jvmArgs = []
      jvmArgs.addAll(args)
    }
  }

  void onScan(Closure newValue) {
    onScan.add newValue
  }

  void onScanFilesChanged(Closure newValue) {
    onScanFilesChanged.add newValue
  }

  void onStart(Closure newValue) {
    onStart.add newValue
  }

  void onStop(Closure newValue) {
    onStop.add newValue
  }

  void systemProperty(String name, String value) {
    if(systemProperties == null)
      systemProperties = [:]
    systemProperties[name] = value
  }

  void systemProperties(Map<String, String> m) {
    if(m) {
      if(systemProperties == null)
        systemProperties = [:]
      systemProperties << m
    }
  }

  void jvmExecutable(String jvmExecutable) {
    this.jvmExecutable = jvmExecutable
  }
}
