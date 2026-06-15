package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 前端展示用的会话消息文档，只保存用户消息和大模型回复。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("chat_display_messages")
public class ChatDisplayMessage {

    /**
     * 消息主键，由 MongoDB 自动生成。
     */
    @Id
    private ObjectId id;

    /**
     * 所属会话 ID。
     */
    private Long sessionId;

    /**
     * 所属登录用户 ID。
     */
    private Long userId;

    /**
     * 消息角色，仅保存 user 或 assistant。
     */
    private String role;

    /**
     * 当前消息的展示文本内容。
     */
    private String content;

    /**
     * 消息在当前会话中的顺序号。
     */
    private Long messageOrder;

    /**
     * 消息创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 当前 AI 回复命中的知识来源名称列表，仅 AI 消息可能有值。
     */
    private List<String> references;
}
