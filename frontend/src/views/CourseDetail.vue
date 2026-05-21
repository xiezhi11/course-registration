<template>
  <div class="course-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <span class="title">{{ course?.name }}</span>
          <div class="actions">
            <el-button
              v-if="canRegister"
              type="success"
              :loading="registering"
              @click="handleRegister"
            >
              立即报名
            </el-button>
            <el-button
              v-if="canCancel"
              type="warning"
              @click="handleCancel"
            >
              取消报名
            </el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="课程类型">
          <el-tag>{{ course?.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="课程状态">
          <el-tag :type="getStatusType(course?.status)">
            {{ getStatusText(course?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="讲师">
          {{ course?.lecturer }}
        </el-descriptions-item>
        <el-descriptions-item label="培训地点">
          {{ course?.location || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ formatDateTime(course?.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ formatDateTime(course?.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="报名名额">
          <span class="capacity">
            <el-progress
              :percentage="registrationPercentage"
              :status="course?.isFull ? 'exception' : ''"
              style="width: 200px; display: inline-block; vertical-align: middle;"
            />
            <span class="capacity-text">
              {{ course?.registeredCount }}/{{ course?.maxCapacity }}
              (剩余{{ course?.maxCapacity - course?.registeredCount }}个)
            </span>
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="取消截止时间">
          {{ course?.cancelDeadline ? formatDateTime(course.cancelDeadline) : '开课前可取消' }}
        </el-descriptions-item>
        <el-descriptions-item label="我的报名状态" v-if="myRegistration">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-tag :type="getRegistrationType(myRegistration?.status)">
              {{ getRegistrationText(myRegistration?.status) }}
            </el-tag>
            <el-tag
              v-if="myRegistration?.registrationCount > 1"
              type="info"
              size="small"
            >
              第{{ myRegistration.registrationCount }}次报名
            </el-tag>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="课程描述" :span="2">
          {{ course?.description || '暂无描述' }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="myRegistration" class="registration-info">
        <h3>我的报名信息</h3>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报名时间">
            {{ formatDateTime(myRegistration?.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核时间" v-if="myRegistration?.reviewTime">
            {{ formatDateTime(myRegistration?.reviewTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见" v-if="myRegistration?.reviewComment" :span="2">
            {{ myRegistration?.reviewComment }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="historyList.length > 0" class="history-section">
          <h3>我的报名历史</h3>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in historyList"
              :key="index"
              :timestamp="formatDateTime(item.createTime)"
              placement="top"
              :type="getRegistrationType(item.status)"
            >
              <div class="history-item">
                <div class="history-header">
                  <span class="history-count">第{{ item.registrationCount }}次报名</span>
                  <el-tag :type="getRegistrationType(item.status)" effect="dark">
                    {{ getRegistrationText(item.status) }}
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
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const store = useStore()

const isAdmin = computed(() => store.getters.isAdmin)

const loading = ref(false)
const registering = ref(false)
const course = ref(null)
const myRegistration = ref(null)
const historyList = ref([])

const formatDateTime = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    CLOSED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    CLOSED: '已关闭'
  }
  return map[status] || status
}

const getRegistrationType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const getRegistrationText = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const registrationPercentage = computed(() => {
  if (!course.value) return 0
  return Math.round(course.value.registeredCount / course.value.maxCapacity * 100)
})

const canRegister = computed(() => {
  if (isAdmin.value) return false
  if (!course.value) return false
  if (course.value.status !== 'PUBLISHED') return false
  if (dayjs(course.value.endTime).isBefore(dayjs())) return false
  if (course.value.registeredCount >= course.value.maxCapacity) return false
  if (myRegistration.value && ['PENDING', 'APPROVED'].includes(myRegistration.value.status)) return false
  return true
})

const canCancel = computed(() => {
  if (isAdmin.value) return false
  if (!myRegistration.value) return false
  if (!['PENDING', 'APPROVED'].includes(myRegistration.value.status)) return false
  if (course.value?.cancelDeadline) {
    return dayjs(course.value.cancelDeadline).isAfter(dayjs())
  }
  return course.value?.startTime ? dayjs(course.value.startTime).isAfter(dayjs()) : false
})

const loadCourse = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await api.get(`/courses/${id}`)
    course.value = res.data
    course.value.isFull = course.value.registeredCount >= course.value.maxCapacity

    const regRes = await api.get('/registrations/my', { params: { page: 1, size: 100 } })
    const registrations = regRes.data.content || []
    
    const courseRegistrations = registrations.filter(r => r.courseId === parseInt(id))
    myRegistration.value = courseRegistrations.find(
      r => r.status === 'PENDING' || r.status === 'APPROVED'
    )
    
    historyList.value = courseRegistrations.slice().sort(
      (a, b) => dayjs(b.createTime).valueOf() - dayjs(a.createTime).valueOf()
    )
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const handleRegister = async () => {
  try {
    await ElMessageBox.confirm(`确定要报名《${course.value.name}》吗？`, '确认报名', {
      type: 'info'
    })
  } catch {
    return
  }

  registering.value = true
  try {
    await api.post('/registrations/register', null, { params: { courseId: course.value.id } })
    ElMessage.success('报名成功')
    loadCourse()
  } catch (e) {
    console.error(e)
  } finally {
    registering.value = false
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm(`确定要取消《${course.value.name}》的报名吗？`, '确认取消', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await api.post(`/registrations/${myRegistration.value.id}/cancel`)
    ElMessage.success('取消报名成功')
    loadCourse()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadCourse()
})

onActivated(() => {
  loadCourse()
})
</script>

<style scoped>
.course-detail {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 15px;
}

.title {
  font-size: 18px;
  font-weight: bold;
  flex: 1;
}

.actions {
  display: flex;
  gap: 10px;
}

.capacity {
  display: flex;
  align-items: center;
  gap: 10px;
}

.capacity-text {
  font-size: 14px;
  color: #606266;
}

.registration-info {
  margin-top: 30px;
}

.registration-info h3 {
  margin-bottom: 15px;
  font-size: 16px;
}

.history-section {
  margin-top: 30px;
}

.history-section h3 {
  margin-bottom: 15px;
  font-size: 16px;
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
