package com.tygilbert.virtualstudyroom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tygilbert.virtualstudyroom.entity.RoomMember;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByUserId(Long userId);
    List<RoomMember> findByRoomId(Long roomId);
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
    long countByRoomId(Long roomId);
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
    void deleteByRoomId(Long roomId);
}