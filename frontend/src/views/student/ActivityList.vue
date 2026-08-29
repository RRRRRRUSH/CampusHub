<template>
  <div class="student-page">
    <el-card shadow="never" v-loading="loading">
      <el-tabs v-model="activeTab" @tab-change="fetchData">

        <el-tab-pane label="校内志愿招募" name="internal">
          <div class="filter-bar mb-4">
            <el-input v-model="searchKey" placeholder="搜索活动名称" prefix-icon="Search" style="width: 300px" />
          </div>

          <div v-if="filteredInternalList.length === 0" class="empty-placeholder text-center text-gray-400 py-10">
            暂无符合条件的活动 (可能受校区限制)
          </div>

          <div class="activity-grid">
            <el-card v-for="item in filteredInternalList" :key="item.id" shadow="hover" class="item-card">
              <div class="card-header bg-blue-50 p-3 relative">
                <div class="flex justify-between items-center">
                  <el-tag effect="dark" size="small">校内</el-tag>
                  <el-tag :type="getStatusTag(item.status).type" effect="light" size="small">
                    {{ getStatusTag(item.status).label }}
                  </el-tag>
                </div>
                <h3 class="mt-3 font-bold text-base text-gray-800 text-ellipsis" :title="item.title">{{ item.title }}</h3>
              </div>

              <div class="p-4">
                <div class="text-sm text-gray-500 space-y-2">
                  <div class="flex items-center">
                    <el-icon class="mr-1"><Timer /></el-icon>
                    <span>认定工时：<b class="text-green-600">{{ item.hours }}h</b></span>
                  </div>
                  <div class="flex items-center">
                    <el-icon class="mr-1"><LocationInformation /></el-icon>
                    <span class="truncate">{{ item.format === 'online' ? '线上开展' : (item.location || '待定') }}</span>
                  </div>
                  <div class="flex items-center">
                    <el-icon class="mr-1"><Clock /></el-icon>
                    <span :class="isOverTime(item.regEndTime) ? 'text-red-400 line-through' : ''">
                      截止：{{ formatShortTime(item.regEndTime) }}
                    </span>
                  </div>
                </div>

                <div class="mt-3">
                  <div v-if="item.quota <= 0" class="flex items-center justify-between text-xs text-blue-500">
                    <span>已报: {{ item.joined }}</span>
                    <el-tag size="small" effect="plain">不限名额</el-tag>
                  </div>
                  <el-progress
                    v-else
                    :percentage="calcPercent(item.joined, item.quota)"
                    :status="item.joined >= item.quota ? 'success' : ''"
                  >
                    <template #default><span class="text-xs">{{ item.joined }}/{{ item.quota }}</span></template>
                  </el-progress>
                </div>

                <el-button type="primary" plain class="w-full mt-4" @click="openDetail(item)">
                  查看详情 & 报名
                </el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="校外实践通知" name="external">
          <el-table :data="externalList" stripe>
            <el-table-column label="标题" min-width="300">
              <template #default="{ row }">
                <span class="font-bold cursor-pointer hover:text-blue-500" @click="openDetail(row)">{{ row.title }}</span>
                <el-tag size="small" type="success" class="ml-2">通知</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.publishTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="currentDetail?.title" width="700px" top="5vh">
      <div v-if="currentDetail">
        <div v-if="currentDetail.sourceType === 'internal'">
          <el-descriptions :column="2" border class="mb-4">
            <el-descriptions-item label="活动形式">
              <el-tag :type="currentDetail.format === 'online' ? 'success' : 'warning'">
                {{ currentDetail.format === 'online' ? '线上活动' : '线下实地' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="认定工时">
              <span class="text-lg font-bold text-green-600">+{{ currentDetail.hours }}</span> 小时
            </el-descriptions-item>
            <el-descriptions-item label="活动地点" :span="2">
              {{ currentDetail.format === 'online' ? '无需到场，线上参与' : currentDetail.location }}
            </el-descriptions-item>

            <el-descriptions-item label="活动时间" :span="2">
              {{ formatDateTime(currentDetail.activityStartTime) }} 至 {{ formatDateTime(currentDetail.activityEndTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="报名时间" :span="2">
              {{ formatDateTime(currentDetail.regStartTime) }} 至 {{ formatDateTime(currentDetail.regEndTime) }}
            </el-descriptions-item>

            <el-descriptions-item label="名额情况">
              <span class="text-blue-600">{{ currentDetail.joined }}</span> / {{ currentDetail.quota <= 0 ? '不限' : currentDetail.quota }}
            </el-descriptions-item>
            <el-descriptions-item label="现场认证">
              {{ currentDetail.needPhoto ? '需上传现场照片' : '仅需签到' }}
            </el-descriptions-item>

            <el-descriptions-item label="面向对象" :span="2">
               <span v-if="!currentDetail.limitCampus && !currentDetail.limitCollege && !currentDetail.limitGrade">
                 全校学生
               </span>
              <span v-else class="text-orange-500 font-bold">
                 {{ [currentDetail.limitCampus, currentDetail.limitCollege, currentDetail.limitGrade].filter(Boolean).join(' + ') }}
               </span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="section-header">活动详情说明</div>
          <div class="desc-content">
            {{ currentDetail.description || '暂无详细说明' }}
          </div>
        </div>

        <div v-else>
          <div class="text-sm text-gray-400 mb-4">发布时间：{{ formatDateTime(currentDetail.publishTime) }}</div>
          <div class="desc-content min-h-[200px]">
            {{ currentDetail.description || '暂无内容' }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>

        <template v-if="currentDetail?.sourceType === 'internal'">
          <el-button
            v-if="currentDetail.status === 'registering' && !isOverTime(currentDetail.regEndTime)"
            type="primary"
            :loading="submitting"
            :disabled="currentDetail.quota > 0 && currentDetail.joined >= currentDetail.quota"
            @click="confirmSignup"
          >
            {{ (currentDetail.quota > 0 && currentDetail.joined >= currentDetail.quota) ? '名额已满' : '确认报名' }}
          </el-button>

          <el-button v-else disabled>
            {{ isOverTime(currentDetail.regEndTime) ? '报名已截止' : '非报名期' }}
          </el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Search, Timer, Clock, LocationInformation } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi } from '@/api/activity'
import { getStatusTag } from '@/utils/dict'
import { useUserStore } from '@/stores/user'

// 🟢 手动补全类型定义，解决报红
interface EventItem {
  id: number
  title: string
  sourceType: string
  format: string
  status: string
  hours: number
  quota: number
  joined: number
  location?: string
  regStartTime?: string
  regEndTime?: string
  activityStartTime?: string
  activityEndTime?: string
  publishTime?: string
  description?: string
  needPhoto?: boolean
  // 新增字段
  limitCampus?: string
  limitCollege?: string
  limitGrade?: string
}

const userStore = useUserStore()
const activeTab = ref('internal')
const searchKey = ref('')
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const currentDetail = ref<EventItem | null>(null)

const internalList = ref<EventItem[]>([])
const externalList = ref<EventItem[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'internal') {
      // 🟢 关键修正：传入 userStore.id，让后端根据学生校区进行过滤
      // TS 忽略检查，因为 activityApi 定义还没更新类型
      // @ts-ignore
      const res = await activityApi.getList('internal', userStore.id)
      internalList.value = (res as unknown as EventItem[]) || []
    } else {
      // @ts-ignore
      const res = await activityApi.getList('external')
      externalList.value = (res as unknown as EventItem[]) || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const filteredInternalList = computed(() => {
  if (!searchKey.value) return internalList.value
  return internalList.value.filter(item => item.title.includes(searchKey.value))
})

onMounted(fetchData)

const openDetail = (item: EventItem) => {
  currentDetail.value = item
  dialogVisible.value = true
}

const confirmSignup = () => {
  if (!currentDetail.value) return
  ElMessageBox.confirm(`确定要报名参加【${currentDetail.value.title}】吗？`, '报名确认').then(async () => {
    submitting.value = true
    try {
      // 🟢 修复：确保 userStore 有数据
      if (!userStore.id) throw new Error('用户信息未加载，请刷新页面')

      await activityApi.signup(currentDetail.value!.id, {
        userId: userStore.id, // 新逻辑传 userId
        name: userStore.name,
        sid: userStore.username,
        college: userStore.department
      })
      ElMessage.success('报名成功！请准时参加活动。')

      currentDetail.value!.joined += 1
      const item = internalList.value.find(i => i.id === currentDetail.value!.id)
      if (item) item.joined += 1

      dialogVisible.value = false
    } catch (err: any) {
      ElMessage.error(err.message || '报名失败')
    } finally {
      submitting.value = false
    }
  })
}

const isOverTime = (timeStr?: string) => {
  if (!timeStr) return false
  return new Date() > new Date(timeStr)
}

const calcPercent = (joined = 0, quota = 0) => {
  if (quota <= 0) return 0
  return Math.min(Math.floor((joined / quota) * 100), 100)
}

const formatDateTime = (str?: string) => str ? str.slice(0, 16) : '待定'
const formatShortTime = (str?: string) => str ? str.slice(5, 16) : '-'
</script>

<style scoped>
.student-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.activity-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.bg-blue-50 { background-color: #ecf5ff; }
.text-ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.w-full { width: 100%; }
.space-y-2 > :not([hidden]) ~ :not([hidden]) { margin-top: 0.5rem; }
.section-header { font-weight: bold; border-left: 4px solid #409EFF; padding-left: 10px; margin: 20px 0 10px; font-size: 15px; }
.desc-content { background: #f8f9fa; padding: 15px; border-radius: 4px; line-height: 1.6; white-space: pre-wrap; color: #606266; }
.text-red-400 { color: #f87171; }
.line-through { text-decoration: line-through; }
</style>
