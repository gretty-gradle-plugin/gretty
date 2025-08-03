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
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.SslClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.eclipse.jetty.security.authentication.FormAuthenticator.*;

/**
 *
 * @author akhikhl
 */
public class SSOClientCertAuthenticator extends SslClientCertAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(SSOClientCertAuthenticator.class);

    public SSOClientCertAuthenticator() {
        super(new SslContextFactory.Client());
    }


    // "login" is copied without changes from FormAuthenticator
    @Override
    public UserIdentity login(String username, Object password, Request request, Response response)
    {

        UserIdentity user = super.login(username,password,request,response);
        if (user!=null)
        {
            HttpSession session = ((HttpServletRequest)request).getSession(true);
            SessionAuthentication cached=new SessionAuthentication(Authenticator.CERT_AUTH,user,password);
            session.setAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE, cached);
        }
        return user;
    }

    @Override
    public AuthenticationState validateRequest(Request req, Response res, Callback callback) throws ServerAuthException
    {
        HttpServletRequest request = (HttpServletRequest)req;

//        if (!mandatory)
//            return new DeferredAuthentication(this);

        // ++ copied from FormAuthenticator

        HttpSession session = request.getSession(true);

        // Look for cached authentication
        SessionAuthentication authentication = (SessionAuthentication) session.getAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);
        if (authentication != null)
        {
            // Has authentication been revoked?
            if (_loginService!=null &&
                !_loginService.validate(authentication.getUserIdentity()))
            {
                LOG.debug("auth revoked {}",authentication);
                session.removeAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);
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
                        LOG.debug("auth retry {}->{}",authentication,j_uri);
                        StringBuffer buf = request.getRequestURL();
                        if (request.getQueryString() != null)
                            buf.append("?").append(request.getQueryString());

                        if (j_uri.equals(buf.toString()))
                        {
                            MultiMap<String> j_post = (MultiMap<String>)session.getAttribute(__J_POST);
                            if (j_post!=null)
                            {
                                LOG.debug("auth rePOST {}->{}",authentication,j_uri);
                                for (Map.Entry<String, List<String>> entry : j_post.entrySet()) {
                                    for (String value : entry.getValue()) {
                                        request.setAttribute(entry.getKey(), value);
                                    }
                                }
                            }
                            session.removeAttribute(__J_URI);
                            session.removeAttribute(__J_METHOD);
                            session.removeAttribute(__J_POST);
                        }
                    }
                }
                LOG.debug("auth {}",authentication);
                return authentication;
            }
        }
        // -- copied from FormAuthenticator

        return super.validateRequest(req, res, callback);
    }
}
