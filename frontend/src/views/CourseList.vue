<template>
  <div class="course-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">课程列表</span>
          <div class="actions">
            <el-button v-if="isAdmin" type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              新建课程
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="课程名称">
          <el-input v-model="queryForm.name" placeholder="请输入课程名称" clearable />
        </el-form-item>
        <el-form-item label="课程类型">
          <el-select v-model="queryForm.type" placeholder="请选择课程类型" clearable>
            <el-option label="技术培训" value="技术培训" />
            <el-option label="管理培训" value="管理培训" />
            <el-option label="技能培训" value="技能培训" />
            <el-option label="软技能" value="软技能" />
          </el-select>
        </el-form-item>
        <el-form-item label="讲师">
          <el-input v-model="queryForm.lecturer" placeholder="请输入讲师姓名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadCourses">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetQuery">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="courses" v-loading="loading" stripe>
        <el-table-column prop="name" label="课程名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="goToDetail(row.id)">
              {{ row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="课程类型" width="100" />
        <el-table-column prop="lecturer" label="讲师" width="100" />
        <el-table-column label="时间" min-width="220">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.startTime) }}</div>
            <div class="sub-text">至 {{ formatDateTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="报名名额" width="140">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.registeredCount / row.maxCapacity * 100)"
              :status="row.isFull ? 'exception' : ''"
            />
            <div class="capacity-text">
              {{ row.registeredCount }}/{{ row.maxCapacity }}
              <span class="available">(剩余{{ row.maxCapacity - row.registeredCount }}个)</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="课程状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名状态" width="130">
          <template #default="{ row }">
            <div v-if="row.registrationStatus" style="display: flex; align-items: center; gap: 4px;">
              <el-tag :type="getRegistrationType(row.registrationStatus)">
                {{ getRegistrationText(row.registrationStatus) }}
              </el-tag>
              <el-tag
                v-if="row.registrationCount > 1"
                type="info"
                size="small"
              >
                第{{ row.registrationCount }}次
              </el-tag>
            </div>
            <span v-else class="sub-text">未报名</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="goToDetail(row.id)">
              详情
            </el-button>
            <el-button
              v-if="canRegister(row)"
              size="small"
              type="success"
              :loading="registeringId === row.id"
              @click="handleRegister(row)"
            >
              报名
            </el-button>
            <el-button
              v-if="canCancel(row)"
              size="small"
              type="warning"
              @click="handleCancel(row)"
            >
              取消报名
            </el-button>
            <template v-if="isAdmin">
              <el-dropdown>
                <el-button size="small" type="primary">
                  管理<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-if="row.status === 'DRAFT'"
                      @click="handlePublish(row)"
                    >
                      发布课程
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="row.status === 'PUBLISHED'"
                      @click="handleClose(row)"
                    >
                      关闭课程
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleEdit(row)">
                      编辑课程
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleDelete(row)">
                      删除课程
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
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
        @size-change="loadCourses"
        @current-change="loadCourses"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑课程' : '新建课程'" width="600px" @close="resetCourseForm">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseFormRules" label-width="100px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="courseForm.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="课程类型" prop="type">
          <el-select v-model="courseForm.type" style="width: 100%" placeholder="请选择课程类型">
            <el-option label="技术培训" value="技术培训" />
            <el-option label="管理培训" value="管理培训" />
            <el-option label="技能培训" value="技能培训" />
            <el-option label="软技能" value="软技能" />
          </el-select>
        </el-form-item>
        <el-form-item label="讲师" prop="lecturer">
          <el-input v-model="courseForm.lecturer" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="courseForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            :disabled-date="disabledStartDate"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="courseForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            :disabled-date="disabledEndDate"
          />
        </el-form-item>
        <el-form-item label="人数上限" prop="maxCapacity">
          <el-input-number v-model="courseForm.maxCapacity" :min="1" :max="1000" />
          <div class="form-tip">人数必须在1-1000之间</div>
        </el-form-item>
        <el-form-item label="取消截止时间" prop="cancelDeadline">
          <el-date-picker
            v-model="courseForm.cancelDeadline"
            type="datetime"
            placeholder="选择取消截止时间（可选）"
            style="width: 100%"
            :disabled-date="disabledCancelDate"
          />
          <div class="form-tip">取消截止时间必须早于课程开始时间</div>
        </el-form-item>
        <el-form-item label="培训地点">
          <el-input v-model="courseForm.location" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="courseForm.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import dayjs from 'dayjs'

const router = useRouter()
const store = useStore()

const isAdmin = computed(() => store.getters.isAdmin)

const loading = ref(false)
const submitting = ref(false)
const registeringId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const courses = ref([])
const total = ref(0)
const myRegistrations = ref([])

const queryForm = ref({
  name: '',
  type: '',
  lecturer: '',
  status: '',
  page: 1,
  size: 10
})

const courseForm = ref({
  id: null,
  name: '',
  type: '',
  lecturer: '',
  startTime: null,
  endTime: null,
  maxCapacity: 30,
  location: '',
  description: '',
  cancelDeadline: null
})

const courseFormRef = ref(null)

const validateEndTime = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择结束时间'))
    return
  }
  if (courseForm.value.startTime && value <= courseForm.value.startTime) {
    callback(new Error('结束时间必须晚于开始时间'))
    return
  }
  callback()
}

