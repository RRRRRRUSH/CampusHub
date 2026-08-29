<template>
  <div class="student-page">
    <el-card shadow="never" class="chat-wrapper" :body-style="{ padding: 0, height: '100%' }">
      <div class="flex h-full">

        <div class="chat-sidebar">
          <div class="p-3 border-b">
            <el-input v-model="searchKey" placeholder="搜索消息" prefix-icon="Search" />
          </div>
          <div class="overflow-y-auto flex-1">
            <div
              v-for="item in sessionList"
              :key="item.id"
              class="session-item"
              :class="{ active: currentId === item.id }"
              @click="selectSession(item)"
            >
              <div class="avatar-box" :class="getAvatarColor(item.type)">
                <el-icon v-if="item.type === 'SYSTEM'"><Bell /></el-icon>
                <el-icon v-else-if="item.type === 'GROUP'"><ChatDotRound /></el-icon>
                <span v-else>{{ item.name[0] }}</span>
                <div v-if="item.unread > 0" class="badge">{{ item.unread }}</div>
              </div>
              <div class="flex-1 overflow-hidden">
                <div class="flex justify-between mb-1">
                  <span class="font-bold text-sm truncate">{{ item.name }}</span>
                  <span class="text-xs text-gray-400">{{ item.time }}</span>
                </div>
                <div class="text-xs text-gray-500 truncate">{{ item.lastMsg }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 flex flex-col bg-white">
          <template v-if="currentSession">
            <div class="chat-header">
              <span class="font-bold">{{ currentSession.name }}</span>
              <el-tag size="small" type="info" class="ml-2">{{ getTypeName(currentSession.type) }}</el-tag>
            </div>

            <div class="chat-content" ref="msgBox">
              <div v-for="msg in messages" :key="msg.id" class="mb-4">
                <div v-if="msg.type === 'SYSTEM'" class="text-center my-3">
                  <span class="bg-gray-100 text-gray-500 text-xs px-2 py-1 rounded">{{ msg.content }}</span>
                </div>
                <div v-else class="flex" :class="msg.isMe ? 'flex-row-reverse' : ''">
                  <div class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs shrink-0"
                       :class="msg.isMe ? 'bg-blue-500 ml-2' : 'bg-orange-400 mr-2'">
                    {{ msg.isMe ? '我' : (msg.sender?.[0] || '人') }}
                  </div>
                  <div class="max-w-[70%]">
                    <div v-if="!msg.isMe && currentSession.type === 'GROUP'" class="text-xs text-gray-400 mb-1 ml-1">
                      {{ msg.sender }}
                    </div>
                    <div class="px-3 py-2 rounded text-sm shadow-sm"
                         :class="msg.isMe ? 'bg-blue-100 text-gray-800' : 'bg-gray-50 border border-gray-100'">
                      {{ msg.content }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="p-4 border-t" v-if="currentSession.type !== 'SYSTEM'">
              <el-input
                v-model="inputText"
                type="textarea"
                :rows="3"
                resize="none"
                placeholder="按 Enter 发送..."
                @keydown.enter.exact.prevent="sendMessage"
              />
              <div class="text-right mt-2">
                <el-button type="primary" size="small" @click="sendMessage" :disabled="!inputText.trim()">发送</el-button>
              </div>
            </div>
            <div v-else class="p-4 border-t text-center text-gray-400 text-sm bg-gray-50">
              系统通知不支持回复
            </div>
          </template>

          <template v-else>
            <div class="h-full flex items-center justify-center text-gray-400">
              <div class="text-center">
                <el-icon size="48"><ChatLineSquare /></el-icon>
                <p class="mt-2">请选择一个会话</p>
              </div>
            </div>
          </template>
        </div>

      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { Search, Bell, ChatDotRound, ChatLineSquare } from '@element-plus/icons-vue'
import { messageApi, type ChatSession, type ChatMessage } from '@/api/message'

const searchKey = ref('')
const sessionList = ref<ChatSession[]>([])
const currentId = ref<number | null>(null)
const currentSession = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const msgBox = ref()

// 初始化加载会话
onMounted(async () => {
  sessionList.value = await messageApi.getSessions()
})

// 切换会话
const selectSession = async (item: ChatSession) => {
  currentId.value = item.id
  currentSession.value = item
  item.unread = 0 // 清除未读
  messages.value = await messageApi.getMessages(item.id)
  scrollToBottom()
}

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim() || !currentId.value) return

  const content = inputText.value
  // 乐观更新 UI
  messages.value.push({ id: Date.now(), type: 'TEXT', isMe: true, content })
  inputText.value = ''
  scrollToBottom()

  // 发送请求
  await messageApi.send(currentId.value, content)
}

