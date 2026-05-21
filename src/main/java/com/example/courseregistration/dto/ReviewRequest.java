package com.example.courseregistration.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReviewRequest {

    @NotNull(message = "报名记录ID不能为空")
    private Long registrationId;

    private String comment;
}
