<template>
  <div class="cockpit-page" v-loading="loading" v-if="myRecord.id || loading">

    <el-card shadow="hover" class="header-card">
      <div class="header-section">
        <div class="left-info">
          <div class="tags-row">
            <el-tag effect="dark" :type="eventInfo.sourceType==='internal'?'primary':'warning'" size="small">
              {{ eventInfo.sourceType==='internal'?'校内赛':'校外赛' }}
            </el-tag>
            <el-tag v-if="eventInfo.format" effect="plain" type="success" size="small" class="ml-2">
              {{ eventInfo.format === 'offline' ? '线下赛' : '线上赛' }}
            </el-tag>
            <el-tag type="info" effect="plain" size="small" class="status-tag">
              当前阶段: {{ getStatusLabel(eventInfo.status) }}
            </el-tag>
          </div>
          <h1 class="page-title">{{ eventInfo.title || '加载中...' }}</h1>

          <div class="time-tip mt-2 text-sm text-gray-500">
            <el-icon class="mr-1 relative top-0.5"><Clock /></el-icon>
            <span v-if="['open', 'registering'].includes(eventInfo.status)">
              距离报名截止还剩：<span class="text-blue-600 font-bold">{{ calculateTimeLeft(eventInfo.regEndTime) }}</span>
            </span>
            <span v-else>
              {{ isOffline ? '比赛时间' : '提交时间' }}：{{ eventInfo.compStartTime }} ~ {{ eventInfo.compEndTime }}
            </span>
          </div>
        </div>
        <el-button plain icon="Back" @click="router.back()">返回列表</el-button>
      </div>

      <div class="steps-container">
        <el-steps :active="currentStep" align-center finish-status="success">
          <el-step title="报名成功" description="已组队报名">
            <template #icon><el-icon><UserFilled /></el-icon></template>
          </el-step>
          <el-step
            :title="isOffline ? '比赛进行' : '作品提交'"
            :description="isOffline ? '线下现场比拼' : '上传参赛作品'"
          >
            <template #icon>
              <el-icon v-if="isOffline"><Flag /></el-icon>
              <el-icon v-else><UploadFilled /></el-icon>
            </template>
          </el-step>
          <el-step title="评审阶段" description="专家评审打分">
            <template #icon><el-icon><DataAnalysis /></el-icon></template>
          </el-step>
          <el-step title="结果公示" description="查看最终成绩">
            <template #icon><el-icon><Trophy /></el-icon></template>
          </el-step>
        </el-steps>
      </div>
    </el-card>

    <div class="main-content-grid">

      <el-card shadow="hover" class="profile-card">
        <template #header>
          <div class="card-header">
            <el-icon><CollectionTag /></el-icon>
            <span>参赛档案</span>
          </div>
        </template>

        <div class="profile-summary">
          <el-avatar :size="64" :icon="UserFilled" class="profile-avatar" :style="{ backgroundColor: myRecord.isTeam ? '#409eff' : '#67c23a' }">
            {{ myRecord.isTeam ? '团队' : '个人' }}
          </el-avatar>
          <h3 class="team-name">{{ myRecord.teamName || userStore.name || '未命名' }}</h3>
          <p class="record-id">参赛ID: {{ myRecord.id }}</p>
        </div>

        <div v-if="isLeader && myRecord.isTeam && myRecord.teamCode" class="invite-code-box">
          <p class="invite-title">队伍邀请码</p>
          <div class="invite-content">
            <span class="code-text">{{ myRecord.teamCode }}</span>
            <el-button type="primary" link size="small" @click="copyCode(myRecord.teamCode)">复制</el-button>
          </div>
          <p class="invite-tip">发送给队员，点击列表页“加入队伍”即可</p>
        </div>

        <el-divider content-position="left">成员名单</el-divider>

        <div class="members-list">
          <div class="member-item leader">
            <el-avatar :size="32" class="member-avatar">
              {{ (myRecord.leader && myRecord.leader[0]) ? myRecord.leader[0].toUpperCase() : '队' }}
            </el-avatar>
            <div class="member-info">
              <span class="member-name">{{ myRecord.leader || '未知队长' }}</span>
              <el-tag size="small" type="danger" effect="dark" class="role-tag">队长</el-tag>
            </div>
          </div>
          <div v-for="(m, idx) in myRecord.members" :key="idx" class="member-item">
            <el-avatar :size="32" class="member-avatar">
              {{ (m.name?.[0] || m.userName?.[0])?.toUpperCase() || '员' }}
            </el-avatar>
            <div class="member-info">
              <span class="member-name">{{ m.name || m.userName }}</span>
              <el-tag size="small" type="info" class="role-tag">队员</el-tag>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="console-card">
        <template #header>
          <div class="card-header-row">
            <div class="card-header">
              <el-icon><Cpu /></el-icon>
              <span>任务控制台</span>
            </div>
            <el-tag effect="dark" :type="statusType" class="console-status-tag">{{ getStatusLabel(eventInfo.status) }}</el-tag>
          </div>
        </template>

        <div class="console-body">

          <div v-if="['open', 'registering'].includes(eventInfo.status)" class="w-full flex justify-center">
            <el-result icon="success" title="报名成功，整装待发" sub-title="请关注以下关键时间节点，做好参赛准备">
              <template #extra>
                <div class="timeline-box">
                  <el-timeline>
                    <el-timeline-item :timestamp="eventInfo.regEndTime" placement="top" type="primary" size="large">
                      <el-card shadow="hover" class="time-card">
                        <div class="font-bold text-gray-700">报名截止</div>
                        <div class="text-xs text-gray-500 mt-1">请确保所有队员在此时间前加入队伍</div>
                      </el-card>
                    </el-timeline-item>

                    <el-timeline-item :timestamp="`${eventInfo.compStartTime} 开始`" placement="top" type="warning" size="large">
                      <el-card shadow="hover" class="time-card warning-border">
                        <div class="font-bold text-orange-600">
                          {{ isOffline ? '线下比赛开始' : '作品提交开启' }}
                        </div>
                        <div class="text-xs text-gray-500 mt-1">
                          截止时间：{{ eventInfo.compEndTime }}
                        </div>
                        <div class="text-xs text-orange-500 mt-1">
                          {{ isOffline ? '请携带证件前往比赛地点' : '系统将自动开启文件上传入口' }}
                        </div>
                      </el-card>
                    </el-timeline-item>
                  </el-timeline>
                </div>
              </template>
            </el-result>
          </div>

          <div v-else-if="['submitting', 'execution'].includes(eventInfo.status)" class="upload-section">
            <div v-if="isOffline" class="text-center">
              <el-result
                icon="success"
                title="比赛进行中"
                sub-title="当前为线下比赛阶段，请前往指定地点参赛。"
              >
                <template #extra>
                  <div class="bg-blue-50 p-4 rounded-lg text-blue-700">
                    <p class="font-bold">比赛时间</p>
                    <p>{{ eventInfo.compStartTime }} ~ {{ eventInfo.compEndTime }}</p>
                    <p class="mt-2 text-sm text-gray-500">无需在线提交，成绩由评委录入</p>
                  </div>
                </template>
              </el-result>
            </div>

            <div v-else>
              <div v-if="myRecord.fileUrl" class="submitted-file-info">
                <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
                <div class="file-details">
                  <h3 class="file-status">作品已提交</h3>
                  <p class="file-name">{{ myRecord.fileName || '未知文件名' }}</p>
                  <p class="submit-time">提交时间: {{ myRecord.submitTime || '刚刚' }}</p>
                </div>
              </div>
              <el-empty v-else description="暂无上传记录，请在截止日期前提交作品。" image-size="100"></el-empty>

              <div class="upload-action">
                <el-upload
                  v-if="isLeader"
                  class="upload-demo"
                  drag
                  action=""
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleUpload"
                >
                  <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                  <div class="el-upload__text">
                    将文件拖到此处，或 <em>点击上传</em>
                  </div>
                  <template #tip>
                    <div class="el-upload__tip">
                      支持 ZIP/RAR 格式文件。提交截止：<span class="text-red-500 font-bold">{{ eventInfo.compEndTime }}</span>
                    </div>
                  </template>
                </el-upload>
                <el-alert v-else title="仅队长拥有提交作品的权限" type="warning" show-icon :closable="false" class="mt-4"/>
              </div>
            </div>
          </div>

          <div v-else class="result-section">
            <el-result
              v-if="myRecord.result"
              icon="success"
              title="恭喜获奖！"
              :sub-title="`您的最终成绩为：${myRecord.result}`"
            >
              <template #extra>
                <el-tag type="warning" effect="dark" size="large" class="award-tag">{{ myRecord.result }}</el-tag>
              </template>
            </el-result>
            <el-result
              v-else-if="eventInfo.status === 'judging'"
              icon="info"
              title="评审进行中"
              sub-title="专家正在紧张评审中，请耐心等待结果公示。"
            >
            </el-result>
            <el-result
              v-else
              icon="warning"
              title="未获奖"
              sub-title="感谢您的参与，希望您在未来的比赛中取得好成绩！"
            >
            </el-result>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { competitionApi } from '@/api/competition'
