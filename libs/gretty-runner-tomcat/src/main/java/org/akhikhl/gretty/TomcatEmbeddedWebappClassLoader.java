/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import java.net.URL;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class TomcatEmbeddedWebappClassLoader extends WebappClassLoader {

	private static final Log log = LogFactory.getLog(TomcatEmbeddedWebappClassLoader.class);

	public TomcatEmbeddedWebappClassLoader() {
	}

	public TomcatEmbeddedWebappClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

		Class<?> resultClass = null;

		// Check local class caches
		resultClass = (resultClass == null ? findLoadedClass0(name) : resultClass);
		resultClass = (resultClass == null ? findLoadedClass(name) : resultClass);
		if (resultClass != null)
			return resolveIfNecessary(resultClass, resolve);

		// Check security
		checkPackageAccess(name);

		// Perform the actual load
		boolean delegateLoad = (this.delegate || filter(name, true));

		if (delegateLoad)
			resultClass = (resultClass == null ? loadFromParent(name) : resultClass);

		resultClass = (resultClass == null ? findClassIgnoringNotFound(name) : resultClass);

		if (!delegateLoad)
			resultClass = (resultClass == null ? loadFromParent(name) : resultClass);

		if (resultClass == null)
			throw new ClassNotFoundException(name);

		return resolveIfNecessary(resultClass, resolve);
	}

	private Class<?> resolveIfNecessary(Class<?> resultClass, boolean resolve) {
		if (resolve)
			resolveClass(resultClass);
		return (resultClass);
	}

	@Override
	protected void addURL(URL url) {
		// Ignore URLs added by the Tomcat 8 implementation (see gh-919)
    log.trace("Ignoring request to add " + url + " to the tomcat classloader");
	}

	private Class<?> loadFromParent(String name) {
		if (this.parent == null)
			return null;
		try {
			return Class.forName(name, false, this.parent);
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}

	private Class<?> findClassIgnoringNotFound(String name) {
		try {
			return findClass(name);
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}

	private void checkPackageAccess(String name) throws ClassNotFoundException {
    SecurityManager sm = System.getSecurityManager(); // Use system-wide SecurityManager
    if (sm != null && name.lastIndexOf('.') >= 0) {
      try {
        sm.checkPackageAccess(name.substring(0, name.lastIndexOf('.')));
      } catch (SecurityException ex) {
        throw new ClassNotFoundException("Security Violation, attempt to use Restricted Class: " + name, ex);
      }
    }
  }
}
