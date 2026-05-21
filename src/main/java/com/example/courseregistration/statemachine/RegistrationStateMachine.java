package com.example.courseregistration.statemachine;

import com.example.courseregistration.enums.RegistrationAction;
import com.example.courseregistration.enums.RegistrationStatus;

public interface RegistrationStateMachine {

    boolean canTransition(RegistrationStatus currentStatus, RegistrationAction action);

    RegistrationStatus transition(RegistrationStatus currentStatus, RegistrationAction action);

    String getTransitionDescription(RegistrationStatus currentStatus, RegistrationAction action);
}