const validateCancelDeadline = (rule, value, callback) => {
  if (value) {
    if (courseForm.value.startTime && value >= courseForm.value.startTime) {
      callback(new Error('取消截止时间必须早于开始时间'))
      return
    }
  }
  callback()
}

const courseFormRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { max: 200, message: '课程名称长度不能超过200', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择课程类型', trigger: 'change' }
  ],
  lecturer: [
    { required: true, message: '请输入讲师姓名', trigger: 'blur' },
    { max: 100, message: '讲师姓名长度不能超过100', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, validator: validateEndTime, trigger: 'change' }
  ],
  maxCapacity: [
    { required: true, message: '请输入人数上限', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '人数上限必须在1-1000之间', trigger: 'blur' }
  ],
  cancelDeadline: [
    { validator: validateCancelDeadline, trigger: 'change' }
  ]
}

const disabledStartDate = (date) => {
  return date && date < dayjs().startOf('day').toDate()
}

const disabledEndDate = (date) => {
  return date && date < dayjs().startOf('day').toDate()
}

const disabledCancelDate = (date) => {
  return date && date < dayjs().startOf('day').toDate()
}

const resetCourseForm = () => {
  courseFormRef.value?.resetFields()
}

const formatDateTime = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
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

const loadMyRegistrations = async () => {
  try {
    const res = await api.get('/registrations/my', { params: { page: 1, size: 100 } })
    myRegistrations.value = res.data.content || []
  } catch (e) {
    console.error(e)
  }
}