import { ElMessage } from 'element-plus'
import {
  UserFilled, UploadFilled, DataAnalysis, Trophy, Flag, Clock,
  CollectionTag, Cpu, CircleCheckFilled, Back
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const uploading = ref(false)
const eventInfo = ref<any>({})
const myRecord = ref<any>({})

const initData = async () => {
  const eventId = route.params.id as string
  if (!eventId) return
  loading.value = true
  try {
    const detail = await competitionApi.getDetail(eventId)
    if (!detail) {
      ElMessage.warning('赛事不存在')
      router.replace('/competitions')
      return
    }
    eventInfo.value = detail

    const myList = await competitionApi.getMyList(userStore.username)
    const target = myList.find((item: any) => String(item.eventId) === String(eventId))

    if (!target || !target.id) {
      ElMessage.error('您尚未报名该赛事，无权访问驾驶舱')
      router.replace('/competitions')
      return
    }

    myRecord.value = target

  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('数据加载失败')
    router.replace('/competitions')
  } finally {
    loading.value = false
  }
}

onMounted(initData)

const currentStep = computed(() => {
  const s = eventInfo.value.status
  if (['open', 'registering'].includes(s)) return 1
  if (s === 'submitting' || s === 'execution') return 2
  if (s === 'judging') return 3
  if (['publicity', 'auditing', 'finished'].includes(s)) return 4
  return 0
})

const statusType = computed(() => ['submitting', 'execution'].includes(eventInfo.value.status) ? 'warning' : 'primary')
const isOffline = computed(() => eventInfo.value.format === 'offline')
const isLeader = computed(() => myRecord.value.sid === userStore.username)

const getStatusLabel = (s: string) => ({
  open:'报名中', registering:'报名中',
  submitting:'提交中', execution:'进行中',
  judging:'评审中', publicity:'公示中', finished:'结束'
}[s] || s)

// 计算剩余天数/小时
const calculateTimeLeft = (endTime: string) => {
  if (!endTime) return '未知'
  const end = new Date(endTime).getTime()
  const now = new Date().getTime()
  const diff = end - now
  if (diff <= 0) return '已截止'

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days > 0) return `${days} 天`
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  return `${hours} 小时`
}

