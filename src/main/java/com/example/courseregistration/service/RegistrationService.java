package com.example.courseregistration.service;

import com.example.courseregistration.entity.Course;
import com.example.courseregistration.entity.Registration;
import com.example.courseregistration.entity.RegistrationStatusLog;
import com.example.courseregistration.entity.User;
import com.example.courseregistration.enums.CourseStatus;
import com.example.courseregistration.enums.RegistrationAction;
import com.example.courseregistration.enums.RegistrationStatus;
import com.example.courseregistration.enums.UserRole;
import com.example.courseregistration.repository.RegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RegistrationService {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private RegistrationStateService stateService;

    public Registration findById(Long id) {
        return registrationRepository.findById(id).orElse(null);
    }

    public Registration findByIdWithLock(Long id) {
        return registrationRepository.findByIdWithLock(id).orElse(null);
    }

    public Registration findByCourseIdAndUserId(Long courseId, Long userId) {
        List<Registration> registrations = registrationRepository.findByCourseIdAndUserId(courseId, userId);
        if (registrations == null || registrations.isEmpty()) {
            return null;
        }
        return registrations.stream()
                .filter(r -> r.getStatus() == RegistrationStatus.PENDING ||
                            r.getStatus() == RegistrationStatus.APPROVED)
                .findFirst()
                .orElse(null);
    }

    public boolean existsByCourseIdAndUserId(Long courseId, Long userId) {
        List<Registration> registrations = registrationRepository.findByCourseIdAndUserId(courseId, userId);
        if (registrations == null || registrations.isEmpty()) {
            return false;
        }
        return registrations.stream()
                .anyMatch(r -> r.getStatus() == RegistrationStatus.PENDING ||
                               r.getStatus() == RegistrationStatus.APPROVED);
    }

    public List<Registration> findByUserId(Long userId) {
        return registrationRepository.findByUserId(userId);
    }

    public Page<Registration> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Registration> result = registrationRepository.findByUserId(userId, pageable);
        return enrichRegistrationWithDetails(result);
    }

    public List<Registration> findByCourseId(Long courseId) {
        List<Registration> registrations = registrationRepository.findByCourseId(courseId);
        return registrations.stream().map(this::enrichRegistration).collect(Collectors.toList());
    }

    public Page<Registration> findByCourseId(Long courseId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Registration> result = registrationRepository.findByCourseId(courseId, pageable);
        return enrichRegistrationWithDetails(result);
    }

    public Page<Registration> findByStatus(RegistrationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Registration> result = registrationRepository.findByStatus(status, pageable);
        return enrichRegistrationWithDetails(result);
    }

    public Page<Registration> findByConditions(Long courseId, Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        RegistrationStatus registrationStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                registrationStatus = RegistrationStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                // ignore
            }
        }
        Page<Registration> result = registrationRepository.findByConditions(courseId, userId, registrationStatus, pageable);
        return enrichRegistrationWithDetails(result);
    }

    public List<Registration> findAll() {
        List<Registration> registrations = registrationRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
        return registrations.stream().map(this::enrichRegistration).collect(Collectors.toList());
    }

    private Page<Registration> enrichRegistrationWithDetails(Page<Registration> page) {
        return page.map(this::enrichRegistration);
    }

    private Registration enrichRegistration(Registration registration) {
        if (registration.getCourseId() != null) {
            Course course = courseService.findById(registration.getCourseId());
            registration.setCourse(course);
        }
        if (registration.getUserId() != null) {
            User user = userService.findById(registration.getUserId());
            registration.setUser(user);
        }
        return registration;
    }

    @Transactional
    public Registration register(Long courseId, Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户信息无效，请重新登录");
        }

        Course course = courseService.findByIdWithLock(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        validateRegister(course, userId);

        courseService.incrementRegisteredCount(courseId);

        List<Registration> history = registrationRepository.findByCourseIdAndUserIdOrderByCreateTimeAsc(courseId, userId);

        Registration registration = new Registration();
        registration.setCourseId(courseId);
        registration.setUserId(userId);
        registration.setStatus(RegistrationStatus.PENDING);
        registration.setRegistrationCount(history.size() + 1);

        if (!history.isEmpty()) {
            Registration first = history.get(0);
            registration.setOriginalRegistrationId(
                first.getOriginalRegistrationId() != null ? first.getOriginalRegistrationId() : first.getId()
            );
        }

        Registration saved = registrationRepository.save(registration);

        String remark = history.isEmpty() ? "用户首次提交报名" : 
            String.format("用户第 %d 次提交报名", history.size() + 1);
        stateService.logStatusChange(
            saved.getId(),
            null,
            RegistrationStatus.PENDING,
            history.isEmpty() ? RegistrationAction.SUBMIT : RegistrationAction.RE_SUBMIT,
            userId,
            UserRole.EMPLOYEE,
            remark
        );

        return saved;
    }

    private void validateRegister(Course course, Long userId) {
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("课程未发布，无法报名");
        }

        if (course.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("课程已开始，无法报名");
        }

        if (course.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("课程已结束，无法报名");
        }

        if (course.getMaxCapacity() <= 0) {
            throw new RuntimeException("课程容量无效，无法报名");
        }

        if (course.getRegisteredCount() == null) {
            throw new RuntimeException("课程报名数据异常");
        }

        if (course.getRegisteredCount() < 0) {
            throw new RuntimeException("课程报名数据异常");
        }

        if (course.getRegisteredCount() >= course.getMaxCapacity()) {
            throw new RuntimeException("课程名额已满");
        }

        if (course.getEndTime().isBefore(course.getStartTime())) {
            throw new RuntimeException("课程时间设置异常，无法报名");
        }

        List<Registration> existingRegistrations = registrationRepository.findByCourseIdAndUserId(course.getId(), userId);
        for (Registration existing : existingRegistrations) {
            if (existing.getStatus() == RegistrationStatus.PENDING) {
                throw new RuntimeException("您已报名该课程，正在等待审核，请勿重复报名");
            }
            if (existing.getStatus() == RegistrationStatus.APPROVED) {
                throw new RuntimeException("您已成功报名该课程，请勿重复报名");
            }
        }
    }

    @Transactional
    public Registration cancel(Long registrationId, Long userId, String reason) {
        Registration registration = findByIdWithLock(registrationId);
        if (registration == null) {
            throw new RuntimeException("报名记录不存在");
        }

        if (!registration.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消他人的报名");
        }

        stateService.validateTransition(registration.getStatus(), RegistrationAction.CANCEL_BY_USER);

        Course course = courseService.findById(registration.getCourseId());
        if (course == null) {
            throw new RuntimeException("关联课程不存在");
        }

        if (!courseService.canCancel(course)) {
            throw new RuntimeException("已超过取消时间，无法取消报名");
        }

        boolean needDecrement = registration.getStatus() == RegistrationStatus.APPROVED ||
                                registration.getStatus() == RegistrationStatus.PENDING;

        RegistrationStatus fromStatus = registration.getStatus();
        stateService.transition(
            registration,
            RegistrationAction.CANCEL_BY_USER,
            userId,
            UserRole.EMPLOYEE,
            reason
        );

        registration.setCancelReason(reason);
        registration.setCancelTime(LocalDateTime.now());

        if (needDecrement) {
            courseService.decrementRegisteredCount(registration.getCourseId());
        }

        return registrationRepository.save(registration);
    }

    @Transactional
    public Registration cancelByAdmin(Long registrationId, Long adminId, String reason) {
        Registration registration = findByIdWithLock(registrationId);
        if (registration == null) {
            throw new RuntimeException("报名记录不存在");
        }

        stateService.validateTransition(registration.getStatus(), RegistrationAction.CANCEL_BY_ADMIN);

        boolean needDecrement = registration.getStatus() == RegistrationStatus.APPROVED ||
                                registration.getStatus() == RegistrationStatus.PENDING;

        stateService.transition(
            registration,
            RegistrationAction.CANCEL_BY_ADMIN,
            adminId,
            UserRole.ADMIN,
            reason
        );

        registration.setCancelReason(reason);
        registration.setCancelTime(LocalDateTime.now());

        if (needDecrement) {
            courseService.decrementRegisteredCount(registration.getCourseId());
        }

        return registrationRepository.save(registration);
    }

    @Transactional
    public Registration approve(Long registrationId, Long reviewerId, String comment) {
        if (reviewerId == null) {
            throw new RuntimeException("审核人信息无效，请重新登录");
        }

        Registration registration = findByIdWithLock(registrationId);
        if (registration == null) {
            throw new RuntimeException("报名记录不存在");
        }

        stateService.validateTransition(registration.getStatus(), RegistrationAction.APPROVE);

        Course course = courseService.findByIdWithLock(registration.getCourseId());
        if (course == null) {
            throw new RuntimeException("关联课程不存在");
        }

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("课程未发布，无法审核");
        }

        if (course.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("课程已结束，无法审核");
        }

        if (course.getMaxCapacity() <= 0) {
            throw new RuntimeException("课程容量无效");
        }

        if (course.getRegisteredCount() == null || course.getRegisteredCount() < 0) {
            throw new RuntimeException("课程报名数据异常");
        }

        if (course.isFull()) {
            throw new RuntimeException("课程名额已满，无法审核通过");
        }

        RegistrationStatus fromStatus = registration.getStatus();
        stateService.transition(
            registration,
            RegistrationAction.APPROVE,
            reviewerId,
            UserRole.ADMIN,
            comment
        );

        registration.setReviewComment(comment);
        registration.setReviewTime(LocalDateTime.now());
        registration.setReviewerId(reviewerId);

        return registrationRepository.save(registration);
    }

    @Transactional
    public Registration reject(Long registrationId, Long reviewerId, String comment) {
        if (reviewerId == null) {
            throw new RuntimeException("审核人信息无效，请重新登录");
        }

        Registration registration = findByIdWithLock(registrationId);
        if (registration == null) {
            throw new RuntimeException("报名记录不存在");
        }

        stateService.validateTransition(registration.getStatus(), RegistrationAction.REJECT);

        Course course = courseService.findByIdWithLock(registration.getCourseId());
        if (course == null) {
            throw new RuntimeException("关联课程不存在");
        }

        if (course.getRegisteredCount() == null || course.getRegisteredCount() <= 0) {
            throw new RuntimeException("课程报名数据异常，无法回退名额");
        }

        RegistrationStatus fromStatus = registration.getStatus();
        stateService.transition(
            registration,
            RegistrationAction.REJECT,
            reviewerId,
            UserRole.ADMIN,
            comment
        );

        registration.setReviewComment(comment);
        registration.setReviewTime(LocalDateTime.now());
        registration.setReviewerId(reviewerId);

        courseService.decrementRegisteredCount(registration.getCourseId());

        return registrationRepository.save(registration);
    }

    public boolean canTransition(Registration registration, RegistrationAction action) {
        return stateService.canTransition(registration, action);
    }

    public Map<String, Long> getStatusStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        statistics.put("PENDING", registrationRepository.countByStatus(RegistrationStatus.PENDING));
        statistics.put("APPROVED", registrationRepository.countByStatus(RegistrationStatus.APPROVED));
        statistics.put("REJECTED", registrationRepository.countByStatus(RegistrationStatus.REJECTED));
        statistics.put("CANCELLED", registrationRepository.countByStatus(RegistrationStatus.CANCELLED));
        statistics.put("TOTAL", registrationRepository.count());
        return statistics;
    }

    @Transactional
    public List<Registration> batchApprove(List<Long> registrationIds, Long reviewerId, String comment) {
        List<Registration> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long id : registrationIds) {
            try {
                Registration registration = approve(id, reviewerId, comment);
                results.add(registration);
            } catch (RuntimeException e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("部分审核失败: " + String.join("; ", errors));
        }

        return results;
    }

    @Transactional
    public List<Registration> batchReject(List<Long> registrationIds, Long reviewerId, String comment) {
        List<Registration> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long id : registrationIds) {
            try {
                Registration registration = reject(id, reviewerId, comment);
                results.add(registration);
            } catch (RuntimeException e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("部分审核失败: " + String.join("; ", errors));
        }

        return results;
    }

    public List<RegistrationStatusLog> getStatusHistory(Long registrationId) {
        return stateService.getStatusHistory(registrationId);
    }

    public List<Registration> getRegistrationHistory(Long courseId, Long userId) {
        List<Registration> registrations = registrationRepository.findByCourseIdAndUserIdOrderByCreateTimeAsc(courseId, userId);
        return registrations.stream().map(this::enrichRegistration).collect(Collectors.toList());
    }

    public List<Registration> getRegistrationChain(Long registrationId) {
        Registration registration = findById(registrationId);
        if (registration == null) {
            return new ArrayList<>();
        }

        Long originalId = registration.getOriginalRegistrationId() != null ? 
            registration.getOriginalRegistrationId() : registration.getId();

        List<Registration> chain = registrationRepository.findByOriginalRegistrationIdOrderByCreateTimeAsc(originalId);
        
        if (chain.isEmpty()) {
            chain = registrationRepository.findByCourseIdAndUserIdOrderByCreateTimeAsc(
                registration.getCourseId(), 
                registration.getUserId()
            );
        }

        return chain.stream().map(this::enrichRegistration).collect(Collectors.toList());
    }

    public long getApprovedCount(Long courseId) {
        return registrationRepository.countByCourseIdAndStatusIn(
                courseId,
                Arrays.asList(RegistrationStatus.APPROVED)
        );
    }

    public long getEffectiveRegistrationCount(Long courseId) {
        return registrationRepository.countByCourseIdAndStatusIn(
                courseId,
                Arrays.asList(RegistrationStatus.PENDING, RegistrationStatus.APPROVED)
        );
    }
}
