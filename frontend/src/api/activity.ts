import request from '@/utils/request'

export const activityApi = {
  // 1. 获取列表 (增加 userId 可选参数)
  getList: (type: 'internal' | 'external', userId?: number) => {
    return request.get('/activity/list', { params: { type, userId } })
  },

  // 2. 获取详情
  getDetail: (id: string | number) => {
    return request.get(`/activity/detail/${id}`)
  },

  // 3. 创建活动
  create: (data: any) => {
    return request.post('/activity/create', data)
  },

  // 4. 更新活动
  update: (id: string | number, data: any) => {
    return request.post(`/activity/update/${id}`, data)
  },

  // 5. 删除活动
  delete: (id: string | number) => {
    return request.post(`/activity/delete/${id}`)
  },

  // 6. 学生报名
  signup: (activityId: number, studentInfo: any) => {
    return request.post('/activity/signup', { activityId, ...studentInfo })
  },

  // 7. 获取报名名单
  getParticipants: (id: string | number) => {
    return request.get(`/activity/participants/${id}`)
  },

  // 8. 变更状态
  changeStatus: (id: string | number, status: string) => {
    return request.post('/activity/status', { id, status })
  },

  // 9. 考勤核验
  verifyAttendance: (signupId: number, isValid: boolean) => {
    return request.post('/activity/verify', { id: signupId, isValid })
  },

  // 10. 获取我的列表
  getMyList: (userId: number) => {
    return request.get('/activity/my-list-by-id', { params: { userId } })
  },

  // 11. 取消报名
  cancelSignup: (activityId: number, userId: number) => {
    return request.post('/activity/cancel', { activityId, userId })
  },

  // 12. 导出名单 (Blob)
  exportParticipants: (activityId: string | number) => {
    return request.get(`/activity/export/participants/${activityId}`, {
      responseType: 'blob'
    })
  },

  // 13. 批量状态
  batchUpdateSignupStatus: (signupIds: number[], status: string) => {
    return request.post('/activity/signup/batch/status', { signupIds, status })
  },

  // 14. 结算工时
  settleActivity: (activityId: string | number) => {
    return request.post(`/activity/settle/${activityId}`)
  },

  // 🟢 15. [新增] 活动回退 (后悔药)
  rollbackActivity: (id: string | number) => {
    return request.post('/activity/rollback', { id })
  }
}
