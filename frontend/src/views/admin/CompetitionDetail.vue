<template>
  <div class="event-detail" v-loading="loading">
    <div class="detail-header">
      <el-breadcrumb separator="/" class="mb-4">
        <el-breadcrumb-item :to="backPath">返回列表</el-breadcrumb-item>
        <el-breadcrumb-item>执行驾驶舱</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold inline align-middle">{{ info.title }}</h1>
          <el-tag :type="getStatusType(info.status)" effect="plain" class="ml-3">
            {{ getStatusLabel(info.status) }}
          </el-tag>
          <el-tag v-if="isCompetition" effect="dark" type="info" size="small" class="ml-2">
            {{ info.format === 'offline' ? '线下' : '线上' }}
          </el-tag>
        </div>
        <div class="space-x-3">
          <el-button @click="router.back()">返回</el-button>
          <el-button type="primary" icon="Download" @click="handleExport">导出名单</el-button>
        </div>
      </div>
    </div>

    <div class="main-body mt-5">
      <el-card shadow="never" class="mb-4">
        <el-steps :active="currentStep" align-center finish-status="success">
          <el-step title="报名阶段" />
          <el-step :title="getSecondStepTitle" />
          <el-step :title="isCompetition ? '评审打分' : '考勤核验'" />
          <el-step title="结果公示" />
          <el-step title="归档" />
        </el-steps>

        <div class="step-actions mt-6 text-center">
          <el-button v-if="info.status === 'registering' || info.status === 'open'" type="danger" @click="nextStep('execution')">
            截止报名，进入{{ isCompetition ? (info.format==='offline'?'比赛':'提交') : '执行' }}
          </el-button>

          <el-button v-else-if="info.status === 'execution'" type="warning" @click="nextStep('judging')">
            结束活动，进入{{ isCompetition ? '评审' : '核验' }}
          </el-button>

          <el-button v-else-if="info.status === 'judging'" type="success" @click="nextStep('publicity')">
            完成核验，发布公示
          </el-button>

          <el-button v-else-if="info.status === 'publicity'" type="primary" @click="nextStep('finished')">
            公示结束，归档 (自动发放学分)
          </el-button>

          <div v-else class="text-center">
            <div class="text-gray-500 bg-gray-100 py-2 rounded mb-2">项目已归档，学分已发放</div>
            <el-popconfirm
              title="警告：此操作将扣除已发放的学分，并删除认定记录，退回到评审阶段。确定吗？"
              confirm-button-type="danger"
              @confirm="handleRollback"
            >
              <template #reference>
                <el-button type="danger" plain size="small">撤销归档 (回退状态)</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-bold">参与名单 ({{ tableData.length }})</span>
            <div class="flex">
              <el-input v-model="keyword" placeholder="搜姓名/学号" prefix-icon="Search" style="width: 200px" />
            </div>
          </div>
        </template>

        <el-table :data="filteredData" border stripe>
          <el-table-column label="参与者信息" min-width="220">
            <template #default="{ row }">
              <div class="font-bold text-base">
                {{ isCompetition && row.teamName ? row.teamName : row.name }}
              </div>
              <div v-if="isCompetition && (row.leaderName || row.leaderId)" class="text-xs text-gray-500 mt-1">
                <span class="bg-blue-50 text-blue-600 px-1 rounded mr-1">队长</span>
                <span>{{ row.leaderName || row.leaderId }}</span>
                <div v-if="row.members && row.members.length > 0" class="mt-1">
                  <span class="bg-gray-100 text-gray-600 px-1 rounded mr-1">队员</span>
                  <span v-for="(m, idx) in row.members" :key="idx" class="mr-2">
                    {{ typeof m === 'string' ? m : (m.name || m.userName || '未知') }}
                  </span>
                </div>
              </div>
              <div v-else class="text-xs text-gray-500 mt-1">
                {{ row.sid }} | {{ row.college }}
              </div>
            </template>
          </el-table-column>

          <template v-if="isCompetition">
            <el-table-column label="作品" width="100" align="center">
              <template #default="{ row }">
                <span v-if="info.format === 'offline'" class="text-gray-400 text-xs">线下参赛</span>
                <div v-else>
                  <el-button v-if="row.fileUrl" link type="primary" @click="downloadFile(row.fileUrl)">下载</el-button>
                  <span v-else class="text-gray-400 text-xs">未提交</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="150" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.awardLevel" type="warning">{{ row.awardLevel }}</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </template>

          <template v-else>
            <el-table-column label="考勤" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isValid === true" type="success">有效</el-tag>
                <el-tag v-else-if="row.isValid === false" type="danger">缺席</el-tag>
                <el-tag v-else type="info">待核验</el-tag>
              </template>
            </el-table-column>
          </template>

          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <div v-if="info.status === 'judging'">
                <template v-if="isCompetition">
                  <el-button link type="primary" @click="openGrade(row)">评分</el-button>
                </template>
                <template v-else>
                  <el-button link type="success" size="small" @click="handleVerify(row, true)">通过</el-button>
                  <el-button link type="danger" size="small" @click="handleVerify(row, false)">缺席</el-button>
                </template>
              </div>
              <span v-else class="text-gray-400 text-xs">查看模式</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog v-model="gradeVisible" title="录入成绩" width="400px">
      <el-select v-model="gradeLevel" class="w-full">
        <el-option label="一等奖" value="一等奖" />
        <el-option label="二等奖" value="二等奖" />
        <el-option label="三等奖" value="三等奖" />
        <el-option label="参与奖" value="参与奖" />
      </el-select>
      <template #footer>
        <el-button type="primary" @click="submitGrade">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { activityApi } from '@/api/activity'
