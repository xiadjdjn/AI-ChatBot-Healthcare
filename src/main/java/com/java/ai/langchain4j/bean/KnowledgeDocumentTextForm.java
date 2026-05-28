package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 手工录入知识库文本请求。
 */
@Data
public class KnowledgeDocumentTextForm {

    /**
     * 文档名称。
     */
    private String name;

    /**
     * 文本内容。
     */
    private String content;
}
