package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 接收前端提交的对话请求参数。
 */
@Data
public class ChatForm {

    /**
     * 当前会话唯一标识。
     */
    private Long memoryId;

    /**
     * 用户本次发送的问题内容。
     */
    private String message;
}
