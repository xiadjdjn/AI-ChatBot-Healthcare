package com.java.ai.langchain4j.store;

import com.java.ai.langchain4j.bean.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * 基于 MongoDB 的聊天记忆存储实现，只服务于模型记忆。
 */
@Component
public class MongoChatMemoryStore implements ChatMemoryStore {

    /**
     * MongoDB 操作模板。
     */
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 按会话 ID 查询历史消息列表。
     * @param memoryId 会话 ID
     * @return 历史消息列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Criteria criteria = Criteria.where("_id").is(memoryId);
        Query query = new Query(criteria);
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);
        if (chatMessages != null) {
            return ChatMessageDeserializer.messagesFromJson(chatMessages.getContent());
        }
        return new LinkedList<>();
    }

    /**
     * 更新指定会话的模型记忆消息内容。
     * @param memoryId 会话 ID
     * @param messages 最新消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Criteria criteria = Criteria.where("_id").is(memoryId);
        Query query = new Query(criteria);
        String messagesJson = ChatMessageSerializer.messagesToJson(messages);
        Update update = new Update().set("content", messagesJson);
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }

    /**
     * 删除指定会话的全部模型记忆消息。
     * @param memoryId 会话 ID
     */
    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("_id").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}
