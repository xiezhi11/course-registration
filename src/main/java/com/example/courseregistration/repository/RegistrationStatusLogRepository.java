package com.example.courseregistration.repository;

import com.example.courseregistration.entity.RegistrationStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationStatusLogRepository extends JpaRepository<RegistrationStatusLog, Long> {

    List<RegistrationStatusLog> findByRegistrationIdOrderByCreateTimeAsc(Long registrationId);

    List<RegistrationStatusLog> findByRegistrationIdOrderByCreateTimeDesc(Long registrationId);
}
