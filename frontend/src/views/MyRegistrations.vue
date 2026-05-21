<template>
  <div class="my-registrations">
    <el-card>
      <template #header>
        <span class="title">我的报名</span>
      </template>

      <el-table :data="registrations" v-loading="loading" stripe>
        <el-table-column label="课程名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="goToCourse(row.courseId)">
              {{ row.course?.name }}
            </el-link>
            <el-tag
              v-if="row.registrationCount > 1"
              type="info"
              size="small"
              style="margin-left: 8px;"
            >
              第{{ row.registrationCount }}次报名
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="课程类型" width="100">
          <template #default="{ row }">
            {{ row.course?.type }}
          </template>
        </el-table-column>
        <el-table-column label="讲师" width="100">
          <template #default="{ row }">
            {{ row.course?.lecturer }}
          </template>
        </el-table-column>
        <el-table-column label="培训时间" min-width="220">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.course?.startTime) }}</div>
            <div class="sub-text">至 {{ formatDateTime(row.course?.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.reviewComment || row.cancelReason || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="goToCourse(row.courseId)">
              查看课程
            </el-button>
            <el-button
              size="small"
              type="info"
              @click="viewHistory(row)"
            >
              报名历史
            </el-button>
            <el-button
              v-if="canCancel(row)"
              size="small"
              type="warning"
              @click="handleCancel(row)"
            >
              取消报名
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadRegistrations"
        @current-change="loadRegistrations"
      />
    </el-card>

    <!-- 报名历史弹窗 -->
    <el-dialog v-model="historyDialogVisible" title="报名历史" width="700px">
      <div v-if="historyList.length > 0">
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in historyList"
            :key="index"
            :timestamp="formatDateTime(item.createTime)"
            placement="top"
            :type="getStatusType(item.status)"
          >
            <div class="history-item">
              <div class="history-header">
                <span class="history-count">第{{ item.registrationCount }}次报名</span>
                <el-tag :type="getStatusType(item.status)" effect="dark">
                  {{ getStatusText(item.status) }}
                </el-tag>
              </div>
              <div class="history-detail">
                <span>报名时间：{{ formatDateTime(item.createTime) }}</span>
              </div>
              <div v-if="item.reviewComment" class="history-detail">
                <span>审核意见：{{ item.reviewComment }}</span>
              </div>
              <div v-if="item.cancelReason" class="history-detail">
                <span>取消原因：{{ item.cancelReason }}</span>
              </div>
              <div v-if="item.reviewTime" class="history-detail">
                <span>审核时间：{{ formatDateTime(item.reviewTime) }}</span>
              </div>
              <div v-if="item.cancelTime" class="history-detail">
                <span>取消时间：{{ formatDateTime(item.cancelTime) }}</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <el-empty v-else description="暂无报名历史记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const registrations = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const historyDialogVisible = ref(false)
const historyList = ref([])

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

const canCancel = (row) => {
  if (!['PENDING', 'APPROVED'].includes(row.status)) return false
  if (row.course?.cancelDeadline) {
    return dayjs(row.course.cancelDeadline).isAfter(dayjs())
  }
  return row.course?.startTime ? dayjs(row.course.startTime).isAfter(dayjs()) : false
}

const loadRegistrations = async () => {
  loading.value = true
  try {
    const res = await api.get('/registrations/my', {
      params: { page: page.value, size: size.value }
    })
    registrations.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const viewHistory = async (row) => {
  historyDialogVisible.value = true
  try {
    const res = await api.get(`/registrations/${row.id}/chain`)
    historyList.value = (res.data || []).slice().reverse()
  } catch (e) {
    console.error(e)
    historyList.value = []
  }
}

const goToCourse = (courseId) => {
  router.push(`/courses/${courseId}`)
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要取消《${row.course?.name}》的报名吗？`, '确认取消', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await api.post(`/registrations/${row.id}/cancel`)
    ElMessage.success('取消报名成功')
    loadRegistrations()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadRegistrations()
})

onActivated(() => {
  loadRegistrations()
})
</script>

<style scoped>
.my-registrations {
  width: 100%;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.sub-text {
  color: #909399;
  font-size: 12px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.history-item {
  padding: 10px 0;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.history-count {
  font-weight: bold;
  font-size: 14px;
}

.history-detail {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}
</style>
