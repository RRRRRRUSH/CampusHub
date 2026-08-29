package com.lnu.controller;

import com.lnu.common.Result;
import com.lnu.entity.Competition;
import com.lnu.entity.CompetitionMember;
import com.lnu.entity.CompetitionTeam;
import com.lnu.mapper.AuditMapper;
import com.lnu.mapper.CompetitionMapper;
import com.lnu.mapper.UserMapper; // 🟢 引入 UserMapper 用于扣分
import com.lnu.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/competition")
@CrossOrigin
public class CompetitionController {

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private CompetitionMapper competitionMapper;

    @Autowired
    private AuditMapper auditMapper;

    @Autowired
    private UserMapper userMapper;

    // --- 查询 ---
    @GetMapping("/list")
    public Result getList(@RequestParam String sourceType, @RequestParam(required = false) Long userId) {
        return Result.success(competitionService.getList(sourceType, userId));
    }

    @GetMapping("/student/list")
    public Result getStudentList(@RequestParam String sourceType, @RequestParam(required = false) Long userId) {
        return Result.success(competitionService.getStudentList(sourceType, userId));
    }

    @GetMapping("/detail/{id}")
    public Result getDetail(@PathVariable Long id) {
        return Result.success(competitionService.getDetail(id));
    }

    // --- 管理维护 ---
    @PostMapping("/create")
    public Result create(@RequestBody Competition competition, @RequestParam(required = false) Long userId) {
        competitionService.create(competition, userId);
        return Result.success();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Competition competition) {
        competitionService.update(competition);
        return Result.success();
    }

    @PostMapping("/status/{id}/{status}")
    public Result changeStatus(@PathVariable Long id, @PathVariable String status) {
        competitionService.changeStatus(id, status);
        return Result.success();
    }

    // 🟢 [新增] 竞赛回退接口
    @PostMapping("/rollback")
    public Result rollback(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Competition comp = competitionMapper.findById(id);

        if (!"finished".equals(comp.getStatus())) {
            return Result.error("只有已归档的赛事才能回退");
        }

        // 1. 资产回滚：重新计算每个人的得分，并扣除
        List<CompetitionTeam> teams = competitionMapper.findTeamsByEventId(id);
        for (CompetitionTeam team : teams) {
            double score = calculateScore(team.getAwardLevel());

            // 扣除队长 (如果存在)
            if (team.getLeaderId() != null) {
                // 根据学号查ID (因为team表存的是学号)
                com.lnu.entity.User u = userMapper.findByUsername(team.getLeaderId());
                if(u != null) userMapper.deductCompetitionScore(u.getId(), score);
            }

            // 扣除队员
            List<CompetitionMember> members = competitionMapper.findMembersByTeamId(team.getId());
            for (CompetitionMember m : members) {
                com.lnu.entity.User u = userMapper.findByUsername(m.getUserId());
                if(u != null) userMapper.deductCompetitionScore(u.getId(), score);
            }
        }

        // 2. 数据清洗：删除自动生成的认定记录
        auditMapper.deleteByOriginId(id);

        // 3. 状态回退：回到 "评审中" (judging) 方便修改成绩
        competitionMapper.updateStatus(id, "judging");

        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        competitionService.delete(id);
        return Result.success();
    }

    // --- 参赛与名单 ---
    @PostMapping("/signup")
    public Result signup(@RequestBody Map<String, Object> params) {
        return Result.success(competitionService.signup(params));
    }

    @PostMapping("/join")
    public Result joinByCode(@RequestBody Map<String, Object> params) {
        return Result.success(competitionService.joinByCode(params));
    }

    @GetMapping("/my-list")
    public Result getMyList(@RequestParam String sid) {
        return Result.success(competitionService.getMyList(sid));
    }

    @GetMapping("/participants/{eventId}")
    public Result getParticipants(@PathVariable Long eventId) {
        return Result.success(competitionService.getParticipants(eventId));
    }

    @PostMapping("/grade/{recordId}")
    public Result submitGrade(@PathVariable Long recordId, @RequestParam String level) {
        competitionService.submitGrade(recordId, level);
        return Result.success();
    }

    @PostMapping("/upload/{recordId}")
    public Result uploadWork(@PathVariable Long recordId, @RequestParam("file") MultipartFile file) {
        competitionService.uploadWork(recordId, file);
        return Result.success();
    }

    // 辅助算分 (与Service保持一致)
    private double calculateScore(String level) {
        if (level == null) level = "";
        if (level.matches(".*(一等|二等|三等|金|银|铜).*")) return 2.0;
        return 1.0;
    }
}