const scrollToBottom = () => {
  nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight })
}

// Helpers
const getAvatarColor = (type: string) => ({ SYSTEM: 'bg-orange-400', GROUP: 'bg-blue-500', PRIVATE: 'bg-green-500' }[type] || 'bg-gray-400')
const getTypeName = (type: string) => ({ SYSTEM: '通知', GROUP: '群聊', PRIVATE: '私信' }[type])
</script>

<style scoped>
.student-page { max-width: 1200px; margin: 0 auto; padding: 20px; height: calc(100vh - 80px); }
.chat-wrapper { height: 100%; border: 1px solid #e4e7ed; }
.chat-sidebar { width: 280px; border-right: 1px solid #f0f0f0; background: #fdfdfd; display: flex; flex-direction: column; }
.session-item { display: flex; padding: 12px; cursor: pointer; transition: 0.2s; border-bottom: 1px solid #f9f9f9; }
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; border-right: 3px solid #409EFF; }

.avatar-box { width: 40px; height: 40px; border-radius: 8px; color: white; display: flex; align-items: center; justify-content: center; font-size: 18px; margin-right: 12px; position: relative; shrink: 0; }
.badge { position: absolute; top: -5px; right: -5px; background: #f56c6c; color: white; font-size: 10px; padding: 0 5px; border-radius: 10px; }

.chat-header { height: 50px; border-bottom: 1px solid #f0f0f0; display: flex; align-items: center; padding: 0 20px; background: #fff; }
.chat-content { flex: 1; padding: 20px; overflow-y: auto; background: #fff; }

/* Tailwind Utilities (Scoped implementation for simplicity) */
.flex { display: flex; }
.h-full { height: 100%; }
.flex-col { flex-direction: column; }
.flex-1 { flex: 1; }
.p-3 { padding: 0.75rem; }
.p-4 { padding: 1rem; }
.mb-1 { margin-bottom: 0.25rem; }
.mb-4 { margin-bottom: 1rem; }
.mr-2 { margin-right: 0.5rem; }
.ml-2 { margin-left: 0.5rem; }
.mt-2 { margin-top: 0.5rem; }
.border-b { border-bottom: 1px solid #eee; }
.border-t { border-top: 1px solid #eee; }
.bg-gray-50 { background-color: #f9fafb; }
.bg-gray-100 { background-color: #f3f4f6; }
.bg-blue-100 { background-color: #ecf5ff; }
.bg-blue-500 { background-color: #409EFF; }
.bg-orange-400 { background-color: #fa8c16; }
.bg-green-500 { background-color: #67c23a; }
.bg-white { background-color: #fff; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 14px; }
.text-gray-400 { color: #9ca3af; }
.text-gray-500 { color: #6b7280; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.font-bold { font-weight: 700; }
.rounded { border-radius: 4px; }
.rounded-full { border-radius: 9999px; }
.truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.justify-between { justify-content: space-between; }
.items-center { align-items: center; }
.justify-center { justify-content: center; }
.flex-row-reverse { flex-direction: row-reverse; }
.shrink-0 { flex-shrink: 0; }
.shadow-sm { box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); }
.overflow-hidden { overflow: hidden; }
.overflow-y-auto { overflow-y: auto; }
</style>
