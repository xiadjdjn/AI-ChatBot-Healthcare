package com.java.ai.langchain4j.service.impl;

import com.java.ai.langchain4j.bean.KnowledgeDocumentSummary;
import com.java.ai.langchain4j.entity.KnowledgeDocument;
import com.java.ai.langchain4j.entity.KnowledgeSegment;
import com.java.ai.langchain4j.service.KnowledgeDocumentService;
import com.java.ai.langchain4j.service.KnowledgeIngestService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库文档入库服务实现。
 */
@Service
public class KnowledgeIngestServiceImpl implements KnowledgeIngestService {

    private static final String SOURCE_TYPE_UPLOAD = "upload";
    private static final String SOURCE_TYPE_MANUAL = "manual";
    private static final int SEGMENT_SIZE = 300;
    private static final int SEGMENT_OVERLAP = 30;
    private static final int PREVIEW_MAX_LENGTH = 200;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${app.knowledge.storage-path:./data/knowledge}")
    private String knowledgeStoragePath;

    private DocumentSplitter documentSplitter;

    /**
     * 初始化切片器和存储目录。
     *
     * @throws IOException 创建目录失败时抛出
     */
    @PostConstruct
    public void init() throws IOException {
        this.documentSplitter = DocumentSplitters.recursive(SEGMENT_SIZE, SEGMENT_OVERLAP);
        Files.createDirectories(Paths.get(knowledgeStoragePath));
    }

    /**
     * 上传文件并写入知识库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentSummary ingestUploadedFile(MultipartFile file, String name) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String documentName = StringUtils.hasText(name) ? name.trim() : originalFilename;
        String type = resolveType(documentName, originalFilename);
        Path storedPath = storeFile(file, type);
        KnowledgeDocument document = knowledgeDocumentService.createProcessingDocument(
            documentName,
            type,
            SOURCE_TYPE_UPLOAD,
            storedPath.toString(),
            null
        );
        try {
            String content = parseStoredFile(storedPath, type);
            return ingestDocument(document, content);
        } catch (Exception e) {
            knowledgeDocumentService.markFailed(document.getId(), e.getMessage());
            throw new IllegalStateException("knowledge document ingest failed", e);
        }
    }

    /**
     * 将手工录入文本写入知识库。
     *
     * @param name 文档名称
     * @param content 文本内容
     * @return 入库结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentSummary ingestText(String name, String content) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content cannot be blank");
        }

        KnowledgeDocument document = knowledgeDocumentService.createProcessingDocument(
            name.trim(),
            "text",
            SOURCE_TYPE_MANUAL,
            null,
            content
        );
        try {
            return ingestDocument(document, content);
        } catch (Exception e) {
            knowledgeDocumentService.markFailed(document.getId(), e.getMessage());
            throw new IllegalStateException("knowledge text ingest failed", e);
        }
    }

    /**
     * 重新入库指定文档。
     *
     * @param documentId 文档 ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reingest(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
        removeVectors(documentId);
        knowledgeDocumentService.deleteSegmentsByDocumentId(documentId);

        String content = loadDocumentContent(document);
        try {
            ingestExistingDocument(document, content);
            return true;
        } catch (Exception e) {
            knowledgeDocumentService.markFailed(documentId, e.getMessage());
            throw new IllegalStateException("knowledge document reingest failed", e);
        }
    }

    /**
     * 删除指定文档及其向量数据。
     *
     * @param documentId 文档 ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
        removeVectors(documentId);
        knowledgeDocumentService.deleteSegmentsByDocumentId(documentId);
        knowledgeDocumentService.deleteDocument(documentId);
        deleteLocalFile(document.getStoragePath());
        return true;
    }

    /**
     * 执行新文档入库。
     *
     * @param document 文档记录
     * @param content 文档内容
     * @return 文档摘要
     */
    private KnowledgeDocumentSummary ingestDocument(KnowledgeDocument document, String content) {
        ingestExistingDocument(document, content);
        return knowledgeDocumentService.getDocumentDetail(document.getId());
    }

    /**
     * 将现有文档重新切片并写入向量库。
     *
     * @param document 文档记录
     * @param content 文档全文
     */
    private void ingestExistingDocument(KnowledgeDocument document, String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("document content cannot be blank");
        }

