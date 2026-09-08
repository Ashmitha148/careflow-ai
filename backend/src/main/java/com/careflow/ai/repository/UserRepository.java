package com.careflow.ai.repository;

import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Eagerly fetches the owning hospital: the loaded User becomes the
     * SECURITY PRINCIPAL (CustomUserDetails), which outlives the
     * authentication-time session. Tenancy checks read
     * {@code principal.getHospital()} on that detached entity — without the
     * join the lazy proxy cannot be initialized later
     * ("could not initialize proxy ... no Session").
     */
    @EntityGraph(attributePaths = "hospital")
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findFirstByRole(Role role);

    List<User> findByRole(Role role);
}
