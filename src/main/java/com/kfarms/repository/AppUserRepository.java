package com.kfarms.repository;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    // Fetch all users by role
    List<AppUser> findByRole(Role role);
    long countByRole(Role role);
    long countByRoleAndEnabledTrue(Role role);
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByPhoneNumber(String phoneNumber);

    @Query("""
        SELECT u FROM AppUser u
        WHERE lower(u.email) LIKE lower(concat('%', :search, '%'))
           OR lower(u.username) LIKE lower(concat('%', :search, '%'))
    """)
    Page<AppUser> searchPlatformUsers(String search, Pageable pageable);
}
