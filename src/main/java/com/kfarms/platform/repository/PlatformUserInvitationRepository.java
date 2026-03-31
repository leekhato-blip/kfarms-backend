package com.kfarms.platform.repository;

import com.kfarms.platform.entity.PlatformUserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformUserInvitationRepository extends JpaRepository<PlatformUserInvitation, Long> {

    Optional<PlatformUserInvitation> findByTokenAndAcceptedFalse(String token);

    Optional<PlatformUserInvitation> findByEmailIgnoreCaseAndAcceptedFalse(String email);

    Optional<PlatformUserInvitation> findByUsernameIgnoreCaseAndAcceptedFalse(String username);
}
