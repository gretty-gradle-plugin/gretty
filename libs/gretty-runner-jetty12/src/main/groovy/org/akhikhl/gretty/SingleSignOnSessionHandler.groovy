package org.akhikhl.gretty

import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.session.ManagedSession
/**
 * A SessionHandler that enables single sign-on across multiple web applications.
 *
 * This handler intercepts session lookups. If a session is not found in the
 * current web application's context, it will search for the session in other
 * contexts on the server that are also configured for SSO.
 */
class SingleSignOnSessionHandler extends SessionHandler {

    @Override
     ManagedSession getManagedSession(String id) {
        // First, try to get the session from the local context using the standard behavior.
        ManagedSession session = super.getManagedSession(id)
        if (session != null) {
            return session
        }

        // If not found locally, search other SSO-enabled contexts.
        def server = getServer()
        if (server?.getHandler() == null) {
            return null
        }

        // Traverse the server's handler tree to find other SSO handlers.
        for (SessionHandler handler : findSsoHandlers(server.getHandler())) {
            if (handler == this) {
                continue
            }

            // Ask the other handler if it manages a session with this ID.
            ManagedSession otherSession = handler.getManagedSession(id)
            if (otherSession != null) {
                // Session found in another context.
                //
                // In modern Jetty, a session object is tightly coupled to its SessionHandler.
                // There is no public API to migrate a session by re-parenting it.
                //
                // Therefore, we return the session object directly, even though it is still
                // managed by the other context's handler. This is sufficient for SSO
                // authenticators which primarily need to read session attributes.
                //
                // Note: Lifecycle events for this session (e.g., invalidation) will be
                // handled by its original SessionHandler in the other context.
                return otherSession
            }
        }

        return null
    }

    /**
     * Finds all SingleSignOnSessionHandler instances by recursively
     * traversing the server's handler structure.
     *
     * @param currentHandler The handler to start searching from (e.g., server.getHandler()).
     * @return A list of all found SingleSignOnSessionHandler instances.
     */
    private List<SessionHandler> findSsoHandlers(Handler currentHandler) {
        List<SessionHandler> handlers = new ArrayList<>();

        if (currentHandler == null) {
            return handlers;
        }

        // Case 1: The current handler is a SessionHandler itself.
        // This is a direct check for the handler we're looking for.
        if (currentHandler instanceof SingleSignOnSessionHandler) {
            handlers.add((SessionHandler) currentHandler);
        }

        // Case 2: The current handler is a wrapper for another handler.
        // We need to unwrap it and check the contained handler.
        if (currentHandler instanceof Wrapper) {
            Handler wrappedHandler = ((Wrapper) currentHandler).getHandler();
            handlers.addAll(findSsoHandlers(wrappedHandler));
        }

        // Case 3: The current handler is a collection of other handlers.
        // We must check each child handler recursively.
        if (currentHandler instanceof Collection) {
            Handler[] childHandlers = ((Collection) currentHandler).getHandlers();
            if (childHandlers != null) {
                for (Handler child : childHandlers) {
                    handlers.addAll(findSsoHandlers(child));
                }
            }
        }

        return handlers;
    }
}
