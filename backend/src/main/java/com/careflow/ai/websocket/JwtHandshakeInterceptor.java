package com.careflow.ai.websocket;

import com.careflow.ai.security.CustomUserDetails;
import com.careflow.ai.security.CustomUserDetailsService;
import com.careflow.ai.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriUtils;

import java.util.Map;

/**
 * Authenticates the WebSocket handshake with a JWT access token and rejects
 * unauthenticated upgrade requests with HTTP 401 before any STOMP frame is
 * processed.
 *
 * Browsers cannot set custom headers on a WebSocket upgrade, so the token is
 * accepted either as:
 * <ol>
 *   <li>{@code Authorization: Bearer <jwt>} header — non-browser STOMP clients, or</li>
 *   <li>{@code ?token=<jwt>} query parameter — browser WebSocket URLs.</li>
 * </ol>
 *
 * On success the authenticated principal is stored in the session attributes
 * under {@link #USER_ID_ATTRIBUTE} / {@link #ROLE_ATTRIBUTE} /
 * {@link #FULL_NAME_ATTRIBUTE} so downstream channel interceptors and message
 * handlers can authorize subscriptions and presence events without re-parsing
 * the JWT.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    /** Key under which the resolved user UUID is stored in the session attributes. */
    public static final String USER_ID_ATTRIBUTE = "careflow.userId";
    /** Key under which the resolved role name is stored in the session attributes. */
    public static final String ROLE_ATTRIBUTE = "careflow.role";
    /** Key under which the resolved full name is stored in the session attributes. */
    public static final String FULL_NAME_ATTRIBUTE = "careflow.fullName";
    /** Key under which the user details are stored (for future handlers). */
    public static final String USER_DETAILS_ATTRIBUTE = "careflow.userDetails";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtHandshakeInterceptor(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = extractToken(request);
            if (token == null || !tokenProvider.validateToken(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            String username = tokenProvider.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            attributes.put("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(authentication));
            attributes.put(USER_ID_ATTRIBUTE, customUserDetails.getId().toString());
            attributes.put(ROLE_ATTRIBUTE, customUserDetails.getUser().getRole().name());
            attributes.put(FULL_NAME_ATTRIBUTE, customUserDetails.getUser().getFullName());
            attributes.put(USER_DETAILS_ATTRIBUTE, customUserDetails);
            return true;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No post-handshake work required.
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        String query = request.getURI().getQuery();
        if (StringUtils.hasText(query)) {
            for (String pair : query.split("&")) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    return UriUtils.decode(keyValue[1], "UTF-8");
                }
            }
        }
        return null;
    }
}