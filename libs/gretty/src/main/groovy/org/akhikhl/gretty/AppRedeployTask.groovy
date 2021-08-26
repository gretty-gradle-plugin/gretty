package org.akhikhl.gretty

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault

/**
 * @author sala
 */
@DisableCachingByDefault
class AppRedeployTask extends AppServiceTask {
  @Internal
  List webapps = []

  def webapp(String webapp) {
    webapps.add(webapp)
  }

  @Override
  String getCommand() {
    return "redeploy ${webapps.join(' ')}"
  }
}
