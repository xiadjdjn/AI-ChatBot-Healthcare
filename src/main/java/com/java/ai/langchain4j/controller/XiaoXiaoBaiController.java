package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.assistant.XiaoXiaoBaiAgent;
import com.java.ai.langchain4j.bean.ChatForm;
import com.java.ai.langchain4j.bean.ChatSession;
import com.java.ai.langchain4j.bean.ChatSessionHistory;
import com.java.ai.langchain4j.bean.CreateSessionForm;
import com.java.ai.langchain4j.rag.RetrievalReferenceHolder;
import com.java.ai.langchain4j.service.ChatDisplayMessageService;
import com.java.ai.langchain4j.service.ChatSessionService;
import com.java.ai.langchain4j.service.impl.ChatDisplayMessageServiceImpl;
import com.java.ai.langchain4j.util.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 小小白对话控制器，提供会话管理与聊天接口。
 */
@Tag(name = "XiaoXiaoBai AI")
@RestController
@RequestMapping("/xiaoxiaobai")
@Slf4j
public class XiaoXiaoBaiController {

    @Autowired
    private XiaoXiaoBaiAgent xiaoXiaoBaiAgent;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatDisplayMessageService chatDisplayMessageService;

    /**
     * 创建一个新的会话。
     *
     * @param createSessionForm 创建会话请求参数
     * @return 新建后的会话信息
     */
    @Operation(summary = "新建会话")
    @PostMapping("/sessions")
    public ChatSession createSession(@RequestBody(required = false) CreateSessionForm createSessionForm,
                                     HttpServletRequest request) {
        String title = createSessionForm == null ? null : createSessionForm.getTitle();
        return chatSessionService.createSession(currentUserId(request), title);
    }

    /**
     * 查询全部会话列表。
     *
     * @param keyword 搜索关键字，按用户消息内容模糊匹配
     * @return 会话列表
     */
    @Operation(summary = "会话列表")
    @GetMapping("/sessions")
    public List<ChatSession> listSessions(@RequestParam(value = "keyword", required = false) String keyword,
                                          HttpServletRequest request) {
        return chatSessionService.listSessions(currentUserId(request), keyword);
    }

    /**
     * 删除指定会话，并同步删除该会话的历史记录。
     * @param memoryId 会话 ID
     */
    @Operation(summary = "删除指定会话")
    @DeleteMapping("/sessions/{memoryId}")
    public void deleteSession(@PathVariable Long memoryId, HttpServletRequest request) {
        chatSessionService.deleteSession(memoryId, currentUserId(request));
    }

    /**
     * 查询指定会话的历史消息。
     * @param memoryId 会话 ID
     * @return 会话元数据和展示消息
     */
    @Operation(summary = "获取指定会话历史消息")
    @GetMapping("/sessions/{memoryId}/history")
    public ChatSessionHistory getSessionHistory(@PathVariable Long memoryId, HttpServletRequest request) {
        return chatSessionService.getSessionHistory(memoryId, currentUserId(request));
    }

    /**
     * 发送用户消息并返回流式回复，同时将展示消息写入独立集合。
     *
     * @param chatForm 对话请求参数
     * @return 模型流式输出结果
     */
    @Operation(summary = "Chat")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm, HttpServletRequest request) {
        log.info("用户信息: {}", chatForm);
        Long userId = currentUserId(request);
        String username = currentUsername(request);
        Long memoryId = resolveMemoryId(chatForm, userId);
        String userMessage = chatForm.getMessage();
        chatSessionService.ensureSession(memoryId, userId);
        chatSessionService.updateTitleIfAbsent(memoryId, userId, userMessage);
        RetrievalReferenceHolder.clear(memoryId, userMessage);
        UserContextHolder.set(userId, username);

        chatDisplayMessageService.saveMessage(memoryId, userId, ChatDisplayMessageServiceImpl.ROLE_USER, userMessage);
        chatSessionService.refreshSession(memoryId, userId, userMessage);

        StringBuilder assistantReplyBuilder = new StringBuilder();
        return xiaoXiaoBaiAgent.chat(memoryId, userMessage)
            .doOnNext(assistantReplyBuilder::append)
            .doOnComplete(() -> saveAssistantReply(memoryId, userId, assistantReplyBuilder.toString(), RetrievalReferenceHolder.get(memoryId, userMessage)))
            .doFinally(signalType -> {
                RetrievalReferenceHolder.clear(memoryId, userMessage);
                UserContextHolder.clear();
            });
    }

    /**
     * 在模型流式回复结束后保存完整回答。
     *
     * @param memoryId 会话 ID
     * @param assistantReply 大模型完整回复
     * @param references 命中的知识来源名称列表
     */
    private void saveAssistantReply(Long memoryId, Long userId, String assistantReply, List<String> references) {
        if (assistantReply == null || assistantReply.isBlank()) {
            return;
        }
        chatDisplayMessageService.saveMessage(memoryId, userId, ChatDisplayMessageServiceImpl.ROLE_ASSISTANT, assistantReply, references);
        chatSessionService.refreshSession(memoryId, userId, assistantReply);
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @param request HTTP 请求
     * @return 当前登录用户 ID
     */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long currentUserId) {
            return currentUserId;
        }
        throw new IllegalArgumentException("current user is not authenticated");
    }

    private String currentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username == null ? null : String.valueOf(username);
    }

    /**
     * 解析本轮对话使用的会话 ID。
     *
     * @param chatForm 对话请求参数
     * @param userId 当前登录用户 ID
     * @return 会话 ID
     */
    private Long resolveMemoryId(ChatForm chatForm, Long userId) {
        if (chatForm.getMemoryId() != null) {
            return chatForm.getMemoryId();
        }
        return chatSessionService.createSession(userId, null).getSessionId();
    }
}
