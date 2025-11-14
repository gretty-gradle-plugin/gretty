/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * SSO Digest Authenticator for Jetty 11
 * Enables session sharing for Digest authentication
 */
class SSODigestAuthenticator extends DigestAuthenticator {

    private static final Logger LOG = Log.getLogger(SSODigestAuthenticator.class);

    @Override
    public UserIdentity login(String username, Object password, ServletRequest request)
    {
        UserIdentity user = super.login(username, password, request);
        if (user != null) {
            SSOHelper.cacheAuthentication(((HttpServletRequest)request).getSession(true), getAuthMethod(), user, password);
        }
        return user;
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException
    {
        Authentication cached = SSOHelper.checkCachedAuthentication(req, res, mandatory, _loginService, LOG);
        return cached != null ? cached : super.validateRequest(req, res, mandatory);
    }
}
