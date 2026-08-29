package com.lnu.service;

import com.lnu.entity.Competition;
import com.lnu.entity.CompetitionMember;
import com.lnu.entity.CompetitionTeam;
import com.lnu.entity.User;
import com.lnu.mapper.CompetitionMapper;
import com.lnu.mapper.CompetitionMemberMapper;
import com.lnu.mapper.CompetitionTeamMapper;
import com.lnu.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CompetitionServiceImpl implements CompetitionService {

    @Autowired private CompetitionMapper competitionMapper;
    @Autowired private CompetitionTeamMapper teamMapper;
    @Autowired private CompetitionMemberMapper memberMapper;
    @Autowired private UserMapper userMapper;

    // 🟢 关键：注入 AuditService，用于调用同步方法
    @Autowired private AuditService auditService;

    @Value("${campus.upload-path}")
    private String uploadPath;

    @Override
    public List<Competition> getList(String sourceType, Long userId) {
        String searchDept = null;
        if (userId != null) {
            User user = userMapper.findById(userId);
            if (user != null && "TEACHER".equals(user.getRole())) {
                searchDept = user.getCollege();
            }
        }
        return competitionMapper.findBySourceType(sourceType, searchDept);
    }

    @Override
    public List<Competition> getStudentList(String sourceType, Long userId) {
        String college = null;
        String grade = null;
        if (userId != null) {
            User user = userMapper.findById(userId);
            if (user != null) {
                college = user.getCollege();
                grade = user.getGrade();
            }
        }
        return competitionMapper.findForStudent(sourceType, college, grade);
    }

    @Override
    public Competition getDetail(Long id) {
        return competitionMapper.findById(id);
    }

    @Override
    public void create(Competition competition) {
        create(competition, null);
    }

    @Override
    public void create(Competition competition, Long userId) {
        if (competition.getStatus() == null) competition.setStatus("open");
        if (competition.getSourceType() == null) competition.setSourceType("internal");

        if (userId != null) {
            User user = userMapper.findById(userId);
            if (user != null && "TEACHER".equals(user.getRole())) {
                competition.setPublishDept(user.getCollege());
            } else if (user != null && ("ROOT".equals(user.getRole()) || "AUDITOR".equals(user.getRole()))) {
                if (competition.getPublishDept() == null) {
                    competition.setPublishDept("校级/全校");
                }
            }
        }
        validateTime(competition);
        competitionMapper.insert(competition);
    }

    @Override
    public void update(Competition competition) {
        validateTime(competition);
        competitionMapper.update(competition);
    }

    // 🟢 核心修复：状态变更时触发同步
    @Override
    public void changeStatus(Long id, String status) {
        competitionMapper.updateStatus(id, status);

        // 只有当状态变成 "公示中" 或 "已归档" 时，才触发自动同步
        if ("publicity".equals(status) || "finished".equals(status)) {
            System.out.println("检测到赛事结束/公示，正在同步认定数据 ID: " + id);
            try {
                auditService.syncCompetitionResult(id);
            } catch (Exception e) {
                System.err.println("同步失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void delete(Long id) {
        competitionMapper.delete(id);
    }

    // --- 参赛相关 ---
    @Override
    public List<Map<String, Object>> getParticipants(Long eventId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<CompetitionTeam> teams = competitionMapper.findTeamsByEventId(eventId);
        for (CompetitionTeam team : teams) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", team.getId());
            map.put("teamName", team.getTeamName());
            map.put("leaderName", team.getLeaderName());
            map.put("leaderId", team.getLeaderId());
            map.put("fileUrl", team.getFileUrl());
            map.put("fileName", team.getFileName());
            map.put("submitTime", team.getSubmitTime());
            map.put("awardLevel", team.getAwardLevel());
            map.put("members", competitionMapper.findMembersByTeamId(team.getId()));
            result.add(map);
        }
        return result;
    }

    @Override
    public void submitGrade(Long recordId, String level) {
        teamMapper.updateAward(recordId, level);
    }

    @Override
    @Transactional
    public Map<String, Object> signup(Map<String, Object> params) {
        Long eventId = Long.valueOf(params.get("eventId").toString());
        Competition comp = competitionMapper.findById(eventId);
        if (comp == null) throw new RuntimeException("赛事不存在");

        if ("internal".equals(comp.getSourceType())) {
            long now = System.currentTimeMillis();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long start = sdf.parse(comp.getRegStartTime()).getTime();
                long end = sdf.parse(comp.getRegEndTime()).getTime();
                if (now < start) throw new RuntimeException("报名尚未开始");
                if (now > end) throw new RuntimeException("报名已截止");
            } catch (ParseException e) {}
        }

        String sid = (String) params.get("sid");
        Boolean isTeam = (Boolean) params.get("isTeam");
        String teamName = (String) params.get("name");
        String leaderName = (String) params.get("leader");

        if (sid == null || sid.trim().isEmpty()) throw new RuntimeException("请先登录");
        if (teamMapper.countByLeader(eventId, sid) > 0) throw new RuntimeException("您已作为队长报名");
        if (memberMapper.countByMember(eventId, sid) > 0) throw new RuntimeException("您已作为队员加入其他队伍");

        CompetitionTeam team = new CompetitionTeam();
        team.setEventId(eventId);
        team.setTeamName(teamName);
        team.setLeaderId(sid);
        team.setLeaderName(leaderName);
        team.setApplyTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        team.setAwardLevel("");
        team.setFileUrl("");
        if (Boolean.TRUE.equals(isTeam)) {
            team.setTeamCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        } else {
            team.setTeamCode("");
        }
        teamMapper.insert(team);
        competitionMapper.incrementJoinedCount(eventId);

        Map<String, Object> res = new HashMap<>();
        res.put("teamCode", team.getTeamCode());
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> joinByCode(Map<String, Object> params) {
        String code = (String) params.get("code");
        String sid = (String) params.get("sid");
        String name = (String) params.get("name");
        String college = (String) params.get("college");

        if (sid == null) throw new RuntimeException("用户信息丢失");
        CompetitionTeam team = teamMapper.findByCode(code);
        if (team == null) throw new RuntimeException("无效的邀请码");

        Competition comp = competitionMapper.findById(team.getEventId());
        try {
            long now = System.currentTimeMillis();
            long end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(comp.getRegEndTime()).getTime();
            if (now > end) throw new RuntimeException("报名已截止，无法加入队伍");
        } catch (Exception e) {}

        if (team.getLeaderId().equals(sid)) throw new RuntimeException("您是队长");
        if (memberMapper.countInTeam(team.getId(), sid) > 0) throw new RuntimeException("您已在该队伍中");
        if (teamMapper.countByLeader(team.getEventId(), sid) > 0) throw new RuntimeException("您已在其他队伍担任队长");
        if (memberMapper.countByMember(team.getEventId(), sid) > 0) throw new RuntimeException("您已加入其他队伍");

        CompetitionMember member = new CompetitionMember();
        member.setTeamId(team.getId());
        member.setUserId(sid);
        member.setUserName(name);
        member.setCollege(college);
        memberMapper.insert(member);

        competitionMapper.incrementJoinedCount(team.getEventId());
        Map<String, Object> res = new HashMap<>();
        res.put("teamName", team.getTeamName());
        return res;
    }

    @Override
    public List<Map<String, Object>> getMyList(String sid) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (sid == null) return result;
        List<CompetitionTeam> leaderTeams = teamMapper.findByLeaderId(sid);
        for (CompetitionTeam t : leaderTeams) result.add(packTeamInfo(t));
        List<Long> joinedTeamIds = memberMapper.findTeamIdsByUserId(sid);
        for (Long teamId : joinedTeamIds) {
            CompetitionTeam t = teamMapper.findById(teamId);
            if (t != null) result.add(packTeamInfo(t));
        }
        result.sort((a, b) -> Long.compare((Long)b.get("id"), (Long)a.get("id")));
        return result;
    }

    @Override
    public void uploadWork(Long recordId, MultipartFile file) {
        CompetitionTeam team = teamMapper.findById(recordId);
        if (team == null) throw new RuntimeException("记录不存在");

        Competition comp = competitionMapper.findById(team.getEventId());
        if ("offline".equals(comp.getFormat())) throw new RuntimeException("线下赛无需提交");

        long now = System.currentTimeMillis();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long start = sdf.parse(comp.getCompStartTime()).getTime();
            long end = sdf.parse(comp.getCompEndTime()).getTime();
            if (now < start) throw new RuntimeException("提交通道尚未开启");
            if (now > end) throw new RuntimeException("提交通道已关闭");
        } catch (Exception e) {}

        if (file.isEmpty()) throw new RuntimeException("文件为空");
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();
            String originalName = file.getOriginalFilename();
            String suffix = originalName.substring(originalName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + suffix;
            File dest = new File(uploadPath + newFileName);
            file.transferTo(dest);
            String fileUrl = "http://localhost:8080/files/" + newFileName;
            String submitTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            team.setFileName(originalName);
            team.setFileUrl(fileUrl);
            team.setSubmitTime(submitTime);
            teamMapper.updateWork(team);
        } catch (IOException e) {
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }

    private void validateTime(Competition c) {
        if ("external".equals(c.getSourceType())) return;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if(c.getRegStartTime() != null && c.getRegEndTime() != null) {
                long regStart = sdf.parse(c.getRegStartTime()).getTime();
                long regEnd = sdf.parse(c.getRegEndTime()).getTime();
                if (regStart >= regEnd) throw new RuntimeException("报名开始时间必须早于截止时间");
            }
        } catch (ParseException e) {}
    }

    private Map<String, Object> packTeamInfo(CompetitionTeam team) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", team.getId());
        map.put("eventId", team.getEventId());
        map.put("teamName", team.getTeamName());
        map.put("leader", team.getLeaderName());
        map.put("sid", team.getLeaderId());
        map.put("teamCode", team.getTeamCode());
        map.put("fileUrl", team.getFileUrl());
        map.put("fileName", team.getFileName());
        map.put("result", team.getAwardLevel());
        map.put("submitTime", team.getSubmitTime());

        List<CompetitionMember> members = competitionMapper.findMembersByTeamId(team.getId());
        List<Map<String, String>> memberList = new ArrayList<>();
        for(CompetitionMember m : members) {
            Map<String, String> mm = new HashMap<>();
            mm.put("name", m.getUserName());
            mm.put("sid", m.getUserId());
            mm.put("college", m.getCollege());
            memberList.add(mm);
        }
        map.put("members", memberList);
        Competition comp = competitionMapper.findById(team.getEventId());
        map.put("_event", comp);
        map.put("isTeam", comp != null && "team".equals(comp.getMode()));
        return map;
    }
}