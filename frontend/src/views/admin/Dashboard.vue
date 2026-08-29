<template>
  <div class="dashboard-message-center">
    <el-container class="chat-container">

      <el-aside width="280px" class="chat-sidebar">
        <div class="sidebar-header">
          <el-input
            v-model="searchKey"
            placeholder="搜索..."
            prefix-icon="Search"
            clearable
            @keyup.enter="handleGlobalSearch"
          />
        </div>

        <div class="session-list" v-loading="loading">
          <div
            class="session-item todo-item"
            :class="{ active: currentSession?.id === 'todo' }"
            @click="selectSession({ id: 'todo', type: 'TODO', name: '待办任务' })"
          >
            <div class="avatar-box bg-danger"><el-icon><BellFilled /></el-icon></div>
            <div class="content">
              <div class="top">
                <span class="name">待办审批</span>
                <span class="time">实时</span>
              </div>
              <div class="preview text-danger">{{ todoTasks.length }} 条新申请待处理</div>
            </div>
          </div>

          <div
            v-for="item in sessionList"
            :key="item.id"
            class="session-item"
            :class="{ active: currentSession?.id === item.id }"
            @click="selectSession(item)"
          >
            <div class="avatar-box" :class="getAvatarClass(item.type)">
              <el-icon v-if="item.type === 'GROUP'"><ChatDotRound /></el-icon>
              <span v-else>{{ item.name.substring(0,1) }}</span>
              <div v-if="item.unread > 0" class="badge">{{ item.unread }}</div>
            </div>
            <div class="content">
              <div class="top">
                <span class="name">{{ item.name }}</span>
                <span class="time">{{ item.time }}</span>
              </div>
              <div class="preview">{{ item.lastMsg }}</div>
            </div>
          </div>
        </div>
      </el-aside>

      <el-main class="chat-main">
        <template v-if="currentSession">
          <div class="chat-header">
            <span class="title">{{ currentSession.name }}</span>
            <el-tag size="small" effect="plain" class="ml-2">{{ getTypeName(currentSession.type) }}</el-tag>
          </div>

          <div v-if="currentSession.type === 'TODO'" class="todo-panel">
            <div v-if="todoTasks.length === 0" class="empty-task">
              <el-empty description="暂无待办，工作已清空~" />
            </div>
            <div v-else class="task-list">
              <div v-for="task in todoTasks" :key="task.id" class="task-card">
                <div class="task-left">
                  <el-tag :type="task.tagType" size="small" effect="dark" class="mb-1">{{ task.tag }}</el-tag>
                  <div class="task-title">{{ task.title }}</div>
                  <div class="task-meta">
                    <span class="mr-3"><el-icon><User /></el-icon> {{ task.applicant }}</span>
                    <span><el-icon><Clock /></el-icon> {{ task.time }}</span>
                  </div>
                </div>
                <div class="task-right">
                  <el-button link type="primary" size="small" @click="startChat(task.applicant)">私信</el-button>
                  <el-button type="primary" size="small" @click="handleJump(task.link)">去处理</el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="chat-panel">
            <div class="message-container" ref="msgBox">
              <div v-for="msg in messages" :key="msg.id" class="message-row-wrapper">
                <div v-if="msg.type === 'SYSTEM'" class="system-msg"><span class="sys-bubble">{{ msg.content }}</span></div>
                <div v-else class="message-row" :class="{ 'me': msg.isMe }">
                  <div class="msg-avatar" v-if="!msg.isMe"><el-avatar :size="32" style="background:#409EFF">{{ msg.sender?.[0] }}</el-avatar></div>
                  <div class="msg-bubble"><div class="text">{{ msg.content }}</div></div>
                </div>
              </div>
            </div>
            <div class="input-area">
              <textarea v-model="inputText" class="custom-textarea" placeholder="输入消息..." @keydown.enter.exact.prevent="sendMessage"></textarea>
              <div class="send-bar"><el-button type="primary" size="small" @click="sendMessage">发送</el-button></div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="empty-placeholder">
            <el-icon size="60" color="#dcdfe6"><ChatLineSquare /></el-icon>
            <p class="mt-4 text-gray-400">选择左侧会话</p>
          </div>
        </template>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Search, ChatDotRound, BellFilled, User, Clock, ChatLineSquare } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { adminAuditApi } from '@/api/admin-audit'
import { messageApi } from '@/api/message'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const searchKey = ref('')
const currentSession = ref<any>(null)
const sessionList = ref<any[]>([])
const todoTasks = ref<any[]>([]) // 🟢 真实待办数据

// 聊天相关
const messages = ref<any[]>([])
const inputText = ref('')
const msgBox = ref()

