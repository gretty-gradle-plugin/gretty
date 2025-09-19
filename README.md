![logo](https://akhikhl.github.io/gretty/media/gretty_logo_1.x.png "gretty logo")

![Build Status](https://github.com/gretty-gradle-plugin/gretty/workflows/CI/badge.svg)
![Maintenance Status](https://img.shields.io/maintenance/yes/2025.svg)
[![Latest release](https://img.shields.io/badge/release-4.1.8-47b31f.svg)](https://github.com/gretty-gradle-plugin/gretty/tree/v4.1.8)
[![Snapshot](https://img.shields.io/badge/current-4.1.9--SNAPSHOT-47b31f.svg)](https://github.com/gretty-gradle-plugin/gretty/tree/master)
[![License](https://img.shields.io/badge/license-MIT-47b31f.svg)](#copyright-and-license)

Gretty is a feature-rich Gradle plugin for running web-apps on embedded servlet containers.
It supports Jetty version 11, Tomcat version 10, multiple web-apps and many more.
It wraps servlet container functions as convenient Gradle tasks and configuration DSL.

A complete list of Gretty features is available in [feature overview](https://gretty-gradle-plugin.github.io/gretty-doc/Feature-overview.html).

You are looking at Gretty's `master` branch which is for Gretty 4. You also might want to look at the [gretty-3.x](https://github.com/gretty-gradle-plugin/gretty/tree/gretty-3.x) one which is for Gretty 3. It is still in development and supports older (non-Jakarta) versions of Jetty and Tomcat.

#### Where to start

[![Join the chat at https://gitter.im/gretty-gradle-plugin/gretty](https://badges.gitter.im/gretty-gradle-plugin/gretty.svg)](https://gitter.im/gretty-gradle-plugin/gretty?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

If you are new with Gretty, try [getting started](https://gretty-gradle-plugin.github.io/gretty-doc/Getting-started.html) page.

#### :star: What's new

### Version 4.1.8
September 19, 2025, Gretty 4.1.8  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Gradle 9 support

### Version 4.1.7
July 22, 2025, Gretty 4.1.7  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Check for Groovy >= 4 and use correct coordinates (thanks [@octylFractal](https://github.com/octylFractal))
* Fix clobbering webapp classpath with Gretty stuff (thanks [@Artur-](https://github.com/Artur-) and [@Yasushi](https://github.com/Yasushi))
* Migrate `groovy.util.XmlSlurper` to `groovy.xml.XmlSlurper` (thanks [@TWiStErRob](https://github.com/TWiStErRob))

### Version 4.1.6
December 6, 2024, Gretty 4.1.6  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Fix product runs (thanks [@aindlq](https://github.com/aindlq))
* Provide a clear list of supported containers (thanks [@ljcyu](https://github.com/ljcyu))

### Version 4.1.5
July 15, 2024, Gretty 4.1.5  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Make Gretty aware of Gradle Java Toolchain (thanks [@mr-serjey](https://github.com/mr-serjey))

### Version 4.1.4
May 22, 2024, Gretty 4.1.4 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Fix jetty redeploy with custom jetty-env.xml
* Support folders under "src/resources" in fastReload configuration property

### Version 4.1.3
March 18, 2024, Gretty 4.1.3  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Update Tomcat and Bouncy Castle (thanks to [@pranav24gupta](https://github.com/pranav24gupta))

### Version 4.1.2
January 6, 2024, Gretty 4.1.2  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* jettyStart throws exception if using jetty-env.xml #299 (special thanks to [@gy-chen](https://github.com/gy-chen).

### Version 4.1.1
October 25, 2023, Gretty 4.1.1  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Fix broken `restart` tasks after runner classpath separation

* Use appropriate class loader for servermanger commands. Thanks to Shane Hird.

* Upgrade Tomcat version to 10.1.5. Thanks to [@pranav24gupta](https://github.com/pranav24gupta).

### Version 4.1.0
July 12, 2023, Gretty 4.1.0  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Drops Gradle 6 support

* Enables stricter plugin validation

* Adds Gradle 8 integration test build

August 24, 2022, Gretty 4.0.3 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Changes in this version:

* Replace internal Gradle API usage with public API #263

* Tomcat 10.0.22

* Jetty 11.0.11

June 2, 2022, Gretty 4.0.2 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Changes in this version:

* Use Tomcat 10.0.21

* Use Jetty 11.0.9

* Use Gradle 7.4.2 for testing Gretty

* Added exclusion patterns for `commons-cli` and `commons-io` classes to FilteringClassLoader #258
  Adding the patterns fixes a bug which gave preference to the `commons-cli` and `commons-io` versions
  that Gretty uses, rather than using the JARs bundled with the webapp (which is the correct behavior).

* Upgrade to Logback 1.3.0-alpha14

* Remove Groovy-based logging configuration in response to Log4Shell #249

February 25, 2022, Gretty 4.0.1 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Changes in this version:

* Fix inability to build a product due to missing Groovy dependencies (#238).

* Version upgrades to mitigate some CVEs (#252). Thanks to [@dutta1kartik3](https://github.com/dutta1kartik3).

September 1, 2021, Gretty 4.0.0 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Changes in this version:

* Gretty requires JDK11+.

* Gretty supports only Tomcat 10 and Jetty 11 (`Jakarta` versions of the containers). All thanks goes to [@f4lco](https://github.com/f4lco).

July 27, 2021, Gretty 3.0.6 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Changes in this version:

* JDK 16 support

June 28, 2021, Gretty 3.0.5 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

**Please note that Gretty 3.0.5 is the first one not using `JCenter` but rather `Maven Central`. Be sure
to change your `jcenter()` to `mavenCentral()` in your Gradle files.**

* Changes in this version:

* Added some release details for Maven Central.

* Added uploadArchives for upload to Maven Central.

* Restored simple GPG signing in prep for replacement of Bintray with Maven Central.

* Build with Gradle 6.x in Jitpack

* Use a fixed Gecko driver version

* Use Gradle 6.9 for the "global" build

* Fix the `gradle clean` task

* Update some packages

* Gradle defines version as an object so we need to make sure we are doing explicit to String conversion

* Revert "Purge identical builds by removing 'pull_request' trigger from GH actions"

* Replace deprecated JavaExec.main usage with JavaExec.mainClass property

* Merge pull request #218 from brandonramirez/loopback_address_bind

* Update Gradle wrapper to 7.0-rc-2

* Remove Gradle wrapper task definitions

* Add Java 16 and build only against currently supported versions of Java

* Update `actions/setup-java`

* Purge identical builds by removing 'pull_request' trigger from GH actions

* Run tests on JUnit 5 on Gradle 7

* Upgrade dependencies in Gradle 7 build

* Replace jcenter repository with Maven Central

* Rely on Groovy version shipped with Gradle

* Add Gradle 7 build job

* Explicitly bind to loopback address rather than local address to fix a BindException.

* Use the `springBoot` option when it is not null (#213)

* Avoid calling afterEvaluate on already evaluated project

* Remove unused import

* Improve the code structure

* Improve the dependency resolving logic

* Update Gradle's version in CI

* [skip ci] Remove mentions of Tomcat 10 in the `changes.md` file too

* Remove publishing to Bintray on push

* Fixed 'multiple plugins are using the same ID' error in publishPlugins. (#211)

March 30, 2021, Gretty 3.0.4 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

Special thanks to all contributors to this release, and especially Boris Petrov, Falco Duersch and Stefan Wolf for multiple contributions.

* Changes in this version:

* Gradle 7 support

* Fix handling of httpsIdleTimeout in Tomcat (#144)

* Fix behavior of maxPostSize in Tomcat (#144)

* Guard remaining calls to Connector#setProperty with assertions (#144)

* Removed calls to Jetty 9.4 deprecated method (soLingerTime) (#171)

* Update Gradle's testing version to 6.6.1 and geckodriver to 0.27.0

* Fix issue #104 - Bug: HotReload Exception with Composite

* Correctly populate the `writer` field in `ServerStartEventImpl`

* Fix issue #104 - Bug: HotReload Exception with Composite

* Ability to add additinal files to product build.

* Update ASM

* Update default Tomcat versions

* Also run the full test suite on JDK 15

* Use a specific Gradle version for all Travis tasks

* Update Gradle's version to 6.8.3

* Non-blocking context initialization. Fix "redeploy" cleanup.

* Update Groovy

* Migrate from Travis CI to GitHub actions

* Annotate `ServerConfig` to fix Gradle deprecation warnings (#195)

* Annotate `WebAppConfig`, StartBaseTask, AppAfterIntegrationTestTask, AppBeforeIntegrationTestTask, AppServiceTask, FarmStartTask, AppRedeployTask, FarmAfterIntegrationTestTask, FarmBeforeIntegrationTestTask, FarmIntegrationTestTask, JacocoHelper

* Rename annotated interfaces for tasks

* Fix a bunch of Gradle deprecation warnings

* Use `api` for `libs/gretty` dependencies

* Lazily add source and classes dirs

* Use Gradle's `Task Configuration Avoidance` APIs in a few places

* Add validation task to gretty plugin

* Use java-gradle-plugin for generating the plugin properties

* Upgrade to newest version of the publishing plugin

* Enable stricter validation for validatePlugins

* Replace deprecated task name in jacocoInstantiateTasks itest

* Move common.gradle to a precompiled script plugin

* Use different configuration for library and plugin projects

* Move some more things out of `afterEvaluate`

* Fix source- and targetCompatibility versions

* Use publication for uploading to bintray

* Remove the maven plugin

* Use new API for publishing javadoc and sources

* Add a missing `bintrayUserOrg` property

* Fix using the wrong configuration for runner-projects

* Use task configuration avoidance (easy instances) #141

* Add some dependencies needed by Groovy 3

* Spring support: avoid classloading of webapp classes at configuration time

May 7, 2020, Gretty 3.0.3 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

* Changes in this version:

* Updated ASM to 8.0.1.

* Fixed excess logging output and set initial log level (#150).

* Removed deprecated check for already in-use ports (#147).

* Added support for Gradle 5.6 debugging API.

* Fixed incorrect serialization of the initParameters in productBuild.

* Updated Tomcat 9 version and TC9 servlet API version.

* Set javaExec debug options properly.

* Updated Gradle 6 testing to use Gradle 6.3.

See [complete list of changes](changes.md) for more information.

March 29, 2020, Gretty 3.0.2 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

This release brings Java 14 support, deprecation fixes for Gradle 6.x and bug-fixes.

https://bintray.com/javabrett/maven/org.gretty/view

December 2, 2019, Gretty 3.0.1 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

This release contains further fixes for Gradle 6.0 support.

December 1, 2019, Gretty 3.0.0 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

This release introduces Gradle 6.0 support and retires support for JDK7, Gradle versions <5.0 and Tomcat 7.x and 8.0.x.

See [complete list of changes](changes.md) for more information.

December 5, 2018, Gretty 2.3.1 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

This maintenance release addresses some issues found in Gretty 2.3.0.  See [complete list of changes](changes.md) for more information.

November 28, 2018, Gretty 2.3.0 is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

This release adds support for Gradle 5.0, which was released this week!  Please raise an issue if you find any issues running Gretty 2.3+ with Gradle 5.0.

See also: [complete list of changes](changes.md) for more information.

May 21, 2018, Gretty(.org) 2.2.0 is out and immediately available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).

* Changes in this version:

* Bumped default Tomcat 9 version to 9.0.6 (was 9.0.5).

* Support added for Tomcat 8.5 and Tomcat support refactoring (thanks Boris Petrov). Tomcat 8.5 replaces deprecated Tomcat 8.0.

* Bumped Spring Boot version to 1.5.9 (was 1.5.4).

* Bumped versions of asm (6.1.1, was 6.0), Groovy (2.4.15, was 2.4.13) and Spring (4.3.16, was 4.3.9) (thanks Henrik Brautaset Aronsen).

* Fixed incompatibility with java-library plugin (thanks Ollie Freeman).

* Dev: various build and test improvements.

See also: [complete list of changes](changes.md) for more information.

#### Documentation

You can learn about all Gretty features in [online documentation](https://gretty-gradle-plugin.github.io/gretty-doc/).

#### System requirements

Gretty requires JDK11+ and Gradle 6.0 or newer.

- Since version 2.0.0 Gretty no longer supports JDK6.
- Since version 3.0.0 Gretty no longer supports JDK7, Gradle <5.0, Tomcat 7.x or Tomcat 8.0.x.
- Since version 4.0.0 Gretty supports only JDK 11+, Gradle 6.0+, Tomcat 10.x and Jetty 11.x

#### Availability

Gretty is an open-source project and is freely available in sources as well as in compiled form.

Releases of Gretty (gretty.org fork) from 2.1.0 onwards are available at [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).
Old releases of Gretty up to and including version 2.0.0 are available at [Bintray](https://bintray.com/akhikhl/maven/gretty/view).

#### Copyright and License

Copyright 2013-2020 (c) Andrey Hihlovskiy, Timur Shakurov and [contributors](CONTRIBUTORS).

All versions, present and past, of Gretty are licensed under [MIT license](LICENSE).
