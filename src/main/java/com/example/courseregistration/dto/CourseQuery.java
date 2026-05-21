package com.example.courseregistration.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseQuery {
    private String name;
    private String type;
    private String lecturer;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer page = 1;
    private Integer size = 10;
}
