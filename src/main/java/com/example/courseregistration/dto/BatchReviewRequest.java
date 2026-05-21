package com.example.courseregistration.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class BatchReviewRequest {

    @NotEmpty(message = "请选择要审核的报名记录")
    private List<@NotNull(message = "报名记录ID不能为空") Long> ids;

    private String comment;
}
