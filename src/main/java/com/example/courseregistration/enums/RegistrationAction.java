package com.example.courseregistration.enums;

public enum RegistrationAction {
    SUBMIT("提交报名"),
    APPROVE("审核通过"),
    REJECT("审核驳回"),
    CANCEL_BY_USER("用户取消"),
    CANCEL_BY_ADMIN("管理员取消"),
    RE_SUBMIT("重新报名");

    private final String description;

    RegistrationAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
