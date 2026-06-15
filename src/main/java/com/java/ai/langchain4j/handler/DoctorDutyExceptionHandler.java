package com.java.ai.langchain4j.handler;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.controller.DoctorDutyController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 值班医生模块异常处理。
 */
@RestControllerAdvice(assignableTypes = DoctorDutyController.class)
public class DoctorDutyExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Boolean> handleRuntimeException(RuntimeException e) {
        return new ApiResponse<>(-1, e.getMessage(), false);
    }
}
