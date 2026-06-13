package com.cinemamemory.api.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    @Modifying
    @Query("delete from OAuthAccount account where account.user.id = :userId")
    int deleteByUserId(Long userId);
}
