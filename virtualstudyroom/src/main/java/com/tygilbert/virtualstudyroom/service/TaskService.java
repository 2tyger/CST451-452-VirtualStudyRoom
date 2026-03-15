package com.tygilbert.virtualstudyroom.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.task.CreateTaskRequest;
import com.tygilbert.virtualstudyroom.dto.task.TaskResponse;
import com.tygilbert.virtualstudyroom.dto.task.UpdateTaskRequest;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.Task;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;
import com.tygilbert.virtualstudyroom.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    public TaskService(TaskRepository taskRepository, RoomRepository roomRepository, RoomService roomService) {
        this.taskRepository = taskRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    public List<TaskResponse> listTasks(Long roomId, String email) {
        User user = roomService.getCurrentUser(email);
        roomService.ensureMembership(roomId, user.getId());
        return taskRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream().map(this::toResponse).toList();
    }

    public TaskResponse createTask(Long roomId, CreateTaskRequest request, String email) {
        User user = roomService.getCurrentUser(email);
        roomService.ensureMembership(roomId, user.getId());

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        Task task = new Task();
        task.setRoom(room);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public TaskResponse updateTask(Long roomId, Long taskId, UpdateTaskRequest request, String email) {
        User user = roomService.getCurrentUser(email);
        roomService.ensureMembership(roomId, user.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (!task.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task does not belong to room");
        }

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.done() != null) {
            task.setDone(request.done());
        }

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public TaskResponse deleteTask(Long roomId, Long taskId, String email) {
        User user = roomService.getCurrentUser(email);
        roomService.ensureMembership(roomId, user.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (!task.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task does not belong to room");
        }

        TaskResponse response = toResponse(task);
        taskRepository.delete(task);
        return response;
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getRoom().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssigneeId(),
                task.isDone(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}