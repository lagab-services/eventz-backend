package com.lagab.eventz.app.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.user.entity.Token;
import com.lagab.eventz.app.user.entity.User;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByToken(String token);

    @Query("SELECT t FROM Token t JOIN FETCH t.user WHERE t.token = :token AND t.isRevoked = false AND t.isUsed = false AND t.expiresAt > :now")
    Optional<Token> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.type = :type AND t.isRevoked = false")
    List<Token> findValidTokensByUserAndType(@Param("user") User user, @Param("type") Token.TokenType type);

    @Modifying
    @Query("UPDATE Token t SET t.isRevoked = true WHERE t.user = :user AND t.type = :type")
    void revokeAllUserTokensByType(@Param("user") User user, @Param("type") Token.TokenType type);

    @Modifying
    @Query("UPDATE Token t SET t.isRevoked = true WHERE t.user = :user")
    void revokeAllUserTokens(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :now OR (t.isRevoked = true AND t.createdAt < :cutoffDate)")
    void deleteExpiredAndOldRevokedTokens(@Param("now") LocalDateTime now, @Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(t) FROM Token t WHERE t.user = :user AND t.type = 'ACCESS_TOKEN' AND t.isRevoked = false")
    long countActiveAccessTokensByUser(@Param("user") User user);
}
