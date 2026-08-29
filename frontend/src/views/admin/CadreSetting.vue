<template>
  <div class="cadre-setting-page">
    <el-card shadow="never">
      <div class="mb-4">
        <h3 class="font-bold text-lg text-gray-700 mb-2">学生干部任命管理</h3>
        <el-alert
          v-if="userStore.role === 'TEACHER'"
          title="您只能任命【本学院】的学生干部。被任命的学生将拥有【班级认定初审】权限。"
          type="info" show-icon :closable="false"
        />
        <el-alert
          v-else
          title="管理员模式：可任命全校任意学生为干部。"
          type="success" show-icon :closable="false"
        />
      </div>

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
        </div>
        <el-button type="primary" icon="Refresh" circle @click="fetchData" />
      </div>

      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="username" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="100" />

        <el-table-column prop="college" label="学院" min-width="140" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="140" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" width="120" />

        <el-table-column label="干部状态" width="180">
          <template #default="{ row }">
            <div class="flex items-center">
              <el-switch
                v-model="row.isCadre"
                inline-prompt
                active-text="是"
                inactive-text="否"
                :loading="row.loading"
                @change="(val) => handleCadreChange(val, row)"
              />
              <span class="ml-2 text-xs text-gray-400">
                {{ row.isCadre ? '已授权' : '未授权' }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user-center'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const userStore = useUserStore()
const list = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')

const fetchData = async () => {
  if (!userStore.id) return
  loading.value = true
  try {
    // 强制只查学生角色
    const res = await userApi.getList({
      keyword: keyword.value,
      role: 'STUDENT',
      currentUserId: userStore.id
    })
    const allList = Array.isArray(res) ? res : (res.data || [])
    list.value = allList.map((item: any) => ({ ...item, loading: false }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 切换干部状态
const handleCadreChange = async (newVal: boolean | string | number, row: any) => {
  const isCadre = !!newVal
  row.loading = true
  try {
    await userApi.setCadre(row.id, isCadre)
    ElMessage.success(isCadre ? `已任命 ${row.name} 为学生干部` : `已撤销 ${row.name} 的干部职务`)
  } catch (e: any) {
    row.isCadre = !isCadre
    ElMessage.error('操作失败')
  } finally {
    row.loading = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.cadre-setting-page { padding: 0; min-height: 80vh; }
.flex { display: flex; }
.justify-between { justify-content: space-between; }
.items-center { align-items: center; }
.gap-2 { gap: 0.5rem; }
.ml-2 { margin-left: 8px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.font-bold { font-weight: 700; }
.text-lg { font-size: 18px; }
.text-gray-700 { color: #374151; }
.text-gray-400 { color: #9ca3af; }
.text-xs { font-size: 12px; }
</style>
