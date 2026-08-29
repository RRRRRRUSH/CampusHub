<template>
  <div class="event-detail" v-loading="loading">
    <div class="detail-header">
      <el-breadcrumb separator="/" class="mb-4">
        <el-breadcrumb-item :to="backPath">返回列表</el-breadcrumb-item>
        <el-breadcrumb-item>执行驾驶舱</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="flex justify-between items-start">
        <div class="header-left">
          <div class="flex items-center">
            <h1 class="text-2xl font-bold inline align-middle">{{ info.title }}</h1>
            <el-tag :type="getStatusType(info.status)" effect="dark" class="ml-3">
              {{ getStatusLabel(info.status) }}
            </el-tag>
            <el-tag v-if="!isCompetition" effect="plain" type="info" class="ml-2">
              {{ info.format === 'online' ? '线上' : '线下' }}
            </el-tag>
          </div>

          <div class="meta-info mt-3 text-sm text-gray-500 flex flex-wrap gap-4">
            <div class="flex items-center">
              <el-icon class="mr-1"><Clock /></el-icon>
              <span>活动时间：{{ formatTime(info.activityStartTime) }} ~ {{ formatTime(info.activityEndTime) }}</span>
            </div>
            <div class="flex items-center" v-if="!isCompetition">
              <el-icon class="mr-1"><Timer /></el-icon>
              <span>认定工时：<b class="text-green-600">{{ info.hours }}h</b></span>
            </div>
          </div>
        </div>

        <div class="space-x-3">
          <el-button @click="router.back()">返回</el-button>
          <el-button type="primary" icon="Download" @click="handleExport" :loading="exporting">导出签到表</el-button>
        </div>
      </div>
    </div>

    <div class="main-body mt-5">
      <el-card shadow="never" class="mb-4">
        <el-steps :active="currentStep" align-center finish-status="success">
          <el-step title="报名阶段" :description="`截止: ${formatShortDate(info.regEndTime)}`" />
          <el-step :title="isCompetition ? '作品提交' : '活动进行'" />
          <el-step :title="isCompetition ? '评审打分' : '考勤核验'" />
          <el-step title="结果公示" />
          <el-step title="归档" />
        </el-steps>

        <div class="data-summary mt-6 bg-gray-50 p-3 rounded flex justify-center items-center gap-8">
          <div class="text-center">
            <div class="text-xs text-gray-500">名额占用</div>
            <div class="text-lg font-bold text-blue-600">
              {{ info.joined }} / {{ info.quota <= 0 ? '∞' : info.quota }}
            </div>
          </div>
          <div class="divider h-8 w-px bg-gray-300"></div>
          <div class="text-center">
            <div class="text-xs text-gray-500">实际签到</div>
            <div class="text-lg font-bold text-green-600">
              {{ validCount }}
            </div>
          </div>
        </div>

        <div class="step-actions mt-6 text-center">
          <el-button v-if="info.status === 'registering' || info.status === 'open'" type="danger" @click="nextStep('execution')">
            截止报名，进入{{ isCompetition ? '提交' : '执行' }}
          </el-button>
          <el-button v-else-if="info.status === 'execution'" type="warning" @click="nextStep('judging')">
            结束活动，进入{{ isCompetition ? '评审' : '核验' }}
          </el-button>

          <template v-else-if="info.status === 'judging'">
            <el-button type="success" @click="nextStep('publicity')">完成核验，进入公示</el-button>
            <el-popconfirm
              v-if="!isCompetition"
              title="确定要结算工时吗？系统将自动为所有“有效”状态的学生发放工时。"
              @confirm="handleSettle"
            >
              <template #reference>
                <el-button type="primary" icon="Coin">一键结算工时</el-button>
              </template>
            </el-popconfirm>
          </template>

          <el-button v-else-if="info.status === 'publicity'" type="primary" @click="nextStep('finished')">
            公示结束，归档
          </el-button>

          <div v-else class="text-center">
            <div class="text-gray-500 bg-gray-100 py-2 rounded mb-2">项目已归档，工时/学分已发放</div>
            <el-popconfirm
              title="警告：此操作将扣除已发放的工时，并删除审核记录，退回到核验阶段。确定吗？"
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
          <div class="flex justify-between items-center h-8">
            <div class="flex items-center gap-4">
              <span class="font-bold">参与名单 ({{ tableData.length }})</span>

              <transition name="el-fade-in">
                <div v-if="selectedRows.length > 0 && !isCompetition" class="bg-blue-50 px-3 py-1 rounded flex items-center gap-2">
                  <span class="text-xs text-blue-600">已选 {{ selectedRows.length }} 人</span>
                  <el-button type="success" size="small" @click="handleBatchVerify(true)">批量通过</el-button>
                  <el-button type="danger" size="small" @click="handleBatchVerify(false)">批量缺席</el-button>
                </div>
              </transition>
            </div>

            <div class="flex gap-2">
              <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 120px" clearable size="small" v-if="!isCompetition">
                <el-option label="有效" :value="true" />
                <el-option label="缺席" :value="false" />
              </el-select>
              <el-input v-model="keyword" placeholder="搜姓名/学号" prefix-icon="Search" style="width: 200px" size="small" />
            </div>
          </div>
        </template>

        <el-table
          :data="filteredData"
          border
          stripe
          @selection-change="handleSelectionChange"
          row-key="id"
        >
          <el-table-column type="selection" width="55" :selectable="canEdit" />

          <el-table-column label="参与者信息" min-width="240">
            <template #default="{ row }">
              <div class="flex items-center">
                <el-avatar :size="32" class="mr-2 bg-blue-100 text-blue-600 font-bold">
                  {{ row.name ? row.name[0] : '学' }}
                </el-avatar>
                <div>
                  <div class="font-bold text-sm">
                    {{ row.name }}
                    <span class="text-xs font-normal text-gray-400 ml-1">({{ row.sid }})</span>
                  </div>
                  <div class="text-xs text-gray-500 mt-0.5">
                    {{ row.college || '未知学院' }}
                    <span v-if="row.className" class="ml-1">· {{ row.className }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>

          <template v-if="isCompetition">
            <el-table-column label="队长/队员" min-width="150" v-if="info.isTeam">
              <template #default="{ row }">
                <div v-if="row.isTeam" class="text-xs">
                  <span class="font-bold">{{ row.leader }}</span> (队长)
                </div>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="120" align="center" prop="result">
              <template #default="{ row }">{{ row.result || '-' }}</template>
            </el-table-column>
          </template>

          <template v-else>
            <el-table-column label="报名时间" width="160" prop="signupTime" sortable>
              <template #default="{ row }">{{ formatShortDate(row.signupTime) }}</template>
            </el-table-column>

            <el-table-column label="考勤状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isValid === true" type="success" effect="dark">有效</el-tag>
                <el-tag v-else-if="row.isValid === false" type="danger" effect="dark">缺席</el-tag>
                <el-tag v-else type="info" effect="plain">待核验</el-tag>
              </template>
            </el-table-column>
          </template>

          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <div v-if="canEdit(row)">
                <template v-if="isCompetition">
                  <el-button link type="primary" @click="openGrade(row)">评分</el-button>
                </template>
                <template v-else>
                  <el-button
                    size="small"
                    :type="row.isValid === true ? 'success' : 'default'"
                    @click="handleVerify(row, true)"
                  >
                    通过
                  </el-button>
                  <el-button
                    size="small"
                    :type="row.isValid === false ? 'danger' : 'default'"
                    @click="handleVerify(row, false)"
                  >
                    缺席
                  </el-button>
                </template>
              </div>
              <span v-else class="text-gray-400 text-xs">查看模式</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog v-model="gradeVisible" title="录入成绩" width="400px">
      <el-select v-model="gradeLevel" class="w-full" placeholder="请选择奖项">
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
import { Search, Clock, Timer, Coin, Download } from '@element-plus/icons-vue'
import { activityApi } from '@/api/activity'
import { competitionApi } from '@/api/competition'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const exporting = ref(false)

const info = reactive<any>({})
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const keyword = ref('')
const filterStatus = ref<boolean | null>(null)

const isCompetition = computed(() => info.type === 'competition' || info.sourceType === 'competition')
const backPath = computed(() => isCompetition.value ? '/admin/competition/internal' : '/admin/activity/internal')
const validCount = computed(() => tableData.value.filter(r => r.isValid === true).length)

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const id = route.params.id
    // 优先尝试作为活动加载
    try {
      let detail = await activityApi.getDetail(id as string)
      if (detail) {
        Object.assign(info, detail)
        if (!info.joined) info.joined = 0
        tableData.value = await activityApi.getParticipants(id as string)
        return
      }
    } catch(e) {
      // 失败则尝试作为竞赛
      try {
        let detail = await competitionApi.getDetail(id as string)
        if (detail) {
          detail.type = 'competition'
          Object.assign(info, detail)
          tableData.value = await competitionApi.getParticipants(id as string)
        }
      } catch(e2) {}
    }
  } finally { loading.value = false }
}

