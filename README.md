# AI-ChatBot-Healthcare

基于 Spring Boot 3、LangChain4j、Vue 3 构建的医疗智能助手项目，面向医疗咨询、知识库问答、预约挂号、医生值班管理和后台运维场景。

项目当前已经从单一流式问答接口扩展为前后端分离的医疗助手系统。普通用户侧提供智能对话、当前值班医生查看、预约挂号和个人中心；管理员侧提供知识库管理、医生值班维护、预约记录查看和用户会话查看。

## 功能概览

### 普通用户端

- 登录、注册，登录后使用 JWT 鉴权。
- 智能医疗对话，支持流式响应。
- 会话管理，支持创建会话、查询会话列表、查看历史记录、删除会话。
- 基于 MongoDB 保存登录用户的对话历史。
- 基于 RAG 检索知识库内容，辅助医疗问答。
- 查看当前值班医生，按上午、下午时间段展示。
- 通过 AI 工具调用完成预约挂号、取消预约、查询号源。
- 个人中心支持查看用户资料、修改密码、查看自己的预约记录。

### 管理员端

- 知识库管理：
  - 上传文件入库。
  - 手动录入文本入库。
  - 查询知识库文档列表。
  - 查看文档详情和切片内容。
  - 重新入库。
  - 删除文档。
- 医生值班管理：
  - 查看医生值班列表。
  - 新增医生。
  - 编辑医生姓名、科室、职称、擅长方向。
  - 调整上午、下午值班状态。
  - 启用或停用医生。
  - 删除医生。
- 预约管理：
  - 分页查看所有用户预约记录。
  - 按关键字和预约状态筛选。
- 会话管理：
  - 分页查看用户会话摘要。
  - 按用户名、昵称筛选。

### 权限控制

- 系统使用 JWT 鉴权。
- 用户表通过 `role` 区分普通用户和管理员。
- 普通用户只能访问对话、个人中心、当前值班医生等用户端接口。
- 管理员可以访问知识库、医生值班、预约记录、会话管理等后台接口。
- JWT 默认密钥可通过配置覆盖，默认值为 `xiaobai`。

## 技术栈

- Java 17
- Spring Boot 3.2.6
- LangChain4j 1.0.0-beta3
- Spring WebFlux
- Vue 3
- Element Plus
- MongoDB
- MyBatis-Plus
- MySQL
- Pinecone
- JWT
- Knife4j / OpenAPI

## 项目结构

```text
src/main/java/com/java/ai/langchain4j
├─ assistant    # LangChain4j AI Service
├─ bean         # 请求对象、响应对象、页面 DTO
├─ config       # Agent、鉴权、向量库、Web MVC 配置
├─ controller   # HTTP 接口
├─ entity       # MySQL / MongoDB 业务实体
├─ handler      # 异常处理
├─ mapper       # MyBatis-Plus Mapper
├─ rag          # RAG 检索引用追踪
├─ service      # 业务服务
├─ store        # MongoDB 聊天记忆存储
├─ tools        # LangChain4j Tool 工具
└─ util         # JWT、用户上下文等工具
```

## 核心模块

### 智能对话

`XiaoXiaoBaiController` 提供 `/xiaoxiaobai/chat` 流式接口，模型响应以 `text/stream` 方式返回给前端。

会话相关接口：

- `POST /xiaoxiaobai/sessions`
- `GET /xiaoxiaobai/sessions`
- `GET /xiaoxiaobai/sessions/{sessionId}/history`
- `DELETE /xiaoxiaobai/sessions/{sessionId}`
- `POST /xiaoxiaobai/chat`

### 登录与用户中心

`AuthController` 和 `UserProfileController` 提供登录、注册、用户资料、修改密码、个人预约记录等能力。

主要接口：

- `POST /auth/login`
- `POST /auth/register`
- `GET /users/me`
- `PUT /users/me/password`
- `GET /users/me/appointments`

### 知识库管理

`KnowledgeDocumentController` 提供知识库文档管理接口。文档入库后会被切片、向量化，并写入 Pinecone，同时保存文档和切片元数据。

主要接口：

