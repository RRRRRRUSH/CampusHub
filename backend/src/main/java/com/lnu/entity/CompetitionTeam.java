package com.lnu.entity;

import lombok.Data;

@Data
public class CompetitionTeam {
    private Long id;
    private Long eventId;
    private String teamName;
    private String leaderId;   // 队长学号
    private String leaderName; // 队长姓名
    private String teamCode;   // 邀请码
    private String fileUrl;
    private String fileName;
    private String submitTime;
    private String awardLevel;
    private Double score;
    private String applyTime;
}