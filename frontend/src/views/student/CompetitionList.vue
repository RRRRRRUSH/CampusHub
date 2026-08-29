<template>
  <div class="comp-list-page">
    <div class="banner-section">
      <div class="banner-content">
        <h1>学科竞赛服务平台</h1>
        <p>汇聚校内外优质赛事资源，助力学生创新实践能力提升</p>
      </div>
    </div>

    <div class="main-container">
      <el-tabs v-model="activeTab" class="custom-tabs" @tab-change="handleTabChange">

        <el-tab-pane label="校内赛事报名" name="internal">
          <div class="join-team-bar">
            <el-alert type="warning" :closable="false" show-icon>
              <template #title>
                <div class="alert-content">
                  <span>提示：团队赛请队长创建队伍后，将邀请码发送给队员。</span>
                  <el-button type="success" size="small" plain @click="joinVisible = true">
                    <el-icon class="mr-1"><Key /></el-icon> 使用邀请码加入队伍
                  </el-button>
                </div>
              </template>
            </el-alert>
          </div>

          <div class="filter-bar">
            <el-radio-group v-model="filterStatus" size="default" @change="fetchData">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="registering">报名中</el-radio-button>
              <el-radio-button label="ongoing">进行中</el-radio-button>
              <el-radio-button label="finished">已结束</el-radio-button>
            </el-radio-group>

            <el-input v-model="keyword" placeholder="搜索赛事名称..." prefix-icon="Search" class="search-input" clearable @clear="fetchData" @keyup.enter="fetchData" />
          </div>

          <div v-loading="loading" class="list-content">
            <el-empty v-if="list.length === 0" description="暂无相关赛事" />
            <div v-else class="grid-layout">
              <el-card v-for="item in list" :key="item.id" shadow="hover" class="comp-card" :body-style="{ padding: '20px' }">

                <div class="card-top-tags">
                  <div class="left-tags">
                    <el-tag size="small" :type="getLevelTagType(item.level)" effect="plain">
                      {{ getLevelLabel(item.level) }}
                    </el-tag>
                    <el-tag v-if="item.format" size="small" type="info" effect="plain" class="ml-2">
                      {{ item.format === 'offline' ? '线下' : '线上' }}
                    </el-tag>
                    <el-tag v-if="checkJoined(item.id)" type="success" effect="dark" class="ml-2">已参赛</el-tag>
                  </div>
                  <el-tag size="small" :type="getStatusType(item.status)" effect="dark">
                    {{ getStatusLabel(item.status) }}
                  </el-tag>
                </div>

                <div class="card-main">
                  <h3 class="comp-title" :title="item.title">{{ item.title }}</h3>

                  <div class="comp-meta">
                    <div class="meta-row">
                      <el-icon><Timer /></el-icon>
                      <span>报名截止: {{ item.regEndTime ? item.regEndTime.split(' ')[0] : '待定' }}</span>
                    </div>
                    <div class="meta-row">
                      <el-icon><User /></el-icon>
                      <span>{{ item.mode === 'team' ? '团队赛' : '个人赛' }}</span>
                    </div>
                  </div>

                  <el-divider style="margin: 15px 0;" />

                  <div class="card-footer">
                    <span class="join-count">
                      <span class="num">{{ item.joinedCount || 0 }}</span> 人已报名
                    </span>

                    <div class="action-btn">
                      <el-button v-if="checkJoined(item.id)" type="success" plain size="small" @click="goCockpit(item.id)">
                        进入驾驶舱
                      </el-button>
                      <el-button v-else-if="['open','registering'].includes(item.status)" type="primary" size="small" @click="openRegisterDialog(item)">
                        立即报名
                      </el-button>
                      <el-button v-else size="small" @click="openDetailDialog(item)">
                        查看详情
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="官方赛事通知 (校外)" name="external">
          <div v-loading="loading" class="external-list">
            <el-empty v-if="list.length === 0" description="暂无通知" />
            <div v-else class="notification-layout">
              <el-card v-for="item in list" :key="item.id" shadow="never" class="notify-card">
                <div class="notify-row">
                  <div class="notify-icon"><el-icon><BellFilled /></el-icon></div>
                  <div class="notify-content">
                    <div class="notify-header">
                      <h3 class="notify-title">{{ item.title }}</h3>
                      <el-tag size="small" effect="plain" type="danger" v-if="item.level">{{ getLevelLabel(item.level) }}</el-tag>
                    </div>
                    <p class="notify-desc" :title="item.description">{{ item.description || '暂无详细描述...' }}</p>
                    <div class="notify-meta">
                      <span>发布时间：{{ item.regStartTime ? item.regStartTime.split(' ')[0] : '近期' }}</span>
                      <el-divider direction="vertical" />
                      <span>来源：{{ item.externalLink ? '外部官网' : '教务处' }}</span>
                    </div>
                  </div>
                  <div class="notify-action">
                    <el-button type="warning" plain round @click="openExternal(item)">
                      前往官网查看 <el-icon class="el-icon--right"><Right /></el-icon>
                    </el-button>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <div v-if="selectedComp">
        <el-descriptions :column="1" border class="mb-4">
          <el-descriptions-item label="赛事名称">{{ selectedComp.title }}</el-descriptions-item>

          <el-descriptions-item label="报名时间">
            {{ selectedComp.regStartTime }} 至 {{ selectedComp.regEndTime }}
          </el-descriptions-item>

          <el-descriptions-item v-if="selectedComp.compStartTime" label="竞赛/提交">
            <span class="text-orange-600 font-bold">
              {{ selectedComp.compStartTime }} 至 {{ selectedComp.compEndTime }}
            </span>
          </el-descriptions-item>

          <el-descriptions-item label="详细规则">
            <div class="desc-text">{{ selectedComp.description || '暂无详细说明' }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="isRegisterMode" class="register-form mt-4">
          <el-divider content-position="left">报名信息填写</el-divider>
          <el-form :model="regForm" label-width="100px">
            <el-form-item label="参赛模式">
              <el-radio-group v-model="regForm.isTeam">
                <el-radio :label="false" :disabled="selectedComp.mode === 'team'">个人参赛</el-radio>
                <el-radio :label="true" :disabled="selectedComp.mode === 'individual'">组队参赛</el-radio>
              </el-radio-group>
            </el-form-item>

            <template v-if="regForm.isTeam">
              <el-form-item label="队伍名称">
                <el-input v-model="regForm.name" placeholder="给队伍起个名字" />
              </el-form-item>
              <el-form-item label="队长姓名">
                <el-input v-model="regForm.leader" disabled />
              </el-form-item>
              <el-alert title="报名成功后将生成邀请码，请分享给队员加入" type="info" :closable="false" show-icon />
            </template>

            <template v-else>
              <el-form-item label="参赛姓名">
                <el-input v-model="regForm.leader" disabled />
              </el-form-item>
            </template>
          </el-form>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
          <el-button v-if="isRegisterMode" type="primary" :loading="submitting" @click="handleSignup">
            确认报名
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="joinVisible" title="加入队伍" width="400px">
      <div class="text-center p-4">
        <p class="mb-2 text-gray-600">请输入队长提供的 6 位邀请码</p>
        <el-input
          v-model="joinCode"
          placeholder="例如：A1B2C3"
          size="large"
          class="text-center text-lg tracking-widest"
          maxlength="6"
          @keyup.enter="handleJoinTeam"
        >
          <template #prefix><el-icon><Key /></el-icon></template>
        </el-input>
      </div>
      <template #footer>
        <el-button @click="joinVisible = false">取消</el-button>
        <el-button type="success" :loading="joining" @click="handleJoinTeam">立即加入</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { competitionApi } from '@/api/competition'
import { Search, Timer, User, BellFilled, Right, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('internal')
const loading = ref(false)
const list = ref<any[]>([])
const myJoinedIds = ref<Set<number>>(new Set())
const filterStatus = ref('all')
const keyword = ref('')

const dialogVisible = ref(false)
const joinVisible = ref(false)
const joinCode = ref('')
const joining = ref(false)
const isRegisterMode = ref(false)
const submitting = ref(false)
const selectedComp = ref<any>(null)
const regForm = reactive({ isTeam: false, name: '', leader: userStore.name, sid: userStore.username })
const dialogTitle = computed(() => isRegisterMode.value ? '立即报名' : '赛事详情')

const fetchData = async () => {
  loading.value = true
  try {
    const sourceType = activeTab.value === 'internal' ? 'internal' : 'external'

    // 🟢 核心修改：调用 getStudentList 并传入 userId
    // 后端会自动根据 userId 获取学生的学院和年级，进行可见性过滤
    const res = await competitionApi.getStudentList(sourceType, userStore.id)

    if (sourceType === 'internal' && userStore.username) {
      try {
        const myRes = await competitionApi.getMyList(userStore.username)
        myJoinedIds.value = new Set(myRes.map((item: any) => Number(item.eventId)))
      } catch(e) { console.error(e) }
    }

    list.value = res.filter((item: any) => {
      const matchKey = !keyword.value || item.title.includes(keyword.value)
      if (sourceType === 'internal') {
        let matchStatus = false
        if (filterStatus.value === 'all') matchStatus = true
        else if (filterStatus.value === 'registering') matchStatus = ['open', 'registering'].includes(item.status)
        else if (filterStatus.value === 'ongoing') matchStatus = ['execution', 'submitting', 'judging'].includes(item.status)
        else if (filterStatus.value === 'finished') matchStatus = ['publicity', 'finished'].includes(item.status)
        return matchKey && matchStatus
      } else {
        return matchKey
      }
    })
  } catch (e) {
    console.error(e)
    ElMessage.error('获取赛事列表失败')
  } finally {
    loading.value = false
  }
}

// ... 下面的代码与之前完全一致，保持不变 ...
// (handleTabChange, checkJoined, goCockpit, openRegisterDialog, handleSignup, handleJoinTeam, openExternal 等方法)

const handleTabChange = () => {
  keyword.value = ''
  filterStatus.value = 'all'
  list.value = []
  fetchData()
}

const checkJoined = (id: number) => myJoinedIds.value.has(Number(id))

const goCockpit = (id: number) => {
  router.push(`/competition/cockpit/${id}`)
}

const openRegisterDialog = (item: any) => {
  selectedComp.value = item
  isRegisterMode.value = true
  dialogVisible.value = true
  regForm.leader = userStore.name
  regForm.sid = userStore.username
  regForm.name = ''
  if (item.mode === 'team') {
    regForm.isTeam = true
    regForm.name = `${userStore.name}的战队`
  } else {
    regForm.isTeam = false
    regForm.name = userStore.name
  }
}

const openDetailDialog = (item: any) => {
  selectedComp.value = item
  isRegisterMode.value = false
  dialogVisible.value = true
}

const handleSignup = async () => {
  if (regForm.isTeam && !regForm.name) return ElMessage.warning('请输入队伍名称')
  submitting.value = true
  try {
    const params = {
      eventId: selectedComp.value.id,
      sid: userStore.username,
      isTeam: regForm.isTeam,
      name: regForm.name,
      leader: userStore.name
    }
    await competitionApi.signup(selectedComp.value.id, params)
    ElMessage.success('报名成功！')
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '报名失败')
  } finally {
    submitting.value = false
  }
}

const handleJoinTeam = async () => {
  if (!joinCode.value) return ElMessage.warning('请输入邀请码')
  joining.value = true
  try {
    const res = await competitionApi.joinByCode(joinCode.value, {
      sid: userStore.username,
      name: userStore.name,
      college: userStore.dept
    })
    ElMessage.success(`成功加入队伍：${res.teamName || '未知队伍'}`)
    joinVisible.value = false
    joinCode.value = ''
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '加入失败')
  } finally {
    joining.value = false
  }
}

