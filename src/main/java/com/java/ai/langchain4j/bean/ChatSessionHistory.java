package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话详情返回对象，包含会话元信息与前端展示消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionHistory {

    /**
     * 当前会话的元数据。
     */
    private ChatSession session;

    /**
     * 当前会话对应的展示消息列表，只包含 user 和 assistant。
     */
    private List<ChatDisplayMessage> messages;
}
