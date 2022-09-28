/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import java.util.List;
import java.util.Map;

/**
 * @author akhikhl
 */
interface ServerManager {

  void setParams(Map params);

  ServerStartEvent startServer();

  void stopServer();

  void redeploy(List<String> webapps);
}
