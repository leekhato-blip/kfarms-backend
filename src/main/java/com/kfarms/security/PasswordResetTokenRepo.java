package com.kfarms.security;

import com.kfarms.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {

    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId")
    Optional<PasswordResetToken> findByUserId(@Param("userId") Long userId);

    Optional<PasswordResetToken> findByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUser(@Param("userId") Long userId);
}
