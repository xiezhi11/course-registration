package com.example.courseregistration.entity;

import com.example.courseregistration.enums.CourseStatus;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "course", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "start_time", "end_time"})
})
@org.hibernate.annotations.Check(constraints = 
    "max_capacity > 0 AND " +
    "registered_count >= 0 AND " +
    "registered_count <= max_capacity AND " +
    "end_time > start_time AND " +
    "(cancel_deadline IS NULL OR cancel_deadline < start_time)"
)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 200, message = "课程名称长度不能超过200")
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank(message = "课程类型不能为空")
    @Size(max = 50, message = "课程类型长度不能超过50")
    @Column(nullable = false, length = 50)
    private String type;

    @NotBlank(message = "讲师不能为空")
    @Size(max = 100, message = "讲师姓名长度不能超过100")
    @Column(nullable = false, length = 100)
    private String lecturer;

    @NotNull(message = "开始时间不能为空")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "人数上限不能为空")
    @Min(value = 1, message = "人数上限必须大于0")
    @Max(value = 1000, message = "人数上限不能超过1000")
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Min(value = 0, message = "已报名人数不能为负数")
    @Column(name = "registered_count", nullable = false)
    private Integer registeredCount = 0;

    private String location;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @Column(name = "cancel_deadline")
    private LocalDateTime cancelDeadline;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (registeredCount == null) {
            registeredCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public boolean isFull() {
        return registeredCount >= maxCapacity;
    }

    public boolean isNotFull() {
        return registeredCount < maxCapacity;
    }

    public int getAvailableSlots() {
        return maxCapacity - registeredCount;
    }
}