const loadCourses = async () => {
  loading.value = true
  try {
    const params = { ...queryForm.value }
    if (!params.name) delete params.name
    if (!params.type) delete params.type
    if (!params.lecturer) delete params.lecturer
    if (!params.status) delete params.status

    const res = await api.get('/courses', { params })
    courses.value = res.data.content || []
    
    courses.value.forEach(course => {
      const activeReg = myRegistrations.value.find(
        r => r.courseId === course.id && 
        (r.status === 'PENDING' || r.status === 'APPROVED')
      )
      course.registrationStatus = activeReg ? activeReg.status : null
      course.registrationId = activeReg ? activeReg.id : null
      course.registrationCount = activeReg ? (activeReg.registrationCount || 1) : 0
      course.isFull = course.registeredCount >= course.maxCapacity
    })
    total.value = res.data.totalElements || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.value = {
    name: '',
    type: '',
    lecturer: '',
    status: '',
    page: 1,
    size: 10
  }
  loadCourses()
}

const canRegister = (course) => {
  if (isAdmin.value) return false
  if (course.status !== 'PUBLISHED') return false
  if (dayjs(course.endTime).isBefore(dayjs())) return false
  if (course.isFull) return false
  if (course.registrationStatus && 
      ['PENDING', 'APPROVED'].includes(course.registrationStatus)) return false
  return true
}

const canCancel = (course) => {
  if (isAdmin.value) return false
  if (!course.registrationStatus) return false
  if (!['PENDING', 'APPROVED'].includes(course.registrationStatus)) return false
  if (course.cancelDeadline) {
    return dayjs(course.cancelDeadline).isAfter(dayjs())
  }
  return dayjs(course.startTime).isAfter(dayjs())
}

const goToDetail = (id) => {
  router.push(`/courses/${id}`)
}

const handleRegister = async (course) => {
  try {
    await ElMessageBox.confirm(`确定要报名《${course.name}》吗？`, '确认报名', {
      type: 'info'
    })
  } catch {
    return
  }

  registeringId.value = course.id
  try {
    await api.post('/registrations/register', null, { params: { courseId: course.id } })
    ElMessage.success('报名成功')
    await loadMyRegistrations()
    await loadCourses()
  } catch (e) {
    console.error(e)
  } finally {
    registeringId.value = null
  }
}

const handleCancel = async (course) => {
  try {
    await ElMessageBox.confirm(`确定要取消《${course.name}》的报名吗？`, '确认取消', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await api.post(`/registrations/${course.registrationId}/cancel`)
    ElMessage.success('取消报名成功')
    await loadMyRegistrations()
    await loadCourses()
  } catch (e) {
    console.error(e)
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  courseForm.value = {
    id: null,
    name: '',
    type: '',
    lecturer: '',
    startTime: null,
    endTime: null,
    maxCapacity: 30,
    location: '',
    description: '',
    cancelDeadline: null
  }
  dialogVisible.value = true
}

const handleEdit = (course) => {
  isEdit.value = true
  courseForm.value = { ...course }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await courseFormRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await api.put(`/courses/${courseForm.value.id}`, courseForm.value)
      ElMessage.success('更新成功')
    } else {
      await api.post('/courses', courseForm.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refreshData()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handlePublish = async (course) => {
  try {
    await ElMessageBox.confirm(`确定要发布《${course.name}》吗？`, '确认发布', {
      type: 'info'
    })
  } catch {
    return
  }

  try {
    await api.post(`/courses/${course.id}/publish`)
    ElMessage.success('发布成功')
    refreshData()
  } catch (e) {
    console.error(e)
  }
}

const handleClose = async (course) => {
  try {
    await ElMessageBox.confirm(`确定要关闭《${course.name}》吗？关闭后将不能再报名。`, '确认关闭', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await api.post(`/courses/${course.id}/close`)
    ElMessage.success('关闭成功')
    refreshData()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (course) => {
  try {
    await ElMessageBox.confirm(`确定要删除《${course.name}》吗？此操作不可恢复。`, '确认删除', {
      type: 'error'
    })
  } catch {
    return
  }

  try {
    await api.delete(`/courses/${course.id}`)
    ElMessage.success('删除成功')
    refreshData()
  } catch (e) {
    console.error(e)
  }
}

const refreshData = async () => {
  await loadMyRegistrations()
  await loadCourses()
}

onMounted(() => {
  refreshData()
})

onActivated(() => {
  refreshData()
})
</script>

<style scoped>
.course-list {
  width: 100%;
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

.search-form {
  margin-bottom: 20px;
}

.sub-text {
  color: #909399;
  font-size: 12px;
}

.capacity-text {
  font-size: 12px;
  color: #606266;
  text-align: center;
  margin-top: 4px;
}

.available {
  color: #67c23a;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
</style>
