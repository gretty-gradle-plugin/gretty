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
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceFactory
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.ee10.webapp.WebInfConfiguration

@CompileStatic
class WebInfConfigurationEx extends WebInfConfiguration implements BaseResourceConfiguration {

  private List<String> extraResourceBases
  private final List<Closure> baseResourceListeners = []

  @Override
  void addBaseResourceListener(Closure closure) {
    baseResourceListeners.add(closure)
  }

  @Override
  void setExtraResourceBases(List extraResourceBases) {
    this.extraResourceBases = extraResourceBases
  }

  /**
   * Accept webapp classpath for compatibility with caller,
   * but no action needed - extra classpath is set directly on WebAppContext
   */
  void setWebappClassPath(List<String> webappClassPath) {
    // No-op: In Jetty 12, extra classpath is set directly on context via setExtraClasspath()
    // This method exists only to maintain compatibility with the configuration call pattern
  }

  @Override
  public void unpack (WebAppContext context) throws IOException {
    super.unpack(context)
    if(extraResourceBases && !extraResourceBases.isEmpty()) {
      // Convert extra resource bases to Resources and combine them with the existing base resource
      List<Resource> resources = []

      // Add existing base resource if it exists
      if (context.getBaseResource() != null) {
        resources.add(context.getBaseResource())
      }

      // Add extra resource bases
      ResourceFactory factory = context.getResourceFactory()
      extraResourceBases.each { extraBase ->
        Resource resource = factory.newResource(java.nio.file.Path.of(extraBase.toString()))
        if (resource != null && resource.exists()) {
          resources.add(resource)
        }
      }

      // Combine all resources
      if (resources.size() > 1) {
        context.setBaseResource(ResourceFactory.combine(resources))
      } else if (resources.size() == 1) {
        context.setBaseResource(resources[0])
      }
    }
    for(Closure closure in baseResourceListeners)
      closure(context)
  }
}
