<template>
  <div class="student-page">
    <el-card shadow="never" header="校内通知公告">
      <el-table :data="list" stripe>
        <el-table-column prop="date" label="发布日期" width="150" />
        <el-table-column label="标题">
          <template #default="{ row }">
            <span class="cursor-pointer hover:text-blue-600 font-medium" @click="viewDetail(row)">
              {{ row.title }}
            </span>
            <el-tag v-if="row.top" type="danger" size="small" class="ml-2">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dept" label="发布部门" width="180" />
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="current.title" width="600px">
      <div class="meta text-gray-400 text-xs mb-4">
        发布部门：{{ current.dept }} &nbsp;|&nbsp; 发布时间：{{ current.date }}
      </div>
      <div class="content p-4 bg-gray-50 rounded leading-7 text-gray-700 whitespace-pre-line">
        {{ current.content }}
      </div>
      <template #footer><el-button @click="visible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const visible = ref(false)
const current = ref<any>({})

const list = ref([
  { date: '2025-01-15', title: '关于2025年寒假放假的通知', dept: '校办公室', top: true, content: '全体师生：\n根据校历安排，寒假时间为...' },
  { date: '2025-01-10', title: '教务处关于选课的补充说明', dept: '教务处', top: false, content: '...' },
])

const viewDetail = (row: any) => { current.value = row; visible.value = true }
</script>

<style scoped>
.student-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.bg-gray-50 { background-color: #f9fafb; }
.rounded { border-radius: 4px; }
</style>