- `POST /knowledge/documents/upload`
- `POST /knowledge/documents/text`
- `GET /knowledge/documents`
- `GET /knowledge/documents/{id}`
- `GET /knowledge/documents/{id}/segments`
- `POST /knowledge/documents/{id}/reingest`
- `DELETE /knowledge/documents/{id}`

### 医生值班管理

`DoctorDutyController` 提供医生和值班时间维护能力。排班不依赖具体日期，只按上午、下午时间段维护。

主要接口：

- `GET /doctor-duties/current`
- `GET /doctor-duties`
- `GET /doctor-duties/stats`
- `POST /doctor-duties`
- `PUT /doctor-duties/{id}`
- `PATCH /doctor-duties/{id}/duty`
- `DELETE /doctor-duties/{id}`

### 预约挂号

预约能力通过 LangChain4j Tool 接入智能助手。用户在对话中提供科室、时间、医生等信息后，系统会检查值班医生和剩余号源，再创建预约记录。

管理员可通过后台查看所有预约：

- `GET /admin/appointments`

### 管理员会话查看

管理员可以分页查看用户会话摘要，用于运营管理和问题排查。

- `GET /admin/chat-sessions`

## 数据存储

### MySQL

MySQL 主要保存登录用户、医生值班、预约记录、知识库文档和知识库切片元数据。

初始化脚本位于：

- `src/main/resources/sql/user.sql`
- `src/main/resources/sql/user-role-migration.sql`
- `src/main/resources/sql/doctor_schedule.sql`
- `src/main/resources/sql/appointment.sql`

### MongoDB

MongoDB 用于保存聊天会话和展示消息，包括：

- 会话列表。
- 用户消息。
- AI 回复。
- 知识库引用来源。

### Pinecone

Pinecone 用于保存知识库切片向量，并为 RAG 问答提供相似内容检索。

默认索引和命名空间已统一为小小白相关命名：

- `xiaoxiaobai-index`
- `xiaoxiaobai-namespace`

## 运行前准备

启动前需要准备以下服务和配置：

- JDK 17。
- MySQL。
- MongoDB。
- Pinecone API Key 和索引。
- 大模型 API Key。
- DashScope / OpenAI 兼容接口配置。

后端配置通过 `application.yaml` 引入 `application-dev.yaml`，建议在本地创建 `src/main/resources/application-dev.yaml` 保存私有配置。

示例配置项：

```yaml
app:
  datasource:
    url: jdbc:mysql://localhost:3306/healthcare_ai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  mongodb:
    uri: mongodb://localhost:27017/healthcare_ai
  jwt:
    secret: xiaobai
    expiration-seconds: 7200
  langchain4j:
    dashscope:
      api-key: your_dashscope_api_key
    open-ai:
      api-key: your_openai_compatible_api_key
  pinecone:
    api-key: your_pinecone_api_key
```

## 启动方式

### 后端

```bash
mvn spring-boot:run
```

后端默认端口：

```text
http://localhost:8080
```

### 前端

前端工程使用 Vue 3 和 Vite。

```bash
npm install
npm run dev
```

前端默认通过 `VITE_API_BASE_URL` 配置后端接口地址。

## 接口返回格式

大多数业务接口使用统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

流式聊天接口直接返回文本流。

## 主要代码入口

- `XiaoXiaoBaiAPP`：Spring Boot 启动类。
- `XiaoXiaoBaiAgent`：LangChain4j AI Service 定义。
- `XiaoXiaoBaiController`：智能对话和会话接口。
- `AuthController`：登录注册接口。
- `AuthInterceptor`：JWT 鉴权和角色权限控制。
- `KnowledgeDocumentController`：知识库管理接口。
- `DoctorDutyController`：医生值班管理接口。
- `AdminAppointmentController`：管理员预约记录接口。
- `AdminChatSessionController`：管理员会话查看接口。
- `AppointmentTools`：预约挂号、取消预约、号源查询工具。
- `DoctorDutyTools`：当前值班医生查询工具。
- `MongoChatMemoryStore`：MongoDB 对话记忆存储。

## 当前能力边界

- 医生值班按上午、下午维护，不按具体日期排班。
- 预约时间按上午、下午匹配。
- 普通用户不能直接访问管理员后台接口。
- 管理员端用于维护数据和查看记录，不参与用户侧对话。
