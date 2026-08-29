package com.lnu.entity;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username; // 学号/工号
    private String password;
    private String name;

    // 角色：STUDENT, TEACHER, AUDITOR, ROOT
    private String role;
    private String token;

    // 状态：active (正常), disabled (禁用)
    private String status;

    // 详细信息
    private String college;      // 学院名称
    private String major;        // 专业名称
    private String grade;        // 年级
    private String className;    // 班级

    private String collegeCode;
    private String majorCode;

    // 是否学生干部
    private Boolean isCadre;

    // 🟢 新增：账户积分
    private Double scoreCompetition; // 竞赛学分
    private Double scoreVolunteer;   // 志愿工时
    private String email;
}