package com.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库切片与向量映射表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_segment")
public class KnowledgeSegment {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档 ID。
     */
    private Long documentId;

    /**
     * 切片序号。
     */
    private Integer segmentIndex;

    /**
     * Pinecone 中的向量 ID。
     */
    private String vectorId;

    /**
     * 切片预览内容。
     */
    private String contentPreview;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
