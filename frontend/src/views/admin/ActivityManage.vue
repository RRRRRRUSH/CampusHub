<template>
  <div class="activity-manage">
    <el-card shadow="never" class="mb-4">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" style="width: 140px" clearable>
            <el-option label="报名中" value="registering" />
            <el-option label="进行中" value="execution" />
            <el-option label="核验中" value="judging" />
            <el-option label="已结束" value="finished" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜标题..." clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <div class="left-panel">
            <span class="header-title" :class="isInternal ? 'border-primary' : 'border-success'">
              {{ pageTitle }}
            </span>
          </div>
          <el-button :type="isInternal ? 'primary' : 'success'" icon="Plus" @click="handlePublish">
            {{ isInternal ? '发布校内活动' : '发布实践通知' }}
          </el-button>
        </div>
      </template>

      <el-table :data="filteredTableData" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" sortable align="center">
          <template #default="{ row }">
            <span class="text-xs text-gray-400">{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column label="活动信息" min-width="320">
          <template #default="{ row }">
            <div class="title-box">
              <el-tag v-if="!isInternal" type="success" effect="plain" size="small">通知</el-tag>
              <el-tag v-else type="primary" effect="plain" size="small">校内</el-tag>

              <el-tooltip v-if="isInternal" :content="row.format === 'online' ? '线上活动' : '线下活动'" placement="top">
                <el-icon class="ml-1 text-gray-500" :size="16">
                  <Monitor v-if="row.format === 'online'" />
                  <Location v-else />
                </el-icon>
              </el-tooltip>

              <span class="title ml-1" @click="goToDetail(row)">
                {{ row.title }}
              </span>
            </div>

            <div v-if="isInternal" class="mt-2 ml-1 flex flex-wrap gap-2">
              <el-tag size="small" type="info" effect="dark">{{ row.hours }} 工时</el-tag>
              <el-tag v-if="row.needPhoto" size="small" type="warning">需现场图</el-tag>

              <el-tag v-if="hasRestrictions(row)" size="small" type="danger" effect="plain">
                限制范围
              </el-tag>
              <el-tag v-else size="small" type="success" effect="plain">面向全校</el-tag>
            </div>

            <div v-else class="mt-1 text-xs text-gray-400">
              发布于 {{ row.publishTime }}
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="isInternal" label="活动时间" width="220">
          <template #default="{ row }">
            <div class="text-xs text-gray-500 mb-1">
              <el-icon><Calendar /></el-icon> 活动时段
            </div>
            <div class="text-sm font-medium">
              <div>{{ formatDateTime(row.activityStartTime) }}</div>
              <div class="text-gray-400 text-center text-xs">至</div>
              <div>{{ formatDateTime(row.activityEndTime) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="isInternal" label="报名进度" width="160" align="center">
          <template #default="{ row }">
            <div v-if="row.quota <= 0">
              <div class="text-2xl font-bold text-success">{{ row.joined }}</div>
              <div class="text-xs text-gray-500">已报名 (不限额)</div>
            </div>
            <div v-else>
              <el-progress
                :percentage="calculatePercent(row.joined, row.quota)"
                :status="row.joined >= row.quota ? 'success' : ''"
              >
                <template #default>
                  <span class="font-bold text-sm">{{ row.joined }} / {{ row.quota }}</span>
                </template>
              </el-progress>
              <div class="text-xs text-gray-400 mt-1">
                截止: {{ formatShortDate(row.regEndTime) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" effect="light">
              {{ getStatusTag(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="isInternal">
              <el-button type="primary" size="small" @click="goToDetail(row)">
                管理执行
              </el-button>
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>

            <template v-else>
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Location, Monitor, Calendar } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi } from '@/api/activity'
import { getStatusTag } from '@/utils/dict'
import type { EventItem } from '@/types/model'

const props = defineProps<{ pageType: 'internal' | 'external' }>()
const router = useRouter()

const isInternal = computed(() => props.pageType === 'internal')
const pageTitle = computed(() => isInternal.value ? '校内志愿活动管理' : '校外实践通知列表')

const searchForm = reactive({ status: '', keyword: '' })
const loading = ref(false)
const list = ref<EventItem[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    list.value = await activityApi.getList(props.pageType)
  } finally {
    loading.value = false
  }
}

const filteredTableData = computed(() => {
  return list.value.filter(item => {
    const statusMatch = !searchForm.status || item.status === searchForm.status
    const keywordMatch = !searchForm.keyword || item.title.includes(searchForm.keyword)
    return statusMatch && keywordMatch
  })
})

const handlePublish = () => {
  router.push({ path: '/admin/activity/publish', query: { type: props.pageType } })
}

const goToDetail = (row: any) => {
  if (!isInternal.value) return handleEdit(row)
  router.push(`/admin/activity/manage/${row.id}`)
}

const handleEdit = (row: any) => {
  router.push({
    path: '/admin/activity/publish',
    query: { type: row.sourceType, id: row.id }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定要删除【${row.title}】吗？此操作不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除' }
  ).then(async () => {
    await activityApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

// 辅助函数：判断是否有范围限制
const hasRestrictions = (row: any) => {
  return (row.limitCampus && row.limitCampus.length > 0) ||
    (row.limitCollege && row.limitCollege.length > 0) ||
    (row.limitGrade && row.limitGrade.length > 0)
}

const calculatePercent = (c: number = 0, t: number = 0) => {
  if (!t || t <= 0) return 0
  return Math.min(Math.floor((c/t)*100), 100)
}

const formatDateTime = (str: string) => str ? str.slice(0, 16) : '-'
const formatShortDate = (str: string) => str ? str.slice(5, 16) : '-'

watch(() => props.pageType, fetchData)
onMounted(fetchData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-title { font-size: 18px; font-weight: bold; border-left: 4px solid; padding-left: 10px; }
.border-primary { border-color: #409EFF; }
.border-success { border-color: #67C23A; }

.title-box { display: flex; align-items: center; cursor: pointer; }
.title { font-weight: bold; font-size: 15px; color: #303133; }
.title:hover { color: #409EFF; }

.ml-1 { margin-left: 4px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mb-1 { margin-bottom: 4px; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 13px; }
.text-2xl { font-size: 1.5rem; }
.text-gray-500 { color: #909399; }
.text-gray-400 { color: #c0c4cc; }
.text-success { color: #67C23A; }
.font-medium { font-weight: 500; }
.font-bold { font-weight: 700; }
.flex { display: flex; }
.flex-wrap { flex-wrap: wrap; }
.gap-2 { gap: 8px; }
</style>
