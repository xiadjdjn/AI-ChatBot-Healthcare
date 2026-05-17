# AI-ChatBot-Healthcare

基于 Spring Boot 3 和 LangChain4j 构建的医疗问答 RAG 项目，面向医疗咨询场景提供流式对话、对话记忆、知识库检索以及预约挂号工具调用能力。

## 项目简介

项目核心目标是实现一个医疗助手 `XiaoXiaoBai`，让用户能够通过统一接口完成以下能力：

- 医疗问答与多轮对话
- 基于 RAG 的知识检索增强回答
- 基于 MongoDB 的聊天记忆持久化
- 基于 Pinecone 的向量存储与检索
- 基于工具调用的预约挂号与取消挂号

当前项目入口为 Spring Boot 应用，主要对外提供一个流式聊天接口。

## 技术栈

- Java 17
- Spring Boot 3.2.6
- LangChain4j 1.0.0-beta3
- Spring WebFlux
- MongoDB
- MyBatis-Plus
- MySQL
- Pinecone
- Knife4j / OpenAPI

## 项目结构

```text
src/main/java/com/java/ai/langchain4j
├─ assistant    # AI Service 接口定义
├─ bean         # 请求与消息对象
├─ config       # 向量库、Agent、记忆等配置
├─ controller   # HTTP 接口
├─ entity       # 业务实体
├─ mapper       # MyBatis Mapper
├─ service      # 业务服务
├─ store        # 自定义聊天记忆存储
└─ tools        # LangChain4j Tool 工具
```

## 核心能力说明

### 1. 流式医疗问答

控制器 `XiaoXiaoBaiController` 暴露 `/xiaoxiaobai/chat` 接口，返回 `Flux<String>`，用于将大模型响应以流式方式输出给前端。

### 2. 对话记忆

项目使用 `MessageWindowChatMemory` 管理会话窗口，并通过自定义 `MongoChatMemoryStore` 将聊天记录持久化到 MongoDB，支持基于 `memoryId` 的多轮对话。

### 3. RAG 检索增强

项目通过 `EmbeddingStoreContentRetriever` 从 Pinecone 向量库中检索相关知识片段，为医疗问答提供外部知识支持。

### 4. 工具调用

项目注册了预约相关工具，支持：

- 查询是否有号源
- 预约挂号
- 取消挂号

相关能力由 LangChain4j Tool 机制接入到 Agent 中。

## 主要代码说明

- `XiaoXiaoBaiAPP`：Spring Boot 启动类
- `XiaoXiaoBaiAgent`：LangChain4j AI Service 定义，绑定流式模型、聊天记忆、工具和内容检索器
- `XiaoXiaoBaiAgentConfig`：配置聊天记忆与内容检索
- `EmbeddingStoreConfig`：配置 Pinecone 向量存储
- `MongoChatMemoryStore`：自定义 MongoDB 聊天记忆存储实现
- `AppointmentTools`：预约挂号工具集合
- `AppointmentServiceImpl`：预约业务实现

## 接口示例

### 聊天接口

`POST /xiaoxiaobai/chat`

请求体示例：

```json
{
  "memoryId": 1,
  "message": "我最近咳嗽并伴有低烧，应该挂什么科？"
}
```

## 运行前准备

项目依赖外部服务，启动前需要准备并配置以下内容：

- 大模型相关配置
- MongoDB 连接信息
- MySQL 连接信息
- Pinecone API Key 与索引配置
