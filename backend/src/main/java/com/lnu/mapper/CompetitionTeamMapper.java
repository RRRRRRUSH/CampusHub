package com.lnu.mapper;

import com.lnu.entity.CompetitionTeam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CompetitionTeamMapper {

    @Insert("INSERT INTO competition_team(event_id, team_name, leader_id, leader_name, team_code, apply_time) " +
            "VALUES(#{eventId}, #{teamName}, #{leaderId}, #{leaderName}, #{teamCode}, #{applyTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CompetitionTeam team);

    @Select("SELECT COUNT(*) FROM competition_team WHERE event_id = #{eventId} AND leader_id = #{leaderId}")
    int countByLeader(Long eventId, String leaderId);

    @Select("SELECT * FROM competition_team WHERE team_code = #{code} LIMIT 1")
    CompetitionTeam findByCode(String code);

    @Select("SELECT * FROM competition_team WHERE id = #{id}")
    CompetitionTeam findById(Long id);

    @Select("SELECT * FROM competition_team WHERE leader_id = #{leaderId}")
    List<CompetitionTeam> findByLeaderId(String leaderId);

    // 🟢 修复1：确保更新作品的字段正确 (file_url, file_name)
    @Update("UPDATE competition_team SET file_url=#{fileUrl}, file_name=#{fileName}, submit_time=#{submitTime} WHERE id=#{id}")
    void updateWork(CompetitionTeam team);

    // 🟢 修复2：新增更新成绩的方法
    @Update("UPDATE competition_team SET award_level = #{awardLevel} WHERE id = #{id}")
    void updateAward(Long id, String awardLevel);
}