const openExternal = (item: any) => {
  if (item.externalLink && item.externalLink.startsWith('http')) {
    window.open(item.externalLink, '_blank')
  } else {
    ElMessage.info('该通知仅供查阅，暂无官网链接')
  }
}

const getLevelLabel = (l: string) => ({ nation:'国家级', province:'省级', school:'校级' }[l] || '校级')
const getLevelTagType = (l: string) => ({ nation:'danger', province:'warning', school:'primary' }[l] || 'info')
const getStatusLabel = (s: string) => ({ open:'报名中', registering:'报名中', execution:'进行中', submitting:'提交中', judging:'评审中', publicity:'公示中', finished:'已结束' }[s] || s)
const getStatusType = (s: string) => {
  if(['open', 'registering'].includes(s)) return 'success'
  if(['execution', 'submitting', 'judging'].includes(s)) return 'primary'
  if(['finished', 'publicity'].includes(s)) return 'info'
  return 'warning'
}
const getBtnText = (s: string) => ['open','registering'].includes(s) ? '立即报名' : '查看详情'

const bgClass = (id: number) => {
  const bgs = ['bg-blue', 'bg-purple', 'bg-indigo', 'bg-teal']
  return bgs[id % bgs.length]
}

onMounted(fetchData)
</script>

<style scoped>
.comp-list-page { min-height: 100vh; background-color: #f5f7fa; }
.banner-section { height: 200px; background: linear-gradient(135deg, #1c64f2 0%, #1e429f 100%); display: flex; align-items: center; justify-content: center; color: white; text-align: center; }
.banner-content h1 { font-size: 32px; margin-bottom: 10px; font-weight: bold; }
.banner-content p { font-size: 16px; opacity: 0.9; }
.main-container { max-width: 1200px; margin: -40px auto 20px; padding: 0 20px; position: relative; z-index: 10; }
.custom-tabs { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); min-height: 500px; }
.join-team-bar { margin-bottom: 20px; }
.alert-content { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.mr-1 { margin-right: 4px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 15px; }
.search-input { width: 250px; }
.grid-layout { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.comp-card { border: 1px solid #e5e7eb; border-radius: 8px; transition: all 0.3s; }
.comp-card:hover { transform: translateY(-3px); box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); border-color: #bfdbfe; }
.card-top-tags { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.left-tags { display: flex; align-items: center; }
.ml-2 { margin-left: 8px; }
.comp-title { font-size: 16px; font-weight: bold; margin-bottom: 12px; color: #1f2937; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; height: 44px; }
.comp-meta { display: flex; flex-direction: column; gap: 6px; }
.meta-row { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #6b7280; }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; padding-top: 10px; border-top: 1px solid #f3f4f6; }
.join-count { font-size: 12px; color: #9ca3af; }
.join-count .num { color: #1c64f2; font-weight: bold; font-size: 14px; }
.notification-layout { display: flex; flex-direction: column; gap: 15px; }
.notify-card { border-radius: 8px; border-left: 4px solid #f59e0b; }
.notify-row { display: flex; align-items: center; padding: 10px; gap: 20px; }
.notify-icon { width: 50px; height: 50px; background: #fef3c7; color: #d97706; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 24px; flex-shrink: 0; }
.notify-content { flex: 1; min-width: 0; }
.notify-header { display: flex; align-items: center; gap: 10px; margin-bottom: 5px; }
.notify-title { font-size: 16px; font-weight: bold; color: #374151; margin: 0; }
.notify-desc { color: #6b7280; font-size: 14px; margin-bottom: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.notify-meta { font-size: 12px; color: #9ca3af; display: flex; gap: 10px; align-items: center; }
.desc-text { white-space: pre-wrap; color: #606266; line-height: 1.6; }
.dialog-title { font-weight: bold; }
.text-orange-600 { color: #ea580c; }
.font-bold { font-weight: 700; }
</style>
