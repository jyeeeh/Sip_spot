package com.jyeeeh.sipspot.repository;

import com.jyeeeh.sipspot.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    @Query("SELECT m FROM Member m WHERE m.tokenHash = :tokenHash")
    Optional<Member> findByTokenHash(@Param("tokenHash") String tokenHash);

    // STOMP 인터셉터 전용: 트랜잭션 밖에서 room.code에 안전하게 접근하기 위해 room을 즉시 로딩
    @Query("SELECT m FROM Member m JOIN FETCH m.room WHERE m.tokenHash = :tokenHash")
    Optional<Member> findByTokenHashWithRoom(@Param("tokenHash") String tokenHash);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.room.id = :roomId")
    int countByRoomId(@Param("roomId") Long roomId);
}
