package org.akhikhl.gretty

import groovy.transform.CompileStatic
import org.eclipse.jetty.session.ManagedSession
import org.eclipse.jetty.session.DefaultSessionIdManager
import org.eclipse.jetty.session.SessionManager
import org.eclipse.jetty.ee10.servlet.SessionHandler

@CompileStatic
class SingleSignOnSessionHandler extends SessionHandler {

    @Override
    ManagedSession getManagedSession(String extendedId) {
        ManagedSession session = getLocalSession(extendedId)

        if (session == null) {
            def sessionIdManager = getSessionIdManager()
            if (sessionIdManager instanceof DefaultSessionIdManager) {
                def managers = ((DefaultSessionIdManager) sessionIdManager).getSessionManagers()

                for (SessionManager manager : managers) {
                    if (manager == this || !(manager instanceof SingleSignOnSessionHandler)) {
                        continue
                    }

                    session = ((SingleSignOnSessionHandler) manager).getLocalSession(extendedId)
                    if (session != null) {
                        break
                    }
                }
            }
        }

        return session
    }

    private ManagedSession getLocalSession(String extendedId) {
        return super.getManagedSession(extendedId)
    }
}
