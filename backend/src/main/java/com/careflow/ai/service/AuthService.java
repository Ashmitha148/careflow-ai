package com.careflow.ai.service;

import com.careflow.ai.dto.AuthDto.*;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.CustomUserDetailsService;
import com.careflow.ai.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       CustomUserDetailsService userDetailsService,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email address already in use");
        }

        // Public self-registration must never grant privileged roles. Only
        // CAREGIVER and READ_ONLY are permitted; privileged roles must be
        // provisioned by an admin/seeded path instead.
        Role requestedRole = request.getRole() != null ? request.getRole() : Role.READ_ONLY;
        if (requestedRole != Role.CAREGIVER && requestedRole != Role.READ_ONLY) {
            throw new RuntimeException("Role not permitted for self-registration: " + requestedRole);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(requestedRole)
                .build();

        user = userRepository.save(user);

        // Auto authenticate after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

        /**
     * Exchanges a valid, non-expired refresh token for a new access/refresh
     * pair. The refresh token itself is a JWT with a "type": "refresh" claim
     * that JwtTokenProvider rejects if presented as an access token, so a
     * leaked refresh token can only ever be used against this one endpoint.
     *
     * Rotation: the presented token is marked used and a new one is issued in
     * the same token family. If the presented token has already been used
     * (ReuseException), the entire family is revoked — a possible theft signal.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String suppliedRefreshToken = request.getRefreshToken();

        if (!tokenProvider.validateRefreshToken(suppliedRefreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        UUID userId = UUID.fromString(
                tokenProvider.getUsernameFromToken(suppliedRefreshToken));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for refresh token"));

        String newAccessToken = tokenProvider.generateTokenFromUser(user);

        String newRefreshToken;
        try {
            newRefreshToken = refreshTokenService.rotateRefreshToken(suppliedRefreshToken);
                } catch (RefreshTokenReuseException ex) {
            log.warn("Refresh token reuse detected for user {}: {}", userId, ex.getMessage());
            throw new RuntimeException("Refresh token reuse detected. All sessions revoked.", ex);
        } catch (RefreshTokenExpiredException ex) {
            throw new RuntimeException("Refresh token expired", ex);
        }

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}