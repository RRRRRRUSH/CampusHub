<template>
  <div class="comp-audit-page">
    <div class="page-header bg-white p-5 border-b mb-4">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>学分认定中心</el-breadcrumb-item>
        <el-breadcrumb-item>竞赛获奖认定</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="flex items-center mt-2">
        <h1 class="text-2xl font-bold mr-4">竞赛获奖审核工作台</h1>
        <el-tag v-if="userStore.role === 'STUDENT'" type="warning">学生干部模式 (仅本班)</el-tag>
        <el-tag v-else type="danger">管理员模式 (全校)</el-tag>
      </div>
    </div>

    <div class="main-body px-5 pb-5">
      <el-card shadow="never" class="mb-4">
        <el-form :inline="true" :model="filters">
          <el-form-item label="审核状态">
            <el-select v-model="filters.status" placeholder="全部" style="width: 120px">
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已驳回" value="rejected" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="姓名 / 学号" prefix-icon="Search" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchData">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" v-loading="loading">
        <div class="mb-4 flex justify-between items-center bg-gray-50 p-2 rounded border">
          <div class="text-sm text-gray-500">
            已选 <span class="text-blue-600 font-bold">{{ selectedIds.length }}</span> 项
          </div>
          <div>
            <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="handleBatchReject">批量驳回</el-button>
            <el-button type="success" :disabled="selectedIds.length === 0" @click="handleBatchApprove">批量通过</el-button>
          </div>
        </div>

        <el-table :data="filteredList" border stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" :selectable="canAudit" />

          <el-table-column label="申请学生" min-width="150">
            <template #default="{ row }">
              <div class="font-bold">{{ row.studentName }} <span class="text-gray-400">({{ row.studentId }})</span></div>
              <div class="text-xs text-gray-500 mt-1">
                {{ row.college }} <span v-if="row.major">· {{ row.major }}</span> <span v-if="row.className">· {{ row.className }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="来源" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.sourceType === 'internal'" type="success" effect="plain" size="small">校内自动</el-tag>
              <el-tag v-else type="warning" effect="plain" size="small">校外申请</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="title" label="赛事名称" min-width="200" show-overflow-tooltip />

          <el-table-column prop="awardLevel" label="获奖等级" width="140" align="center">
            <template #default="{ row }">
              <el-tag effect="plain" type="warning">{{ row.awardLevel || '无' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="score" label="拟计分" width="80" align="center">
            <template #default="{ row }">
              <span class="font-bold text-blue-600">{{ row.score || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="佐证材料" width="100" align="center">
            <template #default="{ row }">
              <el-button v-if="row.proofImgs && row.proofImgs.length" link type="primary" size="small" @click="viewProof(row)">
                查看
              </el-button>
              <span v-else class="text-gray-300 text-xs">无</span>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <div v-if="row.status === 'pending'">
                <el-button type="success" link @click="handleSingleAudit(row, 'approve')">通过</el-button>
                <el-button type="danger" link @click="handleSingleAudit(row, 'reject')">驳回</el-button>
              </div>
              <div v-else>
                <el-popconfirm title="确定要重置为待审核吗？(如果是已通过，系统将自动扣回已加分数)" @confirm="handleReset(row)">
                  <template #reference>
                    <el-button link type="warning" size="small">重置状态</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog v-model="previewVisible" title="获奖证书核验" width="600px">
      <div v-if="currentProof" class="text-center">
        <img v-if="currentProof.proofImgs && currentProof.proofImgs[0]" :src="currentProof.proofImgs[0]" class="max-w-full rounded border max-h-[400px]" />
        <div class="mt-4 text-left bg-gray-50 p-3 rounded">
          <div class="flex justify-between mb-2">
            <span class="font-bold">赛事：{{ currentProof.title }}</span>
            <span class="text-orange-600 font-bold">等级：{{ currentProof.awardLevel }}</span>
          </div>
          <p class="text-gray-600 text-sm">说明：{{ currentProof.desc || '无' }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button v-if="currentProof?.status === 'pending'" type="success" @click="quickApprove">审核通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
// 🟢 修改引用：使用更新后的 auditApi
import { auditApi } from '@/api/audit'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref<any[]>([])
const selectedIds = ref<number[]>([])
const previewVisible = ref(false)
const currentProof = ref<any>(null)

const filters = reactive({ status: 'pending', keyword: '' })

// 🟢 获取竞赛类申请 (带上 userId 鉴权)
const fetchData = async () => {
  loading.value = true
  try {
    // 传入 competition 类型 和 当前用户 ID
    const res = await auditApi.getList('competition', userStore.id)
    list.value = res as any
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

// 前端筛选
const filteredList = computed(() => {
  return list.value.filter(item => {
    if (filters.status && item.status !== filters.status) return false
    if (filters.keyword) {
      const k = filters.keyword
      if (!item.studentName.includes(k) && !item.studentId.includes(k) && !item.title.includes(k)) return false
    }
    return true
  })
})

const resetFilters = () => Object.assign(filters, { status: 'pending', keyword: '' })

const canAudit = (row: any) => row.status === 'pending'
const handleSelectionChange = (val: any[]) => selectedIds.value = val.map(v => v.id)

// 🟢 新增 handleReset
const handleReset = async (row: any) => {
  try {
    await auditApi.reset(row.id)
    ElMessage.success('状态已重置')
    fetchData()
  } catch (e) {
    ElMessage.error('重置失败')
  }
}

const handleSingleAudit = async (row: any, action: 'approve' | 'reject') => {
  try {
    if (action === 'approve') {
      await auditApi.approve([row.id])
      ElMessage.success('审核通过')
    } else {
      const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回确认')
      await auditApi.reject(row.id, value)
      ElMessage.warning('已驳回')
    }
    fetchData()
  } catch(e: any) {
    if(e !== 'cancel') console.error(e)
  }
}

const handleBatchApprove = async () => {
  if (!selectedIds.value.length) return
  await auditApi.approve(selectedIds.value)
  ElMessage.success(`批量通过 ${selectedIds.value.length} 条申请`)
  fetchData()
}

const handleBatchReject = async () => {
  if (!selectedIds.value.length) return
  try {
    const { value } = await ElMessageBox.prompt('请输入批量驳回原因', '批量操作确认')
    const promises = selectedIds.value.map(id => auditApi.reject(id, value))
    await Promise.all(promises)
    ElMessage.warning('已批量驳回')
    fetchData()
  } catch(e) {}
}

const viewProof = (row: any) => { currentProof.value = row; previewVisible.value = true }
const quickApprove = async () => {
  if (currentProof.value) {
    await handleSingleAudit(currentProof.value, 'approve')
    previewVisible.value = false
  }
}

const getStatusLabel = (s: string) => ({ pending: '待审核', approved: '已通过', rejected: '已驳回' }[s] || s)
const getStatusType = (s: string) => ({ pending: 'warning', approved: 'success', rejected: 'danger' }[s] || 'info')
</script>

<style scoped>
.text-blue-600 { color: #2563eb; }
.text-orange-600 { color: #ea580c; }
.bg-gray-50 { background-color: #f9fafb; }
.font-bold { font-weight: 700; }
</style>
