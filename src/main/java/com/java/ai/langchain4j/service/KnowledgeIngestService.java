package com.java.ai.langchain4j.service;

import com.java.ai.langchain4j.bean.KnowledgeDocumentSummary;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档入库服务。
 */
public interface KnowledgeIngestService {

    /**
     * 上传文件并写入知识库。
     */
    KnowledgeDocumentSummary ingestUploadedFile(MultipartFile file, String name);

    /**
     * 将手工录入文本写入知识库。
     */
    KnowledgeDocumentSummary ingestText(String name, String content);

    /**
     * 重新入库指定文档。
     *
     * @param documentId 文档 ID
     * @return 是否成功
     */
    boolean reingest(Long documentId);

    /**
     * 删除指定文档及其向量数据。
     *
     * @param documentId 文档 ID
     * @return 是否成功
     */
    boolean deleteDocument(Long documentId);
}
