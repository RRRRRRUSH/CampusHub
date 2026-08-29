<template>
  <div class="activity-audit">
    <div class="page-header bg-white p-5 border-b mb-4">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>学分认定中心</el-breadcrumb-item>
        <el-breadcrumb-item>志愿工时认定</el-breadcrumb-item>
      </el-breadcrumb>
      <h1 class="text-2xl font-bold mt-2">志愿工时审核工作台</h1>
    </div>

    <div class="main-body px-5 pb-5">
      <el-card shadow="never" class="mb-4">
        <el-form :inline="true" :model="filters" class="demo-form-inline">
          <el-form-item label="审核状态">
            <el-select v-model="filters.status" placeholder="全部" style="width: 120px">
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已驳回" value="rejected" />
            </el-select>
          </el-form-item>

          <el-form-item label="学院">
            <el-select v-model="filters.college" placeholder="选择学院" style="width: 140px" clearable>
              <el-option label="信息学院" value="信息学院" />
              <el-option label="文学院" value="文学院" />
              <el-option label="物理学院" value="物理学院" />
              <el-option label="商学院" value="商学院" />
              <el-option label="机械学院" value="机械学院" />
            </el-select>
          </el-form-item>

          <el-form-item label="专业">
            <el-input v-model="filters.major" placeholder="如: 计算机" style="width: 120px" clearable />
          </el-form-item>

          <el-form-item label="班级">
            <el-input v-model="filters.className" placeholder="如: 1班" style="width: 100px" clearable />
          </el-form-item>

          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="姓名 / 学号" prefix-icon="Search" style="width: 180px" clearable />
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
            已选 <span class="text-blue-600 font-bold">{{ selectedRows.length }}</span> 项
          </div>
          <div>
            <el-button type="danger" plain :disabled="!hasSelection" @click="handleBatchReject">批量驳回</el-button>
            <el-button type="success" :disabled="!hasSelection" @click="handleBatchApprove">批量通过</el-button>
          </div>
        </div>

        <el-table :data="filteredList" border stripe @selection-change="handleSelect">
          <el-table-column type="selection" width="50" align="center" :selectable="canSelect" />

          <el-table-column label="申请学生" min-width="180">
            <template #default="{ row }">
              <div class="font-bold">{{ row.studentName }} <span class="font-normal text-gray-400">({{ row.studentId }})</span></div>
              <div class="text-xs text-gray-500 mt-1">
                {{ row.college }}
                <span v-if="row.major && row.major !== '暂无'">· {{ row.major }}</span>
                <span v-if="row.className && row.className !== '暂无'">· {{ row.className }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="title" label="实践项目名称" min-width="200" show-overflow-tooltip />

          <el-table-column prop="desc" label="补充说明" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.desc || '-' }}</template>
          </el-table-column>

          <el-table-column label="申请时长" width="120" align="center">
            <template #default="{ row }">
              <span class="text-lg font-bold text-green-600">+{{ row.score }}</span> h
            </template>
          </el-table-column>

          <el-table-column label="证明材料" width="100" align="center">
            <template #default="{ row }">
              <el-button v-if="row.proofImgs && row.proofImgs.length" link type="primary" @click="previewProof(row)">
                查看
              </el-button>
              <span v-else class="text-gray-300 text-xs">无附件</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <div v-if="row.status === 'pending'">
                <el-button link type="success" @click="handleApprove(row)">通过</el-button>
                <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
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

    <el-dialog v-model="previewVisible" title="证明材料核验" width="600px">
      <div v-if="currentProof" class="text-center">
        <img :src="currentProof.proofImgs[0]" class="max-w-full rounded border max-h-[400px]" alt="Proof" />
        <div class="mt-4 text-left bg-gray-50 p-3 rounded">
          <div class="flex justify-between mb-2">
            <span class="font-bold">申请项目：{{ currentProof.title }}</span>
            <span class="text-green-600 font-bold">申请时长：{{ currentProof.score }}h</span>
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
// 🟢 替换为 auditApi 以支持 reset 方法
import { auditApi } from '@/api/audit'
import { useUserStore } from '@/stores/user'
import type { AuditApplication } from '@/types/model'

