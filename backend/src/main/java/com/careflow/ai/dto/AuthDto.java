package com.careflow.ai.dto;

import com.careflow.ai.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AuthDto {

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public LoginRequest() {}

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotNull(message = "Role is required")
        private Role role;

        public RegisterRequest() {}

        public RegisterRequest(String email, String password, String fullName, Role role) {
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.role = role;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }

        public static RegisterRequestBuilder builder() {
            return new RegisterRequestBuilder();
        }

        public static class RegisterRequestBuilder {
            private String email;
            private String password;
            private String fullName;
            private Role role;

            public RegisterRequestBuilder email(String email) { this.email = email; return this; }
            public RegisterRequestBuilder password(String password) { this.password = password; return this; }
            public RegisterRequestBuilder fullName(String fullName) { this.fullName = fullName; return this; }
            public RegisterRequestBuilder role(Role role) { this.role = role; return this; }

            public RegisterRequest build() {
                return new RegisterRequest(email, password, fullName, role);
            }
        }
    }

    public static class AuthResponse {
        private String token;
        private UserResponse user;

        public AuthResponse() {}

        public AuthResponse(String token, UserResponse user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public UserResponse getUser() { return user; }
        public void setUser(UserResponse user) { this.user = user; }

        public static AuthResponseBuilder builder() {
            return new AuthResponseBuilder();
        }

        public static class AuthResponseBuilder {
            private String token;
            private UserResponse user;

            public AuthResponseBuilder token(String token) { this.token = token; return this; }
            public AuthResponseBuilder user(UserResponse user) { this.user = user; return this; }

            public AuthResponse build() {
                return new AuthResponse(token, user);
            }
        }
    }

    public static class UserResponse {
        private UUID id;
        private String email;
        private String fullName;
        private Role role;

        public UserResponse() {}

        public UserResponse(UUID id, String email, String fullName, Role role) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }

        public static UserResponseBuilder builder() {
            return new UserResponseBuilder();
        }

        public static class UserResponseBuilder {
            private UUID id;
            private String email;
            private String fullName;
            private Role role;

            public UserResponseBuilder id(UUID id) { this.id = id; return this; }
            public UserResponseBuilder email(String email) { this.email = email; return this; }
            public UserResponseBuilder fullName(String fullName) { this.fullName = fullName; return this; }
            public UserResponseBuilder role(Role role) { this.role = role; return this; }

            public UserResponse build() {
                return new UserResponse(id, email, fullName, role);
            }
        }
    }
}
