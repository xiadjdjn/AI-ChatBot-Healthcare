package com.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.java.ai.langchain4j.bean.AdminChatSessionSummary;
import com.java.ai.langchain4j.bean.ChatDisplayMessage;
import com.java.ai.langchain4j.bean.ChatMessages;
import com.java.ai.langchain4j.bean.ChatSession;
import com.java.ai.langchain4j.bean.ChatSessionHistory;
import com.java.ai.langchain4j.bean.PageResult;
import com.java.ai.langchain4j.entity.User;
import com.java.ai.langchain4j.mapper.UserMapper;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Set<Integer> ADMIN_PAGE_SIZE_OPTIONS = Set.of(10, 15, 20);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ChatDisplayMessageService chatDisplayMessageService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 创建新的会话元数据记录。
     *
     * @param userId 登录用户 ID
     * @param title 会话标题
     * @return 创建后的会话对象
     */
    @Override
    public ChatSession createSession(Long userId, String title) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        Long resolvedSessionId = generateAvailableSessionId();

        LocalDateTime now = LocalDateTime.now();
        ChatSession chatSession = ChatSession.builder()
            .sessionId(resolvedSessionId)
            .userId(userId)
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
    public ChatSession ensureSession(Long sessionId, Long userId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("memoryId cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        ChatSession existingSession = mongoTemplate.findById(sessionId, ChatSession.class);
        if (existingSession != null) {
            assertSessionOwner(existingSession, userId);
            return existingSession;
        }
        throw new IllegalArgumentException("chat session does not exist");
    }

    /**
     * 根据最新展示消息刷新会话摘要、消息数和更新时间。
     *
     * @param sessionId 会话 ID
     * @param lastMessage 最新展示消息内容
     */
    @Override
    public void refreshSession(Long sessionId, Long userId, String lastMessage) {
        if (sessionId == null) {
            return;
        }
        ChatSession chatSession = ensureSession(sessionId, userId);
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
    public void updateTitleIfAbsent(Long sessionId, Long userId, String firstUserMessage) {
        if (sessionId == null || !StringUtils.hasText(firstUserMessage)) {
            return;
        }

        ChatSession chatSession = ensureSession(sessionId, userId);
        if (!shouldGenerateTitle(chatSession)) {
            return;
        }

        chatSession.setTitle(generateSessionTitle(firstUserMessage));
        chatSession.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(chatSession);
    }

    /**
     * 查询全部会话列表，按更新时间倒序返回。
     *
     * @return 会话列表
     */
    @Override
    public List<ChatSession> listSessions(Long userId) {
        return listSessions(userId, null);
    }

    /**
     * 根据关键字模糊查询会话列表，关键字匹配用户发送的消息内容。
     *
     * @param keyword 搜索关键字
     * @return 会话列表
     */
    @Override
    public List<ChatSession> listSessions(Long userId, String keyword) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (!StringUtils.hasText(keyword)) {
            Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"));
            List<ChatSession> sessions = mongoTemplate.find(query, ChatSession.class);
            return sessions == null ? Collections.emptyList() : sessions;
        }

        String escapedKeyword = escapeRegex(keyword.trim());
        Query messageQuery = new Query(new Criteria().andOperator(
            Criteria.where("userId").is(userId),
            Criteria.where("role").is(ChatDisplayMessageServiceImpl.ROLE_USER),
            Criteria.where("content").regex(escapedKeyword, "i")
        ));
        List<ChatDisplayMessage> matchedMessages = mongoTemplate.find(messageQuery, ChatDisplayMessage.class);
        if (matchedMessages == null || matchedMessages.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> matchedSessionIds = new LinkedHashSet<>();
        for (ChatDisplayMessage matchedMessage : matchedMessages) {
            if (matchedMessage.getSessionId() != null) {
                matchedSessionIds.add(matchedMessage.getSessionId());
            }
        }
        if (matchedSessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        Query sessionQuery = new Query(new Criteria().andOperator(
                Criteria.where("_id").in(matchedSessionIds),
                Criteria.where("userId").is(userId)
            ))
            .with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<ChatSession> sessions = mongoTemplate.find(sessionQuery, ChatSession.class);
        return sessions == null ? Collections.emptyList() : sessions;
    }

    /**
     * 管理员分页查询所有用户的会话标题列表。
     *
     * @param username 用户名查询条件
     * @param nickname 昵称查询条件
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 会话标题分页结果
     */
    @Override
    public PageResult<AdminChatSessionSummary> listAdminSessionSummaries(String username, String nickname, Integer pageNum, Integer pageSize) {
        int resolvedPageNum = resolvePageNum(pageNum);
        int resolvedPageSize = resolveAdminPageSize(pageSize);
        List<Long> userIds = findUserIds(username, nickname);
        if ((StringUtils.hasText(username) || StringUtils.hasText(nickname)) && userIds.isEmpty()) {
            return PageResult.<AdminChatSessionSummary>builder()
                .total(0L)
                .records(Collections.emptyList())
                .build();
        }

        Query countQuery = buildAdminSessionQuery(userIds);
        long total = mongoTemplate.count(countQuery, ChatSession.class);
        if (total == 0L) {
            return PageResult.<AdminChatSessionSummary>builder()
                .total(0L)
                .records(Collections.emptyList())
                .build();
        }

        Query pageQuery = buildAdminSessionQuery(userIds)
            .with(Sort.by(Sort.Direction.DESC, "updatedAt"))
            .skip((long) (resolvedPageNum - 1) * resolvedPageSize)
            .limit(resolvedPageSize);
        pageQuery.fields()
            .include("userId")
            .include("title")
            .include("createdAt")
            .include("updatedAt");
        List<ChatSession> sessions = mongoTemplate.find(pageQuery, ChatSession.class);
        return PageResult.<AdminChatSessionSummary>builder()
            .total(total)
            .records(toAdminSummaries(sessions))
            .build();
    }

    /**
     * 删除指定会话，并同步删除会话历史记录。
     *
     * @param sessionId 会话 ID
     */
    @Override
    public void deleteSession(Long sessionId, Long userId) {
        if (sessionId == null) {
            return;
        }

        ensureSession(sessionId, userId);
        chatDisplayMessageService.deleteMessagesBySessionId(sessionId, userId);
        mongoTemplate.remove(new Query(Criteria.where("_id").is(sessionId)), ChatMessages.class);
        mongoTemplate.remove(new Query(new Criteria().andOperator(
            Criteria.where("_id").is(sessionId),
            Criteria.where("userId").is(userId)
        )), ChatSession.class);
    }

    /**
     * 查询指定会话的元数据和展示消息。
     * 如果新展示集合中暂无数据，则从旧记忆集合中过滤 user/assistant 消息并迁移。
     *
     * @param sessionId 会话 ID
     * @return 会话详情
     */
    @Override
    public ChatSessionHistory getSessionHistory(Long sessionId, Long userId) {
        ChatSession session = ensureSession(sessionId, userId);
        List<ChatDisplayMessage> messages = chatDisplayMessageService.listMessages(sessionId, userId);
        if (messages.isEmpty()) {
            messages = migrateMessagesFromMemory(sessionId, userId);
        }
        return new ChatSessionHistory(session, messages);
    }

    /**
     * 生成会话 ID。
     *
     * @return 会话 ID
     */
    private Long generateAvailableSessionId() {
        long generatedId = System.currentTimeMillis();
        while (mongoTemplate.findById(generatedId, ChatSession.class) != null) {
            generatedId++;
        }
        return generatedId;
    }

    /**
     * 查询符合条件的用户 ID。
     *
     * @param username 用户名查询条件
     * @param nickname 昵称查询条件
     * @return 用户 ID 列表
     */
    private List<Long> findUserIds(String username, String nickname) {
        if (!StringUtils.hasText(username) && !StringUtils.hasText(nickname)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(User::getId);
        if (StringUtils.hasText(username)) {
            queryWrapper.like(User::getUsername, username.trim());
        }
        if (StringUtils.hasText(nickname)) {
            queryWrapper.like(User::getNickname, nickname.trim());
        }
        return userMapper.selectList(queryWrapper).stream()
            .map(User::getId)
            .toList();
    }

    /**
     * 转换管理员会话标题列表项。
     *
     * @param sessions 会话元数据列表
     * @return 管理员会话标题列表项
     */
    private List<AdminChatSessionSummary> toAdminSummaries(List<ChatSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = loadUserMap(sessions);
        List<AdminChatSessionSummary> summaries = new ArrayList<>();
        for (ChatSession session : sessions) {
            User user = userMap.get(session.getUserId());
            summaries.add(new AdminChatSessionSummary(
                session.getSessionId(),
                session.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
            ));
        }
        return summaries;
    }

    /**
     * 加载会话所属用户信息。
     *
     * @param sessions 会话元数据列表
     * @return 用户 ID 到用户信息的映射
     */
    private Map<Long, User> loadUserMap(List<ChatSession> sessions) {
        List<Long> userIds = sessions.stream()
            .map(ChatSession::getUserId)
            .filter(userId -> userId != null)
            .distinct()
            .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(User::getId, User::getUsername, User::getNickname)
            .in(User::getId, userIds);
        Map<Long, User> userMap = new HashMap<>();
        for (User user : userMapper.selectList(queryWrapper)) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    /**
     * 构造管理员会话查询。
     *
     * @param userIds 用户 ID 列表
     * @return Mongo 查询对象
     */
    private Query buildAdminSessionQuery(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new Query();
        }
        return new Query(Criteria.where("userId").in(userIds));
    }

    /**
     * 解析管理员分页页码。
     *
     * @param pageNum 页码
     * @return 有效页码
     */
    private int resolvePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    /**
     * 解析管理员分页每页条数。
     *
     * @param pageSize 每页条数
     * @return 有效每页条数
     */
    private int resolveAdminPageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        if (!ADMIN_PAGE_SIZE_OPTIONS.contains(pageSize)) {
            throw new IllegalArgumentException("pageSize must be 10, 15 or 20");
        }
        return pageSize;
    }

    /**
     * 校验会话是否属于当前登录用户。
     *
     * @param chatSession 会话元数据
     * @param userId 当前登录用户 ID
     */
    private void assertSessionOwner(ChatSession chatSession, Long userId) {
        if (chatSession.getUserId() == null) {
            chatSession.setUserId(userId);
            mongoTemplate.save(chatSession);
            return;
        }
        if (!chatSession.getUserId().equals(userId)) {
            throw new IllegalArgumentException("chat session does not belong to current user");
        }
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

        normalized = normalized.replaceFirst("^(你好|您好|请问|麻烦问一下|我想咨询一下|我想问一下|想咨询一下|想问一下)[，,？? ]*", "");
        normalized = normalized.replaceAll("\\s+", "");
        normalized = normalized.replaceAll("[。！!？?，,]+$", "");

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
    private List<ChatDisplayMessage> migrateMessagesFromMemory(Long sessionId, Long userId) {
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
                .userId(userId)
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

    /**
     * 对用户输入的关键字进行正则转义，避免模糊搜索误伤正则元字符。
     *
     * @param keyword 搜索关键字
     * @return 转义后的关键字
     */
    private String escapeRegex(String keyword) {
        return keyword.replaceAll("([\\\\.*+\\[\\](){}|^-])", "\\\\$1");
    }
}
