/*
handles api requests for this domain and delegates work to services
*/
package com.tygilbert.virtualstudyroom.controller;

import com.tygilbert.virtualstudyroom.dto.auth.AuthLoginRequest;
import com.tygilbert.virtualstudyroom.dto.auth.AuthRegisterRequest;
import com.tygilbert.virtualstudyroom.dto.auth.AuthResponse;
import com.tygilbert.virtualstudyroom.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // creates a new user account and returns an auth response with jwt data
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    // authenticates user credentials and returns an auth response with jwt data
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }
}

