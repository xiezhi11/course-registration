package com.example.courseregistration.repository;

import com.example.courseregistration.entity.Course;
import com.example.courseregistration.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(CourseStatus status);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT c FROM Course c WHERE " +
           "(:name IS NULL OR c.name LIKE %:name%) AND " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:lecturer IS NULL OR c.lecturer LIKE %:lecturer%) AND " +
           "(:startTime IS NULL OR c.startTime >= :startTime) AND " +
           "(:endTime IS NULL OR c.endTime <= :endTime) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<Course> findByConditions(
            @Param("name") String name,
            @Param("type") String type,
            @Param("lecturer") String lecturer,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") CourseStatus status,
            Pageable pageable
    );

    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' AND c.endTime > :now")
    List<Course> findAvailableCourses(@Param("now") LocalDateTime now);
}
