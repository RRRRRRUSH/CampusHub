<template>
  <div class="comp-manage">
    <el-card shadow="never">
      <div class="page-header mb-4">
        <h2 class="text-lg font-bold text-gray-700">
          {{ pageType === 'internal' ? '校内学科竞赛管理' : '官方赛事通知管理 (校外)' }}
        </h2>
      </div>

      <div class="toolbar">
        <div class="left">
          <el-input
            v-model="keyword"
            :placeholder="pageType === 'internal' ? '搜索校内赛事...' : '搜索官方通知...'"
            prefix-icon="Search"
            style="width: 250px"
            clearable
            @clear="fetchData"
            @keyup.enter="fetchData"
          />
          <el-button type="primary" class="ml-2" @click="fetchData">查询</el-button>
        </div>
        <el-button
          :type="pageType === 'internal' ? 'primary' : 'success'"
          icon="Plus"
          @click="goPublish"
        >
          {{ pageType === 'internal' ? '发布校内赛事' : '发布官方通知' }}
        </el-button>
      </div>

      <el-table :data="filteredList" v-loading="loading" stripe>
        <el-table-column prop="title" label="赛事/通知名称" min-width="180" show-overflow-tooltip />

        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelTag(row.level)">{{ getLevelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="publishDept" label="发布单位" width="120">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.publishDept || (pageType==='internal'?'校级':'官方') }}</el-tag>
          </template>
        </el-table-column>

        <template v-if="pageType === 'internal'">
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="报名时间" width="280" align="center">
            <template #default="{ row }">
              <div class="text-xs text-gray-500">{{ row.regStartTime }} ~ {{ row.regEndTime }}</div>
            </template>
          </el-table-column>
        </template>

        <template v-else>
          <el-table-column label="外部链接" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <a v-if="row.externalLink" :href="row.externalLink" target="_blank" class="text-blue-500 hover:underline">
                {{ row.externalLink }}
              </a>
              <span v-else class="text-gray-400">无</span>
            </template>
          </el-table-column>
        </template>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="pageType === 'internal'" link type="primary" @click="goDetail(row.id)">管理/审核</el-button>
            <el-button link type="primary" @click="goEdit(row.id)">编辑</el-button>
            <el-popconfirm title="确定删除吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { competitionApi } from '@/api/competition'
import { useUserStore } from '@/stores/user'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 🟢 核心：接收路由传来的 pageType (internal / external)
const props = defineProps<{ pageType: string }>()

const router = useRouter()
const userStore = useUserStore()
const list = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')

const fetchData = async () => {
  loading.value = true
  try {
    // 根据 props.pageType 查询对应的数据
    const res = await competitionApi.getList(props.pageType, userStore.id)
    // 兼容 Result 包装或直接 Array
    list.value = Array.isArray(res) ? res : (res.data || [])
  } catch(e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 🟢 监听 pageType 变化：点击侧边栏切换时自动刷新数据
watch(() => props.pageType, () => {
  keyword.value = ''
  list.value = []
  fetchData()
}, { immediate: true })

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  return list.value.filter(item => item.title.includes(keyword.value))
})

const goPublish = () => {
  router.push(`/admin/competition/publish?type=${props.pageType}`)
}

const goEdit = (id: number) => {
  router.push(`/admin/competition/publish?type=${props.pageType}&id=${id}`)
}

const goDetail = (id: number) => {
  router.push(`/admin/competition/manage/${id}`)
}

const handleDelete = async (id: number) => {
  await competitionApi.delete(id)
  ElMessage.success('删除成功')
  fetchData()
}

// 工具函数
const getLevelLabel = (l: string) => ({ nation:'国家级', province:'省级', school:'校级' }[l] || '校级')
const getLevelTag = (l: string) => ({ nation:'danger', province:'warning', school:'primary' }[l] || 'info')
const getStatusLabel = (s: string) => ({ open:'报名中', registering:'报名中', execution:'进行中', judging:'评审中', publicity:'公示中', finished:'已归档' }[s] || s)
const getStatusType = (s: string) => ({ open:'success', registering:'success', execution:'primary', judging:'warning', publicity:'danger', finished:'info' }[s] || '')

</script>

<style scoped>
.comp-manage { padding: 0; min-height: 80vh; }
.toolbar { margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
.left { display: flex; align-items: center; }
.ml-2 { margin-left: 10px; }
.text-xs { font-size: 12px; }
.text-gray-500 { color: #909399; }
.text-lg { font-size: 18px; }
.font-bold { font-weight: bold; }
.mb-4 { margin-bottom: 16px; }
.text-blue-500 { color: #409eff; text-decoration: none; }
.text-blue-500:hover { text-decoration: underline; }
</style>
