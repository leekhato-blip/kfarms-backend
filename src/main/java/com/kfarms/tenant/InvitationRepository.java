package com.kfarms.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenAndAcceptedFalse(String token);

    boolean existsByTenant_IdAndEmailIgnoreCaseAndAcceptedFalse(Long tenantId, String email);
}
