package com.lnu.mapper;

import com.lnu.entity.CompetitionMember;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CompetitionMemberMapper {

    // 1. 插入成员
    @Insert("INSERT INTO competition_member(team_id, user_id, user_name, college) " +
            "VALUES(#{teamId}, #{userId}, #{userName}, #{college})")
    void insert(CompetitionMember member);

    // 2. 查重：检查用户是否作为【队员】参加了某个比赛关联的队伍
    // 这里需要关联查询：找到该用户所在的所有队伍，且这些队伍属于目标 eventId
    @Select("SELECT COUNT(*) FROM competition_member m " +
            "LEFT JOIN competition_team t ON m.team_id = t.id " +
            "WHERE t.event_id = #{eventId} AND m.user_id = #{userId}")
    int countByMember(Long eventId, String userId);

    // 3. 查重：检查用户是否已经在【这个队伍】里
    @Select("SELECT COUNT(*) FROM competition_member WHERE team_id = #{teamId} AND user_id = #{userId}")
    int countInTeam(Long teamId, String userId);

    // 4. 根据成员ID找队伍ID列表
    @Select("SELECT team_id FROM competition_member WHERE user_id = #{userId}")
    List<Long> findTeamIdsByUserId(String userId);

    // 5. 获取某个队伍的所有成员
    @Select("SELECT * FROM competition_member WHERE team_id = #{teamId}")
    List<CompetitionMember> findByTeamId(Long teamId);
}