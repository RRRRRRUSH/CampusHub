import request from '@/utils/request'

export const auditApi = {
  // 1. 获取列表
  getList: (type: string, userId?: number) => {
    return request.get('/audit/list', { params: { type, userId } })
  },

  // 2. 提交申请
  apply: (data: any) => {
    return request.post('/audit/apply', data)
  },

  // 3. 审批通过
  approve: (ids: number[]) => {
    return request.post('/audit/approve', { ids })
  },

  // 4. 驳回
  reject: (id: number, reason: string) => {
    return request.post('/audit/reject', { id, reason })
  },

  // 🟢 5. [新增] 重置状态 (后悔药)
  reset: (id: number) => {
    return request.post('/audit/reset', { id })
  },

  // 6. 获取我的申请
  getMyList: (userId: number) => {
    return request.get('/audit/list/personal', { params: { userId } })
  }
}
