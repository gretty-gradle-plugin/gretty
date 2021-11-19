# helloGrettyJakartaMigration

This integration test project shows how to deploy a existing webapp which still relies on the `javax` APIs with Tomcat 10.
Normally, Tomcat 10 would only support the Jakarta APIs, but Tomcat also ships with a compatibility mode which rewires the imports once the servlet or filter class is loaded via the webapp classloader.
This should also allow to gradually migrate a webapp.

## Deviations from 'helloGretty'

- A dependency on `org.apache.tomcat:jakartaee-migration`, which contains the actual migration logic.

- A dependency on the old APIs from `javax.servlet:javax.servlet-api`.
  'helloGrettyJakartaMigration' must be the only integration test project with a dependency on the old APIs, because Gretty 4 defaults to Jakarta and ships only Jakarta-enabled servlet containers.

- `inplace = false`. Given the current implementation in Tomcat, the redefined classes _must_ be located within `WEB-INF/classes` such that the class loading mechanism can find and transform the classes.
  An easy way to get there is to use the WAR, because Gretty's in-place webapp would just use the `build/classes` directory.
  See [this GH issue comment][gh-issue-class-loading] pointing out the relevant sources in Tomcat.

- A `context.xml` which enables the load-time transformation of servlet and filters via the `jakartaConverter` attribute on the loader.

 [gh-issue-class-loading]: https://github.com/gretty-gradle-plugin/gretty/issues/239#issuecomment-971378746
