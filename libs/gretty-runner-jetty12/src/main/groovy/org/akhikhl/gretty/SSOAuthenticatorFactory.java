/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Authenticator.Configuration;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.server.Context;
import org.eclipse.jetty.server.Server;

/**
 * SSO Authenticator Factory for Jetty 12
 * Returns SSO-enabled authenticators that share session authentication
 *
 * @author akhikhl
 */
public class SSOAuthenticatorFactory extends DefaultAuthenticatorFactory {

  @Override
  public Authenticator getAuthenticator(Server server, Context context, Configuration configuration) {
    String auth = configuration.getAuthenticationType();

    if (auth==null || "BASIC".equalsIgnoreCase(auth))
      return new SSOBasicAuthenticator();
    if ("DIGEST".equalsIgnoreCase(auth))
      return new SSODigestAuthenticator();
    if ("SPNEGO".equalsIgnoreCase(auth))
      return new SSOSpnegoAuthenticator();
    if ("NEGOTIATE".equalsIgnoreCase(auth))
      return new SSOSpnegoAuthenticator("NEGOTIATE");
    if ("CLIENT_CERT".equalsIgnoreCase(auth)||"CLIENT-CERT".equalsIgnoreCase(auth))
      return new SSOClientCertAuthenticator();

    return super.getAuthenticator(server, context, configuration);
  }
}
