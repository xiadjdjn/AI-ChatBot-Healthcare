package com.java.ai.langchain4j.service;

import com.java.ai.langchain4j.bean.ChatSession;
import com.java.ai.langchain4j.bean.ChatSessionHistory;

import java.util.List;

/**
 * 会话管理服务接口。
 */
public interface ChatSessionService {

    /**
     * 创建一个新的会话元数据记录。
     */
    ChatSession createSession(Long sessionId, String title);

    /**
     * 确保指定会话存在，不存在时自动创建。
     */
    ChatSession ensureSession(Long sessionId);

    /**
     * 根据最新展示消息刷新会话摘要、消息数和更新时间。
     *
     * @param sessionId 会话 ID
     * @param lastMessage 最新展示消息内容
     */
    void refreshSession(Long sessionId, String lastMessage);

    /**
     * 查询全部会话列表，按更新时间倒序返回。
     *
     * @return 会话列表
     */
    List<ChatSession> listSessions();

    /**
     * 查询指定会话的元信息和历史消息。
     *
     * @param sessionId 会话 ID
     * @return 会话详情
     */
    ChatSessionHistory getSessionHistory(Long sessionId);
}
