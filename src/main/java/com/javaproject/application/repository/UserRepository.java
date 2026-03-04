package com.javaproject.application.repository;

import com.javaproject.application.model.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
   Optional<User> getByEmail(@NotBlank String email);
   Optional<User> getByMobile(@NotBlank String mobile);
}
