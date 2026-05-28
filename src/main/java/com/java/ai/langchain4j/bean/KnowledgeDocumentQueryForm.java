package com.java.ai.langchain4j.bean;

import lombok.Data;

/**
 * 知识库文档分页查询参数。
 */
@Data
public class KnowledgeDocumentQueryForm {

     //页码。
    private Long pageNum = 1L;

     //每页大小。
    private Long pageSize = 10L;

     //名称关键字
    private String keyword;


     //文档状态。
    private String status;
}
