package com.java.ai.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档列表/详情摘要对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentSummary {

    /**
     * 文档 ID。
     */
    private Long id;

    /**
     * 文档名称。
     */
    private String name;

    /**
     * 文档类型。
     */
    private String type;

    /**
     * 来源类型，例如 upload、manual。
     */
    private String sourceType;

    /**
     * 文档状态。
     */
    private String status;

    /**
     * 切片数量。
     */
    private Integer segmentCount;

    /**
     * 文档全文。
     */
    private String contentText;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
