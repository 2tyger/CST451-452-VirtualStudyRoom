package com.tygilbert.virtualstudyroom.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "room_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "user_id"})
})
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    public void onCreate() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(OffsetDateTime joinedAt) { this.joinedAt = joinedAt; }
}