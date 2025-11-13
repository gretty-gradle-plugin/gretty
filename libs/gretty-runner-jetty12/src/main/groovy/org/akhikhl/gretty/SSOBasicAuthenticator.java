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
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;
import org.slf4j.LoggerFactory;

/**
 * SSO Basic Authenticator for Jetty 12
 * Enables session sharing between FORM and BASIC authentication
 *
 * @author akhikhl
 */
public class SSOBasicAuthenticator extends BasicAuthenticator {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SSOBasicAuthenticator.class);

    @Override
    public UserIdentity login(String username, Object password, Request request, Response response)
    {
        UserIdentity user = super.login(username, password, request, response);
        if (user != null)
        {
            Session session = request.getSession(true);
            AuthenticationState cached = new SessionAuthentication(getAuthenticationType(), user, password);
            session.setAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE, cached);
            LOG.info("SSO BASIC: Cached authentication for user {} in session {}", username, session.getId());
        }
        return user;
    }

    @Override
    public AuthenticationState validateRequest(Request req, Response res, Callback callback) throws ServerAuthException
    {
        // Check for cached authentication in session first
        Session session = req.getSession(false);
        if (session != null)
        {
            AuthenticationState authenticationState =
                (AuthenticationState)session.getAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);

            if (authenticationState != null)
            {
                LOG.info("SSO BASIC: Found cached authentication in session {}: {}", session.getId(), authenticationState);

                // Validate that the authentication is still valid
                if (authenticationState instanceof LoginAuthenticator.UserAuthenticationSucceeded)
                {
                    LoginAuthenticator.UserAuthenticationSucceeded succeeded =
                        (LoginAuthenticator.UserAuthenticationSucceeded)authenticationState;

                    if (_loginService != null && _loginService.validate(succeeded.getUserIdentity()))
                    {
                        LOG.debug("SSO BASIC: Using cached authentication {}", authenticationState);
                        return authenticationState;
                    }
                    else
                    {
                        LOG.debug("SSO BASIC: Cached authentication revoked {}", authenticationState);
                        session.removeAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);
                    }
                }
            }
            else
            {
                LOG.info("SSO BASIC: No cached authentication in session {}", session.getId());
            }
        }

        // No valid cached authentication, proceed with BASIC auth
        return super.validateRequest(req, res, callback);
    }
}