const userStore = useUserStore()
const loading = ref(false)
const list = ref<AuditApplication[]>([])
const selectedRows = ref<AuditApplication[]>([])
const previewVisible = ref(false)
const currentProof = ref<AuditApplication | null>(null)

const filters = reactive({
  status: 'pending',
  college: '',
  major: '',
  className: '',
  keyword: ''
})

// 数据获取
const fetchData = async () => {
  loading.value = true
  try {
    // 🟢 确保请求的是 volunteer 类型，并传入 userId (支持班长/管理员视角切换)
    // @ts-ignore
    const res = await auditApi.getList('volunteer', userStore.id)
    list.value = res as unknown as AuditApplication[]
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

// 联合筛选逻辑
const filteredList = computed(() => {
  return list.value.filter(item => {
    // 1. 状态匹配
    if (filters.status && item.status !== filters.status) return false
    // 2. 学院匹配
    if (filters.college && item.college !== filters.college) return false
    // 3. 专业匹配 (模糊)
    if (filters.major && !item.major?.includes(filters.major)) return false
    // 4. 班级匹配 (模糊)
    if (filters.className && !item.className?.includes(filters.className)) return false
    // 5. 关键词 (姓名或学号)
    if (filters.keyword) {
      const k = filters.keyword
      if (!item.studentName.includes(k) && !item.studentId.includes(k)) return false
    }
    return true
  })
})

const resetFilters = () => {
  Object.assign(filters, { status: 'pending', college: '', major: '', className: '', keyword: '' })
}

// 选择逻辑
const canSelect = (row: AuditApplication) => row.status === 'pending'
const handleSelect = (val: AuditApplication[]) => selectedRows.value = val
const hasSelection = computed(() => selectedRows.value.length > 0)

// --- 操作逻辑 ---

// 单个通过
const handleApprove = async (row: AuditApplication) => {
  await auditApi.approve([row.id])
  ElMessage.success('已通过')
  await fetchData() // 刷新以获取最新状态
}

// 单个驳回
const handleReject = (row: AuditApplication) => {
  ElMessageBox.prompt('请输入驳回原因', '驳回确认', {
    inputPlaceholder: '例如：证明材料不清晰'
  }).then(async ({ value }) => {
    await auditApi.reject(row.id, value)
    ElMessage.warning('已驳回')
    await fetchData()
  })
}

// 🟢 新增：重置状态
const handleReset = async (row: AuditApplication) => {
  try {
    await auditApi.reset(row.id)
    ElMessage.success('状态已重置，相关分数已扣回')
    await fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '重置失败')
  }
}

// 批量通过
const handleBatchApprove = async () => {
  const ids = selectedRows.value.map(r => r.id)
  await auditApi.approve(ids)
  ElMessage.success(`批量通过 ${ids.length} 条申请`)
  await fetchData()
}

// 批量驳回
const handleBatchReject = () => {
  ElMessageBox.prompt('请输入批量驳回原因', '批量操作确认', {
    inputPlaceholder: '例如：格式不符，请重新提交'
  }).then(async ({ value }) => {
    const promises = selectedRows.value.map(row => auditApi.reject(row.id, value))
    await Promise.all(promises)
    ElMessage.warning(`已批量驳回 ${selectedRows.value.length} 条申请`)
    await fetchData()
  })
}

// 预览
const previewProof = (row: AuditApplication) => {
  currentProof.value = row
  previewVisible.value = true
}

const quickApprove = async () => {
  if (currentProof.value) {
    await handleApprove(currentProof.value)
    previewVisible.value = false
  }
}

// 状态 Helper
const getStatusLabel = (s:string) => ({ pending:'待审核', approved:'已通过', rejected:'已驳回' }[s] || s)
const getStatusTag = (s:string) => ({ pending:'warning', approved:'success', rejected:'danger' }[s] || 'info')
</script>

<style scoped>
.text-green-600 { color: #16a34a; }
.text-blue-600 { color: #2563eb; }
.bg-gray-50 { background-color: #f9fafb; }
.text-xs { font-size: 12px; }
.font-bold { font-weight: 700; }
</style>
