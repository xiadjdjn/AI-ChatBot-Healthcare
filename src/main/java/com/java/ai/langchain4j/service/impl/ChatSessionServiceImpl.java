package com.java.ai.langchain4j.service.impl;

import com.java.ai.langchain4j.bean.ChatDisplayMessage;
import com.java.ai.langchain4j.bean.ChatMessages;
import com.java.ai.langchain4j.bean.ChatSession;
import com.java.ai.langchain4j.bean.ChatSessionHistory;
import com.java.ai.langchain4j.service.ChatDisplayMessageService;
import com.java.ai.langchain4j.service.ChatSessionService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 MongoDB 的会话管理服务实现。
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    /**
     * 默认会话标题中的时间格式。
     */
    private static final DateTimeFormatter TITLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 默认标题前缀。
     */
    private static final String DEFAULT_TITLE_PREFIX = "新会话";

    /**
     * MongoDB 操作模板。
     */
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 前端展示消息服务。
     */
    @Autowired
    private ChatDisplayMessageService chatDisplayMessageService;

    /**
     * 创建新的会话元数据记录。
     *
     * @param sessionId 会话 ID
     * @param title 会话标题
     * @return 创建后的会话对象
     */
    @Override
    public ChatSession createSession(Long sessionId, String title) {
        Long resolvedSessionId = resolveSessionId(sessionId);
        ChatSession existingSession = mongoTemplate.findById(resolvedSessionId, ChatSession.class);
        if (existingSession != null) {
            return existingSession;
        }

        LocalDateTime now = LocalDateTime.now();
        ChatSession chatSession = ChatSession.builder()
            .sessionId(resolvedSessionId)
            .title(resolveTitle(title, now))
            .lastMessage("")
            .messageCount(0)
            .createdAt(now)
            .updatedAt(now)
            .build();
        return mongoTemplate.save(chatSession);
    }

    /**
     * 确保指定会话存在，不存在时自动补建。
     *
     * @param sessionId 会话 ID
     * @return 已存在或新建的会话对象
     */
    @Override
    public ChatSession ensureSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("memoryId cannot be null");
        }
        ChatSession existingSession = mongoTemplate.findById(sessionId, ChatSession.class);
        if (existingSession != null) {
            return existingSession;
        }
        return createSession(sessionId, null);
    }

    /**
     * 根据最新展示消息刷新会话摘要、消息数和更新时间。
     *
     * @param sessionId 会话 ID
     * @param lastMessage 最新展示消息内容
     */
    @Override
    public void refreshSession(Long sessionId, String lastMessage) {
        if (sessionId == null) {
            return;
        }
        ChatSession chatSession = ensureSession(sessionId);
        int currentCount = chatSession.getMessageCount() == null ? 0 : chatSession.getMessageCount();
        chatSession.setMessageCount(currentCount + 1);
        chatSession.setLastMessage(limitLength(lastMessage, 200));
        chatSession.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(chatSession);
    }

    /**
     * 在会话还没有正式标题时，根据用户第一句话生成并更新标题。
     *
     * @param sessionId 会话 ID
     * @param firstUserMessage 用户第一句话
     */
    @Override
    public void updateTitleIfAbsent(Long sessionId, String firstUserMessage) {
        if (sessionId == null || !StringUtils.hasText(firstUserMessage)) {
            return;
        }

        ChatSession chatSession = ensureSession(sessionId);
        if (!shouldGenerateTitle(chatSession)) {
            return;
        }

        chatSession.setTitle(generateSessionTitle(firstUserMessage));
        chatSession.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(chatSession);
    }

    /**
     * 查询所有会话列表，并按更新时间倒序返回。
     *
     * @return 会话列表
     */
    @Override
    public List<ChatSession> listSessions() {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<ChatSession> sessions = mongoTemplate.find(query, ChatSession.class);
        return sessions == null ? Collections.emptyList() : sessions;
    }

    /**
     * 删除指定会话，并同步删除会话历史记录。
     *
     * @param sessionId 会话 ID
     */
    @Override
    public void deleteSession(Long sessionId) {
        if (sessionId == null) {
            return;
        }

        chatDisplayMessageService.deleteMessagesBySessionId(sessionId);
        mongoTemplate.remove(new Query(Criteria.where("_id").is(sessionId)), ChatMessages.class);
        mongoTemplate.remove(new Query(Criteria.where("_id").is(sessionId)), ChatSession.class);
    }

    /**
     * 查询指定会话的元数据和展示消息。
     * 如果新展示集合中暂无数据，则从旧记忆集合中过滤 user/assistant 消息并迁移。
     *
     * @param sessionId 会话 ID
     * @return 会话详情
     */
    @Override
    public ChatSessionHistory getSessionHistory(Long sessionId) {
        ChatSession session = ensureSession(sessionId);
        List<ChatDisplayMessage> messages = chatDisplayMessageService.listMessages(sessionId);
        if (messages.isEmpty()) {
            messages = migrateMessagesFromMemory(sessionId);
        }
        return new ChatSessionHistory(session, messages);
    }

    /**
     * 解析最终会话 ID，未指定时自动生成可用 ID。
     *
     * @param sessionId 前端传入的会话 ID
     * @return 可用的会话 ID
     */
    private Long resolveSessionId(Long sessionId) {
        if (sessionId != null) {
            return sessionId;
        }
        long generatedId = System.currentTimeMillis();
        while (mongoTemplate.findById(generatedId, ChatSession.class) != null) {
            generatedId++;
        }
        return generatedId;
    }

    /**
     * 解析会话标题，未传时生成默认标题。
     *
     * @param title 前端传入标题
     * @param now 当前时间
     * @return 最终会话标题
     */
    private String resolveTitle(String title, LocalDateTime now) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        return DEFAULT_TITLE_PREFIX + " " + TITLE_TIME_FORMATTER.format(now);
    }

    /**
     * 判断当前会话是否需要根据首句生成正式标题。
     *
     * @param chatSession 当前会话
     * @return 是否需要生成标题
     */
    private boolean shouldGenerateTitle(ChatSession chatSession) {
        String title = chatSession.getTitle();
        return !StringUtils.hasText(title) || title.startsWith(DEFAULT_TITLE_PREFIX);
    }

    /**
     * 根据用户第一句话生成会话标题。
     *
     * @param firstUserMessage 用户第一句话
     * @return 生成后的会话标题
     */
    private String generateSessionTitle(String firstUserMessage) {
        String normalized = firstUserMessage
            .replace("\r", " ")
            .replace("\n", " ")
            .replace("\t", " ")
            .trim();
        if (!StringUtils.hasText(normalized)) {
            return DEFAULT_TITLE_PREFIX;
        }

        normalized = normalized.replaceFirst("^(你好|您好|请问|麻烦问一下|我想咨询一下|我想问一下|想咨询一下|想问一下)[，,：: ]*", "");
        normalized = normalized.replaceAll("\\s+", "");
        normalized = normalized.replaceAll("[。！？!?；;，,]+$", "");

        if (!StringUtils.hasText(normalized)) {
            return DEFAULT_TITLE_PREFIX;
        }

        int maxLength = 16;
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    /**
     * 从旧的模型记忆中提取用户消息和模型回复，并迁移到展示集合。
     *
     * @param sessionId 会话 ID
     * @return 迁移后的展示消息列表
     */
    private List<ChatDisplayMessage> migrateMessagesFromMemory(Long sessionId) {
        ChatMessages chatMessages = mongoTemplate.findById(sessionId, ChatMessages.class);
        if (chatMessages == null || !StringUtils.hasText(chatMessages.getContent())) {
            return Collections.emptyList();
        }

        List<ChatMessage> memoryMessages = ChatMessageDeserializer.messagesFromJson(chatMessages.getContent());
        List<ChatDisplayMessage> displayMessages = new ArrayList<>();
        long messageOrder = 1L;
        for (ChatMessage memoryMessage : memoryMessages) {
            String role = resolveRole(memoryMessage);
            String content = resolveContent(memoryMessage);
            if (!StringUtils.hasText(role) || !StringUtils.hasText(content)) {
                continue;
            }
            displayMessages.add(ChatDisplayMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .messageOrder(messageOrder++)
                .createdAt(LocalDateTime.now())
                .build());
        }

        chatDisplayMessageService.saveMessages(displayMessages);
        return displayMessages;
    }

    /**
     * 将模型记忆消息转换为前端展示角色。
     *
     * @param memoryMessage 模型记忆消息
     * @return user、assistant 或空字符串
     */
    private String resolveRole(ChatMessage memoryMessage) {
        if (memoryMessage instanceof UserMessage) {
            return ChatDisplayMessageServiceImpl.ROLE_USER;
        }
        if (memoryMessage instanceof AiMessage) {
            return ChatDisplayMessageServiceImpl.ROLE_ASSISTANT;
        }
        if (memoryMessage instanceof SystemMessage) {
            return "";
        }
        return "";
    }

    /**
     * 提取模型记忆消息中的纯文本内容。
     *
     * @param memoryMessage 模型记忆消息
     * @return 可展示的文本内容
     */
    private String resolveContent(ChatMessage memoryMessage) {
        if (memoryMessage instanceof UserMessage userMessage) {
            return userMessage.singleText();
        }
        if (memoryMessage instanceof AiMessage aiMessage) {
            return aiMessage.text();
        }
        return "";
    }

    /**
     * 将文本限制在指定长度内，避免会话摘要过长。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String limitLength(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