onMounted(loadData)

const filteredData = computed(() => tableData.value.filter(item => {
  const matchKeyword = !keyword.value || (item.name?.includes(keyword.value)) || (item.sid?.includes(keyword.value))
  const matchStatus = filterStatus.value === null || item.isValid === filterStatus.value
  return matchKeyword && matchStatus
}))

const currentStep = computed(() => {
  const map: Record<string, number> = { registering: 0, open: 0, execution: 1, judging: 2, publicity: 3, finished: 4 }
  return map[info.status] || 0
})

const canEdit = (row: any) => {
  // 只有在 核验/评审 阶段才能修改状态
  return info.status === 'judging'
}

const handleSelectionChange = (val: any[]) => {
  selectedRows.value = val
}

const nextStep = (target: string) => {
  let msg = `确定要进入下一阶段（${getStatusLabel(target)}）吗？`
  ElMessageBox.confirm(msg, '阶段变更确认', {
    confirmButtonText: '确定推进'
  }).then(async () => {
    const api = isCompetition.value ? competitionApi : activityApi
    await api.changeStatus(info.id, target)
    info.status = target
    ElMessage.success('状态已更新')
    if (target === 'finished') loadData()
  })
}

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await activityApi.exportParticipants(info.id)
    const url = window.URL.createObjectURL(new Blob([res as any]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `${info.title}-签到表.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch(e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const handleBatchVerify = async (isValid: boolean) => {
  if (!selectedRows.value.length) return
  try {
    const ids = selectedRows.value.map(r => r.id)
    await activityApi.batchUpdateSignupStatus(ids, isValid ? 'present' : 'absent')
    selectedRows.value.forEach(row => row.isValid = isValid)
    ElMessage.success(`批量操作成功：${isValid?'通过':'缺席'} ${ids.length} 人`)
    selectedRows.value = []
  } catch(e) { ElMessage.error('操作失败') }
}

const handleSettle = async () => {
  try {
    await activityApi.settleActivity(info.id)
    ElMessage.success('结算指令已发送，工时入账处理中')
    info.status = 'finished'
  } catch(e) { ElMessage.error('结算失败') }
}

// 🟢 新增：处理回退
const handleRollback = async () => {
  try {
    await activityApi.rollbackActivity(info.id)
    ElMessage.success('已成功撤销归档')
    // 重新加载数据
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '回退失败')
  }
}

const handleVerify = async (row: any, isValid: boolean) => {
  await activityApi.verifyAttendance(row.id, isValid)
  row.isValid = isValid
  ElMessage.success(isValid ? '已标记为有效' : '已标记为缺席')
}

// 竞赛评分
const gradeVisible = ref(false)
const gradeLevel = ref('')
const currentGradeRow = ref<any>(null)
const openGrade = (row: any) => { currentGradeRow.value = row; gradeLevel.value = row.result || ''; gradeVisible.value = true }
const submitGrade = async () => {
  if (currentGradeRow.value) {
    await competitionApi.submitGrade(currentGradeRow.value.id, gradeLevel.value)
    currentGradeRow.value.result = gradeLevel.value
    gradeVisible.value = false
    ElMessage.success('评分成功')
  }
}

const getStatusLabel = (s: string) => ({ registering: '报名中', open: '报名中', execution: '进行中', judging: '核验/评审', publicity: '公示', finished: '已归档' }[s] || s)
const getStatusType = (s: string) => ({ registering: 'success', open: 'success', execution: 'primary', judging: 'warning', publicity: 'danger', finished: 'info' }[s] || 'info')
const formatTime = (s?: string) => s ? s.slice(0, 16) : '待定'
const formatShortDate = (s?: string) => s ? s.slice(5, 16) : '-'
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
.items-start { align-items: flex-start; }
.text-center { text-align: center; }
.w-full { width: 100%; }
.bg-blue-50 { background-color: #ecf5ff; }
.text-blue-600 { color: #409EFF; }
.bg-gray-100 { background-color: #f3f4f6; }
.text-gray-600 { color: #606266; }
.text-gray-500 { color: #909399; }
.text-gray-400 { color: #c0c4cc; }
.rounded { border-radius: 4px; }
.gap-8 { gap: 2rem; }
.gap-4 { gap: 1rem; }
.gap-2 { gap: 0.5rem; }
.h-8 { height: 2rem; }
.w-px { width: 1px; }
.bg-gray-300 { background-color: #d1d5db; }
.bg-gray-50 { background-color: #f9fafb; }
.text-lg { font-size: 1.125rem; }
.text-sm { font-size: 0.875rem; }
.text-xs { font-size: 0.75rem; }
</style>
