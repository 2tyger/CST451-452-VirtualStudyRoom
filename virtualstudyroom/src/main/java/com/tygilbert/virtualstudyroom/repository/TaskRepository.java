package com.tygilbert.virtualstudyroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tygilbert.virtualstudyroom.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByRoomIdOrderByCreatedAtAsc(Long roomId);
    void deleteByRoomId(Long roomId);
}