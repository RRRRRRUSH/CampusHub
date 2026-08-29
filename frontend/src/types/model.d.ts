// src/types/model.d.ts

// 1. 活动/竞赛列表项
export interface EventItem {
  id: number | string
  type: 'activity' | 'competition'
  sourceType: 'internal' | 'external'
  title: string
  status: 'open' | 'registering' | 'full' | 'execution' | 'judging' | 'publicity' | 'finished'
  desc?: string
  publishTime?: string

  // 活动字段
  format?: 'online' | 'offline'
  location?: string
  hours?: number
  joined?: number
  quota?: number
  needPhoto?: boolean
  regStartTime?: string
  regEndTime?: string
  activityTime?: string

  // 竞赛字段
  level?: string
  mode?: 'individual' | 'team'
  isQualified?: boolean
  fileTypes?: string[]
  link?: string
}

// 2. 报名/参与记录 (消除 isValid, hours, grantedHours 报错的关键)
export interface SignupRecord {
  id: number | string
  eventId: number | string
  eventTitle: string
  eventType: 'activity' | 'competition'
  status: string

  // 核心修复点：补全这些字段
  activityTime?: string
  needPhoto?: boolean
  hasUploadedPhoto?: boolean
  isValid?: boolean | null  // 🟢 修复报错
  hours?: number            // 🟢 修复报错
  grantedHours?: number     // 🟢 修复报错

  // 竞赛字段
  format?: string
  endTime?: string
  hasUploadedFile?: boolean
  isTeam?: boolean
  leader?: string
  teamName?: string
  members?: string[]
  fileUrl?: string
  result?: string
  awardLevel?: string
}

// 3. 认定申请 (消除 source 报错的关键)
export interface AuditApplication {
  id: number | string
  studentId: string
  studentName: string
  college: string
  major?: string
  className?: string

  type: 'volunteer' | 'competition'
  source?: 'external' | 'internal_sync' // 🟢 修复报错

  title: string
  desc?: string
  status: 'pending' | 'pass' | 'rejected'

  score: number
  applyTime: string
  proofImgs: string[]
}
