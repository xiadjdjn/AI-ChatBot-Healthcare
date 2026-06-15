package com.java.ai.langchain4j.service;

import com.java.ai.langchain4j.bean.ChatSession;
import com.java.ai.langchain4j.bean.ChatSessionHistory;
import com.java.ai.langchain4j.bean.AdminChatSessionSummary;
import com.java.ai.langchain4j.bean.PageResult;

import java.util.List;

/**
 * 会话管理服务接口。
 */
public interface ChatSessionService {

    /**
     * 创建一个由后端生成 ID 的新会话元数据记录。
     */
    ChatSession createSession(Long userId, String title);

    /**
     * 确保指定会话存在，不存在时自动创建。
     */
    ChatSession ensureSession(Long sessionId, Long userId);

    /**
     * 根据最新展示消息刷新会话摘要、消息数和更新时间。
     *
     * @param sessionId 会话 ID
     * @param lastMessage 最新展示消息内容
     */
    void refreshSession(Long sessionId, Long userId, String lastMessage);

    /**
     * 在会话还没有正式标题时，根据用户第一句话生成并更新标题。
     *
     * @param sessionId 会话 ID
     * @param firstUserMessage 用户第一句话
     */
    void updateTitleIfAbsent(Long sessionId, Long userId, String firstUserMessage);

    /**
     * 查询全部会话列表，按更新时间倒序返回。
     *
     * @return 会话列表
     */
    List<ChatSession> listSessions(Long userId);

    /**
     * 根据关键字模糊查询会话列表，关键字匹配用户发送的消息内容。
     * 关键字为空时返回全部会话。
     *
     * @param keyword 搜索关键字
     * @return 会话列表
     */
    List<ChatSession> listSessions(Long userId, String keyword);

    /**
     * 管理员分页查询所有用户的会话标题列表。
     *
     * @param username 用户名查询条件
     * @param nickname 昵称查询条件
     * @param pageNum 页码
     * @param pageSize 每页条数，仅支持 10、15、20
     * @return 会话标题分页结果
     */
    PageResult<AdminChatSessionSummary> listAdminSessionSummaries(String username, String nickname, Integer pageNum, Integer pageSize);

    /**
     * 删除指定会话，并同步删除会话历史记录。
     *
     * @param sessionId 会话 ID
     */
    void deleteSession(Long sessionId, Long userId);

    /**
     * 查询指定会话的元信息和历史消息。
     *
     * @param sessionId 会话 ID
     * @return 会话详情
     */
    ChatSessionHistory getSessionHistory(Long sessionId, Long userId);
}
