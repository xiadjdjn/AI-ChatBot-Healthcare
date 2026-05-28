package com.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.java.ai.langchain4j.bean.KnowledgeDocumentQueryForm;
import com.java.ai.langchain4j.bean.KnowledgeDocumentSummary;
import com.java.ai.langchain4j.bean.KnowledgeSegmentSummary;
import com.java.ai.langchain4j.bean.PageResult;
import com.java.ai.langchain4j.entity.KnowledgeDocument;
import com.java.ai.langchain4j.entity.KnowledgeSegment;
import com.java.ai.langchain4j.mapper.KnowledgeDocumentMapper;
import com.java.ai.langchain4j.mapper.KnowledgeSegmentMapper;
import com.java.ai.langchain4j.service.KnowledgeDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档元数据服务实现。
 */
@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private KnowledgeSegmentMapper knowledgeSegmentMapper;

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
    @Override
    public KnowledgeDocument createProcessingDocument(String name, String type, String sourceType, String storagePath, String contentText) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocument document = KnowledgeDocument.builder()
            .name(name)
            .type(type)
            .sourceType(sourceType)
            .storagePath(storagePath)
            .contentText(contentText)
            .status("PROCESSING")
            .segmentCount(0)
            .createdAt(now)
            .updatedAt(now)
            .build();
        knowledgeDocumentMapper.insert(document);
        return document;
    }

    /**
     * 将文档更新为入库成功状态。
     *
     * @param documentId 文档 ID
     * @param contentText 文档全文
     * @param segmentCount 切片数量
     */
    @Override
    public void markIngested(Long documentId, String contentText, int segmentCount) {
        KnowledgeDocument document = requireDocument(documentId);
        document.setContentText(contentText);
        document.setSegmentCount(segmentCount);
        document.setStatus("INGESTED");
        document.setRemark(null);
        document.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 将文档更新为入库失败状态。
     *
     * @param documentId 文档 ID
     * @param remark 失败原因
     */
    @Override
    public void markFailed(Long documentId, String remark) {
        KnowledgeDocument document = requireDocument(documentId);
        document.setStatus("FAILED");
        document.setRemark(remark);
        document.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 根据 ID 查询文档。
     *
     * @param documentId 文档 ID
     * @return 文档记录
     */
    @Override
    public KnowledgeDocument getById(Long documentId) {
        return requireDocument(documentId);
    }

    /**
     * 分页查询文档列表。
     *
     * @param queryForm 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<KnowledgeDocumentSummary> pageDocuments(KnowledgeDocumentQueryForm queryForm) {
        long pageNum = queryForm == null || queryForm.getPageNum() == null || queryForm.getPageNum() < 1 ? 1L : queryForm.getPageNum();
        long pageSize = queryForm == null || queryForm.getPageSize() == null || queryForm.getPageSize() < 1 ? 10L : queryForm.getPageSize();

        LambdaQueryWrapper<KnowledgeDocument> wrapper = buildQueryWrapper(queryForm);
        Long total = knowledgeDocumentMapper.selectCount(wrapper);
        if (total == null || total == 0) {
            return PageResult.<KnowledgeDocumentSummary>builder()
                .total(0L)
                .records(Collections.emptyList())
                .build();
        }

        long offset = (pageNum - 1) * pageSize;
        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt)
            .last("limit " + offset + "," + pageSize);
        List<KnowledgeDocumentSummary> records = knowledgeDocumentMapper.selectList(wrapper).stream()
            .map(this::toSummary)
            .collect(Collectors.toList());
        return PageResult.<KnowledgeDocumentSummary>builder()
            .total(total)
            .records(records)
            .build();
    }

    /**
     * 查询文档详情。
     *
     * @param documentId 文档 ID
     * @return 文档详情
     */
    @Override
    public KnowledgeDocumentSummary getDocumentDetail(Long documentId) {
        return toSummary(requireDocument(documentId));
    }

    /**
     * 查询文档切片列表。
     *
     * @param documentId 文档 ID
     * @return 切片摘要列表
     */
    @Override
    public List<KnowledgeSegmentSummary> listSegmentSummaries(Long documentId) {
        return listSegments(documentId).stream()
            .map(segment -> KnowledgeSegmentSummary.builder()
                .segmentIndex(segment.getSegmentIndex())
                .contentPreview(segment.getContentPreview())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 查询文档切片映射。
     *
     * @param documentId 文档 ID
     * @return 切片记录
     */
    @Override
    public List<KnowledgeSegment> listSegments(Long documentId) {
        LambdaQueryWrapper<KnowledgeSegment> wrapper = new LambdaQueryWrapper<KnowledgeSegment>()
            .eq(KnowledgeSegment::getDocumentId, documentId)
            .orderByAsc(KnowledgeSegment::getSegmentIndex);
        List<KnowledgeSegment> segments = knowledgeSegmentMapper.selectList(wrapper);
        return segments == null ? Collections.emptyList() : segments;
    }

    /**
     * 批量保存切片映射。
     *
     * @param segments 切片记录
     */
    @Override
    public void saveSegments(List<KnowledgeSegment> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return;
        }
        for (KnowledgeSegment segment : segments) {
            knowledgeSegmentMapper.insert(segment);
        }
    }

    /**
     * 删除文档关联切片。
     *
     * @param documentId 文档 ID
     */
    @Override
    public void deleteSegmentsByDocumentId(Long documentId) {
        LambdaQueryWrapper<KnowledgeSegment> wrapper = new LambdaQueryWrapper<KnowledgeSegment>()
            .eq(KnowledgeSegment::getDocumentId, documentId);
        knowledgeSegmentMapper.delete(wrapper);
    }

    /**
     * 删除文档元数据。
     *
     * @param documentId 文档 ID
     */
    @Override
    public void deleteDocument(Long documentId) {
        if (knowledgeDocumentMapper.deleteById(documentId) == 0) {
            throw new IllegalArgumentException("knowledge document not found: " + documentId);
        }
    }

    /**
     * 构建列表查询条件。
     *
     * @param queryForm 查询条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<KnowledgeDocument> buildQueryWrapper(KnowledgeDocumentQueryForm queryForm) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        if (queryForm == null) {
            return wrapper;
        }
        if (StringUtils.hasText(queryForm.getKeyword())) {
            wrapper.like(KnowledgeDocument::getName, queryForm.getKeyword().trim());
        }
        if (StringUtils.hasText(queryForm.getStatus())) {
            wrapper.eq(KnowledgeDocument::getStatus, queryForm.getStatus().trim());
        }
        return wrapper;
    }

    /**
     * 将实体转为接口摘要对象。
     *
     * @param document 文档实体
     * @return 摘要对象
     */
    private KnowledgeDocumentSummary toSummary(KnowledgeDocument document) {
        return KnowledgeDocumentSummary.builder()
            .id(document.getId())
            .name(document.getName())
            .type(document.getType())
            .sourceType(document.getSourceType())
            .status(document.getStatus())
            .segmentCount(document.getSegmentCount())
            .contentText(document.getContentText())
            .remark(document.getRemark())
            .createdAt(document.getCreatedAt())
            .build();
    }

    /**
     * 校验并获取文档实体。
     *
     * @param documentId 文档 ID
     * @return 文档实体
     */
    private KnowledgeDocument requireDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("knowledge document not found: " + documentId);
        }
        return document;
    }
}
