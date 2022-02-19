/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.util.ContextUtil
import ch.qos.logback.core.util.OptionHelper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * Extends logback GafferConfigurator with run method accepting Binding
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class GafferConfiguratorEx {

  LoggerContext context
  static final String DEBUG_SYSTEM_PROPERTY_KEY = "logback.debug"

  GafferConfiguratorEx(LoggerContext context) {
    this.context = context
  }

  protected void informContextOfURLUsedForConfiguration(URL url) {
    ConfigurationWatchListUtil.setMainWatchURL(context, url);
  }

  void run(URL url) {
    informContextOfURLUsedForConfiguration(url);
    run(url.text);
  }

  void run(File file) {
    informContextOfURLUsedForConfiguration(file.toURI().toURL());
    run(file.text);
  }


  void run(Binding binding, String dslText) {

    binding.setProperty("hostname", ContextUtil.localHostName);

    def configuration = new CompilerConfiguration()
    configuration.addCompilationCustomizers(importCustomizer())

    String debugAttrib = System.getProperty(DEBUG_SYSTEM_PROPERTY_KEY);
    if (OptionHelper.isEmpty(debugAttrib) || debugAttrib.equalsIgnoreCase("false")
            || debugAttrib.equalsIgnoreCase("null")) {
      // For now, Groovy/Gaffer configuration DSL does not support "debug" attribute. But in order to keep
      // the conditional logic identical to that in XML/Joran, we have this empty block.
    } else {
      OnConsoleStatusListener.addNewInstanceToContext(context);
    }

    // caller data should take into account groovy frames
    new ContextUtil(context).addGroovyPackages(context.getFrameworkPackages());

    Script dslScript = new GroovyShell(binding, configuration).parse(dslText)

    dslScript.metaClass.mixin(ConfigurationDelegate)
    dslScript.setContext(context)
    dslScript.metaClass.getDeclaredOrigin = { dslScript }

    dslScript.run()
  }

  protected ImportCustomizer importCustomizer() {
    def customizer = new ImportCustomizer()


    def core = 'ch.qos.logback.core'
    customizer.addStarImports(core, "${core}.encoder", "${core}.read", "${core}.rolling", "${core}.status",
            "ch.qos.logback.classic.net")

    customizer.addImports(PatternLayoutEncoder.class.name)

    customizer.addStaticStars(Level.class.name)

    customizer.addStaticImport('off', Level.class.name, 'OFF')
    customizer.addStaticImport('error', Level.class.name, 'ERROR')
    customizer.addStaticImport('warn', Level.class.name, 'WARN')
    customizer.addStaticImport('info', Level.class.name, 'INFO')
    customizer.addStaticImport('debug', Level.class.name, 'DEBUG')
    customizer.addStaticImport('trace', Level.class.name, 'TRACE')
    customizer.addStaticImport('all', Level.class.name, 'ALL')

    customizer
  }
}

