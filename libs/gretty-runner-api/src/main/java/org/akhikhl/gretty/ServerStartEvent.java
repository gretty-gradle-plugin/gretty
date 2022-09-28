/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author akhikhl
 */
public class ServerStartEvent {

  private final Map<String, String> serverStartInfo;

  public ServerStartEvent(Map<String, String> serverStartInfo) {
    this.serverStartInfo = Collections.unmodifiableMap(new HashMap<>(serverStartInfo));
  }

  public Map<String, String> getServerStartInfo() {
    return serverStartInfo;
  }
}
