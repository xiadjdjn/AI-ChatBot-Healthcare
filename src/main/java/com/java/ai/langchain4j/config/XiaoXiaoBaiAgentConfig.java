package com.java.ai.langchain4j.config;

import com.java.ai.langchain4j.rag.TracingContentRetriever;
import com.java.ai.langchain4j.store.MongoChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XiaoXiaoBaiAgentConfig {
    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;

    /**
     * 设置记忆存储--Mongo
     *
     * @return 聊天记忆提供器
     */
    @Bean
    ChatMemoryProvider chatMemoryProviderXiaoXiaoBai() {
        return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(20)
            .chatMemoryStore(mongoChatMemoryStore)
            .build();
    }

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore embeddingStore;

    /**
     * 设置向量检索器，并记录命中的知识来源。
     *
     * @return 内容检索器
     */
    @Bean
    ContentRetriever contentRetrieverXiaoXiaoBaiPincone() {
        ContentRetriever delegate = EmbeddingStoreContentRetriever
            .builder()
            // 设置用于生成嵌入向量的嵌入模型
            .embeddingModel(embeddingModel)
            // 指定要使用的嵌入存储
            .embeddingStore(embeddingStore)
            // 设置最大检索结果数量，这里表示最多返回 4 条匹配结果
            .maxResults(1)
            // 设置最小得分阈值，只有得分大于等于 0.6 的结果才会被返回
            .minScore(0.8)
            .build();
        //使用自定义信息来源追踪的包装器，记录本轮命中的知识来源名称
        return new TracingContentRetriever(delegate);
    }
}
