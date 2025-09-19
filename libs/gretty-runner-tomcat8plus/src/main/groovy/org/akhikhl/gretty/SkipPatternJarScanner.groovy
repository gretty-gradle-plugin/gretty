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
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory
import org.apache.tomcat.Jar

import javax.servlet.ServletContext
import org.apache.tomcat.JarScanFilter
import org.apache.tomcat.JarScanner
import org.apache.tomcat.JarScannerCallback
import org.apache.tomcat.util.scan.StandardJarScanFilter
import org.apache.tomcat.util.scan.StandardJarScanner
import org.apache.tomcat.JarScanType
import org.apache.tomcat.util.file.Matcher;

/**
 *
 * @author akhikhl
 */
class SkipPatternJarScanner extends StandardJarScanner {

  private static final Log log = LogFactory.getLog(TomcatServerManager)

	protected final JarScanner jarScanner
	protected final JarSkipPatterns skipPatterns

	SkipPatternJarScanner(JarScanner jarScanner, JarSkipPatterns skipPatterns) {
		assert jarScanner != null
		this.jarScanner = jarScanner
		this.skipPatterns = skipPatterns
    setJarScanFilter(new StandardJarScanFilter())
	}

  protected JarScannerCallback augmentCallback(final JarScannerCallback callback) {

    if(!log.isDebugEnabled())
      return callback

    return new JarScannerCallback() {

      void scan(Jar jar, String webappPath, boolean isWebapp) throws IOException {
        log.debug("jarScannerCallback.scan ${jar}, ${webappPath}, ${isWebapp}")
        callback.scan(jar, webappPath, isWebapp)
      }

      void scan(File file, String webappPath, boolean isWebapp) throws IOException {
        log.debug("jarScannerCallback.scan ${file}, ${webappPath}, ${isWebapp}")
        callback.scan(file, webappPath, isWebapp)
      }

      void scanWebInfClasses() throws IOException {
        log.debug('jarScannerCallback.scanWebInfClasses')
        callback.scanWebInfClasses()
      }
    }
  }

  protected boolean checkJar(String path) {
    String name = path.substring(path.lastIndexOf('/') + 1)
    boolean result = !Matcher.matchName(skipPatterns.asSet(), name)
    log.debug "filter jar: ${name} -> ${result}"
    result
  }

  @Override
  @CompileStatic
  public void setJarScanFilter(JarScanFilter newFilter) {
    super.setJarScanFilter(newFilter)
    jarScanner.setJarScanFilter(new TomcatJarScanFilter(newFilter))
  }

	@Override
  public void scan(JarScanType scanType, ServletContext context, JarScannerCallback callback) {
		jarScanner.scan(scanType, context, augmentCallback(callback))
	}

  private class TomcatJarScanFilter implements JarScanFilter {

    private final JarScanFilter additionalScanFilter

    TomcatJarScanFilter(JarScanFilter additionalScanFilter) {
      this.additionalScanFilter = additionalScanFilter
    }

    @Override
    boolean check(JarScanType jarScanType, String jarName) {
      checkJar(jarName) && (additionalScanFilter == null || additionalScanFilter.check(jarScanType, jarName))
    }
  }
}
