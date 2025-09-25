/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

import org.eclipse.jetty.ee10.webapp.WebAppClassLoader
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.resource.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 *  Inspired by maven-jetty-plugin's JettyWebAppContext
 *
 * @author sala
 */
class JettyWebAppContext extends WebAppContext {

    private static final Logger LOG = LoggerFactory.getLogger(JettyWebAppContext.class)

    private static final String WEB_INF_LIB_PREFIX = '/WEB-INF/lib'
    private final Map<String, File> webInfJarMap = [:]
    private final List<File> webInfJars = []

    void setWebInfLib(List<File> jars) {
        webInfJars.addAll(jars);
    }

    @Override
    List<Handler> getHandlers() {
        Handler handler = super.getHandler()
        return (handler == null) ? Collections.emptyList() : Collections.singletonList(handler)
    }

    @Override
    void setExtraClasspath(String classpath) {
        def resourceFactory = getResourceFactory()
        // Explicitly declare the list type to satisfy the static type checker
        List<Resource> resources = []

        if (classpath != null && !classpath.isEmpty()) {
            classpath.split(File.pathSeparator).each { path ->
                try {
                    Resource resource = resourceFactory.newResource(path)

                    if (resource != null && resource.exists()) {
                        resources.add(resource)
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to create resource for classpath entry: " + path, e)
                }
            }
        }
        super.setExtraClasspath(resources)
    }

    @Override
    protected void doStart() throws Exception {
        // preparing our pathes patch
        webInfJarMap.clear()
        webInfJars.each {
            String fileName = it.getName()
            if (fileName.endsWith('.jar')) {
                webInfJarMap.put(fileName, it)
            }
        }

        WebAppClassLoader.runWithServerClassAccess {
            super.doStart()
        }
    }

    @Override
    protected void doStop() throws Exception {
        // cancelling our pathes patch
        if (webInfJarMap != null) {
            webInfJarMap.clear()
        }
        webInfJars.clear()
        super.doStop()
    }

    @Override
    Set<String> getResourcePaths(String path) {
        Set<String> paths = super.getResourcePaths(path)
        // Tinkering with pathes, adding pathes provided manually
        if (path != null) {
            def allPaths = new TreeSet<String>()
            allPaths.addAll(paths)

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
                        return Resource.newInstance(jarFile.getPath())
                    }
                    return null
                }
            } catch (MalformedURLException e) {
                throw e
            } catch (IOException e) {
                LOG.warn(e.getMessage())
            }
        }
        return resource
    }
}
