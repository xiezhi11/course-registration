package com.example.courseregistration.entity;

import com.example.courseregistration.enums.RegistrationAction;
import com.example.courseregistration.enums.RegistrationStatus;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registration_status_log")
public class RegistrationStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "from_status")
    @Enumerated(EnumType.STRING)
    private RegistrationStatus fromStatus;

    @Column(name = "to_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RegistrationStatus toStatus;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private RegistrationAction action;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_role", length = 50)
    private String operatorRole;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
