package com.careflow.ai.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Registers the STOMP-over-WebSocket endpoint.
 *
 * <ul>
 *   <li><b>/ws</b> — raw STOMP over WebSocket (browser and native clients).</li>
 *   <li><b>/ws/&#42;&#42;</b> — SockJS fallback via {@code withSockJS()} for
 *       networks/proxies that do not support the WebSocket upgrade.</li>
 * </ul>
 *
 * Every handshake goes through {@link JwtHandshakeInterceptor} (401 on missing
 * or invalid JWT), and every SUBSCRIBE is authorized by
 * {@link WebSocketTopicAuthorizer} on the client-inbound channel.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String allowedOrigins;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final WebSocketTopicAuthorizer topicAuthorizer;

    public WebSocketConfig(@Value("${careflow.cors.allowed-origins}") String allowedOrigins,
                           JwtHandshakeInterceptor jwtHandshakeInterceptor,
                           WebSocketTopicAuthorizer topicAuthorizer) {
        this.allowedOrigins = allowedOrigins;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.topicAuthorizer = topicAuthorizer;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Defense in depth: authorize every SUBSCRIBE before the broker accepts it.
        registration.interceptors(topicAuthorizer);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }
}