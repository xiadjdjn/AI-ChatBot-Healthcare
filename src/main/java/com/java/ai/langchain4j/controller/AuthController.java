package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.bean.LoginForm;
import com.java.ai.langchain4j.bean.LoginResult;
import com.java.ai.langchain4j.bean.RegisterForm;
import com.java.ai.langchain4j.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证接口。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录并返回 JWT。
     *
     * @param loginForm 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@RequestBody LoginForm loginForm) {
        return ApiResponse.success(userService.login(loginForm));
    }

    /**
     * 注册普通用户并返回 JWT。
     *
     * @param registerForm 注册请求参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<LoginResult> register(@RequestBody RegisterForm registerForm) {
        return ApiResponse.success(userService.register(registerForm));
    }
}
