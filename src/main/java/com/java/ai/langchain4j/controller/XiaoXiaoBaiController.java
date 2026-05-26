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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /**
     * 小小白智能体接口。
     */
    @Autowired
    private XiaoXiaoBaiAgent xiaoXiaoBaiAgent;

    /**
     * 会话管理服务。
     */
    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 前端展示消息服务。
     */
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
    public ChatSession createSession(@RequestBody(required = false) CreateSessionForm createSessionForm) {
        Long memoryId = createSessionForm == null ? null : createSessionForm.getMemoryId();
        String title = createSessionForm == null ? null : createSessionForm.getTitle();
        return chatSessionService.createSession(memoryId, title);
    }

    /**
     * 查询全部会话列表。
     *
     * @return 会话列表
     */
    @Operation(summary = "会话列表")
    @GetMapping("/sessions")
    public List<ChatSession> listSessions() {
        return chatSessionService.listSessions();
    }

    /**
     * 删除指定会话，并同步删除该会话的历史记录。
     *
     * @param memoryId 会话 ID
     */
    @Operation(summary = "删除指定会话")
    @DeleteMapping("/sessions/{memoryId}")
    public void deleteSession(@PathVariable Long memoryId) {
        chatSessionService.deleteSession(memoryId);
    }

    /**
     * 查询指定会话的历史消息。
     *
     * @param memoryId 会话 ID
     * @return 会话元数据和展示消息
     */
    @Operation(summary = "获取指定会话历史消息")
    @GetMapping("/sessions/{memoryId}/history")
    public ChatSessionHistory getSessionHistory(@PathVariable Long memoryId) {
        return chatSessionService.getSessionHistory(memoryId);
    }

    /**
     * 发送用户消息并返回流式回复，同时将展示消息写入独立集合。
     *
     * @param chatForm 对话请求参数
     * @return 模型流式输出结果
     */
    @Operation(summary = "Chat")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        log.info("用户信息: {}", chatForm);
        Long memoryId = chatForm.getMemoryId();
        String userMessage = chatForm.getMessage();
        chatSessionService.ensureSession(memoryId);
        chatSessionService.updateTitleIfAbsent(memoryId, userMessage);
        RetrievalReferenceHolder.clear(memoryId, userMessage);

        chatDisplayMessageService.saveMessage(memoryId, ChatDisplayMessageServiceImpl.ROLE_USER, userMessage);
        chatSessionService.refreshSession(memoryId, userMessage);

        StringBuilder assistantReplyBuilder = new StringBuilder();
        return xiaoXiaoBaiAgent.chat(memoryId, userMessage)
            .doOnNext(assistantReplyBuilder::append)
            .doOnComplete(() -> saveAssistantReply(memoryId, assistantReplyBuilder.toString(), RetrievalReferenceHolder.get(memoryId, userMessage)))
            .doFinally(signalType -> RetrievalReferenceHolder.clear(memoryId, userMessage));
    }

    /**
     * 在模型流式回复结束后保存完整回答。
     *
     * @param memoryId 会话 ID
     * @param assistantReply 大模型完整回复
     * @param references 命中的知识来源名称列表
     */
    private void saveAssistantReply(Long memoryId, String assistantReply, List<String> references) {
        if (assistantReply == null || assistantReply.isBlank()) {
            return;
        }
        chatDisplayMessageService.saveMessage(memoryId, ChatDisplayMessageServiceImpl.ROLE_ASSISTANT, assistantReply, references);
        chatSessionService.refreshSession(memoryId, assistantReply);
    }
}
