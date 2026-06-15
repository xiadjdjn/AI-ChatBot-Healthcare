package com.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
    wiringMode= EXPLICIT,
    //chatModel = "qwenChatModel",
    streamingChatModel = "qwenStreamingChatModel", //配置流式输出模型
    chatMemoryProvider = "chatMemoryProviderXiaoXiaoBai",   //配置聊天记忆存储--使用MongoDB存储聊天记录
    tools = {"appointmentTools", "doctorDutyTools"},
    contentRetriever = "contentRetrieverXiaoXiaoBaiPincone"    //配置向量存储--使用Pincone
)
public interface XiaoXiaoBaiAgent {

    @SystemMessage(fromResource = "xiaoxiaobai-prompt-template.txt")
    Flux<String> chat(@MemoryId Long id, @UserMessage String userMessage);

}
