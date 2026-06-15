package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 注册请求参数。
 */
@Data
public class RegisterForm {

    //用户名。
    private String username;

    //密码。
    private String password;

    //确认密码。
    private String confirmPassword;

    //昵称。
    private String nickname;
}
