package org.akhikhl.gretty;

import org.eclipse.jetty.security.authentication.SslClientCertAuthenticator;

public class SSOClientCertAuthenticator extends SslClientCertAuthenticator {
  public SSOClientCertAuthenticator() {
    super(null); // no SslContextFactory
  }
}