import { competitionApi } from '@/api/competition'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const info = reactive<any>({})
const tableData = ref<any[]>([])
const keyword = ref('')

const isCompetition = computed(() => info.sourceType === 'internal' || info.type === 'competition' || info.mode === 'team')
const backPath = computed(() => isCompetition.value ? '/admin/competition/internal' : '/admin/activity/internal')

const getSecondStepTitle = computed(() => {
  if (!isCompetition.value) return '活动进行'
  return info.format === 'offline' ? '线下比赛' : '作品提交'
})

const loadData = async () => {
  loading.value = true
  try {
    const id = route.params.id
    if (!id) return

    if (route.path.includes('competition')) {
      const detail = await competitionApi.getDetail(id as string)
      if (detail) Object.assign(info, detail)
      tableData.value = await competitionApi.getParticipants(id as string)
    } else {
      const detail = await activityApi.getDetail(id as string)
      if (detail) Object.assign(info, detail)
      tableData.value = await activityApi.getParticipants(id as string)
    }
  } catch(e) {
    console.error(e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const filteredData = computed(() => tableData.value.filter(item => {
  const search = keyword.value.toLowerCase()
  const matchName = item.teamName?.includes(search) || item.name?.includes(search)
  const matchSid = item.sid?.includes(search)
  const matchLeader = item.leaderName?.includes(search)
  return !search || matchName || matchSid || matchLeader
}))

const currentStep = computed(() => {
  const map: Record<string, number> = { open: 0, registering: 0, execution: 1, judging: 2, publicity: 3, finished: 4 }
  return map[info.status] || 0
})

const nextStep = (target: string) => {
  let msg = `确定要进入下一阶段（${getStatusLabel(target)}）吗？`
  if (target === 'finished' && isCompetition.value) {
    msg = `⚠️ 警告：进入归档后，系统将自动为所有获奖学生发放学分。此操作建议在公示期结束后进行。`
  }

  ElMessageBox.confirm(msg, '警告', { type: 'warning' }).then(async () => {
    const api = isCompetition.value ? competitionApi : activityApi
    await api.changeStatus(info.id, target)
    info.status = target
    ElMessage.success('操作成功')
    if (target === 'finished') loadData()
  })
}

// 🟢 新增：竞赛回退逻辑
const handleRollback = async () => {
  try {
    await competitionApi.rollback(info.id)
    ElMessage.success('已成功撤销归档，学分已扣回')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '回退失败')
  }
}

const handleVerify = async (row: any, isValid: boolean) => {
  await activityApi.verifyAttendance(row.id, isValid)
  row.isValid = isValid
  ElMessage.success('已标记')
}

const gradeVisible = ref(false)
const gradeLevel = ref('')
const currentGradeRow = ref<any>(null)

const openGrade = (row: any) => {
  currentGradeRow.value = row
  gradeLevel.value = row.awardLevel || ''
  gradeVisible.value = true
}

const submitGrade = async () => {
  if (currentGradeRow.value) {
    await competitionApi.submitGrade(currentGradeRow.value.id, gradeLevel.value)
    currentGradeRow.value.awardLevel = gradeLevel.value
    gradeVisible.value = false
    ElMessage.success('评分成功')
  }
}

const downloadFile = (url: string) => {
  window.open(url, '_blank')
}

const handleExport = () => ElMessage.success('导出功能开发中...')
const getStatusLabel = (s: string) => ({ registering: '报名中', open: '报名中', execution: '进行中', judging: '核验/评审', publicity: '公示', finished: '归档' }[s] || s)
const getStatusType = (s: string) => ({ registering: 'success', open: 'success', execution: 'primary', judging: 'warning', publicity: 'danger', finished: 'info' }[s] || 'info')
</script>

<style scoped>
.event-detail { padding: 20px; min-height: 100vh; background: #f5f7fa; }
.detail-header { background: #fff; padding: 20px; border-bottom: 1px solid #e4e7ed; }
.main-body { max-width: 1200px; margin: 20px auto; }
.text-2xl { font-size: 1.5rem; }
.font-bold { font-weight: 700; }
.ml-3 { margin-left: 0.75rem; }
.mt-5 { margin-top: 1.25rem; }
.mb-4 { margin-bottom: 1rem; }
.space-x-3 > :not([hidden]) ~ :not([hidden]) { margin-left: 0.75rem; }
.flex { display: flex; }
.justify-between { justify-content: space-between; }
.items-center { align-items: center; }
.text-center { text-align: center; }
.w-full { width: 100%; }
.bg-blue-50 { background-color: #ecf5ff; }
.text-blue-600 { color: #409EFF; }
.bg-gray-100 { background-color: #f3f4f6; }
.text-gray-600 { color: #606266; }
.text-gray-500 { color: #909399; }
.rounded { border-radius: 4px; }
.px-1 { padding-left: 0.25rem; padding-right: 0.25rem; }
.mr-1 { margin-right: 0.25rem; }
.ml-2 { margin-left: 0.5rem; }
.mt-1 { margin-top: 0.25rem; }
.text-base { font-size: 1rem; }
.mr-2 { margin-right: 0.5rem; }
</style>
