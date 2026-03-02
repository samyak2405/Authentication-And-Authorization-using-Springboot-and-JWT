package com.javaproject.application.repository;

import com.javaproject.application.model.SecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, UUID> {

    Optional<SecurityPolicy> findByName(String name);
}
