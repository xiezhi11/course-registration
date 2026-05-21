package com.example.courseregistration.controller;

import com.example.courseregistration.common.Result;
import com.example.courseregistration.dto.CourseQuery;
import com.example.courseregistration.entity.Course;
import com.example.courseregistration.entity.Registration;
import com.example.courseregistration.service.CourseService;
import com.example.courseregistration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@Validated
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private RegistrationService registrationService;

    @GetMapping
    public Result<Page<Course>> list(CourseQuery query) {
        return Result.success(courseService.findByConditions(query));
    }

    @GetMapping("/{id}")
    public Result<Course> getById(@PathVariable @NotNull(message = "课程ID不能为空") Long id) {
        Course course = courseService.findById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }
        return Result.success(course);
    }

    @PostMapping
    public Result<Course> create(@Valid @RequestBody Course course) {
        course.setId(null);
        return Result.success(courseService.save(course));
    }

    @PutMapping("/{id}")
    public Result<Course> update(
            @PathVariable @NotNull(message = "课程ID不能为空") Long id,
            @Valid @RequestBody Course course
    ) {
        course.setId(id);
        return Result.success(courseService.save(course));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            courseService.deleteById(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    public Result<Course> publish(@PathVariable Long id) {
        try {
            return Result.success(courseService.publishCourse(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/close")
    public Result<Course> close(@PathVariable Long id) {
        try {
            return Result.success(courseService.closeCourse(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/registrations")
    public Result<List<Registration>> getRegistrations(@PathVariable Long id) {
        return Result.success(registrationService.findByCourseId(id));
    }

    @GetMapping("/available")
    public Result<List<Course>> getAvailableCourses() {
        return Result.success(courseService.findAvailableCourses());
    }

    @GetMapping("/published")
    public Result<List<Course>> getPublishedCourses() {
        return Result.success(courseService.findPublishedCourses());
    }
}
