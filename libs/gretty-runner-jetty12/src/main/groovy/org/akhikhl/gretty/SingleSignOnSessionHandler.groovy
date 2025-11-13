package org.akhikhl.gretty

import groovy.transform.CompileStatic
import org.eclipse.jetty.session.ManagedSession
import org.eclipse.jetty.session.DefaultSessionIdManager
import org.eclipse.jetty.session.SessionManager
import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.slf4j.LoggerFactory

@CompileStatic
class SingleSignOnSessionHandler extends SessionHandler {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SingleSignOnSessionHandler.class)

    @Override
    ManagedSession getManagedSession(String extendedId) {
        ManagedSession session = getLocalSession(extendedId)
        LOG.info("SSO getManagedSession({}): local session = {}", extendedId, session?.id)

        if (session == null) {
            def sessionIdManager = getSessionIdManager()
            LOG.info("SSO sessionIdManager = {}, type = {}", sessionIdManager, sessionIdManager?.class?.name)
            if (sessionIdManager instanceof DefaultSessionIdManager) {
                def managers = ((DefaultSessionIdManager) sessionIdManager).getSessionManagers()
                LOG.info("SSO searching across {} session managers", managers.size())

                for (SessionManager manager : managers) {
                    LOG.info("SSO checking manager: {}, type: {}, isThis: {}, isSSO: {}",
                        manager, manager?.class?.name, manager == this, manager instanceof SingleSignOnSessionHandler)
                    if (manager == this || !(manager instanceof SingleSignOnSessionHandler)) {
                        continue
                    }

                    session = ((SingleSignOnSessionHandler) manager).getLocalSession(extendedId)
                    if (session != null) {
                        LOG.info("SSO found session {} in another context", session.id)
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
