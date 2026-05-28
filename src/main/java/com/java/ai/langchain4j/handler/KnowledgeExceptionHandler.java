package com.java.ai.langchain4j.handler;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.controller.KnowledgeDocumentController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 知识库模块异常处理。
 */
@RestControllerAdvice(assignableTypes = KnowledgeDocumentController.class)
public class KnowledgeExceptionHandler {
    /**
     * 捕获知识库接口中的运行时异常并返回统一结构。
     * @param e 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Boolean> handleRuntimeException(RuntimeException e) {
        return new ApiResponse<>(-1, e.getMessage(), false);
    }
}
