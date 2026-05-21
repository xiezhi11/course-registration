package com.example.courseregistration.repository;

import com.example.courseregistration.entity.Registration;
import com.example.courseregistration.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);

    List<Registration> findByCourseIdAndUserId(Long courseId, Long userId);

    List<Registration> findByCourseIdAndUserIdOrderByCreateTimeAsc(Long courseId, Long userId);

    List<Registration> findByCourseIdAndUserIdOrderByCreateTimeDesc(Long courseId, Long userId);

    List<Registration> findByOriginalRegistrationIdOrderByCreateTimeAsc(Long originalRegistrationId);

    List<Registration> findByUserId(Long userId);

    Page<Registration> findByUserId(Long userId, Pageable pageable);

    List<Registration> findByCourseId(Long courseId);

    Page<Registration> findByCourseId(Long courseId, Pageable pageable);

    List<Registration> findByCourseIdAndStatus(Long courseId, RegistrationStatus status);

    List<Registration> findByUserIdAndStatus(Long userId, RegistrationStatus status);

    Page<Registration> findByStatus(RegistrationStatus status, Pageable pageable);

    long countByStatus(RegistrationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Registration r WHERE r.id = :id")
    Optional<Registration> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT r FROM Registration r WHERE " +
           "(:courseId IS NULL OR r.courseId = :courseId) AND " +
           "(:userId IS NULL OR r.userId = :userId) AND " +
           "(:status IS NULL OR r.status = :status)")
    Page<Registration> findByConditions(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId,
            @Param("status") RegistrationStatus status,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.courseId = :courseId AND r.status IN :statuses")
    long countByCourseIdAndStatusIn(@Param("courseId") Long courseId, @Param("statuses") List<RegistrationStatus> statuses);
}
