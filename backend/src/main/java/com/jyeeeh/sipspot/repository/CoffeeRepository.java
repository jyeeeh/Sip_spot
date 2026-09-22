package com.jyeeeh.sipspot.repository;

import com.jyeeeh.sipspot.domain.Coffee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoffeeRepository extends JpaRepository<Coffee, Long> {

    @Query("SELECT c FROM Coffee c WHERE c.room.code = :code ORDER BY c.createdAt DESC")
    List<Coffee> findByRoomCodeOrderByCreatedAtDesc(@Param("code") String code);

    @Query("SELECT c FROM Coffee c JOIN FETCH c.room r JOIN FETCH r.host WHERE c.id = :id AND r.code = :code")
    Optional<Coffee> findByIdAndRoomCode(@Param("id") Long id, @Param("code") String code);
}
