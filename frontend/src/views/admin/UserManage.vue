<template>
  <div class="user-manage-page">
    <el-card shadow="never">
      <div class="flex justify-between items-center mb-4">
        <div class="flex gap-2">
          <el-input
            v-model="keyword"
            placeholder="搜索姓名 / 学号"
            style="width: 240px"
            clearable
            @clear="fetchData"
            @keyup.enter="fetchData"
          >
            <template #append>
              <el-button @click="fetchData">搜索</el-button>
            </template>
          </el-input>

          <el-select
            v-if="userStore.role === 'ROOT' || userStore.role === 'ADMIN'"
            v-model="roleFilter"
            placeholder="角色筛选"
            style="width: 120px"
            clearable
            @change="fetchData"
          >
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </div>

        <el-button type="primary" icon="Refresh" circle @click="fetchData" />
      </div>

      <el-alert
        v-if="userStore.role === 'TEACHER'"
        title="您当前查看的是【本学院】的学生信息。"
        type="info" show-icon class="mb-4" :closable="false"
      />
      <el-alert
        v-else-if="userStore.role === 'STUDENT'"
        title="您当前查看的是【本班级】的同学信息。"
        type="warning" show-icon class="mb-4" :closable="false"
      />

      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="username" label="学号/工号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />

        <el-table-column label="学籍信息" min-width="180">
          <template #default="{ row }">
            <div class="text-xs">
              <div>{{ row.college || '-' }}</div>
              <div class="text-gray-400">{{ row.className || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="身份" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isCadre" type="warning" size="small">学生干部</el-tag>
            <el-tag v-else-if="row.role === 'TEACHER'" type="success" size="small">教师</el-tag>
            <el-tag v-else type="info" size="small">学生</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="竞赛学分" width="100" align="center">
          <template #default="{ row }">
            <span class="font-bold text-blue">{{ row.scoreCompetition || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="志愿工时" width="100" align="center">
          <template #default="{ row }">
            <span class="font-bold text-green">{{ row.scoreVolunteer || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="安全邮箱" min-width="160" show-overflow-tooltip />

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定重置该用户的密码为 123456 吗？" @confirm="handleResetPwd(row)">
              <template #reference>
                <el-button link type="primary" size="small">重置密码</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user-center.ts'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const userStore = useUserStore()
const list = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const roleFilter = ref('')

const fetchData = async () => {
  if (!userStore.id) return
  loading.value = true
  try {
    const res = await userApi.getList({
      keyword: keyword.value,
      role: roleFilter.value,
      currentUserId: userStore.id // 🟢 关键：把当前登录人ID传给后端，触发围栏逻辑
    })
    const allList = Array.isArray(res) ? res : (res.data || [])
    list.value = allList
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleResetPwd = async (row: any) => {
  try {
    await userApi.resetPassword(row.id)
    ElMessage.success(`已重置 ${row.name} 的密码为 123456`)
  } catch(e: any) {
    ElMessage.error('操作失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.user-manage-page { padding: 0; min-height: 80vh; }
.flex { display: flex; }
.justify-between { justify-content: space-between; }
.items-center { align-items: center; }
.gap-2 { gap: 0.5rem; }
.text-xs { font-size: 12px; line-height: 1.4; }
.text-gray-400 { color: #9ca3af; }
.text-blue { color: #2563eb; }
.text-green { color: #16a34a; }
.font-bold { font-weight: 700; }
.mb-4 { margin-bottom: 16px; }
</style>
