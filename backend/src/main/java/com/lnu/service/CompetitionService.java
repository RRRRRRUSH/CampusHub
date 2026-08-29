package com.lnu.service;

import com.lnu.entity.Competition;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface CompetitionService {

    // 管理端查询
    List<Competition> getList(String sourceType, Long userId);

    // 学生端查询
    List<Competition> getStudentList(String sourceType, Long userId);

    Competition getDetail(Long id);

    // 🟢 修复：保留两个 create 方法 (重载)，解决 @Override 报错
    void create(Competition competition);
    void create(Competition competition, Long userId);

    void update(Competition competition);
    void changeStatus(Long id, String status);
    void delete(Long id);

    // 参赛相关
    Map<String, Object> signup(Map<String, Object> params);
    Map<String, Object> joinByCode(Map<String, Object> params);
    List<Map<String, Object>> getMyList(String sid);

    // 管理相关
    List<Map<String, Object>> getParticipants(Long eventId);
    void submitGrade(Long recordId, String level);
    void uploadWork(Long recordId, MultipartFile file);
}