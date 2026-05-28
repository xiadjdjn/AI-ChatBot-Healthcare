package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库切片展示对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSegmentSummary {

    /**
     * 切片序号。
     */
    private Integer segmentIndex;

    /**
     * 切片预览内容。
     */
    private String contentPreview;
}
