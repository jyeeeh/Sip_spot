package com.jyeeeh.sipspot.repository;

import com.jyeeeh.sipspot.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT a FROM Account a WHERE lower(a.username) = lower(:username)")
    Optional<Account> findByUsernameLower(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM Account a WHERE lower(a.username) = lower(:username)")
    boolean existsByUsernameLower(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM Account a WHERE lower(a.nickname) = lower(:nickname)")
    boolean existsByNicknameLower(@Param("nickname") String nickname);
}
