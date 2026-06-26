<template>
  <div class="knowledge-layout">
    <aside class="knowledge-sidebar">
      <div class="sidebar-brand panel-card">
        <span class="eyebrow">ADMIN DIRECTORY</span>
        <h2 class="sidebar-title">医疗助手管理台</h2>
      </div>

      <div class="sidebar-nav panel-card">
        <button type="button" class="directory-item active" @click="goToSection('knowledge')">
          <span class="directory-icon">库</span>
          <span class="directory-label">知识库管理</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('doctor-duty')">
          <span class="directory-icon">医</span>
          <span class="directory-label">值班医生列表</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('appointment')">
          <span class="directory-icon">约</span>
          <span class="directory-label">用户预约情况</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('chat-session')">
          <span class="directory-icon">聊</span>
          <span class="directory-label">用户会话历史</span>
        </button>
      </div>
    </aside>

    <main class="knowledge-main">
      <section class="hero-card panel-card">
        <div>
          <span class="eyebrow">Document Console</span>
          <h1 class="hero-title">统一管理知识文档与切片</h1>
          <p class="hero-description">
            顶部完成录入与筛选，中间查看文档状态，右侧抽屉检查原始内容与切片结果。
          </p>
        </div>

        <div class="hero-side">
          <div class="hero-metrics">
            <div class="metric-card panel-card">
              <div class="metric-label">文档总数</div>
              <div class="metric-value">{{ total }}</div>
            </div>
            <div class="metric-card panel-card">
              <div class="metric-label">入库成功</div>
              <div class="metric-value">{{ ingestedCount }}</div>
            </div>
            <div class="metric-card panel-card">
              <div class="metric-label">处理中</div>
              <div class="metric-value">{{ processingCount }}</div>
            </div>
          </div>

          <div class="hero-highlights">
            <div class="highlight-chip">
              <el-icon><Document /></el-icon>
              文档详情抽屉
            </div>
            <div class="highlight-chip">
              <el-icon><Tickets /></el-icon>
              切片内容预览
            </div>
            <div class="highlight-chip">
              <el-icon><RefreshRight /></el-icon>
              一键重新入库
            </div>
          </div>
        </div>
      </section>

      <section class="toolbar-card panel-card">
        <div class="toolbar-row">
          <div class="toolbar-actions">
            <el-upload
              :show-file-list="false"
              :http-request="handleUploadRequest"
              accept=".txt,.md,.pdf"
            >
              <el-button type="primary" class="action-button">
                <el-icon><Upload /></el-icon>
                上传文件
              </el-button>
            </el-upload>

            <el-button class="action-button action-button-secondary" @click="openTextDialog">
              <el-icon><EditPen /></el-icon>
              新增文本
            </el-button>

            <el-button class="action-button action-button-ghost" @click="loadKnowledgeList">
              <el-icon><RefreshRight /></el-icon>
              刷新列表
            </el-button>
          </div>

          <div class="toolbar-filters">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索文档名称或关键词"
              @clear="resetAndLoadKnowledgeList"
              @keyup.enter="resetAndLoadKnowledgeList"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-select
              v-model="filters.status"
              clearable
              placeholder="全部状态"
              @change="resetAndLoadKnowledgeList"
              @clear="resetAndLoadKnowledgeList"
            >
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已入库" value="INGESTED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
          </div>
        </div>
      </section>

      <section class="table-card panel-card">
        <div class="table-head">
          <div>
            <div class="table-title">文档列表</div>
            <div class="table-subtitle">支持查看详情、重新入库和删除文档</div>
          </div>
        </div>

        <el-table
          v-loading="listLoading"
          :data="knowledgeList"
          class="knowledge-table"
          empty-text="暂无知识文档"
        >
          <el-table-column prop="name" label="文档名称" min-width="220">
            <template #default="{ row }">
              <div class="table-name-cell">
                <div class="table-name">{{ row.name }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="type" label="类型" min-width="120">
            <template #default="{ row }">
              <span>{{ formatType(row.type) }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="sourceType" label="来源" min-width="120">
            <template #default="{ row }">
              <span>{{ formatSource(row.sourceType) }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light" round>
                {{ row.status || 'UNKNOWN' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="segmentCount" label="切片数" width="100" align="center" />

          <el-table-column prop="createdAt" label="创建时间" min-width="170">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">查看</el-button>
                <el-button
                  link
                  type="primary"
                  :loading="reingestingIds.has(row.id)"
                  @click="handleReingest(row)"
                >
                  重新入库
                </el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :current-page="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>

    <el-dialog
      v-model="textDialogVisible"
      title="新增文本"
      width="560px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="文档名称" required>
          <el-input
            v-model="textForm.name"
            maxlength="100"
            placeholder="例如：门诊常见问答"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="文本内容" required>
          <el-input
            v-model="textForm.content"
            type="textarea"
            :rows="10"
            maxlength="12000"
            placeholder="请输入要入库的文本内容"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="textDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="textSubmitting" @click="submitTextEntry">
          保存并入库
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailDrawerVisible"
      title="文档详情"
      size="520px"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-drawer">
        <template v-if="activeKnowledge">
          <section class="detail-section">
            <div class="detail-section-title">基本信息</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">名称</span>
                <span class="detail-value">{{ activeKnowledge.name }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">类型</span>
                <span class="detail-value">{{ formatType(activeKnowledge.type) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">来源</span>
                <span class="detail-value">{{ formatSource(activeKnowledge.sourceType) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">状态</span>
                <span class="detail-value">
                  <el-tag :type="statusTagType(activeKnowledge.status)" effect="light" round>
                    {{ activeKnowledge.status || 'UNKNOWN' }}
                  </el-tag>
                </span>
              </div>
              <div class="detail-item">
                <span class="detail-label">切片数</span>
                <span class="detail-value">{{ activeKnowledge.segmentCount }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">创建时间</span>
                <span class="detail-value">{{ formatTime(activeKnowledge.createdAt) }}</span>
              </div>
            </div>

            <div v-if="activeKnowledge.remark" class="remark-box">
              <div class="detail-section-title detail-section-title-inline">失败备注</div>
              <p>{{ activeKnowledge.remark }}</p>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title">原始内容预览</div>
            <div class="content-preview">
              {{ activeKnowledge.content || '暂无原始内容预览' }}
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title">
              切片预览
              <span class="detail-section-count">{{ detailSegments.length }}</span>
            </div>

            <div class="detail-section-note">后端当前返回的是每个切片的预览内容。</div>

            <div v-if="detailSegments.length" class="segment-list">
              <article
                v-for="segment in detailSegments"
                :key="segment.id"
                class="segment-card"
              >
                <div class="segment-card-head">
                  <span class="segment-index">片段 {{ segment.index }}</span>
                  <span v-if="segment.length" class="segment-length">{{ segment.length }} 字</span>
                </div>
                <div class="segment-text">{{ segment.content }}</div>
              </article>
            </div>

            <el-empty v-else description="暂无切片内容" :image-size="72" />
          </section>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import {
  Document,
  EditPen,
  RefreshRight,
  Search,
  Tickets,
  Upload,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createKnowledgeText,
  deleteKnowledge,
  fetchKnowledgeDetail,
  fetchKnowledgeList,
  fetchKnowledgeSegments,
  reingestKnowledge,
  uploadKnowledge,
} from '@/api/knowledge'

const emit = defineEmits(['navigate'])

const filters = ref({
  keyword: '',
  status: '',
})

const listLoading = ref(false)
const detailLoading = ref(false)
const textSubmitting = ref(false)
const textDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const knowledgeList = ref([])
const total = ref(0)
const pagination = ref({
  pageNum: 1,
  pageSize: 10,
})
const activeKnowledge = ref(null)
const detailSegments = ref([])
const reingestingIds = ref(new Set())
const textForm = ref({
  name: '',
  content: '',
})

const ingestedCount = computed(() => {
  return knowledgeList.value.filter((item) => item.status === 'INGESTED').length
})

const processingCount = computed(() => {
  return knowledgeList.value.filter((item) => item.status === 'PROCESSING').length
})

const goToSection = (section) => {
  emit('navigate', section)
}

const pickValue = (source, keys, fallback = '') => {
  for (const key of keys) {
    const value = source?.[key]
    if (value !== undefined && value !== null && value !== '') {
      return value
    }
  }

  return fallback
}

const normalizeKnowledgeRecord = (source = {}) => {
  const rawType = String(pickValue(source, ['type', 'documentType', 'fileType'], 'text'))

  return {
    id: String(pickValue(source, ['id'], '--')),
    name: pickValue(source, ['name'], '未命名文档'),
    type: rawType,
    sourceType: pickValue(
      source,
      ['sourceType', 'source_type', 'source'],
      rawType.toLowerCase() === 'text' ? 'manual' : 'upload'
    ),
    status: String(pickValue(source, ['status', 'ingestStatus'], 'PROCESSING')).toUpperCase(),
    segmentCount: Number(pickValue(source, ['segmentCount'], 0)),
    createdAt: pickValue(source, ['createdAt']),
    content: pickValue(source, ['contentText']),
    remark: pickValue(source, ['remark']),
  }
}

const normalizeKnowledgeList = (payload) => {
  const list = Array.isArray(payload) ? payload : pickValue(payload, ['records'], [])

  return Array.isArray(list) ? list.map(normalizeKnowledgeRecord) : []
}

const normalizeSegments = (payload) => {
  const list = Array.isArray(payload)
    ? payload
    : pickValue(payload, ['list', 'items', 'records', 'segments', 'data'], [])

  if (!Array.isArray(list)) {
    return []
  }

  return list.map((item, index) => {
    const content = pickValue(item, ['contentPreview'], '')

    return {
      id: String(pickValue(item, ['segmentIndex'], index + 1)),
      index: Number(pickValue(item, ['segmentIndex'], index + 1)),
      content,
      length: content.length,
    }
  })
}

const loadKnowledgeList = async () => {
  listLoading.value = true

  try {
    const payload = await fetchKnowledgeList({
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
      keyword: filters.value.keyword || undefined,
      status: filters.value.status || undefined,
    })

    knowledgeList.value = normalizeKnowledgeList(payload)
    total.value = Number(payload?.total || knowledgeList.value.length || 0)
  } catch (error) {
    console.error('加载知识库列表失败:', error)
    ElMessage.error(error?.message || '加载知识库列表失败，请稍后重试')
    knowledgeList.value = []
    total.value = 0
  } finally {
    listLoading.value = false
  }
}

const resetAndLoadKnowledgeList = () => {
  pagination.value.pageNum = 1
  loadKnowledgeList()
}

const handlePageChange = (pageNum) => {
  pagination.value.pageNum = pageNum
  loadKnowledgeList()
}

const handleUploadRequest = async (options) => {
  try {
    await uploadKnowledge(options.file, options.file?.name)
    options.onSuccess?.({}, options.file)
    ElMessage.success('文件上传成功')
    await loadKnowledgeList()
  } catch (error) {
    console.error('上传知识文件失败:', error)
    options.onError?.(error)
    ElMessage.error(error?.message || '文件上传失败，请检查接口配置')
  }
}

const openTextDialog = () => {
  textForm.value = {
    name: '',
    content: '',
  }
  textDialogVisible.value = true
}

const submitTextEntry = async () => {
  const name = textForm.value.name.trim()
  const content = textForm.value.content.trim()

  if (!name || !content) {
    ElMessage.warning('请先填写文档名称和文本内容')
    return
  }

  textSubmitting.value = true

  try {
    await createKnowledgeText({
      name,
      content,
    })

    ElMessage.success('文本入库请求已提交')
    textDialogVisible.value = false
    await loadKnowledgeList()
  } catch (error) {
    console.error('新增知识文本失败:', error)
    ElMessage.error(error?.message || '新增文本失败，请检查接口配置')
  } finally {
    textSubmitting.value = false
  }
}

const openDetail = async (row) => {
  detailDrawerVisible.value = true
  detailLoading.value = true
  activeKnowledge.value = row
  detailSegments.value = []

  const [detailResult, segmentResult] = await Promise.allSettled([
    fetchKnowledgeDetail(row.id),
    fetchKnowledgeSegments(row.id),
  ])

  if (detailResult.status === 'fulfilled') {
    activeKnowledge.value = normalizeKnowledgeRecord(detailResult.value)
  } else {
    console.error('加载知识详情失败:', detailResult.reason)
    ElMessage.error(detailResult.reason?.message || '加载文档详情失败')
  }

  if (segmentResult.status === 'fulfilled') {
    detailSegments.value = normalizeSegments(segmentResult.value)

    if (activeKnowledge.value && !activeKnowledge.value.segmentCount) {
      activeKnowledge.value = {
        ...activeKnowledge.value,
        segmentCount: detailSegments.value.length,
      }
    }
  } else {
    console.error('加载知识切片失败:', segmentResult.reason)
    ElMessage.error(segmentResult.reason?.message || '加载切片列表失败')
  }

  detailLoading.value = false
}

const handleReingest = async (row) => {
  const nextSet = new Set(reingestingIds.value)
  nextSet.add(row.id)
  reingestingIds.value = nextSet

  try {
    await reingestKnowledge(row.id)
    ElMessage.success('重新入库任务已触发')

    if (activeKnowledge.value?.id === row.id) {
      activeKnowledge.value = {
        ...activeKnowledge.value,
        status: 'PROCESSING',
      }
    }

    await loadKnowledgeList()
  } catch (error) {
    console.error('重新入库失败:', error)
    ElMessage.error(error?.message || '重新入库失败，请稍后重试')
  } finally {
    const currentSet = new Set(reingestingIds.value)
    currentSet.delete(row.id)
    reingestingIds.value = currentSet
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除后将无法恢复，确认删除“${row.name}”吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    return
  }

  try {
    await deleteKnowledge(row.id)
    ElMessage.success('文档已删除')

    if (activeKnowledge.value?.id === row.id) {
      detailDrawerVisible.value = false
      activeKnowledge.value = null
      detailSegments.value = []
    }

    await loadKnowledgeList()
  } catch (error) {
    console.error('删除知识文档失败:', error)
    ElMessage.error(error?.message || '删除失败，请稍后重试')
  }
}

const statusTagType = (status) => {
  switch (String(status || '').toUpperCase()) {
    case 'PROCESSING':
      return 'primary'
    case 'INGESTED':
      return 'success'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

const formatSource = (value) => {
  const source = String(value || '').toUpperCase()

  if (source.includes('FILE') || source.includes('UPLOAD')) {
    return '文件上传'
  }

  if (source.includes('MANUAL') || source.includes('TEXT')) {
    return '手工录入'
  }

  return value || '未知来源'
}

const formatType = (value) => {
  const type = String(value || '').toUpperCase()

  if (type === 'TEXT') {
    return '文本'
  }

  if (type === 'PDF') {
    return 'PDF'
  }

  if (type === 'MD') {
    return 'Markdown'
  }

  if (type === 'TXT') {
    return 'TXT'
  }

  return value || '未分类'
}

const formatTime = (value) => {
  if (!value) {
    return '--'
  }

  const normalizedValue = String(value).replace(' ', 'T')
  const date = new Date(normalizedValue)

  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

loadKnowledgeList()
</script>

<style scoped>
.knowledge-layout {
  display: flex;
  height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(202, 227, 206, 0.42), transparent 28%),
    linear-gradient(180deg, #f7faf7 0%, #eef4ec 100%);
}

.knowledge-sidebar {
  width: 308px;
  padding: 88px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(180deg, #f8fbf8 0%, #edf3ec 100%);
  border-right: 1px solid #dfe9dd;
  overflow-y: auto;
}

.knowledge-main {
  min-width: 0;
  flex: 1;
  padding: 88px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.panel-card {
  border: 1px solid #dbe7d8;
  border-radius: 20px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.96),
    rgba(244, 249, 242, 0.94)
  );
  box-shadow: 0 20px 45px rgba(96, 130, 103, 0.08);
}

.sidebar-brand,
.sidebar-nav {
  padding: 22px 20px;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: #6d8472;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sidebar-title {
  margin: 0;
  color: #284333;
  font-size: 28px;
  line-height: 1.1;
}

.sidebar-description,
.hero-description {
  margin: 12px 0 0;
  color: #627668;
  font-size: 14px;
  line-height: 1.7;
}

.sidebar-nav {
  display: grid;
  gap: 10px;
}

.directory-item {
  display: grid;
  grid-template-columns: 28px 1fr;
  align-items: center;
  min-height: 52px;
  padding: 0 16px;
  border: 1px solid #d8e6d5;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.76);
  color: #486a5a;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.directory-item:hover {
  border-color: #8cb890;
  box-shadow: 0 8px 18px rgba(86, 126, 94, 0.1);
  transform: translateY(-1px);
}

.directory-item.active {
  background: linear-gradient(180deg, #f3fbf1 0%, #e5f2e4 100%);
  border-color: #6fa173;
  box-shadow: 0 10px 24px rgba(92, 145, 102, 0.16);
  color: #1f4031;
}

.directory-icon {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 1px solid currentColor;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1;
}

.directory-label {
  font-size: 16px;
  font-weight: 800;
}

.metric-card {
  padding: 18px 20px;
}

.metric-label {
  color: #607665;
  font-size: 13px;
  font-weight: 700;
}

.metric-value {
  margin-top: 8px;
  color: #233a2b;
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.tips-title,
.table-title {
  color: #2d4334;
  font-size: 16px;
  font-weight: 800;
}

.tips-list {
  margin: 14px 0 0;
  padding-left: 18px;
  color: #627668;
  line-height: 1.8;
}

.hero-card {
  padding: 24px 24px 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
}

.hero-title {
  margin: 0;
  color: #21352a;
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.05;
  letter-spacing: -0.03em;
}

.hero-highlights {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 118px));
  gap: 8px;
}

.hero-metrics .metric-card {
  min-width: 0;
  padding: 10px 12px;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(96, 130, 103, 0.06);
}

.hero-metrics .metric-label {
  font-size: 11px;
}

.hero-metrics .metric-value {
  margin-top: 5px;
  font-size: 24px;
  text-align: center;
}

.highlight-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid rgba(143, 186, 148, 0.56);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #385240;
  font-size: 13px;
  font-weight: 700;
}

.toolbar-card,
.table-card {
  padding: 18px;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-actions,
.toolbar-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-filters {
  width: min(100%, 560px);
}

.toolbar-filters :deep(.el-input) {
  flex: 1;
  min-width: 260px;
}

.toolbar-filters :deep(.el-select) {
  flex: 0 0 168px;
  min-width: 168px;
}

.action-button {
  min-height: 42px;
  padding-inline: 16px;
  border-radius: 14px;
  font-weight: 700;
}

.action-button-secondary {
  border-color: #d6e3d5;
  color: #31503a;
}

.action-button-ghost {
  border-color: transparent;
  background: rgba(111, 161, 115, 0.12);
  color: #31503a;
}

.table-head {
  margin-bottom: 14px;
}

.table-subtitle {
  margin-top: 6px;
  color: #708274;
  font-size: 13px;
}

.knowledge-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.knowledge-table :deep(th.el-table__cell) {
  background: rgba(237, 245, 234, 0.8);
  color: #4d6454;
  font-weight: 700;
}

.knowledge-table :deep(th.el-table-fixed-column--right) {
  background: rgba(237, 245, 234, 0.8) !important;
}

.knowledge-table :deep(td.el-table-fixed-column--right) {
  background: #ffffff !important;
}

.knowledge-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.knowledge-table :deep(.el-table__row) {
  transition: background-color 0.2s ease;
}

.knowledge-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(243, 251, 241, 0.82);
}

.knowledge-table :deep(.el-table__row:hover > td.el-table-fixed-column--right) {
  background: rgba(243, 251, 241, 0.82) !important;
}

.table-name-cell {
  min-width: 0;
}

.table-name {
  color: #284333;
  font-size: 14px;
  font-weight: 700;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.table-pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.detail-drawer {
  padding-right: 6px;
}

.detail-section + .detail-section {
  margin-top: 24px;
}

.detail-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #273c2f;
  font-size: 15px;
  font-weight: 800;
}

.detail-section-title-inline {
  margin-bottom: 8px;
}

.detail-section-count {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(111, 161, 115, 0.14);
  color: #45624d;
  font-size: 12px;
}

.detail-section-note {
  margin-top: 8px;
  color: #7a8d7e;
  font-size: 12px;
  line-height: 1.6;
}

.detail-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  padding: 14px;
  border: 1px solid #dbe7d8;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
}

.detail-label {
  display: block;
  color: #7a8d7e;
  font-size: 12px;
}

.detail-value {
  display: block;
  margin-top: 6px;
  color: #284333;
  font-size: 14px;
  font-weight: 700;
  word-break: break-word;
}

.remark-box {
  margin-top: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(245, 108, 108, 0.25);
  border-radius: 16px;
  background: rgba(255, 246, 246, 0.92);
  color: #8b3a3a;
}

.remark-box p {
  margin: 0;
  line-height: 1.7;
}

.content-preview {
  margin-top: 14px;
  padding: 16px;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #dbe7d8;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  color: #324638;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.segment-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.segment-card {
  padding: 14px 16px;
  border: 1px solid #dbe7d8;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
}

.segment-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.segment-index {
  color: #284333;
  font-size: 13px;
  font-weight: 800;
}

.segment-length {
  color: #7b8f80;
  font-size: 12px;
}

.segment-text {
  margin-top: 10px;
  color: #435646;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .knowledge-layout {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
  }

  .knowledge-sidebar {
    width: auto;
    padding-top: 92px;
    border-right: none;
    border-bottom: 1px solid #dfe9dd;
  }

  .knowledge-main {
    padding-top: 20px;
  }

  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-side {
    width: 100%;
    align-items: flex-start;
  }

  .hero-highlights {
    justify-content: flex-start;
  }

  .hero-metrics {
    width: 100%;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .knowledge-main,
  .knowledge-sidebar {
    padding-left: 14px;
    padding-right: 14px;
  }

  .knowledge-main {
    padding-bottom: 14px;
  }

  .toolbar-actions,
  .toolbar-filters {
    width: 100%;
  }

  .toolbar-actions > * {
    flex: 1;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