const handleUpload = async (file: any) => {
  if (!myRecord.value.id) return ElMessage.error('记录ID丢失，无法上传')
  uploading.value = true
  try {
    await competitionApi.uploadWork(myRecord.value.id, file.raw)
    ElMessage.success('上传成功')
    await initData()
  } catch(e: any) {
    ElMessage.error(e.message || '上传失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

const copyCode = (code: string) => {
  navigator.clipboard.writeText(code)
  ElMessage.success('邀请码已复制')
}
</script>

<style scoped>
.cockpit-page { max-width: 1200px; margin: 20px auto; padding: 0 20px; min-height: 80vh; }
.header-card { margin-bottom: 20px; border-radius: 8px; }
.header-section { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 25px; }
.tags-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.page-title { font-size: 24px; font-weight: 600; color: #303133; margin: 0; }
.steps-container { padding: 20px 0; background-color: #f8f9fa; border-radius: 6px; }
.main-content-grid { display: grid; grid-template-columns: 300px 1fr; gap: 20px; align-items: start; }
.card-header { display: flex; align-items: center; font-weight: 600; font-size: 16px; color: #303133; }
.card-header .el-icon { margin-right: 8px; font-size: 18px; }
.profile-card { border-radius: 8px; }
.profile-summary { text-align: center; padding: 20px 0; }
.profile-avatar { margin-bottom: 15px; font-size: 24px; color: #fff; }
.team-name { font-size: 18px; font-weight: 600; margin-bottom: 5px; color: #303133; }
.record-id { font-size: 12px; color: #909399; }
.invite-code-box { background-color: #fff7e6; border: 1px dashed #ffd591; border-radius: 6px; padding: 15px; margin: 0 10px 20px 10px; text-align: center; }
.invite-title { font-size: 12px; color: #fa8c16; font-weight: bold; margin-bottom: 8px; }
.invite-content { display: flex; justify-content: center; align-items: center; gap: 10px; margin-bottom: 5px; }
.code-text { font-family: monospace; font-size: 18px; font-weight: bold; color: #d46b08; letter-spacing: 2px; }
.invite-tip { font-size: 10px; color: #ffbb96; }
.members-list { display: flex; flex-direction: column; gap: 10px; }
.member-item { display: flex; align-items: center; padding: 10px; background-color: #f5f7fa; border-radius: 6px; transition: background-color 0.3s; }
.member-item:hover { background-color: #eef1f6; }
.member-item.leader { background-color: #ecf5ff; border: 1px solid #d9ecff; }
.member-avatar { margin-right: 12px; background-color: #d9ecff; color: #409eff; font-weight: bold; }
.leader .member-avatar { background-color: #409eff; color: white; }
.member-info { flex: 1; display: flex; align-items: center; justify-content: space-between; }
.member-name { font-size: 14px; font-weight: 500; color: #606266; }
.empty-members { text-align: center; color: #909399; font-size: 12px; padding: 10px 0; }
.console-card { border-radius: 8px; min-height: 500px; display: flex; flex-direction: column; }
.card-header-row { display: flex; justify-content: space-between; align-items: center; }
.console-body { flex: 1; display: flex; justify-content: center; align-items: center; padding: 20px 0; width: 100%; }
.upload-section { width: 100%; max-width: 600px; }
.submitted-file-info { display: flex; align-items: flex-start; background-color: #f0f9eb; padding: 20px; border-radius: 8px; margin-bottom: 30px; border: 1px solid #e1f3d8; }
.success-icon { font-size: 40px; color: #67c23a; margin-right: 20px; }
.file-status { font-size: 18px; font-weight: 600; color: #67c23a; margin-bottom: 8px; }
.file-name { font-size: 14px; font-weight: 500; color: #303133; margin-bottom: 4px; }
.submit-time { font-size: 12px; color: #909399; }
.upload-action { margin-top: 30px; }
.upload-demo :deep(.el-upload), .upload-demo :deep(.el-upload-dragger) { width: 100% !important; }
.el-upload__tip { text-align: center; margin-top: 10px; color: #909399; }
.result-section { text-align: center; width: 100%; }
.award-tag { font-size: 18px; padding: 8px 20px; margin-top: 10px; letter-spacing: 2px; }
.ml-2 { margin-left: 8px; }
.mr-1 { margin-right: 4px; }
.text-center { text-align: center; }
.text-gray-500 { color: #909399; }
.text-sm { font-size: 12px; }
.text-blue-600 { color: #2563eb; }
.text-orange-600 { color: #ea580c; }
.font-bold { font-weight: 700; }

/* 🟢 时间轴样式优化 */
.timeline-box { width: 100%; min-width: 320px; padding: 10px; }
.time-card { border-radius: 8px; }
.warning-border { border-left: 3px solid #f97316; }
@media (max-width: 768px) { .main-content-grid { grid-template-columns: 1fr; } .header-section { flex-direction: column; gap: 15px; } .header-section .el-button { width: 100%; } }
</style>
