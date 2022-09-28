/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

import ch.qos.logback.classic.Level
import groovy.cli.commons.CliBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
final class Runner {

  protected final Map params

  static void main(String[] args) {
    def cli = new CliBuilder()
    cli.with {
      d longOpt: 'debug', type: Boolean, 'enable debug logging'
      st longOpt: 'statusPort', required: true, args: 1, argName: 'statusPort', type: Integer, 'status port'
      smf longOpt: 'serverManagerFactory', required: true, args: 1, argName: 'serverManagerFactory', type: String, 'server manager factory'
    }
    def options = cli.parse(args)
    Map params = [statusPort: options.statusPort as int, serverManagerFactory: options.serverManagerFactory, debug: options.debug]
    new Runner(params).run()
  }

  void initLogback(Map serverParams) {
    LogUtil.reset()
    String logbackConfigFile = serverParams.logbackConfigFile
    if (logbackConfigFile) {
      initLogbackFromConfigurationFile(logbackConfigFile)
    } else {
      initLogbackFromGrettyConfig(serverParams)
    }
  }

  private void initLogbackFromConfigurationFile(String logbackConfigFile) {
    if (!logbackConfigFile.endsWith(".xml")) {
      throw new IllegalArgumentException("""
          | Gretty only supports XML for configuring Logback, and does not support
          | $logbackConfigFile
          | Please note Logback dropped support for Gaffer (configuration from Groovy script) in 1.2.9.
          """.stripMargin()
      )
    }

    LogUtil.configureLoggingWithJoran(new File(logbackConfigFile))
  }

  private void initLogbackFromGrettyConfig(Map serverParams) {
    Level level = stringToLoggingLevel(serverParams.loggingLevel?.toString())
    boolean consoleLogEnabled = serverParams.getOrDefault('consoleLogEnabled', true)
    boolean fileLogEnabled = serverParams.getOrDefault('fileLogEnabled', true)
    boolean grettyDebug = params.getOrDefault('debug', true)
    LogUtil.configureLogging(
            level,
            consoleLogEnabled,
            fileLogEnabled,
            serverParams.logFileName?.toString(),
            serverParams.logDir?.toString(),
            grettyDebug,
    )
  }

  private static Level stringToLoggingLevel(String str) {
    switch(str?.toUpperCase()) {
      case 'ALL':
        return Level.ALL
      case 'DEBUG':
        return Level.DEBUG
      case 'ERROR':
        return Level.ERROR
      case 'INFO':
        return Level.INFO
      case 'OFF':
        return Level.OFF
      case 'TRACE':
        return Level.TRACE
      case 'WARN':
        return Level.WARN
      default:
        return Level.INFO
    }
  }

  private Runner(Map params) {
    this.params = params
  }

  private void run() {
    LogUtil.setLevel(params.debug)
    boolean paramsLoaded = false
    def serverManager = null
    final def reader = ServiceProtocol.createReader()
    final def writer = ServiceProtocol.createWriter(params.statusPort)
    try {
      writer.write("init ${reader.port}")
      while(true) {
        def data = reader.readMessage()
        if(!paramsLoaded) {
          params << new JsonSlurper().parseText(data)
          paramsLoaded = true
          if(!Boolean.valueOf(System.getProperty('grettyProduct')))
            initLogback(params)

          def cl = createClassLoader()
          ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader()
          Thread.currentThread().setContextClassLoader(cl)
          try {
            def ServerManagerFactory = Class.forName(params.serverManagerFactory, true, cl)
            serverManager = ServerManagerFactory.createServerManager()
            serverManager.setParams(params)
            def event = serverManager.startServer()
            onServerStarted(writer, event.getServerStartInfo())
          }
          finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader)
          }
          // Note that server is already in listening state.
          // If client sends a command immediately after 'started' signal,
          // the command is queued, so that socket.accept gets it anyway.
          continue
        }
        if(data == 'status')
          writer.write('started')
        else if(data == 'stop') {
          serverManager.stopServer()
          break
        }
        else if(data == 'restart') {
          serverManager.stopServer()
          serverManager.startServer(null)
        }
        else if(data == 'restartWithEvent') {
          serverManager.stopServer()
          def event = serverManager.startServer()
          onServerStarted(writer, event.getServerStartInfo())
        }
        else if (data.startsWith('redeploy ')) {
          List<String> webappList = data.replace('redeploy ', '').split(' ').toList()
          serverManager.redeploy(webappList)
          writer.writeMayFail('redeployed')
        }
      }
    } finally {
      reader.close()
    }
  }

  private ClassLoader createClassLoader() {
    URL[] urls = new URL[params.servletContainerClasspath.size()]
    for (int index = 0; index < params.servletContainerClasspath.size(); index++) {
      urls[index] = new File(params.servletContainerClasspath[index]).toURI().toURL()
    }
    return new URLClassLoader(urls, findBootClassLoader())
  }

  protected ClassLoader findBootClassLoader() {
    def bootClassLoader = getClass().getClassLoader()
      while(bootClassLoader.getParent() != null) {
        bootClassLoader = bootClassLoader.getParent();
      }
    bootClassLoader
  }

  private onServerStarted(ServiceProtocol.Writer writer, Map<String, String> serverStartInfo) {
    JsonBuilder json = new JsonBuilder()
    json serverStartInfo
    writer.writeMayFail(json.toString())
  }
}
