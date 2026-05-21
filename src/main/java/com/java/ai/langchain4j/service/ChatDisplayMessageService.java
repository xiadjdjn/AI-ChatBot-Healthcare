package com.java.ai.langchain4j.service;

import com.java.ai.langchain4j.bean.ChatDisplayMessage;

import java.util.List;

/**
 * 前端展示消息服务接口。
 */
public interface ChatDisplayMessageService {

    /**
     * 保存一条前端展示消息。
     *
     * @param sessionId 会话 ID
     * @param role 消息角色
     * @param content 消息内容
     * @return 保存后的消息对象
     */
    ChatDisplayMessage saveMessage(Long sessionId, String role, String content);

    /**
     * 查询指定会话的展示消息列表。
     *
     * @param sessionId 会话 ID
     * @return 展示消息列表
     */
    List<ChatDisplayMessage> listMessages(Long sessionId);

    /**
     * 批量保存展示消息列表。
     *
     * @param messages 待保存的展示消息
     */
    void saveMessages(List<ChatDisplayMessage> messages);
}
