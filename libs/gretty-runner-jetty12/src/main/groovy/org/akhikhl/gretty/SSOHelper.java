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
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Session;
import org.slf4j.Logger;

/**
 * Helper class for Single Sign-On authentication support in Jetty 12.
 * Provides common functionality for caching and validating authentication across multiple webapps.
 */
public class SSOHelper {

    /**
     * Cache the authentication in the session for SSO support.
     */
    public static void cacheAuthentication(Session session, String authType,
                                          UserIdentity user, Object password) {
        AuthenticationState cached = new SessionAuthentication(authType, user, password);
        session.setAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE, cached);
    }

    /**
     * Check for cached authentication in the session.
     * Returns the cached authentication if valid, null otherwise.
     */
    public static AuthenticationState checkCachedAuthentication(
            Request req, LoginService loginService, Logger log, String authPrefix) {
        Session session = req.getSession(false);
        if (session == null) {
            return null;
        }

        AuthenticationState authenticationState =
            (AuthenticationState)session.getAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);

        if (authenticationState != null) {
            // Validate that the authentication is still valid
            if (authenticationState instanceof LoginAuthenticator.UserAuthenticationSucceeded) {
                LoginAuthenticator.UserAuthenticationSucceeded succeeded =
                    (LoginAuthenticator.UserAuthenticationSucceeded)authenticationState;

                if (loginService != null && loginService.validate(succeeded.getUserIdentity())) {
                    log.debug("{}: Using cached authentication {}", authPrefix, authenticationState);
                    return authenticationState;
                } else {
                    log.debug("{}: Cached authentication revoked {}", authPrefix, authenticationState);
                    session.removeAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);
                }
            }
        }

        return null;
    }
}
