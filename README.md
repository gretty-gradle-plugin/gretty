![logo](https://akhikhl.github.io/gretty/media/gretty_logo_1.x.png "gretty logo")

![Build Status](https://github.com/gretty-gradle-plugin/gretty/workflows/CI/badge.svg)
![Maintenance Status](https://img.shields.io/maintenance/yes/2025.svg)
[![Latest release](https://img.shields.io/badge/release-4.1.10-47b31f.svg)](https://github.com/gretty-gradle-plugin/gretty/tree/v4.1.10)
[![Snapshot](https://img.shields.io/badge/current-4.1.11--SNAPSHOT-47b31f.svg)](https://github.com/gretty-gradle-plugin/gretty/tree/master)
[![License](https://img.shields.io/badge/license-MIT-47b31f.svg)](#copyright-and-license)

Gretty is a feature-rich Gradle plugin for running web-apps on embedded servlet containers.
It supports Jetty version 12, Tomcat version 11, multiple web-apps and many more.
It wraps servlet container functions as convenient Gradle tasks and configuration DSL.

A complete list of Gretty features is available in [feature overview](https://gretty-gradle-plugin.github.io/gretty-doc/Feature-overview.html).

You are looking at Gretty's `master` branch which is for Gretty 5. You also might want to look at the [gretty-3.x](https://github.com/gretty-gradle-plugin/gretty/tree/gretty-3.x) one which is for Gretty 3 or [gretty-4.x](https://github.com/gretty-gradle-plugin/gretty/tree/gretty-4.x) one which is for Gretty 4. They are still in development and supports older versions of Jetty and Tomcat.

#### Where to start

[![Join the chat at https://gitter.im/gretty-gradle-plugin/gretty](https://badges.gitter.im/gretty-gradle-plugin/gretty.svg)](https://gitter.im/gretty-gradle-plugin/gretty?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

If you are new with Gretty, try [getting started](https://gretty-gradle-plugin.github.io/gretty-doc/Getting-started.html) page.

#### :star: What's new

### Version 5.0.0
September 19, 2025, Gretty 4.1.10  is out and available at [Gradle Plugins](https://plugins.gradle.org/plugin/org.gretty) and [Maven Central](https://search.maven.org/artifact/org.gretty/gretty).

* Support Tomcat 11 and Jetty 12 (thanks [@codeconsole](https://github.com/codeconsole))

See also: [complete list of changes](changes.md) for more information.

#### Documentation

You can learn about all Gretty features in [online documentation](https://gretty-gradle-plugin.github.io/gretty-doc/).

#### System requirements

Gretty requires JDK17+ and Gradle 7.0 or newer.

- Since version 2.0.0 Gretty no longer supports JDK6.
- Since version 3.0.0 Gretty no longer supports JDK7, Gradle <5.0, Tomcat 7.x or Tomcat 8.0.x.
- Since version 4.0.0 Gretty supports only JDK 11+, Gradle 6.0+, Tomcat 10.x and Jetty 11.x
- Since version 5.0.0 Gretty supports only JDK 17+, Gradle 7.0+, Tomcat 11.x and Jetty 12.x

#### Availability

Gretty is an open-source project and is freely available in sources as well as in compiled form.

Releases of Gretty (gretty.org fork) from 2.1.0 onwards are available at [Bintray](https://bintray.com/javabrett/maven/org.gretty/view).
Old releases of Gretty up to and including version 2.0.0 are available at [Bintray](https://bintray.com/akhikhl/maven/gretty/view).

#### Copyright and License

Copyright 2013-2020 (c) Andrey Hihlovskiy, Timur Shakurov and [contributors](CONTRIBUTORS).

All versions, present and past, of Gretty are licensed under [MIT license](LICENSE).
