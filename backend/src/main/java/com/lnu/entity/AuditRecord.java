package com.lnu.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class AuditRecord {
    private Long id;
    private Long userId;

    // 核心业务字段
    private String type;        // volunteer / competition
    private String sourceType;  // internal (校内自动) / external (校外申请) 🟢 新增
    private Long originId;      // 关联的原始ID (比如赛事ID) 🟢 新增

    private String title;       // 标题
    private Double score;       // 分数/工时
    private String awardLevel;  // 奖项等级

    private String proofUrl;    // 证明材料
    private String description; // 描述

    private String status;      // pending / approved / rejected
    private String rejectReason;// 驳回原因

    // 审核人信息
    private String auditorName;
    private String auditComment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditTime;

    // 申请时间 (之前可能叫 createTime，为了配合你的Service，这里改为 applyTime)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date applyTime;

    // --- 辅助字段 (用于连表查询展示，不存数据库) ---
    // 配合 MyBatis 的查询结果映射
    private String studentName;
    private String studentId;
    private String college;
    private String major;
    private String className;
}