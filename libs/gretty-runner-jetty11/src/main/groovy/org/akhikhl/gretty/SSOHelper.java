/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty;

import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.log.Logger;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.eclipse.jetty.security.authentication.FormAuthenticator.*;

/**
 * Helper class for Single Sign-On authentication support in Jetty 11.
 * Provides common functionality for caching and validating authentication across multiple webapps.
 */
public class SSOHelper {

    /**
     * Cache the authentication in the session for SSO support.
     */
    public static void cacheAuthentication(HttpSession session, String authMethod,
                                          UserIdentity user, Object password) {
        Authentication cached = new SessionAuthentication(authMethod, user, password);
        session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, cached);
    }

    /**
     * Check for cached authentication in the session.
     * Returns the cached authentication if valid, null otherwise.
     * Includes FormAuthenticator redirect and POST parameter restoration logic.
     */
    public static Authentication checkCachedAuthentication(
            ServletRequest req, ServletResponse res, boolean mandatory,
            LoginService loginService, Logger log) throws ServerAuthException {

        HttpServletRequest request = (HttpServletRequest)req;

        if (!mandatory)
            return new DeferredAuthentication(null);

        HttpSession session = request.getSession(true);

        // Look for cached authentication
        Authentication authentication = (Authentication) session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
        if (authentication != null)
        {
            // Has authentication been revoked?
            if (authentication instanceof Authentication.User &&
                loginService != null &&
                !loginService.validate(((Authentication.User)authentication).getUserIdentity()))
            {
                log.debug("auth revoked {}",authentication);
                session.removeAttribute(SessionAuthentication.__J_AUTHENTICATED);
            }
            else
            {
                synchronized (session)
                {
                    String j_uri=(String)session.getAttribute(__J_URI);
                    if (j_uri!=null)
                    {
                        //check if the request is for the same url as the original and restore
                        //params if it was a post
                        log.debug("auth retry {}->{}",authentication,j_uri);
                        StringBuffer buf = request.getRequestURL();
                        if (request.getQueryString() != null)
                            buf.append("?").append(request.getQueryString());

                        if (j_uri.equals(buf.toString()))
                        {
                            MultiMap<String> j_post = (MultiMap<String>)session.getAttribute(__J_POST);
                            if (j_post!=null)
                            {
                                log.debug("auth rePOST {}->{}",authentication,j_uri);
                                Request base_request = Request.getBaseRequest(request);
                                base_request.setContentParameters(j_post);
                            }
                            session.removeAttribute(__J_URI);
                            session.removeAttribute(__J_METHOD);
                            session.removeAttribute(__J_POST);
                        }
                    }
                }
                log.debug("auth {}",authentication);
                return authentication;
            }
        }

        return null;
    }
}
