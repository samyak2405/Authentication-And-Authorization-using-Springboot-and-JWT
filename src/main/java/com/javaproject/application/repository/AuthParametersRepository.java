package com.javaproject.application.repository;

import com.javaproject.application.model.AuthParameters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthParametersRepository extends JpaRepository<AuthParameters, Long> {
    Optional<AuthParameters> findByParamId(String paramId);
}
