package com.java.ai.langchain4j.rag;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存当前会话命中的知识来源列表。
 */
public final class RetrievalReferenceHolder {

    private static final Map<String, List<String>> REFERENCES = new ConcurrentHashMap<>();

    private RetrievalReferenceHolder() {
    }

    /**
     * 写入指定请求的知识来源列表。
     *
     * @param sessionId 会话 ID
     * @param queryText 本轮问题文本
     * @param references 来源列表
     */
    public static void set(Long sessionId, String queryText, List<String> references) {
        List<String> normalizedReferences = references == null ? Collections.emptyList() : references;
        String key = buildKey(sessionId, queryText);
        if (StringUtils.hasText(key)) {
            REFERENCES.put(key, normalizedReferences);
        }

        String sessionKey = buildSessionKey(sessionId);
        if (StringUtils.hasText(sessionKey) && !normalizedReferences.isEmpty()) {
            REFERENCES.put(sessionKey, normalizedReferences);
        }
    }

    /**
     * 获取指定请求的知识来源列表。
     *
     * @param sessionId 会话 ID
     * @param queryText 本轮问题文本
     * @return 来源列表
     */
    public static List<String> get(Long sessionId, String queryText) {
        String key = buildKey(sessionId, queryText);
        if (StringUtils.hasText(key)) {
            List<String> references = REFERENCES.get(key);
            if (references != null && !references.isEmpty()) {
                return references;
            }
        }

        String sessionKey = buildSessionKey(sessionId);
        if (StringUtils.hasText(sessionKey)) {
            return REFERENCES.getOrDefault(sessionKey, Collections.emptyList());
        }
        return Collections.emptyList();
    }

    /**
     * 清理指定请求缓存。
     *
     * @param sessionId 会话 ID
     * @param queryText 本轮问题文本
     */
    public static void clear(Long sessionId, String queryText) {
        String key = buildKey(sessionId, queryText);
        if (StringUtils.hasText(key)) {
            REFERENCES.remove(key);
        }

        String sessionKey = buildSessionKey(sessionId);
        if (StringUtils.hasText(sessionKey)) {
            REFERENCES.remove(sessionKey);
        }
    }

    /**
     * 构造请求关联键，优先使用会话 ID + 问题文本。
     *
     * @param sessionId 会话 ID
     * @param queryText 本轮问题文本
     * @return 关联键
     */
    private static String buildKey(Long sessionId, String queryText) {
        String normalizedQueryText = queryText == null ? "" : queryText.trim();
        if (sessionId != null) {
            return sessionId + "::" + normalizedQueryText;
        }
        return normalizedQueryText;
    }

    /**
     * 构造会话级兜底键，用于处理检索查询被框架改写导致文本不一致的情况。
     *
     * @param sessionId 会话 ID
     * @return 会话级关联键
     */
    private static String buildSessionKey(Long sessionId) {
        return sessionId == null ? "" : sessionId + "::LATEST";
    }
}