        List<TextSegment> segments = splitDocument(document, content);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("document segments cannot be empty");
        }

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        List<String> vectorIds = buildVectorIds(document.getId(), segments);
        embeddingStore.addAll(vectorIds, embeddings, segments);

        knowledgeDocumentService.saveSegments(buildSegmentRecords(document.getId(), vectorIds, segments));
        knowledgeDocumentService.markIngested(document.getId(), content, segments.size());
    }

    /**
     * 切分文档内容并附带元数据。
     *
     * @param document 文档记录
     * @param content 文档全文
     * @return 文本切片列表
     */
    private List<TextSegment> splitDocument(KnowledgeDocument document, String content) {
        Metadata metadata = new Metadata()
            .put(Document.FILE_NAME, document.getName())
            .put(Document.ABSOLUTE_DIRECTORY_PATH, resolveDirectoryPath(document.getStoragePath()))
            .put("documentId", document.getId())
            .put("documentName", document.getName())
            .put("source", document.getName());
        return documentSplitter.split(Document.from(content, metadata));
    }

    /**
     * 构造 Pinecone 向量 ID。
     *
     * @param documentId 文档 ID
     * @param segments 文本切片
     * @return 向量 ID 列表
     */
    private List<String> buildVectorIds(Long documentId, List<TextSegment> segments) {
        List<String> vectorIds = new ArrayList<>(segments.size());
        for (TextSegment segment : segments) {
            Integer segmentIndex = resolveSegmentIndex(segment);
            vectorIds.add("doc-" + documentId + "-seg-" + segmentIndex);
        }
        return vectorIds;
    }

    /**
     * 构造切片映射记录。
     *
     * @param documentId 文档 ID
     * @param vectorIds 向量 ID 列表
     * @param segments 文本切片
     * @return 切片记录列表
     */
    private List<KnowledgeSegment> buildSegmentRecords(Long documentId, List<String> vectorIds, List<TextSegment> segments) {
        LocalDateTime now = LocalDateTime.now();
        List<KnowledgeSegment> records = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            records.add(KnowledgeSegment.builder()
                .documentId(documentId)
                .segmentIndex(resolveSegmentIndex(segment))
                .vectorId(vectorIds.get(i))
                .contentPreview(buildPreview(segment.text()))
                .createdAt(now)
                .build());
        }
        return records;
    }

    /**
     * 删除 Pinecone 中的向量。
     *
     * @param documentId 文档 ID
     */
    private void removeVectors(Long documentId) {
        List<String> vectorIds = knowledgeDocumentService.listSegments(documentId).stream()
            .map(KnowledgeSegment::getVectorId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        if (!vectorIds.isEmpty()) {
            embeddingStore.removeAll(vectorIds);
        }
    }

    /**
     * 从存储文件或数据库内容中加载文档全文。
     *
     * @param document 文档记录
     * @return 文档全文
     */
    private String loadDocumentContent(KnowledgeDocument document) {
        if (StringUtils.hasText(document.getStoragePath())) {
            return parseStoredFile(Paths.get(document.getStoragePath()), document.getType());
        }
        return document.getContentText();
    }

    /**
     * 保存上传文件到本地目录。
     *
     * @param file 上传文件
     * @param type 文件类型
     * @return 存储路径
     */
    private Path storeFile(MultipartFile file, String type) {
        String fileName = UUID.randomUUID() + "." + type;
        Path targetPath = Paths.get(knowledgeStoragePath).resolve(fileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath;
        } catch (IOException e) {
            throw new IllegalStateException("store knowledge file failed", e);
        }
    }

    /**
     * 解析本地存储文件内容。
     *
     * @param path 文件路径
     * @param type 文件类型
     * @return 解析出的文本
     */
    private String parseStoredFile(Path path, String type) {
        try {
            if ("pdf".equalsIgnoreCase(type)) {
                return parsePdf(path);
            }
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("parse knowledge file failed", e);
        }
    }

    /**
     * 解析 PDF 文件内容。
     *
     * @param path PDF 路径
     * @return 提取出的文本
     */
    private String parsePdf(Path path) {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("parse pdf failed", e);
        }
    }

    /**
     * 根据名称推导文档类型。
     *
     * @param displayName 展示名称
     * @param originalFilename 原始文件名
     * @return 文档类型
     */
    private String resolveType(String displayName, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(displayName);
        if (!StringUtils.hasText(extension)) {
            extension = StringUtils.getFilenameExtension(originalFilename);
        }
        return StringUtils.hasText(extension) ? extension.toLowerCase() : "txt";
    }

    /**
     * 提取切片索引。
     *
     * @param segment 文本切片
     * @return 切片索引
     */
    private Integer resolveSegmentIndex(TextSegment segment) {
        Integer segmentIndex = segment.metadata() == null ? null : segment.metadata().getInteger("index");
        return segmentIndex == null ? 0 : segmentIndex;
    }

    /**
     * 截取切片预览。
     *
     * @param text 原文
     * @return 预览文本
     */
    private String buildPreview(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.length() <= PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_MAX_LENGTH);
    }

    /**
     * 提取文件所在目录。
     *
     * @param storagePath 存储路径
     * @return 目录字符串
     */
    private String resolveDirectoryPath(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return "";
        }
        Path path = Paths.get(storagePath).getParent();
        return path == null ? "" : path.toString();
    }

    /**
     * 删除本地文件。
     *
     * @param storagePath 存储路径
     */
    private void deleteLocalFile(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException ignored) {
        }
    }
}
