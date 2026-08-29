package com.lnu.service;

import com.lnu.entity.*;
import com.lnu.mapper.*;
import com.lnu.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired private AuditMapper auditMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CompetitionMapper competitionMapper;

    // 1. 提交申请
    @Override
    public void apply(Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        User user = userMapper.findById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        AuditRecord record = new AuditRecord();
        record.setUserId(userId);
        record.setType((String) params.get("type"));
        record.setTitle((String) params.get("title"));
        record.setProofUrl((String) params.get("proofUrl"));
        record.setDescription((String) params.get("description"));

        record.setStudentId(user.getUsername());
        record.setStudentName(user.getName());
        record.setClassName(user.getClassName());

        record.setStatus("pending");
        record.setApplyTime(new Date()); // 🟢 修复：直接存 Date 对象
        record.setSourceType("external");

        if ("volunteer".equals(record.getType())) {
            if (params.get("hours") != null) {
                record.setScore(Double.valueOf(params.get("hours").toString()));
            }
            record.setAwardLevel(null);
        } else {
            record.setScore(0.0);
            record.setAwardLevel((String) params.get("awardLevel"));
        }

        auditMapper.insert(record);
    }

    // 2. 获取列表 (目前逻辑主要在 Controller 实现，这里保留空实现或按需补充)
    @Override
    public List<AuditRecord> getList(Long userId, String queryType) {
        return new ArrayList<>();
    }

    // 3. 审核 (注意：目前 Controller 是直接调用的 Mapper，这个方法可能未被触发)
    // 如果你想用这个方法，需要在 Controller 中改为调用 auditService.audit(...)
    @Override
    @Transactional
    public void audit(Long recordId, String action, String comment, Long userId) {
        User auditor = userMapper.findById(userId);
        AuditRecord record = auditMapper.findById(recordId);
        if (record == null) throw new RuntimeException("记录不存在");

        String nextStatus = record.getStatus();

        // 简化的权限逻辑 (与 Controller 保持一致)
        if ("AUDITOR".equals(auditor.getRole()) || "ROOT".equals(auditor.getRole()) || "ADMIN".equals(auditor.getRole())) {
            if ("pass".equals(action) || "approved".equals(action)) {
                nextStatus = "approved";
                // 自动加分
                addScoreToUser(record.getStudentId(), record.getScore(), record.getType());
            } else {
                nextStatus = "rejected";
            }
        }

        record.setStatus(nextStatus);
        record.setAuditorName(auditor.getName());
        record.setAuditComment(comment);
        record.setAuditTime(new Date()); // 🟢 修复：直接存 Date

        if ("approved".equals(nextStatus)) {
            auditMapper.approve(record.getId());
        } else {
            auditMapper.reject(record.getId(), comment);
        }
    }

    // --- 4. 核心：同步校内赛结果 (系统自动调用) ---
    @Override
    @Transactional
    public void syncCompetitionResult(Long competitionId) {
        Competition comp = competitionMapper.findById(competitionId);
        if (comp == null) return;

        List<CompetitionTeam> teams = competitionMapper.findTeamsByEventId(competitionId);

        for (CompetitionTeam team : teams) {
            double score = calculateScore(team.getAwardLevel(), true);
            List<TempStudent> allStudents = getAllStudents(team);

            for (TempStudent s : allStudents) {
                // 简单的查重逻辑 (根据 学号 + 赛事名)
                // 注意：这里 findByType 返回的是 List<AuditRecord>
                List<AuditRecord> exists = auditMapper.findByType("competition");
                boolean hasRec = exists.stream().anyMatch(r ->
                        r.getStudentId().equals(s.sid) &&
                                r.getTitle().equals("【校内】" + comp.getTitle())
                );

                if (!hasRec) {
                    // 查用户ID
                    User u = userMapper.findByUsername(s.sid);
                    if (u != null) {
                        AuditRecord record = new AuditRecord();
                        record.setUserId(u.getId()); // 关键：必须有 userId
                        record.setStudentId(s.sid);
                        record.setStudentName(s.name);
                        record.setClassName(s.cls);

                        record.setType("competition");
                        record.setSourceType("internal");
                        record.setOriginId(comp.getId());
                        record.setTitle("【校内】" + comp.getTitle());
                        record.setAwardLevel(team.getAwardLevel() == null ? "完赛" : team.getAwardLevel());
                        record.setScore(score);
                        record.setProofUrl("");

                        record.setStatus("approved"); // 校内赛直接通过
                        record.setApplyTime(new Date()); // 🟢 修复：直接存 Date

                        auditMapper.insert(record);

                        // 自动加分
                        addScoreToUser(s.sid, score, "competition");
                    }
                }
            }
        }
    }

    // --- 辅助方法 ---

    private void addScoreToUser(String studentId, Double delta, String type) {
        User student = userMapper.findByUsername(studentId);
        if (student != null) {
            if ("competition".equals(type)) userMapper.addCompetitionScore(student.getId(), delta);
            else if ("volunteer".equals(type)) userMapper.addVolunteerScore(student.getId(), delta);
        }
    }

    private double calculateScore(String level, boolean isInternal) {
        if (level == null) level = "";
        if (isInternal) return (level.matches(".*(一等|二等|三等|金|银|铜).*")) ? 2.0 : 1.0;
        if (level.contains("国家")) {
            if (level.contains("一等")) return 8.0;
            if (level.contains("二等")) return 7.0;
            if (level.contains("三等")) return 6.0;
            return 5.0;
        }
        if (level.contains("省")) {
            if (level.contains("一等")) return 5.0;
            if (level.contains("二等")) return 4.0;
            if (level.contains("三等")) return 3.0;
            return 2.0;
        }
        if (level.contains("校")) {
            if (level.matches(".*(一等|二等|三等|奖).*")) return 2.0;
            return 1.0;
        }
        return 1.0;
    }

    private List<TempStudent> getAllStudents(CompetitionTeam team) {
        List<TempStudent> list = new ArrayList<>();
        if (team.getLeaderId() != null) {
            User u = userMapper.findByUsername(team.getLeaderId());
            list.add(new TempStudent(team.getLeaderId(),
                    u != null ? u.getName() : team.getLeaderName(),
                    u != null ? u.getClassName() : ""));
        }
        List<CompetitionMember> members = competitionMapper.findMembersByTeamId(team.getId());
        for (CompetitionMember m : members) {
            boolean exists = false;
            for (TempStudent t : list) if (t.sid.equals(m.getUserId())) exists = true;
            if (!exists) list.add(new TempStudent(m.getUserId(), m.getUserName(), m.getCollege()));
        }
        return list;
    }

    // 内部类
    class TempStudent {
        String sid, name, cls;
        public TempStudent(String s, String n, String c) { sid = s; name = n; cls = c; }
    }
}