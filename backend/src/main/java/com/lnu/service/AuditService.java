package com.lnu.service;

import com.lnu.entity.AuditRecord;
import java.util.List;
import java.util.Map;

public interface AuditService {
    // 学生提交申请
    void apply(Map<String, Object> params);

    // 获取列表 (queryType: 'personal' 查自己, 'manage' 查待办)
    List<AuditRecord> getList(Long userId, String queryType);

    // 审核 / 确认
    void audit(Long recordId, String action, String comment, Long userId);

    // 🟢 核心：同步校内赛结果 (系统自动调用)
    void syncCompetitionResult(Long competitionId);
}