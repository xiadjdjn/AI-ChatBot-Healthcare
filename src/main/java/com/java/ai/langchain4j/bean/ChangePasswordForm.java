package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 修改密码请求。
 */
@Data
public class ChangePasswordForm {

    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
}
