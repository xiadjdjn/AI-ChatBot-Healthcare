package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 创建新会话时的请求参数。
 */
@Data
public class CreateSessionForm {

    /**
     * 前端指定的会话 ID，未传时由后端生成。
     */
    private Long memoryId;

    /**
     * 会话标题，未传时由后端生成默认标题。
     */
    private String title;
}
