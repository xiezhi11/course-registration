package com.example.courseregistration.service;

import com.example.courseregistration.dto.CourseQuery;
import com.example.courseregistration.entity.Course;
import com.example.courseregistration.enums.CourseStatus;
import com.example.courseregistration.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public Course findById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    public Course findByIdWithLock(Long id) {
        return courseRepository.findByIdWithLock(id).orElse(null);
    }

    public List<Course> findAll() {
        return courseRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
    }

    public Page<Course> findByConditions(CourseQuery query) {
        Pageable pageable = PageRequest.of(
                query.getPage() - 1,
                query.getSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        CourseStatus status = null;
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            try {
                status = CourseStatus.valueOf(query.getStatus().toUpperCase());
            } catch (Exception e) {
                // ignore
            }
        }

        return courseRepository.findByConditions(
                query.getName(),
                query.getType(),
                query.getLecturer(),
                query.getStartTime(),
                query.getEndTime(),
                status,
                pageable
        );
    }

    public List<Course> findPublishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED);
    }

    public List<Course> findAvailableCourses() {
        return courseRepository.findAvailableCourses(LocalDateTime.now());
    }

    @Transactional
    public Course save(Course course) {
        validateCourse(course);

        if (course.getId() == null) {
            if (course.getStatus() == null) {
                course.setStatus(CourseStatus.DRAFT);
            }
            if (course.getRegisteredCount() == null) {
                course.setRegisteredCount(0);
            }
        } else {
            Course existing = findByIdWithLock(course.getId());
            if (existing == null) {
                throw new RuntimeException("课程不存在");
            }
            course.setRegisteredCount(existing.getRegisteredCount());
            course.setCreateTime(existing.getCreateTime());

            if (course.getMaxCapacity() < existing.getRegisteredCount()) {
                throw new RuntimeException("课程容量不能小于当前已报名人数: " + existing.getRegisteredCount());
            }

            if (existing.getStatus() == CourseStatus.PUBLISHED && 
                course.getEndTime().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("已发布课程的结束时间不能早于当前时间");
            }
        }
        return courseRepository.save(course);
    }

    private void validateCourse(Course course) {
        if (course.getName() == null || course.getName().trim().isEmpty()) {
            throw new RuntimeException("课程名称不能为空");
        }
        if (course.getType() == null || course.getType().trim().isEmpty()) {
            throw new RuntimeException("课程类型不能为空");
        }
        if (course.getLecturer() == null || course.getLecturer().trim().isEmpty()) {
            throw new RuntimeException("讲师不能为空");
        }
        if (course.getStartTime() == null) {
            throw new RuntimeException("开始时间不能为空");
        }
        if (course.getEndTime() == null) {
            throw new RuntimeException("结束时间不能为空");
        }
        if (course.getMaxCapacity() == null) {
            throw new RuntimeException("人数上限不能为空");
        }
        if (course.getMaxCapacity() <= 0) {
            throw new RuntimeException("人数上限必须大于0");
        }
        if (course.getEndTime().isBefore(course.getStartTime())) {
            throw new RuntimeException("结束时间不能早于开始时间");
        }
        if (course.getCancelDeadline() != null && 
            course.getCancelDeadline().isAfter(course.getStartTime())) {
            throw new RuntimeException("取消截止时间不能晚于开始时间");
        }
        if (course.getCancelDeadline() != null && 
            course.getCancelDeadline().isAfter(course.getEndTime())) {
            throw new RuntimeException("取消截止时间不能晚于结束时间");
        }
    }

    @Transactional
    public Course publishCourse(Long id) {
        Course course = findByIdWithLock(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new RuntimeException("课程已发布");
        }
        if (course.getStatus() == CourseStatus.CLOSED) {
            throw new RuntimeException("已关闭的课程无法重新发布");
        }
        if (course.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("课程已结束，无法发布");
        }
        if (course.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("课程已开始，无法发布");
        }
        if (course.getMaxCapacity() <= 0) {
            throw new RuntimeException("课程容量必须大于0才能发布");
        }
        validateCourse(course);
        course.setStatus(CourseStatus.PUBLISHED);
        return courseRepository.save(course);
    }

    @Transactional
    public Course closeCourse(Long id) {
        Course course = findByIdWithLock(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        course.setStatus(CourseStatus.CLOSED);
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteById(Long id) {
        Course course = findById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (course.getRegisteredCount() > 0) {
            throw new RuntimeException("课程已有学员报名，无法删除");
        }
        courseRepository.deleteById(id);
    }

    @Transactional
    public Course incrementRegisteredCount(Long courseId) {
        Course course = findByIdWithLock(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (course.isFull()) {
            throw new RuntimeException("课程名额已满");
        }
        course.setRegisteredCount(course.getRegisteredCount() + 1);
        return courseRepository.save(course);
    }

    @Transactional
    public Course decrementRegisteredCount(Long courseId) {
        Course course = findByIdWithLock(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (course.getRegisteredCount() > 0) {
            course.setRegisteredCount(course.getRegisteredCount() - 1);
        }
        return courseRepository.save(course);
    }

    public boolean canRegister(Course course) {
        if (course == null) {
            return false;
        }
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            return false;
        }
        if (course.getEndTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (course.isFull()) {
            return false;
        }
        return true;
    }

    public boolean canCancel(Course course) {
        if (course == null) {
            return false;
        }
        if (course.getCancelDeadline() != null) {
            return course.getCancelDeadline().isAfter(LocalDateTime.now());
        }
        return course.getStartTime().isAfter(LocalDateTime.now());
    }
}
