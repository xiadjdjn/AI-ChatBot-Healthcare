package com.java.ai.langchain4j.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 包装原始检索器，记录本轮命中的知识来源名称。
 */
public class TracingContentRetriever implements ContentRetriever {

    private final ContentRetriever delegate;

    public TracingContentRetriever(ContentRetriever delegate) {
        this.delegate = delegate;
    }

    /**
     * 执行检索，并按会话和问题文本缓存命中的来源名称。
     *
     * @param query 检索问题
     * @return 命中的内容片段
     */
    @Override
    public List<Content> retrieve(Query query) {
        List<Content> contents = delegate.retrieve(query);
        RetrievalReferenceHolder.set(resolveSessionId(query), resolveQueryText(query), extractReferences(contents));
        return contents;
    }

    /**
     * 从检索查询中提取会话 ID。
     *
     * @param query 检索查询
     * @return 会话 ID
     */
    private Long resolveSessionId(Query query) {
        if (query == null || query.metadata() == null || query.metadata().chatMemoryId() == null) {
            return null;
        }

        Object chatMemoryId = query.metadata().chatMemoryId();
        if (chatMemoryId instanceof Long longId) {
            return longId;
        }
        if (chatMemoryId instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(chatMemoryId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从检索查询中提取问题文本。
     *
     * @param query 检索查询
     * @return 问题文本
     */
    private String resolveQueryText(Query query) {
        return query == null ? "" : query.text();
    }

    /**
     * 从命中片段中提取来源名称。
     *
     * @param contents 命中片段
     * @return 去重后的来源名称列表
     */
    private List<String> extractReferences(List<Content> contents) {
        Set<String> references = new LinkedHashSet<>();
        if (contents == null) {
            return new ArrayList<>();
        }

        for (Content content : contents) {
            String sourceName = resolveSourceName(content.textSegment());
            if (StringUtils.hasText(sourceName)) {
                references.add(sourceName);
            }
        }
        return new ArrayList<>(references);
    }

    /**
     * 优先从元数据标准键读取来源，读不到时从路径推导文件名。
     *
     * @param textSegment 命中文本片段
     * @return 来源名称
     */
    private String resolveSourceName(TextSegment textSegment) {
        if (textSegment == null) {
            return "";
        }

        Metadata metadata = textSegment.metadata();
        if (metadata == null) {
            return "";
        }

        String[] candidateKeys = {"source", "file_name", "filename", "title", "path"};
        for (String candidateKey : candidateKeys) {
            String value = metadata.getString(candidateKey);
            if (StringUtils.hasText(value)) {
                return simplifySourceName(value);
            }
        }
        return "";
    }

    /**
     * 将来源值规整成展示友好的名称。
     *
     * @param value 原始来源值
     * @return 规整后的来源名
     */
    private String simplifySourceName(String value) {
        String normalized = value.replace("\\", "/").trim();
        int lastSlashIndex = normalized.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < normalized.length() - 1) {
            return normalized.substring(lastSlashIndex + 1);
        }
        return normalized;
    }
}
