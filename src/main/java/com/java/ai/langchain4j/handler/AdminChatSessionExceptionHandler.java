package com.java.ai.langchain4j.handler;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.controller.AdminChatSessionController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 管理员会话历史模块异常处理。
 */
@RestControllerAdvice(assignableTypes = AdminChatSessionController.class)
public class AdminChatSessionExceptionHandler {

    /**
     * 捕获管理员会话历史查询参数错误。
     *
     * @param e 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Boolean> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ApiResponse<>(-1, e.getMessage(), false);
    }
}
