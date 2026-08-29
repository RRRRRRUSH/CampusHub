package com.lnu.entity;

import lombok.Data;

@Data
public class Competition {
    private Long id;
    private String title;
    private String level;       // nation, province, school
    private String sourceType;  // internal, external
    private String status;      // open, registering, execution, judging, publicity, finished
    private String mode;        // individual, team
    private String format;      // online, offline

    // 时间配置
    private String regStartTime;
    private String regEndTime;
    private String compStartTime;
    private String compEndTime;

    // 详情配置
    private String description;
    private String externalLink;
    private Boolean isQualified;
    private Integer joinedCount;

    // 🟢 新增：发布者部门 (权限隔离)
    private String publishDept;

    // 🟢 新增：可见性限制 (学生端过滤)
    private String limitCollege;
    private String limitGrade;
    private String limitCampus;
}