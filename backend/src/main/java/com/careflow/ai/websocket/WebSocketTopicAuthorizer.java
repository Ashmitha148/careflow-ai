package com.careflow.ai.websocket;

import com.careflow.ai.entity.Role;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorizes every STOMP SUBSCRIBE before the broker registers it (defense in
 * depth on top of the JWT handshake). Unauthorized subscriptions are dropped by
 * returning {@code null}, so no data ever reaches a client that is not allowed
 * to see it — even if the client guesses a destination.
 *
 * Topic rules (see RealtimeTopics and docs/adr/0001-websocket-topic-design.md):
 * <ul>
 *   <li>{@code /user/queue/notifications} — any authenticated user; rewritten to
 *       a session-scoped unicast queue so delivery is deterministic per user.</li>
 *   <li>{@code /topic/patient/{id}/timeline|vitals|presence} — patient access rule.</li>
 *   <li>{@code /topic/nurse/{id}/queue} — the nurse themselves or an ADMIN.</li>
 *   <li>anything else — denied by default.</li>
 * </ul>
 */
@Component
public class WebSocketTopicAuthorizer implements ChannelInterceptor {

    private static final Pattern PATIENT_TOPIC =
            Pattern.compile("^/topic/patient/([0-9a-fA-F-]{36})/(timeline|vitals|presence|tasks)$");
    private static final Pattern NURSE_QUEUE_TOPIC =
            Pattern.compile("^/topic/nurse/([0-9a-fA-F-]{36})/queue$");

    private final WebSocketSessionRegistry sessionRegistry;
    private final PatientAccessChecker patientAccessChecker;

    public WebSocketTopicAuthorizer(WebSocketSessionRegistry sessionRegistry,
                                    PatientAccessChecker patientAccessChecker) {
        this.sessionRegistry = sessionRegistry;
        this.patientAccessChecker = patientAccessChecker;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        String sessionId = accessor.getSessionId();

        if (accessor.getCommand() == StompCommand.CONNECT) {
            WebSocketPrincipal principal = WebSocketPrincipal.from(message.getHeaders());
            if (principal != null) {
                sessionRegistry.register(sessionId, principal.userId());
            }
            return message;
        }

        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            // SEND / UNSUBSCRIBE / DISCONNECT frames carry no sensitive payload
            // and are already gated by the authenticated handshake.
            return message;
        }

        WebSocketPrincipal principal = WebSocketPrincipal.from(message.getHeaders());
        if (principal == null) {
            // Handshake-authenticated sessions always carry a principal; a
            // missing one means the handshake was bypassed → deny.
            return null;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return null;
        }

        if (isUserNotifications(destination)) {
            // Rewrite to the session unicast queue; the client keeps using
            // /user/queue/notifications and never learns the internal name.
            accessor.setDestination(RealtimeTopics.userQueue(sessionId));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        Matcher patientMatcher = PATIENT_TOPIC.matcher(destination);
        if (patientMatcher.matches()) {
            return patientAccessChecker.canAccess(principal, UUID.fromString(patientMatcher.group(1)))
                    ? message
                    : null;
        }

        Matcher nurseMatcher = NURSE_QUEUE_TOPIC.matcher(destination);
        if (nurseMatcher.matches()) {
            UUID nurseId = UUID.fromString(nurseMatcher.group(1));
            boolean isSelf = principal.userId().equals(nurseId);
            boolean isAdmin = Role.ADMIN.name().equals(principal.role());
            return isSelf || isAdmin ? message : null;
        }

        // Default-deny any destination we did not explicitly whitelist.
        return null;
    }

    private static boolean isUserNotifications(String destination) {
        return RealtimeTopics.USER_NOTIFICATIONS.equals(destination);
    }
}