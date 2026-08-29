<template>
  <div class="my-participations">
    <el-card shadow="never">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-bold text-lg">我的志愿活动</span>
          <el-button link type="primary" @click="fetchData" icon="Refresh">刷新</el-button>
        </div>
      </template>

      <el-table :data="list" stripe v-loading="loading">
        <el-table-column label="活动名称" min-width="250">
          <template #default="{ row }">
            <div class="font-bold text-gray-800">{{ row.title }}</div>
            <div class="text-xs text-gray-500 mt-1 flex items-center">
              <el-icon class="mr-1"><Location /></el-icon>
              {{ row.location || '线上' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间信息" width="240">
          <template #default="{ row }">
            <div class="text-xs text-gray-500">活动时间</div>
            <div class="text-sm font-medium">{{ formatTime(row.activity_start_time) }}</div>
            <div class="text-xs text-gray-400">至 {{ formatTime(row.activity_end_time) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="预计工时" width="100" align="center">
          <template #default="{ row }">
            <span class="text-blue-600 font-bold">+{{ row.hours }}h</span>
          </template>
        </el-table-column>

        <el-table-column label="当前状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.signupStatus === 'present' || row.signupStatus === 'valid'" type="success" effect="dark">有效/已完赛</el-tag>
            <el-tag v-else-if="row.signupStatus === 'absent'" type="danger" effect="dark">缺席</el-tag>
            <el-tag v-else-if="row.status === 'finished'" type="info" effect="plain">未核验/已归档</el-tag>
            <el-tag v-else type="primary">已报名</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <div v-if="['registering', 'open', 'execution'].includes(row.status)">

              <el-popconfirm
                v-if="checkTimeLimit(row)"
                title="确定要放弃参与此活动吗？名额将释放给他人。"
                confirm-button-text="狠心取消"
                cancel-button-text="留下来"
                @confirm="handleCancel(row)"
              >
                <template #reference>
                  <el-button link type="danger" size="small">取消报名</el-button>
                </template>
              </el-popconfirm>

              <el-tooltip v-else content="活动开始前24小时内不可取消，请联系负责老师请假" placement="top">
                <span class="text-xs text-orange-400 cursor-not-allowed">
                  <el-icon class="mr-1 align-middle"><Warning /></el-icon>临近开始
                </span>
              </el-tooltip>
            </div>

            <span v-else class="text-xs text-gray-300">无法操作</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Location, Refresh, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { activityApi } from '@/api/activity'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref<any[]>([])

const fetchData = async () => {
  if (!userStore.id) return
  loading.value = true
  try {
    const res = await activityApi.getMyList(userStore.id)
    list.value = Array.isArray(res) ? res : (res.data || [])
  } finally {
    loading.value = false
  }
}

// 🟢 核心逻辑：检查是否允许取消 (前端预判)
// 规则：活动开始前 24 小时以上才允许取消
const checkTimeLimit = (row: any) => {
  if (!row.activity_start_time) return true // 数据缺失时默认允许，交给后端拦截

  const now = new Date().getTime()
  const start = new Date(row.activity_start_time).getTime()
  const diff = start - now

  // 24小时 = 24 * 60 * 60 * 1000 = 86400000 毫秒
  return diff > 86400000
}

const handleCancel = async (row: any) => {
  try {
    // 双重保险：虽然UI上拦截了，但这里调接口如果后端拦截也会报错，需要捕获
    await activityApi.cancelSignup(row.id, userStore.id!)
    ElMessage.success('已取消报名，名额已释放')
    fetchData() // 刷新列表
  } catch (e: any) {
    // 显示后端返回的具体错误 (比如 "活动即将开始，无法取消")
    ElMessage.error(e.message || '取消失败')
  }
}

const formatTime = (str: string) => str ? str.slice(0, 16) : '-'

onMounted(fetchData)
</script>

<style scoped>
.my-participations { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
.mr-1 { margin-right: 4px; }
.mt-1 { margin-top: 4px; }
.font-medium { font-weight: 500; }
.cursor-not-allowed { cursor: not-allowed; }
.align-middle { vertical-align: middle; }
</style>
