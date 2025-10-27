package com.kfarms.repository;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    // Fetch all users by role
    List<AppUser> findByRole(Role role);
}
