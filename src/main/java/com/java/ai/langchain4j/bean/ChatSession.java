package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 会话元数据文档，用于展示会话列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("chat_sessions")
public class ChatSession {

     //会话唯一标识，同时作为 MongoDB 文档主键。
    @Id
    private Long sessionId;

     //会话标题，用于前端列表展示。
    private String title;

     //会话最后一条消息摘要。
    private String lastMessage;

     // 当前会话累计消息条数。
    private Integer messageCount;


     //会话创建时间。
    private LocalDateTime createdAt;


     //会话最后更新时间。
    private LocalDateTime updatedAt;
}
