package com.kfarms.tenant.repository;

import com.kfarms.tenant.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenAndAcceptedFalse(String token);

    Optional<Invitation> findByIdAndTenant_IdAndAcceptedFalse(Long id, Long tenantId);

    List<Invitation> findByTenant_IdAndAcceptedFalseOrderByCreatedAtDesc(Long tenantId);

    boolean existsByTenant_IdAndEmailIgnoreCaseAndAcceptedFalse(Long tenantId, String email);

    int countByTenant_IdAndAcceptedFalse(Long tenantId);
}
