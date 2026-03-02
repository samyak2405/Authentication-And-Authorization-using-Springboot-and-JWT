package com.javaproject.application.model;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "auth_parameters")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String paramId;
    private String paramValue;
    @Column(name = "is_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean isEnabled;
}
