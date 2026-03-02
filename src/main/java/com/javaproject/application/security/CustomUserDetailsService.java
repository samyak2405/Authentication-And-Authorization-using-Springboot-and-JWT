package com.javaproject.application.security;

import com.javaproject.application.model.User;
import com.javaproject.application.model.UserRole;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads the user and their roles from the database for Spring Security.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.getByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        List<String> roles = userRoleRepository.findByUser(user).stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .collect(Collectors.toList());

        return new CustomUserDetails(user, roles);
    }
}
