package com.web.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_users")
@Getter
@Setter
public class PendingUserEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Long roleId;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "otp_expiry", nullable = false)
    private LocalDateTime otpExpiry;
}