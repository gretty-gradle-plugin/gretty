/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.SPNEGOAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.LoggerFactory;

/**
 * SSO SPNEGO Authenticator for Jetty 12
 * Enables session sharing for SPNEGO/Kerberos authentication
 */
public class SSOSpnegoAuthenticator extends SPNEGOAuthenticator {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SSOSpnegoAuthenticator.class);

    public SSOSpnegoAuthenticator() {
        super();
    }

    public SSOSpnegoAuthenticator(String authMethod) {
        super(authMethod);
    }

    @Override
    public UserIdentity login(String username, Object password, Request request, Response response)
    {
        UserIdentity user = super.login(username, password, request, response);
        if (user != null) {
            SSOHelper.cacheAuthentication(request.getSession(true), getAuthenticationType(), user, password);
        }
        return user;
    }

    @Override
    public AuthenticationState validateRequest(Request req, Response res, Callback callback) throws ServerAuthException
    {
        AuthenticationState cached = SSOHelper.checkCachedAuthentication(req, _loginService, LOG, "SSO SPNEGO");
        return cached != null ? cached : super.validateRequest(req, res, callback);
    }
}
