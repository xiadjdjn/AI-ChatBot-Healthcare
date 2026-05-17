package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 接受前端传来的用户聊天信息
 */
@Data
public class ChatForm {
    private Long memoryId;//对话id
    private String message;//用户问题
}