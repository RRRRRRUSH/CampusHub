// src/api/message.ts

// 0延迟模拟
const mockRequest = <T>(data: T): Promise<T> => Promise.resolve(data)

const KEY_SESSIONS = 'LNU_DB_MSG_SESSIONS'
const KEY_MESSAGES = 'LNU_DB_MSG_LOGS'

export interface ChatSession {
  id: number
  type: 'SYSTEM' | 'GROUP' | 'PRIVATE' | 'TODO'
  name: string
  avatar?: string
  unread: number
  lastMsg: string
  time: string
}

export interface ChatMessage {
  id: number
  sessionId: number // 关联会话ID
  type: 'TEXT' | 'SYSTEM'
  isMe: boolean     // true=我发的, false=对方发的
  sender?: string   // 发送者姓名 (群聊用)
  content: string
  time?: string
}

// --- 种子数据 (初始会话) ---
const SEED_SESSIONS: ChatSession[] = [
  { id: 1, type: 'SYSTEM', name: '系统通知', unread: 2, lastMsg: '您的志愿工时已到账', time: '10:00' },
  { id: 2, type: 'GROUP', name: '网页设计大赛-咨询群', unread: 5, lastMsg: '老师: 请大家注意截止时间', time: '09:30' },
  { id: 3, type: 'PRIVATE', name: '张老师 (团委)', unread: 0, lastMsg: '好的，收到。', time: '昨天' }
]

// --- 种子数据 (初始消息记录) ---
const SEED_MESSAGES: ChatMessage[] = [
  { id: 101, sessionId: 1, type: 'SYSTEM', isMe: false, content: '欢迎登录一站式服务大厅。' },
  { id: 102, sessionId: 1, type: 'SYSTEM', isMe: false, content: '您的“社区扫雪”志愿活动工时（2.0h）已审核通过。' },

  { id: 201, sessionId: 2, type: 'SYSTEM', isMe: false, content: '您已加入群聊' },
  { id: 202, sessionId: 2, type: 'TEXT', isMe: false, sender: '李同学', content: '请问作品格式有限制吗？' },
  { id: 203, sessionId: 2, type: 'TEXT', isMe: false, sender: '老师', content: '仅限 ZIP 压缩包，不超过 50MB。' },

  { id: 301, sessionId: 3, type: 'TEXT', isMe: true, content: '老师您好，我想咨询一下补录的事情。' },
  { id: 302, sessionId: 3, type: 'TEXT', isMe: false, sender: '张老师', content: '请在“认定申请”里提交相关证明即可。' },
  { id: 303, sessionId: 3, type: 'TEXT', isMe: true, content: '好的，收到。' }
]

// --- DB Helpers ---
function getSessionsDB(): ChatSession[] {
  const json = localStorage.getItem(KEY_SESSIONS)
  if (!json) {
    localStorage.setItem(KEY_SESSIONS, JSON.stringify(SEED_SESSIONS))
    return SEED_SESSIONS
  }
  return JSON.parse(json)
}

function saveSessionsDB(data: ChatSession[]) {
  localStorage.setItem(KEY_SESSIONS, JSON.stringify(data))
}

function getMessagesDB(): ChatMessage[] {
  const json = localStorage.getItem(KEY_MESSAGES)
  if (!json) {
    localStorage.setItem(KEY_MESSAGES, JSON.stringify(SEED_MESSAGES))
    return SEED_MESSAGES
  }
  return JSON.parse(json)
}

function saveMessagesDB(data: ChatMessage[]) {
  localStorage.setItem(KEY_MESSAGES, JSON.stringify(data))
}

export const messageApi = {
  // 1. 获取会话列表
  getSessions: async (): Promise<ChatSession[]> => {
    const list = getSessionsDB()
    return mockRequest(list)
  },

  // 2. 获取某会话的消息 (并自动清除未读)
  getMessages: async (sessionId: number): Promise<ChatMessage[]> => {
    // A. 获取消息
    const all = getMessagesDB()
    const msgs = all.filter(m => m.sessionId == sessionId)

    // B. 清除该会话的未读数 (副作用)
    const sessions = getSessionsDB()
    const session = sessions.find(s => s.id == sessionId)
    if (session && session.unread > 0) {
      session.unread = 0
      saveSessionsDB(sessions)
    }

    return mockRequest(msgs)
  },

  // 3. 发送消息
  send: async (sessionId: number, content: string) => {
    // A. 写入消息表
    const allMsgs = getMessagesDB()
    allMsgs.push({
      id: Date.now(),
      sessionId: Number(sessionId),
      type: 'TEXT',
      isMe: true, // 既然是“发送”，那肯定是我发的
      content,
      time: new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})
    })
    saveMessagesDB(allMsgs)

    // B. 更新会话表的“最后一条消息”
    const allSessions = getSessionsDB()
    const session = allSessions.find(s => s.id == sessionId)
    if (session) {
      session.lastMsg = content
      session.time = '刚刚'
      // 注意：如果是真实的聊天，给对方发消息会导致对方的 unread +1
      // 但这里是本地模拟，自己发的消息不增加自己的 unread
      saveSessionsDB(allSessions)
    }

    return mockRequest({ success: true })
  }
}
