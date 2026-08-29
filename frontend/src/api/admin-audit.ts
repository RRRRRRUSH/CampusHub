import request from '@/utils/request'

export const adminAuditApi = {
  // 1. 获取审核列表 (管理端)
  // type: 'volunteer' | 'competition'
  // userId: 可选，用于学生干部鉴权
  getList: (type: string, userId?: number) => {
    return request.get('/audit/list', { params: { type, userId } })
  },

  // 2. 批量通过
  approve: (ids: (number | string)[]) => {
    return request.post('/audit/approve', { ids })
  },

  // 3. 单条驳回
  reject: (id: number | string, reason: string) => {
    return request.post('/audit/reject', { id, reason })
  }
}
