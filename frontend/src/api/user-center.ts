import request from '@/utils/request'

export const userApi = {
  // 1. 获取个人详细信息
  getInfo: (userId: number) => {
    return request.get(`/user/info/${userId}`)
  },

  // 2. 更新基础资料
  updateInfo: (data: any) => {
    return request.post('/user/update/info', data)
  },

  // 3. 绑定邮箱
  bindEmail: (data: { userId: number; email: string }) => {
    return request.post('/user/bind/email', data)
  },

  // 4. 修改密码
  updatePwd: (data: any) => {
    return request.post('/user/update/password', data)
  },

  // --- 🟢 新增：管理端接口 ---

  // 5. 获取用户列表 (带权限围栏)
  getList: (params: { keyword?: string; role?: string; currentUserId: number }) => {
    return request.get('/user/list', { params })
  },

  // 6. 重置密码
  resetPassword: (id: number) => {
    return request.post('/user/reset-password', { id })
  },

  // 7. 封禁/解封
  toggleStatus: (id: number, status: string) => {
    return request.post('/user/status', { id, status })
  },

  // 8. 任命/取消干部
  setCadre: (id: number, isCadre: boolean) => {
    return request.post('/user/set-cadre', { id, isCadre })
  }
}
