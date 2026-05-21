package com.java.ai.langchain4j.service.impl;

import com.java.ai.langchain4j.bean.ChatDisplayMessage;
import com.java.ai.langchain4j.service.ChatDisplayMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 前端展示消息服务实现。
 */
@Service
public class ChatDisplayMessageServiceImpl implements ChatDisplayMessageService {

    /**
     * 用户消息角色标识。
     */
    public static final String ROLE_USER = "user";

    /**
     * 大模型回复角色标识。
     */
    public static final String ROLE_ASSISTANT = "assistant";

    /**
     * MongoDB 操作模板。
     */
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存一条前端展示消息。
     *
     * @param sessionId 会话 ID
     * @param role 消息角色
     * @param content 消息内容
     * @return 保存后的消息对象
     */
    @Override
    public ChatDisplayMessage saveMessage(Long sessionId, String role, String content) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId cannot be null");
        }
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("role cannot be blank");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content cannot be blank");
        }

        ChatDisplayMessage message = ChatDisplayMessage.builder()
            .sessionId(sessionId)
            .role(role)
            .content(content)
            .messageOrder(nextMessageOrder(sessionId))
            .createdAt(LocalDateTime.now())
            .build();
        return mongoTemplate.save(message);
    }

    /**
     * 查询指定会话的展示消息列表。
     *
     * @param sessionId 会话 ID
     * @return 展示消息列表
     */
    @Override
    public List<ChatDisplayMessage> listMessages(Long sessionId) {
        Query query = new Query(Criteria.where("sessionId").is(sessionId))
            .with(Sort.by(Sort.Direction.ASC, "messageOrder"));
        List<ChatDisplayMessage> messages = mongoTemplate.find(query, ChatDisplayMessage.class);
        return messages == null ? Collections.emptyList() : messages;
    }

    /**
     * 批量保存展示消息列表。
     *
     * @param messages 待保存的展示消息
     */
    @Override
    public void saveMessages(List<ChatDisplayMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }
        mongoTemplate.insert(messages, ChatDisplayMessage.class);
    }

    /**
     * 计算指定会话下一条消息的顺序号。
     *
     * @param sessionId 会话 ID
     * @return 下一条顺序号
     */
    private Long nextMessageOrder(Long sessionId) {
        Query query = new Query(Criteria.where("sessionId").is(sessionId))
            .with(Sort.by(Sort.Direction.DESC, "messageOrder"))
            .limit(1);
        ChatDisplayMessage latestMessage = mongoTemplate.findOne(query, ChatDisplayMessage.class);
        return latestMessage == null ? 1L : latestMessage.getMessageOrder() + 1;
    }
}
