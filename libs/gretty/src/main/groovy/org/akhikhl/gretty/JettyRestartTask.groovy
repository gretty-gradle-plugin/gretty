/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

import org.gradle.work.DisableCachingByDefault

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @author akhikhl
 */
@DisableCachingByDefault
class JettyRestartTask extends AppRestartTask {

  protected static final Logger log = LoggerFactory.getLogger(JettyBeforeIntegrationTestTask)
	
  JettyRestartTask() {
    doFirst {
      log.warn 'JettyRestartTask is deprecated and will be removed in Gretty 2.0. Please use AppRestartTask instead.'
    }
  }
}

