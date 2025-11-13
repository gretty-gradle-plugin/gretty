package org.akhikhl.gretty;

import org.eclipse.jetty.security.authentication.SPNEGOAuthenticator;

public class SSOSpnegoAuthenticator extends SPNEGOAuthenticator {
  public SSOSpnegoAuthenticator() {
    super();
  }
  
  public SSOSpnegoAuthenticator(String authMethod) {
    super(authMethod);
  }
}
