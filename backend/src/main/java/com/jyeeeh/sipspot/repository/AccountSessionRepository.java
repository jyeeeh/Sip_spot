package com.jyeeeh.sipspot.repository;

import com.jyeeeh.sipspot.domain.AccountSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountSessionRepository extends JpaRepository<AccountSession, Long> {

    // account가 LAZY이므로 JOIN FETCH로 즉시 로딩 — 트랜잭션 밖에서 안전하게 접근 가능
    @Query("SELECT s FROM AccountSession s JOIN FETCH s.account WHERE s.tokenHash = :tokenHash")
    Optional<AccountSession> findByTokenHashWithAccount(@Param("tokenHash") String tokenHash);

    void deleteByTokenHash(String tokenHash);
}
