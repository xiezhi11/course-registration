<template>
  <div class="admin-review">
    <!-- 统计看板 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="4">
        <el-card class="stat-card pending" shadow="hover">
          <div class="stat-content">
            <div class="stat-label">待审核</div>
            <div class="stat-value">{{ statistics.PENDING || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card approved" shadow="hover">
          <div class="stat-content">
            <div class="stat-label">已通过</div>
            <div class="stat-value">{{ statistics.APPROVED || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card rejected" shadow="hover">
          <div class="stat-content">
            <div class="stat-label">已驳回</div>
            <div class="stat-value">{{ statistics.REJECTED || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card cancelled" shadow="hover">
          <div class="stat-content">
            <div class="stat-label">已取消</div>
            <div class="stat-value">{{ statistics.CANCELLED || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card total" shadow="hover">
          <div class="stat-content">
            <div class="stat-label">总计</div>
            <div class="stat-value">{{ statistics.TOTAL || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card action-card" shadow="hover" @click="refreshData">
          <div class="stat-content">
            <el-icon class="refresh-icon"><Refresh /></el-icon>
            <div class="stat-label">刷新数据</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">审核管理</span>
          <div class="header-actions">
            <el-button
              type="success"
              :disabled="selectedRows.length === 0"
              @click="handleBatchApprove"
            >
              <el-icon><Check /></el-icon>
              批量通过 ({{ selectedRows.length }})
            </el-button>
            <el-button
              type="danger"
              :disabled="selectedRows.length === 0"
              @click="handleBatchReject"
            >
              <el-icon><Close /></el-icon>
              批量驳回 ({{ selectedRows.length }})
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="课程名称">
          <el-select
            v-model="queryForm.courseId"
            placeholder="请选择课程"
            clearable
            filterable
          >
            <el-option
              v-for="course in courses"
              :key="course.id"
              :label="course.name"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="报名人">
          <el-input
            v-model="queryForm.userName"
            placeholder="输入报名人姓名"
            clearable
          />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadRegistrations">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetQuery">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="registrations"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" :selectable="isSelectable" />
        <el-table-column label="课程名称" min-width="180">
          <template #default="{ row }">
            <span class="course-name" @click="viewDetail(row)" style="cursor: pointer; color: #409EFF;">
              {{ row.course?.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="课程类型" width="100">
          <template #default="{ row }">
            {{ row.course?.type }}
          </template>
        </el-table-column>
        <el-table-column label="报名人" width="100">
          <template #default="{ row }">
            {{ row.user?.name }}
          </template>
        </el-table-column>
        <el-table-column label="部门" width="120">
          <template #default="{ row }">
            {{ row.user?.department }}
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="课程名额" width="130">
          <template #default="{ row }">
            <div class="capacity-info">
              <span>{{ row.course?.registeredCount }}/{{ row.course?.maxCapacity }}</span>
              <el-progress
                :percentage="calculateCapacityPercent(row)"
                :stroke-width="6"
                :color="getCapacityColor(row)"
                style="margin-top: 4px;"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="dark">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.reviewComment || row.cancelReason || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="viewDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="success"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="danger"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="['PENDING', 'APPROVED'].includes(row.status)"
              size="small"
              type="warning"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadRegistrations"
        @current-change="loadRegistrations"
      />
    </el-card>

    <!-- 审核意见弹窗 -->
    <el-dialog v-model="reviewDialogVisible" :title="reviewTitle" width="500px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="常用意见">
          <el-select
            v-model="selectedTemplate"
            placeholder="选择常用意见"
            clearable
            @change="applyTemplate"
          >
            <el-option
              v-for="(template, index) in reviewTemplates"
              :key="index"
              :label="template.label"
              :value="template.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 报名详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="报名详情" width="800px">
      <div v-if="currentDetail" class="detail-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="课程名称" :span="2">
            <div style="display: flex; align-items: center; gap: 8px;">
              {{ currentDetail.course?.name }}
              <el-tag
                v-if="currentDetail.registrationCount > 1"
                type="info"
                size="small"
              >
                第{{ currentDetail.registrationCount }}次报名
              </el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="课程类型">{{ currentDetail.course?.type }}</el-descriptions-item>
          <el-descriptions-item label="讲师">{{ currentDetail.course?.lecturer }}</el-descriptions-item>
          <el-descriptions-item label="课程时间" :span="2">
            {{ formatDateTime(currentDetail.course?.startTime) }} ~ {{ formatDateTime(currentDetail.course?.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="培训地点">{{ currentDetail.course?.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="课程名额">
            {{ currentDetail.course?.registeredCount }}/{{ currentDetail.course?.maxCapacity }}
          </el-descriptions-item>
          <el-descriptions-item label="报名人" :span="2">{{ currentDetail.user?.name }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentDetail.user?.department }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentDetail.user?.email }}</el-descriptions-item>
          <el-descriptions-item label="报名状态">
            <el-tag :type="getStatusType(currentDetail.status)" effect="dark">
              {{ getStatusText(currentDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报名时间">{{ formatDateTime(currentDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" v-if="currentDetail.reviewComment" :span="2">
            {{ currentDetail.reviewComment }}
          </el-descriptions-item>
          <el-descriptions-item label="取消原因" v-if="currentDetail.cancelReason" :span="2">
            {{ currentDetail.cancelReason }}
          </el-descriptions-item>
          <el-descriptions-item label="审核时间" v-if="currentDetail.reviewTime">
            {{ formatDateTime(currentDetail.reviewTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="取消时间" v-if="currentDetail.cancelTime">
            {{ formatDateTime(currentDetail.cancelTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">完整报名历史</el-divider>
        <el-timeline v-if="registrationChain.length > 0" class="timeline">
          <el-timeline-item
            v-for="(item, index) in registrationChain"
            :key="index"
            :timestamp="formatDateTime(item.createTime)"
            placement="top"
            :type="getStatusType(item.status)"
            :dot="getChainDot(item.status)"
          >
            <div class="timeline-content">
              <div class="timeline-header">
                <span class="timeline-title">
                  第{{ item.registrationCount }}次报名
                  <el-tag
                    v-if="item.id === currentDetail.id"
                    type="primary"
                    size="small"
                    effect="dark"
                  >
                    当前
                  </el-tag>
                </span>
                <el-tag :type="getStatusType(item.status)" effect="dark">
                  {{ getStatusText(item.status) }}
                </el-tag>
              </div>
              <div class="timeline-info">
                <span>报名时间：{{ formatDateTime(item.createTime) }}</span>
              </div>
              <div v-if="item.reviewComment" class="timeline-info">
                <span>审核意见：{{ item.reviewComment }}</span>
              </div>
              <div v-if="item.cancelReason" class="timeline-info">
                <span>取消原因：{{ item.cancelReason }}</span>
              </div>
              <div v-if="item.reviewTime" class="timeline-info">
                <span>审核时间：{{ formatDateTime(item.reviewTime) }}</span>
              </div>
              <div v-if="item.cancelTime" class="timeline-info">
                <span>取消时间：{{ formatDateTime(item.cancelTime) }}</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无报名历史记录" />

        <el-divider content-position="left">状态变更历史（本次报名）</el-divider>
        <el-timeline v-if="statusHistory.length > 0" class="timeline">
          <el-timeline-item
            v-for="(log, index) in statusHistory"
            :key="index"
            :timestamp="formatDateTime(log.createTime)"
            placement="top"
            :type="getTimelineType(log.action)"
          >
            <div class="timeline-content">
              <div class="timeline-title">{{ getActionText(log.action) }}</div>
              <div class="timeline-status">
                <el-tag size="small" :type="getStatusType(log.fromStatus)" v-if="log.fromStatus">
                  {{ getStatusText(log.fromStatus) }}
                </el-tag>
                <el-icon v-if="log.fromStatus"><ArrowRight /></el-icon>
                <el-tag size="small" :type="getStatusType(log.toStatus)">
                  {{ getStatusText(log.toStatus) }}
                </el-tag>
              </div>
              <div class="timeline-info">
                <span>操作人：{{ log.operatorRole === 'ADMIN' ? '管理员' : '员工' }}</span>
                <span v-if="log.remark" class="timeline-remark">备注：{{ log.remark }}</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无状态变更记录" />

        <div class="detail-actions">
          <el-button
            v-if="currentDetail.status === 'PENDING'"
            type="success"
            @click="handleApprove(currentDetail); detailDialogVisible = false"
          >
            审核通过
          </el-button>
          <el-button
            v-if="currentDetail.status === 'PENDING'"
            type="danger"
            @click="handleReject(currentDetail); detailDialogVisible = false"
          >
            审核驳回
          </el-button>
          <el-button
            v-if="['PENDING', 'APPROVED'].includes(currentDetail.status)"
            type="warning"
            @click="handleCancel(currentDetail); detailDialogVisible = false"
          >
            取消报名
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Check, Close, ArrowRight } from '@element-plus/icons-vue'
import api from '../utils/api'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const registrations = ref([])
const courses = ref([])
const total = ref(0)
const statistics = ref({})
const selectedRows = ref([])
const reviewDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const reviewType = ref('')
const currentRegistration = ref(null)
const currentDetail = ref(null)
const statusHistory = ref([])
const registrationChain = ref([])
const selectedTemplate = ref('')

const queryForm = ref({
  courseId: null,
  userName: '',
  status: '',
  dateRange: null,
  page: 1,
  size: 10
})

const reviewForm = ref({
  comment: ''
})

const reviewTemplates = [
  { label: '符合要求，审核通过', value: '符合报名条件，审核通过' },
  { label: '名额已满，暂不通过', value: '课程名额已满，暂不通过' },
  { label: '已参加过同类课程', value: '已参加过同类培训课程，建议选择其他课程' },
  { label: '信息不完整，请补充', value: '报名信息不完整，请补充后重新提交' },
  { label: '课程时间冲突', value: '与其他课程时间冲突，无法安排' }
]

const formatDateTime = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const getActionText = (action) => {
  const map = {
    SUBMIT: '提交报名',
    APPROVE: '审核通过',
    REJECT: '审核驳回',
    CANCEL_BY_USER: '用户取消',
    CANCEL_BY_ADMIN: '管理员取消',
    RE_SUBMIT: '重新报名'
  }
  return map[action] || action
}

const getTimelineType = (action) => {
  const map = {
    SUBMIT: 'primary',
    APPROVE: 'success',
    REJECT: 'danger',
    CANCEL_BY_USER: 'warning',
    CANCEL_BY_ADMIN: 'warning',
    RE_SUBMIT: 'primary'
  }
  return map[action] || 'info'
}

const getChainDot = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'primary'
}

const calculateCapacityPercent = (row) => {
  if (!row.course?.maxCapacity) return 0
  return Math.round((row.course.registeredCount / row.course.maxCapacity) * 100)
}

const getCapacityColor = (row) => {
  const percent = calculateCapacityPercent(row)
  if (percent >= 100) return '#f56c6c'
  if (percent >= 80) return '#e6a23c'
  return '#67c23a'
}

const isSelectable = (row) => {
  return row.status === 'PENDING'
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection.filter(row => row.status === 'PENDING')
}

const loadStatistics = async () => {
  try {
    const res = await api.get('/registrations/statistics')
    statistics.value = res.data || {}
  } catch (e) {
    console.error(e)
  }
}

const loadCourses = async () => {
  try {
    const res = await api.get('/courses', { params: { page: 1, size: 100 } })
    courses.value = res.data.content || []
  } catch (e) {
    console.error(e)
  }
}

const loadRegistrations = async () => {
  loading.value = true
  try {
    const params = { ...queryForm.value }
    if (!params.courseId) delete params.courseId
    if (!params.status) delete params.status
    if (!params.userName) delete params.userName
    if (!params.dateRange) {
      delete params.dateRange
    } else {
      params.startTime = params.dateRange[0]
      params.endTime = params.dateRange[1]
      delete params.dateRange
    }

    const res = await api.get('/registrations', { params })
    registrations.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.value = {
    courseId: null,
    userName: '',
    status: '',
    dateRange: null,
    page: 1,
    size: 10
  }
  loadRegistrations()
}

const viewDetail = async (row) => {
  currentDetail.value = row
  detailDialogVisible.value = true
  try {
    const [historyRes, chainRes] = await Promise.all([
      api.get(`/registrations/${row.id}/history`),
      api.get(`/registrations/${row.id}/chain`)
    ])
    statusHistory.value = historyRes.data || []
    registrationChain.value = chainRes.data || []
  } catch (e) {
    console.error(e)
    statusHistory.value = []
    registrationChain.value = []
  }
}

const applyTemplate = (value) => {
  reviewForm.value.comment = value
}

const handleApprove = (row) => {
  currentRegistration.value = row
  reviewType.value = 'approve'
  reviewForm.value.comment = ''
  selectedTemplate.value = ''
  reviewDialogVisible.value = true
}

const handleReject = (row) => {
  currentRegistration.value = row
  reviewType.value = 'reject'
  reviewForm.value.comment = ''
  selectedTemplate.value = ''
  reviewDialogVisible.value = true
}

const reviewTitle = computed(() => {
  if (reviewType.value === 'approve') return '审核通过'
  if (reviewType.value === 'reject') return '审核驳回'
  if (reviewType.value === 'batchApprove') return '批量审核通过'
  if (reviewType.value === 'batchReject') return '批量审核驳回'
  return '审核'
})

const handleBatchApprove = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要审核的报名记录')
    return
  }
  reviewType.value = 'batchApprove'
  reviewForm.value.comment = ''
  selectedTemplate.value = ''
  reviewDialogVisible.value = true
}

const handleBatchReject = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要审核的报名记录')
    return
  }
  reviewType.value = 'batchReject'
  reviewForm.value.comment = ''
  selectedTemplate.value = ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  submitting.value = true
  try {
    if (reviewType.value === 'approve') {
      await api.post(`/registrations/${currentRegistration.value.id}/approve`, {
        comment: reviewForm.value.comment
      })
      ElMessage.success('审核通过成功')
    } else if (reviewType.value === 'reject') {
      await api.post(`/registrations/${currentRegistration.value.id}/reject`, {
        comment: reviewForm.value.comment
      })
      ElMessage.success('审核驳回成功')
    } else if (reviewType.value === 'batchApprove') {
      const ids = selectedRows.value.map(r => r.id)
      await api.post('/registrations/batch/approve', {
        ids,
        comment: reviewForm.value.comment
      })
      ElMessage.success(`批量审核通过成功，共处理 ${ids.length} 条`)
    } else if (reviewType.value === 'batchReject') {
      const ids = selectedRows.value.map(r => r.id)
      await api.post('/registrations/batch/reject', {
        ids,
        comment: reviewForm.value.comment
      })
      ElMessage.success(`批量审核驳回成功，共处理 ${ids.length} 条`)
    }
    reviewDialogVisible.value = false
    selectedRows.value = []
    refreshData()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消${row.user?.name}对《${row.course?.name}》的报名吗？`,
      '确认取消',
      { type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await api.post(`/registrations/${row.id}/cancel-by-admin`)
    ElMessage.success('取消报名成功')
    refreshData()
  } catch (e) {
    console.error(e)
  }
}

const refreshData = async () => {
  await Promise.all([
    loadStatistics(),
    loadCourses(),
    loadRegistrations()
  ])
}

onMounted(() => {
  refreshData()
})

onActivated(() => {
  refreshData()
})
</script>

<style scoped>
.admin-review {
  width: 100%;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card :deep(.el-card__body) {
  padding: 15px;
}

.stat-content {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-card.pending .stat-value {
  color: #e6a23c;
}

.stat-card.approved .stat-value {
  color: #67c23a;
}

.stat-card.rejected .stat-value {
  color: #f56c6c;
}

.stat-card.cancelled .stat-value {
  color: #909399;
}

.stat-card.total .stat-value {
  color: #409eff;
}

.action-card .stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.refresh-icon {
  font-size: 24px;
  color: #409eff;
  margin-bottom: 5px;
}

.main-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 20px;
}

.course-name {
  color: #409EFF;
  cursor: pointer;
}

.course-name:hover {
  text-decoration: underline;
}

.capacity-info {
  text-align: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
}

.timeline {
  padding: 10px 0;
}

.timeline-content {
  padding: 10px;
}

.timeline-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.timeline-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.timeline-info {
  font-size: 12px;
  color: #909399;
}

.timeline-remark {
  margin-left: 15px;
}

.detail-actions {
  margin-top: 20px;
  text-align: right;
}
</style>
