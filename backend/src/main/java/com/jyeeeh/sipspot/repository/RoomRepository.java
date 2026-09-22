package com.jyeeeh.sipspot.repository;

import com.jyeeeh.sipspot.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // host가 LAZY이므로 JOIN FETCH — 트랜잭션 밖에서도 host.nickname 등 안전하게 접근 가능
    @Query("SELECT r FROM Room r JOIN FETCH r.host WHERE r.code = :code")
    Optional<Room> findByCodeWithHost(@Param("code") String code);

    @Query("SELECT r FROM Room r JOIN FETCH r.host WHERE r.host.id = :accountId")
    Optional<Room> findByHostAccountId(@Param("accountId") Long accountId);

    boolean existsByCode(String code);
}
