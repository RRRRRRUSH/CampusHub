import request from '@/utils/request'

export const competitionApi = {
  // 1. 管理端列表
  getList: (sourceType: string, userId?: number) => {
    return request.get('/competition/list', { params: { sourceType, userId } })
  },

  // 2. 学生端列表
  getStudentList: (sourceType: string, userId?: number) => {
    return request.get('/competition/student/list', { params: { sourceType, userId } })
  },

  getDetail: (id: string) => {
    return request.get(`/competition/detail/${id}`)
  },

  // 3. 创建
  create: (data: any, userId?: number) => {
    return request.post('/competition/create', data, { params: { userId } })
  },

  update: (id: string, data: any) => {
    return request.post('/competition/update', { ...data, id })
  },

  changeStatus: (id: number, status: string) => {
    return request.post(`/competition/status/${id}/${status}`)
  },

  // 🟢 4. [新增] 状态回退 (后悔药)
  rollback: (id: number) => {
    return request.post('/competition/rollback', { id })
  },

  delete: (id: number) => {
    return request.delete(`/competition/delete/${id}`)
  },

  // --- 参赛相关 ---
  signup: (eventId: number, params: any) => {
    return request.post('/competition/signup', { ...params, eventId })
  },

  joinByCode: (code: string, params: any) => {
    return request.post('/competition/join', { ...params, code })
  },

  getMyList: (sid: string) => {
    return request.get('/competition/my-list', { params: { sid } })
  },

  // --- 管理相关 ---
  getParticipants: (id: string) => {
    return request.get(`/competition/participants/${id}`)
  },

  submitGrade: (recordId: number, level: string) => {
    return request.post(`/competition/grade/${recordId}`, null, { params: { level } })
  },

  uploadWork: (recordId: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`/competition/upload/${recordId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
