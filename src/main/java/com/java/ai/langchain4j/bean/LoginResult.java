package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功返回信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    private String token;

    private Long userId;

    private String username;

    private String nickname;

    private String role;

    private Boolean isAdmin;
}
