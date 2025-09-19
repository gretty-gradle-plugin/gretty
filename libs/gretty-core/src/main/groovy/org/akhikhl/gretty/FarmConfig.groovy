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

@CompileStatic
class FarmConfig {

  @Delegate
  protected final ServerConfig serverConfig

  // key is project path or war path, value is options
  protected final Map webAppRefs_ = [:]

  // list of projects or project paths
  protected final List integrationTestProjects_ = []

  FarmConfig(Map options) {
    serverConfig = (ServerConfig) options.serverConfig ?: new ServerConfig()
    webAppRefs_ = [:]
    if(options.containsKey('webAppRefs'))
      webAppRefs_ << (options.webAppRefs as Map)
    if(options.containsKey('integrationTestProjects'))
      integrationTestProjects_.addAll(options.integrationTestProjects as Collection)
    if(options.containsKey('integrationTestProject'))
      integrationTestProjects_.add(options.integrationTestProject)
  }

  List getIntegrationTestProjects() {
    integrationTestProjects_.asImmutable()
  }

  Map getWebAppRefs() {
    webAppRefs_.asImmutable()
  }

  void integrationTestProject(Object project) {
    integrationTestProjects_.add(project)
  }

  void setWebAppRefs(Map newValue) {
    if(!webAppRefs_.is(newValue)) {
      webAppRefs_.clear()
      webAppRefs_ << newValue
    }
  }

  void webapp(Map options = [:], w) {
    webAppRefs_[w] = options
  }
}
