package com.javaproject.application.repository;

import com.javaproject.application.model.User;
import com.javaproject.application.model.UserRole;
import com.javaproject.application.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser(User user);
}
