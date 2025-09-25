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
 *
 * @author akhikhl
 */
public class SSOAuthenticatorFactory extends DefaultAuthenticatorFactory {


  @Override
  public Authenticator getAuthenticator(Server server, Context context, Configuration configuration) {
    String auth = configuration.getAuthenticationType();
    if (auth==null || Authenticator.BASIC_AUTH.equalsIgnoreCase(auth))
      return new SSOBasicAuthenticator();
    if (Authenticator.DIGEST_AUTH.equalsIgnoreCase(auth))
      return new SSODigestAuthenticator();
    if ( Authenticator.SPNEGO_AUTH.equalsIgnoreCase(auth) )
      return new SSOSpnegoAuthenticator();
    if ( Authenticator.NEGOTIATE_AUTH.equalsIgnoreCase(auth) ) // see Bug #377076
      return new SSOSpnegoAuthenticator(Authenticator.NEGOTIATE_AUTH);
    if (Authenticator.CERT_AUTH.equalsIgnoreCase(auth)||Authenticator.CERT_AUTH2.equalsIgnoreCase(auth))
      return new SSOClientCertAuthenticator();
    return super.getAuthenticator(server, context, configuration);

  }
}
