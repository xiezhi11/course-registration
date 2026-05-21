package com.example.courseregistration.statemachine;

import com.example.courseregistration.enums.RegistrationAction;
import com.example.courseregistration.enums.RegistrationStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class RegistrationStateMachineImpl implements RegistrationStateMachine {

    private final Map<RegistrationStatus, Map<RegistrationAction, RegistrationStatus>> transitionRules = new HashMap<>();

    public RegistrationStateMachineImpl() {
        initTransitionRules();
    }

    private void initTransitionRules() {
        Map<RegistrationAction, RegistrationStatus> pendingTransitions = new HashMap<>();
        pendingTransitions.put(RegistrationAction.APPROVE, RegistrationStatus.APPROVED);
        pendingTransitions.put(RegistrationAction.REJECT, RegistrationStatus.REJECTED);
        pendingTransitions.put(RegistrationAction.CANCEL_BY_USER, RegistrationStatus.CANCELLED);
        pendingTransitions.put(RegistrationAction.CANCEL_BY_ADMIN, RegistrationStatus.CANCELLED);
        transitionRules.put(RegistrationStatus.PENDING, pendingTransitions);

        Map<RegistrationAction, RegistrationStatus> approvedTransitions = new HashMap<>();
        approvedTransitions.put(RegistrationAction.CANCEL_BY_USER, RegistrationStatus.CANCELLED);
        approvedTransitions.put(RegistrationAction.CANCEL_BY_ADMIN, RegistrationStatus.CANCELLED);
        transitionRules.put(RegistrationStatus.APPROVED, approvedTransitions);

        Map<RegistrationAction, RegistrationStatus> rejectedTransitions = new HashMap<>();
        rejectedTransitions.put(RegistrationAction.RE_SUBMIT, RegistrationStatus.PENDING);
        transitionRules.put(RegistrationStatus.REJECTED, rejectedTransitions);

        Map<RegistrationAction, RegistrationStatus> cancelledTransitions = new HashMap<>();
        cancelledTransitions.put(RegistrationAction.RE_SUBMIT, RegistrationStatus.PENDING);
        transitionRules.put(RegistrationStatus.CANCELLED, cancelledTransitions);
    }

    @Override
    public boolean canTransition(RegistrationStatus currentStatus, RegistrationAction action) {
        if (currentStatus == null) {
            return action == RegistrationAction.SUBMIT || action == RegistrationAction.RE_SUBMIT;
        }
        Map<RegistrationAction, RegistrationStatus> transitions = transitionRules.get(currentStatus);
        return transitions != null && transitions.containsKey(action);
    }

    @Override
    public RegistrationStatus transition(RegistrationStatus currentStatus, RegistrationAction action) {
        if (!canTransition(currentStatus, action)) {
            throw new IllegalStateException(
                String.format("无法从状态 %s 执行动作 %s", currentStatus, action)
            );
        }

        if (currentStatus == null) {
            return RegistrationStatus.PENDING;
        }

        return transitionRules.get(currentStatus).get(action);
    }

    @Override
    public String getTransitionDescription(RegistrationStatus currentStatus, RegistrationAction action) {
        if (!canTransition(currentStatus, action)) {
            return "非法状态流转";
        }
        RegistrationStatus targetStatus = transition(currentStatus, action);
        return String.format("%s → %s (%s)", 
            getStatusText(currentStatus), 
            getStatusText(targetStatus), 
            action.getDescription());
    }

    private String getStatusText(RegistrationStatus status) {
        if (status == null) {
            return "未报名";
        }
        switch (status) {
            case PENDING: return "待审核";
            case APPROVED: return "已通过";
            case REJECTED: return "已驳回";
            case CANCELLED: return "已取消";
            default: return status.name();
        }
    }

    public Set<RegistrationAction> getAllowedActions(RegistrationStatus status) {
        if (status == null) {
            return Set.of(RegistrationAction.SUBMIT);
        }
        Map<RegistrationAction, RegistrationStatus> transitions = transitionRules.get(status);
        return transitions != null ? transitions.keySet() : Set.of();
    }

    public void printStateDiagram() {
        System.out.println("=== 报名状态机图 ===");
        System.out.println("初始状态: 未报名");
        System.out.println();
        
        for (Map.Entry<RegistrationStatus, Map<RegistrationAction, RegistrationStatus>> entry : transitionRules.entrySet()) {
            RegistrationStatus from = entry.getKey();
            for (Map.Entry<RegistrationAction, RegistrationStatus> trans : entry.getValue().entrySet()) {
                RegistrationAction action = trans.getKey();
                RegistrationStatus to = trans.getValue();
                System.out.printf("%s --[%s]--> %s%n", 
                    getStatusText(from), 
                    action.getDescription(), 
                    getStatusText(to));
            }
        }
        System.out.println("====================");
    }
}
