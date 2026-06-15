package com.java.ai.langchain4j.handler;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.controller.AuthController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证模块异常处理。
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    /**
     * 捕获登录参数或账号密码错误。
     *
     * @param e 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Boolean> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ApiResponse<>(-1, e.getMessage(), false);
    }
}
