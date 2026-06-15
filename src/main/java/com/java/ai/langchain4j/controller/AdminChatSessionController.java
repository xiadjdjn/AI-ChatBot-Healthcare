package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.bean.AdminChatSessionSummary;
import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.bean.PageResult;
import com.java.ai.langchain4j.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员会话历史查看接口。
 */
@Tag(name = "Admin Chat Session")
@RestController
@RequestMapping("/admin/chat-sessions")
public class AdminChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 分页查看所有用户的会话标题列表，可按用户名和昵称筛选。
     *
     * @param username 用户名查询条件
     * @param nickname 昵称查询条件
     * @param pageNum 页码
     * @param pageSize 每页条数，仅支持 10、15、20
     * @return 会话标题分页结果
     */
    @Operation(summary = "管理员分页查看用户会话标题")
    @GetMapping
    public ApiResponse<PageResult<AdminChatSessionSummary>> list(
        @RequestParam(value = "username", required = false) String username,
        @RequestParam(value = "nickname", required = false) String nickname,
        @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success(chatSessionService.listAdminSessionSummaries(username, nickname, pageNum, pageSize));
    }
}
