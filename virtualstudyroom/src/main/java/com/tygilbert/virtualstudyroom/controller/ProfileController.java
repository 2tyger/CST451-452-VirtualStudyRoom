package com.tygilbert.virtualstudyroom.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tygilbert.virtualstudyroom.dto.profile.ProfileResponse;
import com.tygilbert.virtualstudyroom.dto.profile.UpdateProfileRequest;
import com.tygilbert.virtualstudyroom.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse getMyProfile(Authentication authentication) {
        return profileService.getMyProfile(authentication.getName());
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request,
                                           Authentication authentication) {
        return profileService.updateMyProfile(authentication.getName(), request);
    }
}