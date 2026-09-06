package com.careflow.ai.websocket;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Cleans up realtime state when a WebSocket session goes away:
 * <ul>
 *   <li>removes the session from presence tracking (and re-broadcasts),</li>
 *   <li>removes the session from the user→sessions registry so stale sessions
 *       never receive private notifications again.</li>
 * </ul>
 */
@Component
public class PresenceDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

    private final PresenceTracker presenceTracker;
    private final WebSocketSessionRegistry sessionRegistry;

    public PresenceDisconnectListener(PresenceTracker presenceTracker,
                                      WebSocketSessionRegistry sessionRegistry) {
        this.presenceTracker = presenceTracker;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        presenceTracker.leaveAll(event.getSessionId());
        sessionRegistry.unregister(event.getSessionId());
    }
}