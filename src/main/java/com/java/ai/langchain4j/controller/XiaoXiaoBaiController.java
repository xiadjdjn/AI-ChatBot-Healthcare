package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.assistant.XiaoXiaoBaiAgent;
import com.java.ai.langchain4j.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "小小白AI")
@RestController
@RequestMapping("/xiaoxiaobai")
@Slf4j
public class XiaoXiaoBaiController {
    @Autowired
    private XiaoXiaoBaiAgent xiaoXiaoBaiAgent;

    @Operation(summary = "对话")
    @PostMapping(value = "/chat",produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm){
        log.info("用户信息：{}", chatForm);
        return xiaoXiaoBaiAgent.chat(chatForm.getMemoryId(), chatForm.getMessage());
    }
}
