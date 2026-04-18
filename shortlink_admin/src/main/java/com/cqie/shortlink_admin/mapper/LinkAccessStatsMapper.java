package com.cqie.shortlink_admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqie.shortlink_admin.entity.LinkAccessStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 短链访问统计 Mapper
 */
public interface LinkAccessStatsMapper extends BaseMapper<LinkAccessStatsDO> {

    /**
     * 按小时统计短链访问量：存在则更新，不存在则插入
     * 唯一索引：full_short_url + gid + date + hour
     *
     * @param stats 统计实体
     * @return 影响行数
     */
    @Insert("INSERT INTO t_link_access_stats " +
            "(gid, full_short_url, date, pv, uv, uip, hour, weekday) " +
            "VALUES " +
            "(#{stats.gid}, #{stats.fullShortUrl}, #{stats.date}, #{stats.pv}, #{stats.uv}, #{stats.uip}, #{stats.hour}, #{stats.weekday}) " +
            "ON DUPLICATE KEY UPDATE " +
            "pv = pv + 1, " +
            "uv = uv + 1, " +
            "uip = uip + 1")
    boolean statsLinkAccess(@Param("stats") LinkAccessStatsDO stats);

    /**
     * 批量插入/更新访问统计
     * 等价于你 XML 里的 batchUpsert
     */
    @Insert({
            "<script>",
            "INSERT INTO t_link_access_stats ",
            "(full_short_url, `date`, `hour`, pv, uv, weekday) ",
            "VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.fullShortUrl}, #{item.date}, #{item.hour}, #{item.pv}, #{item.uv}, #{item.weekday})",
            "</foreach> ",
            "ON DUPLICATE KEY UPDATE ",
            "pv = IF(VALUES(pv) = -1, pv, VALUES(pv)), ",
            "uv = IF(VALUES(uv) = -1, uv, VALUES(uv)), ",
            "weekday = VALUES(weekday)",
            "</script>"
    })
    int batchUpsert(@Param("list") List<LinkAccessStatsDO> statsList);
}