onMounted(async () => {
  loading.value = true
  try {
    // 1. 获取聊天会话
    sessionList.value = await messageApi.getSessions()

    // 2. 🟢 获取待办数据 (核心逻辑修复)
    // 只有 ADMIN/AUDITOR 需要看审核待办
    if (userStore.canAudit) {
      const [volApps, compApps] = await Promise.all([
        adminAuditApi.getList('volunteer'),
        adminAuditApi.getList('competition')
      ])

      // 将申请单转换为 Todo Task 格式
      const volTasks = volApps.filter(i => i.status === 'pending').map(i => ({
        id: 'v-' + i.id, tag: '志愿认定', tagType: 'success',
        title: i.title, applicant: i.studentName, time: i.applyTime,
        link: '/admin/audit/volunteer'
      }))

      const compTasks = compApps.filter(i => i.status === 'pending').map(i => ({
        id: 'c-' + i.id, tag: '竞赛认定', tagType: 'warning',
        title: `${i.title} (${i.desc})`, applicant: i.studentName, time: i.applyTime,
        link: '/admin/audit/competition'
      }))

      todoTasks.value = [...volTasks, ...compTasks]
    }
  } finally {
    loading.value = false
  }
})

// 交互逻辑
const selectSession = async (item: any) => {
  currentSession.value = item
  if (item.type !== 'TODO') {
    messages.value = await messageApi.getMessages(item.id)
    scrollToBottom()
  }
}

const sendMessage = async () => {
  if (!inputText.value.trim()) return
  messages.value.push({ id: Date.now(), type: 'TEXT', isMe: true, content: inputText.value })
  inputText.value = ''
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight })
}

const handleGlobalSearch = () => {
  if (searchKey.value) ElMessage.success(`发起与 ${searchKey.value} 的会话`)
}

const startChat = (name: string) => {
  ElMessage.success(`发起与 ${name} 的会话`)
}

const handleJump = (path: string) => router.push(path)
const getAvatarClass = (type: string) => type === 'GROUP' ? 'bg-blue-500' : 'bg-orange-400'
const getTypeName = (type: string) => ({ TODO:'工作台', GROUP:'活动群', PRIVATE:'私信', SYSTEM:'通知' }[type])
</script>

<style scoped>
/* 保持原有 CSS 布局 */
.dashboard-message-center { height: calc(100vh - 80px); background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden; display: flex; }
.chat-container { width: 100%; height: 100%; display: flex; }
.chat-sidebar { border-right: 1px solid #eee; background: #f7f7f7; display: flex; flex-direction: column; }
.sidebar-header { padding: 15px; border-bottom: 1px solid #eee; }
.session-list { flex: 1; overflow-y: auto; }
.session-item { display: flex; padding: 12px 15px; cursor: pointer; transition: 0.2s; position: relative; }
.session-item:hover { background-color: #efefef; }
.session-item.active { background-color: #e6f1fc; border-right: 3px solid #409EFF; }
.session-item.todo-item { background-color: #fffbfb; border-bottom: 1px solid #eee; }
.session-item.todo-item:hover { background-color: #fff0f0; }
.avatar-box { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white; margin-right: 12px; font-size: 16px; position: relative; }
.bg-danger { background-color: #ff4d4f; }
.bg-blue-500 { background-color: #1890ff; }
.bg-orange-400 { background-color: #fa8c16; }
.badge { position: absolute; top: -4px; right: -4px; background: #ff4d4f; color: white; font-size: 10px; padding: 0 4px; border-radius: 8px; }
.content { flex: 1; overflow: hidden; }
.top { display: flex; justify-content: space-between; margin-bottom: 4px; }
.name { font-weight: 600; font-size: 14px; color: #333; }
.time { font-size: 11px; color: #999; }
.preview { font-size: 12px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.text-danger { color: #ff4d4f; }
.chat-main { padding: 0; display: flex; flex-direction: column; background: #fff; flex: 1; }
.chat-header { height: 50px; border-bottom: 1px solid #eee; display: flex; align-items: center; padding: 0 20px; justify-content: space-between; }
.title { font-size: 15px; font-weight: bold; }
.ml-2 { margin-left: 8px; }
.todo-panel { padding: 20px; background: #f5f7fa; flex: 1; overflow-y: auto; }
.task-list { display: flex; flex-direction: column; gap: 10px; }
.task-card { background: white; padding: 15px; border-radius: 6px; border: 1px solid #ebeef5; display: flex; justify-content: space-between; align-items: center; }
.task-title { font-size: 15px; font-weight: bold; color: #303133; margin: 4px 0; }
.task-meta { font-size: 12px; color: #909399; display: flex; align-items: center; }
.mr-3 { margin-right: 12px; }
.chat-panel { display: flex; flex-direction: column; height: 100%; }
.message-container { flex: 1; overflow-y: auto; padding: 20px; background: #f5f5f5; }
.message-row-wrapper { margin-bottom: 15px; }
.system-msg { text-align: center; margin: 10px 0; }
.sys-bubble { background: #e0e0e0; color: #666; font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.message-row { display: flex; }
.message-row.me { flex-direction: row-reverse; }
.msg-avatar { margin: 0 10px; }
.msg-bubble { max-width: 70%; }
.text { background: white; padding: 8px 12px; border-radius: 6px; font-size: 14px; line-height: 1.5; word-break: break-all; }
.me .text { background: #95ec69; }
.input-area { height: 140px; border-top: 1px solid #eee; display: flex; flex-direction: column; background: #fff; }
.custom-textarea { flex: 1; border: none; outline: none; resize: none; padding: 15px; font-size: 14px; font-family: inherit; }
.send-bar { padding: 8px 15px; text-align: right; }
.empty-placeholder { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.mt-4 { margin-top: 1rem; }
.text-gray-400 { color: #9ca3af; }
</style>
