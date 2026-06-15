package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员查看用户会话历史的列表项，只展示会话标题和用户基础信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminChatSessionSummary {

    // 会话 ID。
    private Long sessionId;

    // 会话所属用户 ID。
    private Long userId;

    // 会话所属用户名。
    private String username;

    // 会话所属用户昵称。
    private String nickname;

    // 会话标题。
    private String title;

    // 会话创建时间。
    private LocalDateTime createdAt;

    // 会话最后更新时间。
    private LocalDateTime updatedAt;
}
