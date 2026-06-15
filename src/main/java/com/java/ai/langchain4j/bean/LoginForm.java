package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
public class LoginForm {

    private String username;

    private String password;
}
