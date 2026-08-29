import request from '@/utils/request'

// 定义登录参数接口
export interface LoginParams {
  username: string
  password?: string
}

// 定义后端返回的用户信息接口 (参考 Java 的 User 实体类)
export interface UserInfo {
  id: number
  username: string // 学号
  name: string     // 真实姓名
  role: string     // 角色
  college?: string // 学院
  token: string    // 登录凭证
}

export const authApi = {
  /**
   * 登录接口
   * 对应后端: POST /user/login
   */
  login: (params: LoginParams) => {
    return request<any, UserInfo>({
      url: '/user/login',
      method: 'post',
      data: params
    })
  },

  /**
   * 获取用户信息 (可选，用于刷新页面后重新拉取信息)
   * 对应后端: GET /user/info
   */
  getUserInfo: (token: string) => {
    return request<any, UserInfo>({
      url: '/user/info',
      method: 'get',
      params: { token }
    })
  },

  /**
   * 退出登录
   * 前端直接清空 Token 即可，后端如果是无状态 JWT 可以不调接口
   */
  logout: () => {
    // 如果后端有 logout 接口可以在这里调用
    return Promise.resolve()
  }
}
