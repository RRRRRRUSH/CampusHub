package com.lnu.controller;

import com.lnu.common.Result;
import com.lnu.entity.AuditRecord;
import com.lnu.entity.User;
import com.lnu.mapper.AuditMapper;
import com.lnu.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/audit")
@CrossOrigin
public class AuditController {

    @Autowired
    private AuditMapper auditMapper;

    @Autowired
    private UserMapper userMapper;

    // 1. 管理端：获取审核列表
    @GetMapping("/list")
    public Result list(@RequestParam String type, @RequestParam(required = false) Long userId) {
        List<AuditRecord> list;
        if (userId != null) {
            User user = userMapper.findById(userId);
            if (user != null && "STUDENT".equals(user.getRole()) && Boolean.TRUE.equals(user.getIsCadre())) {
                list = auditMapper.findByTypeAndClass(type, user.getClassName());
            } else {
                list = auditMapper.findByType(type);
            }
        } else {
            list = auditMapper.findByType(type);
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (AuditRecord r : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("studentName", r.getStudentName());
            map.put("studentId", r.getStudentId());
            map.put("college", r.getCollege());
            map.put("major", r.getMajor());
            map.put("className", r.getClassName());
            map.put("title", r.getTitle());
            map.put("score", r.getScore());
            map.put("awardLevel", r.getAwardLevel());
            map.put("desc", r.getDescription());
            map.put("status", r.getStatus());
            map.put("sourceType", r.getSourceType());

            List<String> imgs = new ArrayList<>();
            if (r.getProofUrl() != null && !r.getProofUrl().isEmpty()) {
                imgs.add(r.getProofUrl());
            }
            map.put("proofImgs", imgs);
            resultList.add(map);
        }
        return Result.success(resultList);
    }

    // 2. 学生端：获取我的申请记录
    @GetMapping("/list/personal")
    public Result personalList(@RequestParam Long userId) {
        List<Map<String, Object>> list = auditMapper.findByUserId(userId);
        return Result.success(list);
    }

    // 🟢 3. 学生端：提交申请 (修复 500 错误的健壮版本)
    @PostMapping("/apply")
    public Result apply(@RequestBody Map<String, Object> params) {
        // 1. 安全获取 userId
        Object userIdObj = params.get("userId");
        if (userIdObj == null) return Result.error("参数错误：缺少 userId");

        Long userId;
        try {
            userId = Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException e) {
            return Result.error("参数错误：userId 格式不正确");
        }

        User user = userMapper.findById(userId);
        if (user == null) return Result.error("用户不存在");

        AuditRecord record = new AuditRecord();
        record.setUserId(userId);
        record.setStudentId(user.getUsername());
        record.setStudentName(user.getName());
        record.setClassName(user.getClassName());
        record.setCollege(user.getCollege());
        record.setMajor(user.getMajor());

        // 2. 基础信息
        String type = (String) params.get("type");
        if (type == null) type = "external";
        record.setType(type);

        record.setTitle((String) params.get("title"));
        record.setProofUrl((String) params.get("proofUrl"));
        record.setDescription((String) params.get("description"));

        record.setStatus("pending");
        record.setApplyTime(new Date());
        record.setSourceType("external");
        record.setOriginId(null);

        // 3. 安全算分逻辑
        if ("volunteer".equals(type)) {
            // 🟢 修复点：安全解析 hours，防止空指针
            Object hoursObj = params.get("hours");
            if (hoursObj == null) hoursObj = params.get("score"); // 兼容前端传 score

            if (hoursObj != null && !hoursObj.toString().isEmpty()) {
                try {
                    record.setScore(Double.valueOf(hoursObj.toString()));
                } catch (Exception e) {
                    record.setScore(0.0);
                }
            } else {
                record.setScore(0.0);
            }
            record.setAwardLevel(null);
        } else {
            // 竞赛：自动判分
            Object levelObj = params.get("awardLevel");
            String level = levelObj != null ? levelObj.toString() : "";

            double autoScore = 0.5;
            if (level.contains("国家")) {
                if (level.contains("一等")) autoScore = 8.0;
                else if (level.contains("二等")) autoScore = 7.0;
                else if (level.contains("三等")) autoScore = 6.0;
                else autoScore = 5.0;
            } else if (level.contains("省")) {
                if (level.contains("一等")) autoScore = 5.0;
                else if (level.contains("二等")) autoScore = 4.0;
                else if (level.contains("三等")) autoScore = 3.0;
                else autoScore = 2.0;
            } else if (level.contains("校")) {
                if (level.contains("一等") || level.contains("金")) autoScore = 2.0;
                else autoScore = 1.0;
            }

            record.setScore(autoScore);
            record.setAwardLevel(level);
        }

        try {
            auditMapper.insert(record);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("提交失败，请检查数据库结构是否包含 origin_id 字段");
        }

        return Result.success();
    }

    // 4. 管理端：批量通过
    @PostMapping("/approve")
    public Result approve(@RequestBody Map<String, Object> params) {
        List<Integer> ids = (List<Integer>) params.get("ids");
        if (ids == null) return Result.error("未选择记录");

        for (Integer idInt : ids) {
            Long id = Long.valueOf(idInt);
            AuditRecord record = auditMapper.findById(id);
            if (record == null) continue;

            auditMapper.approve(id);

            Long userId = record.getUserId();
            String type = record.getType();
            Double score = record.getScore() != null ? record.getScore() : 0.0;

            if ("volunteer".equals(type)) {
                userMapper.addVolunteerScore(userId, score);
            } else if ("competition".equals(type)) {
                userMapper.addCompetitionScore(userId, score);
            }
        }
        return Result.success();
    }

    // 5. 管理端：驳回
    @PostMapping("/reject")
    public Result reject(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String reason = (String) params.get("reason");
        auditMapper.reject(id, reason);
        return Result.success();
    }

    // 6. 管理端：重置状态
    @PostMapping("/reset")
    public Result reset(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        AuditRecord record = auditMapper.findById(id);
        if (record == null) return Result.error("记录不存在");

        String oldStatus = record.getStatus();

        if ("approved".equals(oldStatus)) {
            Double score = record.getScore();
            if (score != null && score > 0) {
                Long userId = record.getUserId();
                if ("volunteer".equals(record.getType())) {
                    userMapper.deductVolunteerScore(userId, score);
                } else {
                    userMapper.deductCompetitionScore(userId, score);
                }
            }
        }

        auditMapper.reset(id);
        return Result.success();
    }
}