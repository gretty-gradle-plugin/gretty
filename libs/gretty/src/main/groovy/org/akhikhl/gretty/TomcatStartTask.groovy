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

/**
 *
 * @author akhikhl
 */
@DisableCachingByDefault
class TomcatStartTask extends AppStartTask {

  @Override
  protected String getCompatibleServletContainer(String servletContainer) {
    ServletContainerConfig.getTomcatCompatibleServletContainer(project, servletContainer)
  }
}
