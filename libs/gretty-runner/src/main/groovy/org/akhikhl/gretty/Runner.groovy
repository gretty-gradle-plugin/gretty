/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

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

  private Runner(Map params) {
    this.params = params
  }

  private void run() {
    boolean paramsLoaded = false
    def serverManager = null
    final def reader = ServiceProtocol.createReader()
    final def writer = ServiceProtocol.createWriter(params.statusPort)
    def cl = null
    try {
      writer.write("init ${reader.port}")
      while(true) {
        def data = reader.readMessage()
        if(!paramsLoaded) {
          params << new JsonSlurper().parseText(data)
          paramsLoaded = true

          cl = createClassLoader()
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
          ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader()
          Thread.currentThread().setContextClassLoader(cl)
          try {
            serverManager.startServer()
          }
          finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader)
          }
        }
        else if(data == 'restartWithEvent') {
          serverManager.stopServer()
          ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader()
          Thread.currentThread().setContextClassLoader(cl)
          try {
            def event = serverManager.startServer()
            onServerStarted(writer, event.getServerStartInfo())
          }
          finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader)
          }
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
