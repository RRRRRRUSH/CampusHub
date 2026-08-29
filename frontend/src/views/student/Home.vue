<template>
  <div class="student-home">
    <el-row :gutter="20" class="mb-6">
      <el-col :span="16">
        <el-carousel height="240px" class="rounded-lg overflow-hidden shadow-sm">
          <el-carousel-item v-for="item in 3" :key="item">
            <div class="banner-item" :style="`background: linear-gradient(135deg, ${['#0056D2', '#3a8ee6'][item-1]}, #66b1ff)`">
              <div class="banner-text">
                <h3 class="text-2xl font-bold mb-2">2025春季学期素质教育活动月</h3>
                <p class="text-sm opacity-80">Voluntary Service & Disciplinary Competitions</p>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="h-full notice-card">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold border-l-4 border-blue-600 pl-2">最新公告</span>
              <router-link to="/notices" class="text-blue-600 text-sm no-underline hover:underline">更多</router-link>
            </div>
          </template>
          <ul class="notice-list">
            <li v-for="n in 4" :key="n">
              <span class="tag">通知</span>
              <span class="text text-ellipsis">关于开展寒假社会实践认定的补充通知...</span>
              <span class="date">12-{{ 20 + n }}</span>
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <div class="content-area" v-loading="loading">

      <div class="section-header mb-4 flex justify-between items-end">
        <h3 class="text-xl font-bold text-gray-800"><el-icon class="mr-2 text-blue-600"><Flag /></el-icon> 热门志愿活动</h3>
        <el-button link @click="router.push('/activities')">查看全部</el-button>
      </div>
      <div class="grid-list mb-8">
        <el-card v-for="item in recommendActivities" :key="item.id" shadow="hover" class="item-card group" @click="router.push('/activities')">
          <div class="card-img bg-blue-50 relative">
            <div class="abs-tag bg-blue-600">志愿</div>
            <div class="abs-status" v-if="item.status === 'registering'">报名中</div>
          </div>
          <div class="p-3">
            <h4 class="font-bold text-gray-800 truncate mb-2 group-hover:text-blue-600 transition">{{ item.title }}</h4>
            <div class="text-xs text-gray-500 space-y-1">
              <p><el-icon><Clock /></el-icon> {{ item.regEndTime }} 截止</p>
              <p><el-icon><Timer /></el-icon> 工时: <span class="text-green-600 font-bold">{{ item.hours }}h</span></p>
            </div>
          </div>
        </el-card>
      </div>

      <div class="section-header mb-4 flex justify-between items-end">
        <h3 class="text-xl font-bold text-gray-800"><el-icon class="mr-2 text-orange-500"><Trophy /></el-icon> 热门学科竞赛</h3>
        <el-button link @click="router.push('/competitions')">查看全部</el-button>
      </div>
      <div class="grid-list">
        <el-card v-for="item in recommendCompetitions" :key="item.id" shadow="hover" class="item-card group" @click="router.push('/competitions')">
          <div class="card-img bg-orange-50 relative">
            <div class="abs-tag bg-orange-500">竞赛</div>
            <div class="abs-level">{{ item.level === 'nation' ? '国赛' : '校赛' }}</div>
          </div>
          <div class="p-3">
            <h4 class="font-bold text-gray-800 truncate mb-2 group-hover:text-orange-500 transition">{{ item.title }}</h4>
            <div class="text-xs text-gray-500 space-y-1">
              <p><el-icon><Calendar /></el-icon> {{ item.regEndTime }} 截止</p>
              <p><el-icon><Files /></el-icon> 形式: {{ item.format === 'online' ? '线上' : '线下' }}</p>
            </div>
          </div>
        </el-card>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Flag, Trophy, Clock, Timer, Calendar, Files } from '@element-plus/icons-vue'
import { activityApi } from '@/api/activity'
import { competitionApi } from '@/api/competition'
import type { EventItem } from '@/types/model'

const router = useRouter()
const loading = ref(true)
const recommendActivities = ref<EventItem[]>([])
const recommendCompetitions = ref<EventItem[]>([])

onMounted(async () => {
  try {
    // 并行获取数据
    const [acts, comps] = await Promise.all([
      activityApi.getList('internal'),
      competitionApi.getList('internal')
    ])
    // 只取前 4 个展示
    recommendActivities.value = acts.slice(0, 4)
    recommendCompetitions.value = comps.slice(0, 4)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.student-home { padding-bottom: 40px; }
.mb-6 { margin-bottom: 24px; }
.mb-4 { margin-bottom: 16px; }
.mb-8 { margin-bottom: 32px; }
.mr-2 { margin-right: 8px; }
.h-full { height: 100%; }
.rounded-lg { border-radius: 8px; }
.overflow-hidden { overflow: hidden; }

/* Banner */
.banner-item { height: 100%; display: flex; align-items: center; padding-left: 60px; color: white; }
.text-2xl { font-size: 1.5rem; }
.opacity-80 { opacity: 0.8; }

/* Notice List */
.notice-list { padding: 0; margin-top: 10px; }
.notice-list li { display: flex; align-items: center; height: 34px; font-size: 13px; color: #606266; cursor: pointer; }
.notice-list li:hover { color: #0056D2; }
.notice-list .tag { background: #f0f9eb; color: #67c23a; font-size: 12px; padding: 1px 5px; border-radius: 2px; margin-right: 8px; }
.notice-list .text { flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.notice-list .date { color: #999; font-size: 12px; }

/* Grid Cards */
.grid-list { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.item-card { border-radius: 8px; border: none; cursor: pointer; transition: all 0.3s; }
.item-card:hover { transform: translateY(-3px); box-shadow: 0 10px 20px rgba(0,0,0,0.08); }

.card-img { height: 100px; width: 100%; border-radius: 8px 8px 0 0; }
.bg-blue-50 { background: #eef5ff; }
.bg-orange-50 { background: #fff7e6; }

.abs-tag { position: absolute; top: 10px; left: 10px; color: white; font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: bold; }
.abs-status { position: absolute; bottom: 8px; right: 10px; background: rgba(0,0,0,0.6); color: white; font-size: 10px; padding: 1px 6px; border-radius: 10px; }
.abs-level { position: absolute; bottom: 8px; left: 10px; color: #E6A23C; font-size: 12px; font-weight: bold; }

.bg-blue-600 { background-color: #0056D2; }
.bg-orange-500 { background-color: #fa8c16; }
.text-blue-600 { color: #0056D2; }
.text-orange-500 { color: #fa8c16; }
.text-green-600 { color: #67C23A; }
.text-gray-800 { color: #303133; }
.text-gray-500 { color: #909399; }

.flex { display: flex; }
.justify-between { justify-content: space-between; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.font-bold { font-weight: 700; }
.truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.space-y-1 > :not([hidden]) ~ :not([hidden]) { margin-top: 0.25rem; }
.transition { transition-property: color, background-color, border-color, text-decoration-color, fill, stroke; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1); transition-duration: 150ms; }
.pl-2 { padding-left: 0.5rem; }
.border-l-4 { border-left-width: 4px; }
</style>
