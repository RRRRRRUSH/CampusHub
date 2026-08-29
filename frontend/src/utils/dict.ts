// src/utils/dict.ts

// 状态对应的颜色与文案配置
export const STATUS_MAP: Record<string, { label: string; type: string }> = {
  // 通用状态
  open: { label: '进行中', type: 'primary' },
  finished: { label: '已结束', type: 'info' },

  // 报名阶段
  registering: { label: '报名中', type: 'success' },
  full: { label: '名额已满', type: 'danger' },

  // 执行阶段
  submitting: { label: '提交作品', type: 'warning' }, // 橙色提醒去提交
  judging: { label: '评审中', type: 'warning' },
  publicity: { label: '公示期', type: 'danger' }, // 红色醒目

  // 审核状态
  pending: { label: '审核中', type: 'warning' },
  pass: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' },
  approved: { label: '已认定', type: 'success' }
}

// 获取状态标签属性
export function getStatusTag(status: string) {
  return STATUS_MAP[status] || { label: '未知', type: 'info' }
}

// 级别字典
export const LEVEL_MAP: Record<string, string> = {
  nation: '国家级',
  province: '省级',
  school: '校级'
}
