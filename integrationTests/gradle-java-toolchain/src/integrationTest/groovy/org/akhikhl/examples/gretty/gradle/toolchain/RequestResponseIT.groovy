/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.examples.gretty.gradle.toolchain

import geb.spock.GebReportingSpec

class RequestResponseIT extends GebReportingSpec {

    private static String baseURI
    private static String toolchainJavaVersion

    void setupSpec() {
        baseURI = System.getProperty('gretty.baseURI')
        toolchainJavaVersion = System.getProperty('toolchainJavaVersion') ?: System.getProperty('java.vm.version')
    }

    def 'should have toolchain java version returned from server side'() {
        when:
        go baseURI
        $('#sendRequest').click()
        waitFor { $("p.hide#result").size() == 0 }
        then:
        $('#result').text().startsWith('Got from server: ' + "$toolchainJavaVersion")
    }
}
