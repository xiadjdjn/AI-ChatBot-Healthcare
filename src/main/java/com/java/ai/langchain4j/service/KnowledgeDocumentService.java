package com.java.ai.langchain4j.service;

import com.java.ai.langchain4j.bean.KnowledgeDocumentQueryForm;
import com.java.ai.langchain4j.bean.KnowledgeDocumentSummary;
import com.java.ai.langchain4j.bean.KnowledgeSegmentSummary;
import com.java.ai.langchain4j.bean.PageResult;
import com.java.ai.langchain4j.entity.KnowledgeDocument;
import com.java.ai.langchain4j.entity.KnowledgeSegment;

import java.util.List;

/**
 * 知识库文档元数据服务。
 */
public interface KnowledgeDocumentService {

    /**
     * 新建处理中状态的文档记录。
     *
     * @param name 文档名称
     * @param type 文档类型
     * @param sourceType 来源类型
     * @param storagePath 存储路径
     * @param contentText 原始文本
     * @return 文档记录
     */
    KnowledgeDocument createProcessingDocument(String name, String type, String sourceType, String storagePath, String contentText);

    /**
     * 将文档更新为入库成功状态。
     *
     * @param documentId 文档 ID
     * @param contentText 文档全文
     * @param segmentCount 切片数量
     */
    void markIngested(Long documentId, String contentText, int segmentCount);

    /**
     * 将文档更新为入库失败状态。
     *
     * @param documentId 文档 ID
     * @param remark 失败原因
     */
    void markFailed(Long documentId, String remark);

    /**
     * 根据 ID 查询文档。
     *
     * @param documentId 文档 ID
     * @return 文档记录
     */
    KnowledgeDocument getById(Long documentId);

    /**
     * 分页查询文档列表。
     *
     * @param queryForm 查询条件
     * @return 分页结果
     */
    PageResult<KnowledgeDocumentSummary> pageDocuments(KnowledgeDocumentQueryForm queryForm);

    /**
     * 查询文档详情。
     *
     * @param documentId 文档 ID
     * @return 文档详情
     */
    KnowledgeDocumentSummary getDocumentDetail(Long documentId);

    /**
     * 查询文档切片列表。
     *
     * @param documentId 文档 ID
     * @return 切片摘要列表
     */
    List<KnowledgeSegmentSummary> listSegmentSummaries(Long documentId);

    /**
     * 查询文档切片映射。
     *
     * @param documentId 文档 ID
     * @return 切片记录
     */
    List<KnowledgeSegment> listSegments(Long documentId);

    /**
     * 批量保存切片映射。
     *
     * @param segments 切片记录
     */
    void saveSegments(List<KnowledgeSegment> segments);

    /**
     * 删除文档关联切片。
     *
     * @param documentId 文档 ID
     */
    void deleteSegmentsByDocumentId(Long documentId);

    /**
     * 删除文档元数据。
     *
     * @param documentId 文档 ID
     */
    void deleteDocument(Long documentId);
}
