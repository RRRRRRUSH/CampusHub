package com.lnu.controller;

import com.alibaba.excel.EasyExcel;
import com.lnu.common.Result;
import com.lnu.entity.Activity;
import com.lnu.entity.AuditRecord;
import com.lnu.entity.User;
import com.lnu.mapper.ActivityMapper;
import com.lnu.mapper.AuditMapper;
import com.lnu.mapper.CollegeMapper;
import com.lnu.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/activity")
@CrossOrigin
public class ActivityController {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuditMapper auditMapper;

    @Autowired
    private CollegeMapper collegeMapper;

    // 1. 获取列表
    @GetMapping("/list")
    public Result list(@RequestParam String type, @RequestParam(required = false) Long userId) {
        String userCampus = null;
        if (userId != null) {
            User user = userMapper.findById(userId);
            if (user != null && user.getCollege() != null) {
                userCampus = collegeMapper.findCampusByCollege(user.getCollege());
            }
        }
        List<Activity> list = activityMapper.findByType(type, userCampus);
        return Result.success(list);
    }

    // 2. 获取详情
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Activity activity = activityMapper.findById(id);
        if (activity == null) return Result.error("Activity Not Found");
        return Result.success(activity);
    }

    // 3. 获取参与名单
    @GetMapping("/participants/{id}")
    public Result getParticipants(@PathVariable Long id) {
        List<Map<String, Object>> list = activityMapper.getParticipants(id);
        return Result.success(list);
    }

    // 4. 状态流转
    @PostMapping("/status")
    public Result changeStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String status = (String) params.get("status");
        activityMapper.updateStatus(id, status);
        return Result.success();
    }

    // 5. 创建
    @PostMapping("/create")
    public Result create(@RequestBody Map<String, Object> params) {
        Activity activity = mapToActivity(params);
        activity.setStatus("registering");
        activityMapper.insert(activity);
        return Result.success();
    }

    // 6. 更新
    @PostMapping("/update/{id}")
    public Result update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Activity activity = mapToActivity(params);
        activity.setId(id);
        activityMapper.update(activity);
        return Result.success();
    }

    // 7. 删除
    @PostMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        activityMapper.delete(id);
        return Result.success();
    }

    // 8. 报名
    @PostMapping("/signup")
    public Result signup(@RequestBody Map<String, Object> params) {
        Long activityId = Long.valueOf(params.get("activityId").toString());
        String sid = (String) params.get("sid");
        if(sid == null && params.get("userId") != null) {
            Long uid = Long.valueOf(params.get("userId").toString());
            User u = userMapper.findById(uid);
            if(u != null) sid = u.getUsername();
        }

        User user = userMapper.findByUsername(sid);
        if (user == null) return Result.error("用户不存在");

        Activity act = activityMapper.findById(activityId);
        if (act.getQuota() != -1 && act.getJoined() >= act.getQuota()) {
            return Result.error("名额已满");
        }

        int count = activityMapper.checkSignup(activityId, user.getId());
        if (count > 0) return Result.error("您已报名，请勿重复提交");

        activityMapper.insertSignup(activityId, user.getId(), user.getName(), user.getUsername(), user.getCollege());
        activityMapper.increaseJoined(activityId);

        return Result.success();
    }

    // 9. 单个考勤核验
    @PostMapping("/verify")
    public Result verify(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Boolean isValid = (Boolean) params.get("isValid");
        String status = Boolean.TRUE.equals(isValid) ? "present" : "absent";
        activityMapper.updateAttendanceStatus(id, status);
        return Result.success();
    }

    // 10. 我的活动列表
    @GetMapping("/my-list-by-id")
    public Result myListById(@RequestParam Long userId) {
        List<Map<String, Object>> list = activityMapper.findMySignups(userId);
        return Result.success(list);
    }

    // 11. 取消报名
    @PostMapping("/cancel")
    public Result cancel(@RequestBody Map<String, Object> params) {
        Long activityId = Long.valueOf(params.get("activityId").toString());
        Long userId = Long.valueOf(params.get("userId").toString());

        Activity act = activityMapper.findById(activityId);

        if (!"registering".equals(act.getStatus()) && !"open".equals(act.getStatus())) {
            return Result.error("活动已进入执行阶段，无法取消");
        }

        if (act.getActivityStartTime() != null) {
            long start = act.getActivityStartTime().getTime();
            long now = System.currentTimeMillis();
            long diff = start - now;
            if (diff < 86400000) {
                return Result.error("距离活动开始已不足24小时，无法取消。请联系老师请假。");
            }
        }

        int rows = activityMapper.deleteSignup(activityId, userId);
        if (rows > 0) {
            activityMapper.decreaseJoined(activityId);
            return Result.success();
        }
        return Result.error("取消失败，可能未报名");
    }

    // 12. 导出 Excel
    @GetMapping("/export/participants/{activityId}")
    public void exportParticipants(@PathVariable Long activityId, HttpServletResponse response) throws IOException {
        Activity act = activityMapper.findById(activityId);
        List<Map<String, Object>> list = activityMapper.selectParticipantsForExport(activityId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(act.getTitle() + "-签到表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<List<String>> data = new ArrayList<>();
        data.add(Arrays.asList("姓名", "学号", "学院", "报名时间", "签到签字"));

        for (Map<String, Object> map : list) {
            List<String> row = new ArrayList<>();
            row.add((String) map.get("student_name"));
            row.add((String) map.get("student_id"));
            row.add((String) map.get("college"));
            row.add(map.get("signup_time").toString());
            row.add("");
            data.add(row);
        }

        EasyExcel.write(response.getOutputStream()).sheet("名单").doWrite(data);
    }

    // 13. 批量状态修改
    @PostMapping("/signup/batch/status")
    public Result batchUpdateStatus(@RequestBody Map<String, Object> params) {
        List<Integer> idsInt = (List<Integer>) params.get("signupIds");
        String status = (String) params.get("status");

        List<Long> ids = new ArrayList<>();
        for(Integer i : idsInt) ids.add(Long.valueOf(i));

        if (!ids.isEmpty()) {
            activityMapper.batchUpdateSignupStatus(ids, status);
        }
        return Result.success();
    }

    // 14. 结算工时
    @PostMapping("/settle/{activityId}")
    public Result settleActivity(@PathVariable Long activityId) {
        Activity act = activityMapper.findById(activityId);
        if (act == null) return Result.error("活动不存在");

        List<Map<String, Object>> validList = activityMapper.selectValidSignups(activityId);

        int count = 0;
        for (Map<String, Object> map : validList) {
            Long userId = (Long) map.get("user_id");
            String sid = (String) map.get("student_id");
            String name = (String) map.get("student_name");
            String college = (String) map.get("college");

            String uniqueTitle = "【校内活动】" + act.getTitle();

            AuditRecord record = new AuditRecord();
            record.setUserId(userId);
            record.setStudentId(sid);
            record.setStudentName(name);
            record.setCollege(college);

            record.setType("volunteer");
            record.setSourceType("internal");
            record.setOriginId(activityId);

            record.setTitle(uniqueTitle);
            record.setScore(act.getHours());
            record.setStatus("approved");
            record.setApplyTime(new Date());
            record.setAuditTime(new Date());
            record.setAuditorName("SYSTEM");
            record.setAuditComment("活动结算自动入账");

            auditMapper.insert(record);
            userMapper.addVolunteerScore(userId, act.getHours());
            count++;
        }

        activityMapper.updateStatus(activityId, "finished");
        return Result.success("结算完成，共入账 " + count + " 人");
    }

    // 🟢 15. [新增] 活动状态回退 (后悔药)
    // 从 "已归档" 回退到 "核验中"，并扣回工时，删除审核记录
    @PostMapping("/rollback")
    public Result rollback(@RequestBody Map<String, Object> params) {
        Long activityId = Long.valueOf(params.get("id").toString());
        Activity act = activityMapper.findById(activityId);

        if (!"finished".equals(act.getStatus())) {
            return Result.error("只有已归档的活动才能回退");
        }

        // 1. 资产回滚：扣回分数
        List<Map<String, Object>> validList = activityMapper.selectValidSignups(activityId);
        for (Map<String, Object> map : validList) {
            Long userId = (Long) map.get("user_id");
            // 调用之前在 UserMapper 加好的扣分方法
            userMapper.deductVolunteerScore(userId, act.getHours());
        }

        // 2. 数据清洗：删除之前自动生成的 AuditRecord
        // ⚠️ 这里的 deleteByOriginId 方法需要在下一步加到 AuditMapper 中
        auditMapper.deleteByOriginId(activityId);

        // 3. 状态回滚：退回到 'judging' (核验/评审阶段)
        activityMapper.updateStatus(activityId, "judging");

        return Result.success("回退成功，已扣回工时并重置状态");
    }

    // 辅助方法
    private Activity mapToActivity(Map<String, Object> params) {
        Activity a = new Activity();
        a.setTitle((String) params.get("title"));
        a.setSourceType((String) params.get("sourceType"));
        a.setFormat((String) params.get("format"));
        a.setLocation((String) params.get("location"));
        a.setNeedPhoto((Boolean) params.get("needPhoto"));
        a.setRegStartTime(parseDate(params.get("regStartTime")));
        a.setRegEndTime(parseDate(params.get("regEndTime")));
        a.setActivityStartTime(parseDate(params.get("activityStartTime")));
        a.setActivityEndTime(parseDate(params.get("activityEndTime")));
        a.setLimitCampus(listToString(params.get("limitCampus")));
        a.setLimitCollege(listToString(params.get("limitCollege")));
        a.setLimitGrade(listToString(params.get("limitGrade")));
        a.setDescription((String) params.get("desc"));
        if (params.get("hours") != null) a.setHours(Double.valueOf(params.get("hours").toString()));
        if (params.get("quota") != null) a.setQuota(Integer.valueOf(params.get("quota").toString()));
        return a;
    }

    private Date parseDate(Object obj) {
        if (obj == null || "".equals(obj)) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.parse(obj.toString());
        } catch (ParseException e) { return null; }
    }

    private String listToString(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) return null;
            return String.join(",", list.stream().map(Object::toString).toArray(String[]::new));
        }
        return null;
    }
}
