package com.tygilbert.virtualstudyroom.security;

import com.tygilbert.virtualstudyroom.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .authorities("ROLE_USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}