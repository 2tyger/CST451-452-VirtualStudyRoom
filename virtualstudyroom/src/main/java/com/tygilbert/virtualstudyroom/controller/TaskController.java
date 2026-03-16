/*
handles api requests for this domain and delegates work to services
*/
package com.tygilbert.virtualstudyroom.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tygilbert.virtualstudyroom.dto.task.CreateTaskRequest;
import com.tygilbert.virtualstudyroom.dto.task.TaskResponse;
import com.tygilbert.virtualstudyroom.dto.task.UpdateTaskRequest;
import com.tygilbert.virtualstudyroom.service.RealtimeEventService;
import com.tygilbert.virtualstudyroom.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms/{roomId}/tasks")
public class TaskController {

    private final TaskService taskService;
    private final RealtimeEventService realtimeEventService;

    public TaskController(TaskService taskService, RealtimeEventService realtimeEventService) {
        this.taskService = taskService;
        this.realtimeEventService = realtimeEventService;
    }

    // returns all tasks for a room when the user is a member
    @GetMapping
    public List<TaskResponse> listTasks(@PathVariable Long roomId, Authentication authentication) {
        return taskService.listTasks(roomId, authentication.getName());
    }

    // creates a task and publishes a realtime task added event
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse addTask(@PathVariable Long roomId,
                                @Valid @RequestBody CreateTaskRequest request,
                                Authentication authentication) {
        TaskResponse task = taskService.createTask(roomId, request, authentication.getName());
        realtimeEventService.publishTaskUpdate(roomId, "added", task);
        return task;
    }

    // updates task fields and publishes a realtime task updated event
    @PatchMapping("/{taskId}")
    public TaskResponse updateTask(@PathVariable Long roomId,
                                   @PathVariable Long taskId,
                                   @Valid @RequestBody UpdateTaskRequest request,
                                   Authentication authentication) {
        TaskResponse task = taskService.updateTask(roomId, taskId, request, authentication.getName());
        realtimeEventService.publishTaskUpdate(roomId, "updated", task);
        return task;
    }

    // deletes a task and publishes a realtime task deleted event
    @DeleteMapping("/{taskId}")
    public TaskResponse deleteTask(@PathVariable Long roomId,
                                   @PathVariable Long taskId,
                                   Authentication authentication) {
        TaskResponse task = taskService.deleteTask(roomId, taskId, authentication.getName());
        realtimeEventService.publishTaskUpdate(roomId, "deleted", task);
        return task;
    }
}

