package com.example.courseregistration.controller;

import com.example.courseregistration.common.Result;
import com.example.courseregistration.dto.BatchReviewRequest;
import com.example.courseregistration.dto.ReviewRequest;
import com.example.courseregistration.entity.Registration;
import com.example.courseregistration.entity.RegistrationStatusLog;
import com.example.courseregistration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
@Validated
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @GetMapping
    public Result<Page<Registration>> list(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(registrationService.findByConditions(courseId, userId, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<Registration> getById(@PathVariable @NotNull(message = "报名记录ID不能为空") Long id) {
        Registration registration = registrationService.findById(id);
        if (registration == null) {
            return Result.error("报名记录不存在");
        }
        return Result.success(registration);
    }

    @GetMapping("/my")
    public Result<Page<Registration>> getMyRegistrations(
            @RequestHeader("X-User-Id") @NotNull(message = "用户未登录") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(registrationService.findByUserId(userId, page, size));
    }

    @GetMapping("/check")
    public Result<Map<String, Boolean>> checkRegistration(
            @RequestParam @NotNull(message = "课程ID不能为空") Long courseId,
            @RequestHeader("X-User-Id") @NotNull(message = "用户未登录") Long userId
    ) {
        boolean exists = registrationService.existsByCourseIdAndUserId(courseId, userId);
        return Result.success(Map.of("registered", exists));
    }

    @PostMapping("/register")
    public Result<Registration> register(
            @RequestParam @NotNull(message = "课程ID不能为空") Long courseId,
            @RequestHeader("X-User-Id") @NotNull(message = "用户未登录") Long userId
    ) {
        try {
            Registration registration = registrationService.register(courseId, userId);
            return Result.success("报名成功", registration);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public Result<Registration> cancel(
            @PathVariable @NotNull(message = "报名记录ID不能为空") Long id,
            @RequestHeader("X-User-Id") @NotNull(message = "用户未登录") Long userId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        try {
            String reason = body != null ? body.get("reason") : null;
            Registration registration = registrationService.cancel(id, userId, reason);
            return Result.success("取消报名成功", registration);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel-by-admin")
    public Result<Registration> cancelByAdmin(
            @PathVariable @NotNull(message = "报名记录ID不能为空") Long id,
            @RequestHeader("X-User-Id") @NotNull(message = "管理员未登录") Long adminId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        try {
            String reason = body != null ? body.get("reason") : null;
            Registration registration = registrationService.cancelByAdmin(id, adminId, reason);
            return Result.success("取消报名成功", registration);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    public Result<Registration> approve(
            @PathVariable @NotNull(message = "报名记录ID不能为空") Long id,
            @RequestHeader("X-User-Id") @NotNull(message = "审核人未登录") Long reviewerId,
            @RequestBody(required = false) ReviewRequest request
    ) {
        try {
            String comment = request != null ? request.getComment() : null;
            Registration registration = registrationService.approve(id, reviewerId, comment);
            return Result.success("审核通过", registration);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public Result<Registration> reject(
            @PathVariable @NotNull(message = "报名记录ID不能为空") Long id,
            @RequestHeader("X-User-Id") @NotNull(message = "审核人未登录") Long reviewerId,
            @RequestBody(required = false) ReviewRequest request
    ) {
        try {
            String comment = request != null ? request.getComment() : null;
            Registration registration = registrationService.reject(id, reviewerId, comment);
            return Result.success("审核驳回", registration);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Long>> getStatistics() {
        return Result.success(registrationService.getStatusStatistics());
    }

    @PostMapping("/batch/approve")
    public Result<List<Registration>> batchApprove(
            @RequestBody @Valid BatchReviewRequest request,
            @RequestHeader("X-User-Id") @NotNull(message = "审核人未登录") Long reviewerId
    ) {
        try {
            List<Registration> results = registrationService.batchApprove(
                request.getIds(), 
                reviewerId, 
                request.getComment()
            );
            return Result.success("批量审核通过成功，共处理 " + results.size() + " 条", results);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/batch/reject")
    public Result<List<Registration>> batchReject(
            @RequestBody @Valid BatchReviewRequest request,
            @RequestHeader("X-User-Id") @NotNull(message = "审核人未登录") Long reviewerId
    ) {
        try {
            List<Registration> results = registrationService.batchReject(
                request.getIds(), 
                reviewerId, 
                request.getComment()
            );
            return Result.success("批量审核驳回成功，共处理 " + results.size() + " 条", results);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/history")
    public Result<List<RegistrationStatusLog>> getStatusHistory(
            @PathVariable @NotNull(message = "报名记录ID不能为空") Long id
    ) {
        return Result.success(registrationService.getStatusHistory(id));
    }

    @GetMapping("/{id}/chain")
    public Result<List<Registration>> getRegistrationChain(
            @PathVariable @NotNull(message = "报名记录ID不能为空") Long id
    ) {
        return Result.success(registrationService.getRegistrationChain(id));
    }

    @GetMapping("/course/{courseId}/user/{userId}/history")
    public Result<List<Registration>> getRegistrationHistory(
            @PathVariable @NotNull(message = "课程ID不能为空") Long courseId,
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId
    ) {
        return Result.success(registrationService.getRegistrationHistory(courseId, userId));
    }

    @GetMapping("/all")
    public Result<List<Registration>> getAll() {
        return Result.success(registrationService.findAll());
    }
}
