package com.example.courseregistration.service;

import com.example.courseregistration.entity.Registration;
import com.example.courseregistration.entity.RegistrationStatusLog;
import com.example.courseregistration.enums.RegistrationAction;
import com.example.courseregistration.enums.RegistrationStatus;
import com.example.courseregistration.enums.UserRole;
import com.example.courseregistration.repository.RegistrationStatusLogRepository;
import com.example.courseregistration.statemachine.RegistrationStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegistrationStateService {

    @Autowired
    private RegistrationStateMachine stateMachine;

    @Autowired
    private RegistrationStatusLogRepository statusLogRepository;

    public boolean canTransition(Registration registration, RegistrationAction action) {
        if (registration == null) {
            return stateMachine.canTransition(null, action);
        }
        return stateMachine.canTransition(registration.getStatus(), action);
    }

    @Transactional
    public Registration transition(
            Registration registration,
            RegistrationAction action,
            Long operatorId,
            UserRole operatorRole,
            String remark
    ) {
        RegistrationStatus fromStatus = registration != null ? registration.getStatus() : null;
        
        if (!stateMachine.canTransition(fromStatus, action)) {
            throw new IllegalStateException(
                String.format("状态流转失败：当前状态为 %s，不允许执行动作 %s",
                    fromStatus != null ? fromStatus.name() : "空",
                    action.name())
            );
        }

        RegistrationStatus toStatus = stateMachine.transition(fromStatus, action);

        if (registration != null) {
            registration.setStatus(toStatus);
        }

        if (registration != null && registration.getId() != null) {
            RegistrationStatusLog log = new RegistrationStatusLog();
            log.setRegistrationId(registration.getId());
            log.setFromStatus(fromStatus);
            log.setToStatus(toStatus);
            log.setAction(action);
            log.setOperatorId(operatorId);
            log.setOperatorRole(operatorRole != null ? operatorRole.name() : null);
            log.setRemark(remark);
            statusLogRepository.save(log);
        }

        return registration;
    }

    @Transactional
    public RegistrationStatusLog logStatusChange(
            Long registrationId,
            RegistrationStatus fromStatus,
            RegistrationStatus toStatus,
            RegistrationAction action,
            Long operatorId,
            UserRole operatorRole,
            String remark
    ) {
        RegistrationStatusLog log = new RegistrationStatusLog();
        log.setRegistrationId(registrationId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole != null ? operatorRole.name() : null);
        log.setRemark(remark);
        return statusLogRepository.save(log);
    }

    public List<RegistrationStatusLog> getStatusHistory(Long registrationId) {
        return statusLogRepository.findByRegistrationIdOrderByCreateTimeAsc(registrationId);
    }

    public String getTransitionDescription(RegistrationStatus status, RegistrationAction action) {
        return stateMachine.getTransitionDescription(status, action);
    }

    public void validateTransition(RegistrationStatus currentStatus, RegistrationAction action) {
        if (!stateMachine.canTransition(currentStatus, action)) {
            throw new IllegalStateException(
                String.format("非法状态流转：当前状态 [%s] 不允许执行动作 [%s]",
                    currentStatus != null ? currentStatus.name() : "空",
                    action.name())
            );
        }
    }
}
