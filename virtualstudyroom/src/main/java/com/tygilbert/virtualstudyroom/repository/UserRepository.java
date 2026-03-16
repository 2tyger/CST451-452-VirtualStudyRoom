/*
provides data access methods for this entity and query patterns
*/
package com.tygilbert.virtualstudyroom.repository;

import com.tygilbert.virtualstudyroom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

