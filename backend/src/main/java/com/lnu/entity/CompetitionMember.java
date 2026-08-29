package com.lnu.entity;

import lombok.Data;

@Data
public class CompetitionMember {
    private Long id;
    private Long teamId;   // 归属队伍ID
    private String userId; // 成员学号
    private String userName;
    private String college;
}