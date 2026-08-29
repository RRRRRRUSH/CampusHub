package com.lnu.task;

import com.lnu.entity.Competition;
import com.lnu.mapper.CompetitionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class CompetitionTask {

    @Autowired
    private CompetitionMapper competitionMapper;

    /**
     * 每分钟执行一次，检查赛事状态流转
     * Cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void autoChangeStatus() {
        // 1. 查出所有未归档的活跃赛事
        List<Competition> activeList = competitionMapper.findActiveCompetitions();
        if (activeList == null || activeList.isEmpty()) return;

        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Competition c : activeList) {
            try {
                // 跳过校外赛（通常由外部控制）或时间未设置的
                if ("external".equals(c.getSourceType())) continue;
                if (c.getRegEndTime() == null || c.getCompEndTime() == null) continue;

                long regEnd = sdf.parse(c.getRegEndTime()).getTime();
                long compEnd = sdf.parse(c.getCompEndTime()).getTime();

                // 🟢 逻辑 1: 报名截止 -> 自动进入 [执行/提交阶段]
                // 条件：当前时间 > 报名截止时间  AND  状态是 [报名中]
                if (now > regEnd && ("open".equals(c.getStatus()) || "registering".equals(c.getStatus()))) {
                    System.out.println(">>> 定时任务：赛事 [" + c.getTitle() + "] 报名结束，自动进入执行阶段");
                    competitionMapper.updateStatus(c.getId(), "execution");
                }

                // 🟢 逻辑 2: 竞赛结束 -> 自动进入 [评审阶段]
                // 条件：当前时间 > 竞赛结束时间  AND  状态是 [进行中/提交中]
                if (now > compEnd && ("execution".equals(c.getStatus()) || "submitting".equals(c.getStatus()))) {
                    System.out.println(">>> 定时任务：赛事 [" + c.getTitle() + "] 比赛结束，自动进入评审阶段");
                    competitionMapper.updateStatus(c.getId(), "judging");
                }

            } catch (Exception e) {
                System.err.println("赛事 ID " + c.getId() + " 时间解析错误: " + e.getMessage());
            }
        }
    }
}