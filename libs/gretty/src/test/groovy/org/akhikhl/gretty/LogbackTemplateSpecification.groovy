package org.akhikhl.gretty

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import spock.lang.Specification

class LogbackTemplateSpecification extends Specification {

    private static logDir = "/path/to/logs"
    private static logFileName = "test"
    private static level = 'ERROR'

    def "should expand all variables"() {
        when:
        def actual = LogbackTemplate.render(
                logDir,
                logFileName,
                level,
                true,
                true,
        )

        then:
        assert actual.contains(logDir)
        assert actual.contains(logFileName)
        assert actual.contains(level)
    }

    def "can parse complete Logback template"() {
        given:
        def actual = LogbackTemplate.render(
                logDir,
                logFileName,
                level,
                true,
                true,
        )

        when:
        tryParse(actual)

        then:
        noExceptionThrown()
    }

    def "can parse template with disabled console log"() {
        given:
        def actual = LogbackTemplate.render(
                logDir,
                logFileName,
                level,
                false,
                true,
        )

        when:
        tryParse(actual)

        then:
        noExceptionThrown()
    }

    def "can parse template with disabled file log"() {
        given:
        def actual = LogbackTemplate.render(
                logDir,
                logFileName,
                level,
                true,
                false,
        )

        when:
        tryParse(actual)

        then:
        noExceptionThrown()
    }

    private static tryParse(String logbackConfigText) {
        def configurator = new JoranConfigurator()
        configurator.context = new LoggerContext()
        configurator.doConfigure(new ByteArrayInputStream(logbackConfigText.getBytes('UTF-8')))
    }
}
