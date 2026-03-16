/*
provides data access methods for this entity and query patterns
*/
package com.tygilbert.virtualstudyroom.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tygilbert.virtualstudyroom.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findTop50ByRoomIdOrderByCreatedAtDesc(Long roomId);
    Page<Message> findByRoomId(Long roomId, Pageable pageable);
    void deleteByRoomId(Long roomId);
}

