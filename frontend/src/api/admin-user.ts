import request from '@/utils/request'

export interface UserItem {
  id?: number | string
  username: string
  name: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN' | 'ROOT'
  college: string
  major?: string
  className?: string
  status?: 'active' | 'disabled'
}

const API_PREFIX = '/user'

export const adminUserApi = {
  // 1. 获取列表
  // roleType: 'student' | 'teacher'
  getList: (roleType: string) => {
    return request<any, UserItem[]>({
      url: `${API_PREFIX}/list`,
      method: 'get',
      params: { roleType }
    })
  },

  // 2. 保存用户 (新增或编辑)
  saveUser: (user: UserItem) => {
    return request({
      url: `${API_PREFIX}/save`,
      method: 'post',
      data: user
    })
  },

  // 3. 重置密码
  resetPassword: (id: number | string) => {
    return request({
      url: `${API_PREFIX}/reset-pwd/${id}`,
      method: 'put'
    })
  },

  // 4. 切换状态 (后端暂未实现，先保留接口定义)
  toggleStatus: (id: number | string, status: string) => {
    // 预留接口，等后端加了 status 字段再放开
    return Promise.resolve()
    /*
    return request({
      url: `${API_PREFIX}/status/${id}`,
      method: 'put',
      params: { status }
    })
    */
  },

  // 辅助查找 (前端可能用到，但最好是用后端查)
  findUser: (username: string) => {
    // 这个方法之前是用在 Mock 登录里的，现在登录已经改用真实接口了
    // 所以这个方法理论上可以废弃了，为了防止报错，先留个空
    return undefined
  }
}
