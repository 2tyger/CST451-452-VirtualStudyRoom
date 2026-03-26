/*
verifies service behavior and edge cases for this module
*/
package com.tygilbert.virtualstudyroom.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.task.CreateTaskRequest;
import com.tygilbert.virtualstudyroom.dto.task.TaskResponse;
import com.tygilbert.virtualstudyroom.dto.task.UpdateTaskRequest;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.Task;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;
import com.tygilbert.virtualstudyroom.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTask_createsTaskWithinRoomForMember() {
        User user = new User();
        user.setId(4L);

        Room room = new Room();
        room.setId(10L);

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(25L);
            return task;
        });

        TaskResponse response = taskService.createTask(
                10L,
                new CreateTaskRequest("  Write tests  ", "add backend coverage"),
                "member@example.com"
        );

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task saved = taskCaptor.getValue();
        assertEquals(room, saved.getRoom());
        assertEquals("Write tests", saved.getTitle());
        assertEquals("add backend coverage", saved.getDescription());

        assertEquals(25L, response.id());
        assertEquals(10L, response.roomId());
        assertEquals("Write tests", response.title());
    }

    @Test
    void createTask_throwsNotFoundWhenRoomDoesntExist() {
        User user = new User();
        user.setId(4L);

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(roomRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> taskService.createTask(10L, new CreateTaskRequest("task", ""), "member@example.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateTask_updatesOnlyProvidedFields() {
        User user = new User();
        user.setId(4L);

        Room room = new Room();
        room.setId(10L);

        Task task = new Task();
        task.setId(7L);
        task.setRoom(room);
        task.setTitle("Old");
        task.setDescription("Old description");
        task.setDone(false);

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask(
                10L,
                7L,
                new UpdateTaskRequest("  Updated  ", null, true),
                "member@example.com"
        );

        assertEquals("Updated", response.title());
        assertEquals("Old description", response.description());
        assertEquals(true, response.done());
    }

    @Test
    void deleteTask_returnsDeletedTaskResponse() {
        User user = new User();
        user.setId(4L);

        Room room = new Room();
        room.setId(10L);

        Task task = new Task();
        task.setId(9L);
        task.setRoom(room);
        task.setTitle("Delete me");
        task.setDone(false);

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(taskRepository.findById(9L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.deleteTask(10L, 9L, "member@example.com");

        verify(taskRepository).delete(task);
        assertEquals(9L, response.id());
        assertEquals(10L, response.roomId());
        assertEquals("Delete me", response.title());
    }

    @Test
    void listTasks_returnsTasksForRoomInCreatedOrder() {
        User user = new User();
        user.setId(4L);

        Room room = new Room();
        room.setId(10L);

        Task taskOne = new Task();
        taskOne.setId(1L);
        taskOne.setRoom(room);
        taskOne.setTitle("First");

        Task taskTwo = new Task();
        taskTwo.setId(2L);
        taskTwo.setRoom(room);
        taskTwo.setTitle("Second");

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(taskRepository.findByRoomIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(taskOne, taskTwo));

        List<TaskResponse> responses = taskService.listTasks(10L, "member@example.com");

        assertEquals(2, responses.size());
        assertEquals("First", responses.get(0).title());
        assertEquals("Second", responses.get(1).title());
    }
}
