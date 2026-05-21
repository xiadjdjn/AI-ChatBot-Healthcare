package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 会话历史消息文档。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_messages")
public class ChatMessages {

    /**
     * 会话唯一标识，同时作为 MongoDB 文档主键。
     */
    @Id
    private Long memoryId;

    /**
     * 当前会话完整消息列表的 JSON 字符串。
     */
    private String content;
}
