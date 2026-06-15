package com.java.ai.langchain4j.handler;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.controller.UserProfileController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 个人中心模块异常处理。
 */
@RestControllerAdvice(assignableTypes = UserProfileController.class)
public class UserProfileExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Boolean> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ApiResponse<>(-1, e.getMessage(), false);
    }
}
