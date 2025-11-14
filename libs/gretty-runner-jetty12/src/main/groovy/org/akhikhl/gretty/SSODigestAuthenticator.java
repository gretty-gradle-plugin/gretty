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
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.LoggerFactory;

/**
 * SSO Digest Authenticator for Jetty 12
 * Enables session sharing for Digest authentication
 */
public class SSODigestAuthenticator extends DigestAuthenticator {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SSODigestAuthenticator.class);

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
        AuthenticationState cached = SSOHelper.checkCachedAuthentication(req, _loginService, LOG, "SSO DIGEST");
        return cached != null ? cached : super.validateRequest(req, res, callback);
    }
}
