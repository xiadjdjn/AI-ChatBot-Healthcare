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
 * 知识库文档元数据表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_document")
public class KnowledgeDocument {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档名称。
     */
    private String name;

    /**
     * 文档类型，例如 txt、md、pdf。
     */
    private String type;

    /**
     * 来源类型，例如 upload、manual。
     */
    private String sourceType;

    /**
     * 本地存储路径。
     */
    private String storagePath;

    /**
     * 文档全文内容。
     */
    private String contentText;

    /**
     * 文档状态：UPLOADED、PROCESSING、INGESTED、FAILED。
     */
    private String status;

    /**
     * 切片数量。
     */
    private Integer segmentCount;

    /**
     * 失败原因或备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
