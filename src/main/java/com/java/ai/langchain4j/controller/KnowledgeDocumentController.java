package com.java.ai.langchain4j.controller;

import com.java.ai.langchain4j.bean.ApiResponse;
import com.java.ai.langchain4j.bean.KnowledgeDocumentQueryForm;
import com.java.ai.langchain4j.bean.KnowledgeDocumentSummary;
import com.java.ai.langchain4j.bean.KnowledgeDocumentTextForm;
import com.java.ai.langchain4j.bean.KnowledgeSegmentSummary;
import com.java.ai.langchain4j.bean.PageResult;
import com.java.ai.langchain4j.service.KnowledgeDocumentService;
import com.java.ai.langchain4j.service.KnowledgeIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档管理控制器。
 */
@Tag(name = "Knowledge Document")
@RestController
@RequestMapping("/knowledge/documents")
public class KnowledgeDocumentController {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private KnowledgeIngestService knowledgeIngestService;

    /**
     * 上传文件并写入知识库。
     *
     * @param file 上传文件
     * @param name 文档名称
     * @return 入库结果
     */
    @Operation(summary = "上传文件并入库")
    @PostMapping("/upload")
    public ApiResponse<KnowledgeDocumentSummary> upload(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(value = "name", required = false) String name) {
        return ApiResponse.success(knowledgeIngestService.ingestUploadedFile(file, name));
    }

    /**
     * 将手工录入文本写入知识库。
     */
    @Operation(summary = "手工录入文本并入库")
    @PostMapping("/text")
    public ApiResponse<KnowledgeDocumentSummary> ingestText(@RequestBody KnowledgeDocumentTextForm form) {
        return ApiResponse.success(knowledgeIngestService.ingestText(form.getName(), form.getContent()));
    }

    /**
     * 分页查询知识库文档。
     */
    @Operation(summary = "分页查询文档")
    @GetMapping
    public ApiResponse<PageResult<KnowledgeDocumentSummary>> pageDocuments(@RequestParam(value = "pageNum", required = false) Long pageNum,
                                                                           @RequestParam(value = "pageSize", required = false) Long pageSize,
                                                                           @RequestParam(value = "keyword", required = false) String keyword,
                                                                           @RequestParam(value = "status", required = false) String status) {
        KnowledgeDocumentQueryForm queryForm = new KnowledgeDocumentQueryForm();
        queryForm.setPageNum(pageNum);
        queryForm.setPageSize(pageSize);
        queryForm.setKeyword(keyword);
        queryForm.setStatus(status);
        return ApiResponse.success(knowledgeDocumentService.pageDocuments(queryForm));
    }

    /**
     * 查询文档详情。
     *
     * @param id 文档 ID
     * @return 文档详情
     */
    @Operation(summary = "查询文档详情")
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocumentSummary> getDetail(@PathVariable Long id) {
        return ApiResponse.success(knowledgeDocumentService.getDocumentDetail(id));
    }

    /**
     * 查询文档切片列表。
     *
     * @param id 文档 ID
     * @return 切片列表
     */
    @Operation(summary = "查询文档切片列表")
    @GetMapping("/{id}/segments")
    public ApiResponse<List<KnowledgeSegmentSummary>> listSegments(@PathVariable Long id) {
        return ApiResponse.success(knowledgeDocumentService.listSegmentSummaries(id));
    }

    /**
     * 重新入库指定文档。
     *
     * @param id 文档 ID
     * @return 执行结果
     */
    @Operation(summary = "重新入库")
    @PostMapping("/{id}/reingest")
    public ApiResponse<Boolean> reingest(@PathVariable Long id) {
        return ApiResponse.success("reingest success", knowledgeIngestService.reingest(id));
    }

    /**
     * 删除指定知识库文档。
     *
     * @param id 文档 ID
     * @return 执行结果
     */
    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success("delete success", knowledgeIngestService.deleteDocument(id));
    }
}
