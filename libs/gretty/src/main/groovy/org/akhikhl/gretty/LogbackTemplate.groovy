package org.akhikhl.gretty

import groovy.text.SimpleTemplateEngine
import groovy.transform.PackageScope

@PackageScope
class LogbackTemplate {

    private LogbackTemplate() {}

    static String render(
            String logDir,
            String logFileName,
            String loggingLevel,
            boolean consoleLogEnabled,
            boolean fileLogEnabled
    ) {
        return new SimpleTemplateEngine().createTemplate(read()).make([
                logDir           : logDir,
                logFileName      : logFileName,
                loggingLevel     : loggingLevel,
                consoleLogEnabled: consoleLogEnabled,
                fileLogEnabled   : fileLogEnabled,
        ]).toString()
    }

    static String read() {
        LogbackTemplate.classLoader.getResourceAsStream('logback-config-template/logback.xml').withStream {
            it.text
        }
    }
}
