<template>
  <div class="p-4">
    <el-card shadow="never">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-bold text-lg">我的参赛记录</span>
          <el-button link type="primary" @click="fetchData">刷新数据</el-button>
        </div>
      </template>

      <el-table :data="myList" v-loading="loading" stripe>
        <el-table-column label="赛事名称" min-width="200">
          <template #default="{ row }">
            <div class="font-bold text-base">{{ row._event?.title }}</div>
            <div class="text-xs text-gray-500 mt-1">
              <el-tag size="small" :type="row._event?.sourceType==='internal'?'primary':'warning'">
                {{ row._event?.sourceType==='internal'?'校内':'校外' }}
              </el-tag>
              <span class="ml-2">当前阶段: {{ getStatusLabel(row._event?.status) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="参赛身份" width="180">
          <template #default="{ row }">
            <div v-if="row.isTeam">
              <span class="font-bold text-blue-600">{{ row.teamName }}</span>
              <el-tag size="small" class="ml-2" v-if="row.sid === userStore.username">我是队长</el-tag>
              <el-tag size="small" class="ml-2" type="info" v-else>队员</el-tag>
            </div>
            <div v-else>个人参赛</div>
          </template>
        </el-table-column>

        <el-table-column label="作品状态" width="200">
          <template #default="{ row }">
            <div v-if="canSubmit(row)">
              <div v-if="row.fileUrl" class="flex items-center text-green-600 mb-2">
                <el-icon><CircleCheck /></el-icon> <span class="ml-1">已提交</span>
              </div>
              <div v-else class="text-red-400 mb-2 text-xs">未提交</div>

              <el-button type="primary" size="small" @click="enterCockpit(row._event?.id)">
                去提交作品
              </el-button>
            </div>

            <div v-else>
              <span v-if="row.fileUrl" class="text-gray-500 text-xs">文件已归档</span>
              <span v-else-if="row._event?.format === 'offline'" class="text-gray-500 text-xs">线下参赛无需上传</span>
              <span v-else-if="row._event?.status !== 'submitting'" class="text-gray-400 text-xs">非提交时间</span>
              <span v-else class="text-red-300 text-xs">仅队长可提交</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="最终成绩" width="150" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.result" type="warning" effect="dark">{{ row.result }}</el-tag>
            <span v-else class="text-gray-400 text-xs">等待公布</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="right">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="enterCockpit(row._event?.id)">
              进入驾驶舱 <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus' // 引入 Message
import { competitionApi } from '@/api/competition'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const myList = ref<any[]>([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    myList.value = await competitionApi.getMyList(userStore.username)
    // 调试建议：可以在这里打印一下数据结构，确认 ID 到底在哪
    // console.log('我的竞赛数据:', myList.value)
  } finally {
    loading.value = false
  }
}

// 权限判断逻辑
const canSubmit = (row: any) => {
  const event = row._event
  if (!event) return false
  const isTime = event.sourceType === 'internal' && event.format === 'online' && event.status === 'submitting'
  const hasRight = row.isTeam ? (row.sid === userStore.username) : true
  return isTime && hasRight
}

// 🟢 修复点3：增加空值校验和日志
const enterCockpit = (eventId: number) => {
  console.log('尝试跳转驾驶舱，ID:', eventId)

  if (!eventId) {
    ElMessage.error('无法获取赛事ID，跳转失败')
    return
  }

  router.push({
    name: 'CompetitionCockpit', // 确保你的路由里 name 确实叫这个
    params: { id: eventId }
  })
}

const getStatusLabel = (s: string) => ({ open:'报名中', registering:'报名中', submitting:'提交作品中', judging:'评审中', publicity:'公示中', finished:'已结束' }[s] || s)

onMounted(fetchData)
</script>
