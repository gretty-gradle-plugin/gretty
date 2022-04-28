/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Properly implements "server classes" concept.
 *
 * @author akhikhl
 */
public class FilteringClassLoader extends URLClassLoader {

  private final List<String> serverClasses = new ArrayList<String>();

  private final List<String> serverResources = new ArrayList<String>();

  private ClassLoader bootClassLoader;

  public FilteringClassLoader(ClassLoader parent) {
    super(new URL[0], parent);
    findBootClassLoader();
  }

  protected void findBootClassLoader() {
    bootClassLoader = getParent();
    if (bootClassLoader != null) {
      while(bootClassLoader.getParent() != null) {
        bootClassLoader = bootClassLoader.getParent();
      }
    }
  }

  public void addServerClass(String serverClass) {
    serverClasses.add(serverClass);
    serverResources.add(serverClass.replace('.', '/'));
    serverResources.add("META-INF/services/" + serverClass);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    for(String serverClass : serverClasses)
      if(name.startsWith(serverClass)) {
        Class<?> c = findLoadedClass(name);
        if(c == null)
          c = findClass(name);
        if(c != null) {
          if(resolve)
            resolveClass(c);
          return c;
        }
        throw new ClassNotFoundException(name);
      }
    return super.loadClass(name, resolve);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    for(String serverResource : serverResources) {
      if(name.startsWith(serverResource)) {
        final List<URL> resources = new ArrayList<>();
        resources.addAll(Collections.list(getBootstrapResources(name)));
        resources.addAll(Collections.list(findResources(name)));
        return Collections.enumeration(resources);
      }
    }

    // FIXME one-off patch for upgrading Logback past 1.3.0-alpha11
    // If we remove below lines, we get a `ClassNotFoundException` for `LogbackServletContainerInitializer`. Causes:
    // (1) Logback adding Jakarta-enabled servlet-related classes into their main distribution in 1.3.0-alpha11
    // (2) Gretty leaking runner dependencies (such as logging) onto the webapp classpath
    // (3) Gretty's `FilteringClassLoader`, which does not hide the leaked dependencies a 100% from the webapp.
    //
    // In particular, Gretty makes `ch.qos.logback.` a server class, a class which we hide from the webapp.
    // This deny list works for loading classes, but does not cope with the workings of service loader.
    // Hence, we allowed searching for all implementations of `ServletContainerInitializer`, which will implicitly
    // find the Logback SCI, but block actual loading of the class because of the deny list.
    //
    // So below is a one-off fix: when we look for SCI implementations, we remove the Logback SCI from the list
    // of possible implementations, because we would refuse to load it anyway due to the deny list.
    //
    // Alternatively, we could remove Logback from the list of server classes, and live with the fact that Gretty
    // leaks Logback onto the webapp classpath. We fail to estimate the impact of that on existing webapps
    // which then possibly find multiple logging frameworks.
    //
    // To fix all of these issues, we would require to split the class paths of runner projects from the classpath
    // of servlet containers, and establish a new class loader hierarchy for webapps, which looked like the following:
    //
    // → boot classloader
    //    |-→ server class loader, containing only (transitive) dependencies of Jetty / Tomcat
    //    |   |-→ webapp classloader
    //    |-→ runner classloader, which contains the bootstrap classes for Gretty
    //
    // This design would not only stop leakage of Logback onto the classpath of webapps, but also of all other
    // runner dependencies (at the time of writing: groovy-cli, groovy-json, commons-cli, commons-io, slf4j),
    // and get rid off the (incomplete) mitigation of this problem in form of `FilteringClassLoader` and its
    // exclusion patterns.
    if (name.equals("META-INF/services/jakarta.servlet.ServletContainerInitializer")) {
      List<URL> resources = Collections.list(super.getResources(name));
      Pattern logbackJar = Pattern.compile("logback-classic(.*?).jar");
      resources.removeIf(resource -> logbackJar.matcher(resource.toString()).find());
      return Collections.enumeration(resources);
    }

    return super.getResources(name);
  }

  private Enumeration<URL> getBootstrapResources(String name) throws IOException {
    return bootClassLoader.getResources(name);
  }
}
