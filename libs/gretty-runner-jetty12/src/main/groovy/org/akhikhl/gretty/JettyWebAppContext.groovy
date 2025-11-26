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
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceFactory
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Custom WebAppContext for Jetty 12 that makes runtime JARs visible in WEB-INF/lib
 */
@CompileStatic(TypeCheckingMode.SKIP)
class JettyWebAppContext extends WebAppContext {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JettyWebAppContext.class)

  private static final String WEB_INF_LIB_PREFIX = '/WEB-INF/lib'
  private final Map<String, File> webInfJarMap = [:]
  private final List<File> webInfJars = []

  void setWebInfLib(List<File> jars) {
    webInfJars.addAll(jars)
  }

  @Override
  List getHandlers() {
    // In Jetty 12, WebAppContext manages handlers internally through the handler hierarchy
    // (SessionHandler -> SecurityHandler -> ServletHandler) via getHandler() (singular)
    // WebAppContext itself is a wrapper handler, not a container for multiple handlers
    // We return the internal handler if it exists
    def handler = getHandler()
    if (handler != null) {
      return Collections.singletonList(handler)
    }
    return Collections.emptyList()
  }

  @Override
  protected void doStart() throws Exception {
    // Preparing our paths patch
    webInfJarMap.clear()
    webInfJars.each {
      String fileName = it.getName()
      if (fileName.endsWith('.jar')) {
        webInfJarMap.put(fileName, it)
      }
    }

    super.doStart()
  }

  @Override
  protected void doStop() throws Exception {
    // Cancelling our paths patch
    if (webInfJarMap != null) {
      webInfJarMap.clear()
    }
    webInfJars.clear()
    super.doStop()
  }

  @Override
  Set<String> getResourcePaths(String path) {
    Set<String> paths = super.getResourcePaths(path)
    // Tinkering with paths, adding paths provided manually
    if (path != null) {
      def allPaths = new TreeSet<String>()
      if (paths != null) {
        allPaths.addAll(paths)
      }

      if (path.startsWith(WEB_INF_LIB_PREFIX)) {
        webInfJarMap.keySet().each {
          allPaths.add(WEB_INF_LIB_PREFIX + '/' + it)
        }
      }
      return allPaths
    }

    return paths
  }

  @Override
  Resource getResource(String uriInContext) throws MalformedURLException {
    Resource resource = super.getResource(uriInContext)

    if ((resource == null || !resource.exists()) && uriInContext != null) {
      String uri = URIUtil.canonicalPath(uriInContext)
      if (uri == null) {
        return null
      }

      try {
        if (uri.startsWith(WEB_INF_LIB_PREFIX)) {
          String jarName = uri.replace(WEB_INF_LIB_PREFIX, '')
          if (jarName.startsWith('/') || jarName.startsWith('\\')) {
            jarName = jarName.substring(1)
          }
          if (jarName.isEmpty()) {
            return null
          }
          File jarFile = webInfJarMap.get(jarName)
          if (jarFile != null) {
            ResourceFactory factory = this.getResourceFactory()
            return factory.newResource(Path.of(jarFile.getPath()))
          }
          return null
        }
      } catch (MalformedURLException e) {
        throw e
      } catch (IOException e) {
        LOG.debug("Failed to get resource for {}", uri, e)
      }
    }
    return resource
  }
}
