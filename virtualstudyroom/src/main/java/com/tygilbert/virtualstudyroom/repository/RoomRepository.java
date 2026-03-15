package com.tygilbert.virtualstudyroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tygilbert.virtualstudyroom.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
	List<Room> findByRunningTrue();
}