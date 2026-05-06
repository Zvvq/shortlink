package com.cqie.shortlink_project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqie.shortlink_project.dto.response.LinkClickStatsResponse;
import com.cqie.shortlink_project.entity.LinkDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for t_link.
 */
public interface LinkMapper extends BaseMapper<LinkDO> {

    /**
     * Sum hourly PV records only. Records with hour = -1 are daily rollups.
     */
    @Select({
            "<script>",
            "SELECT full_short_url AS linkKey, COALESCE(SUM(pv), 0) AS clickNum ",
            "FROM t_link_access_stats ",
            "WHERE del_flag = 0 ",
            "AND hour BETWEEN 0 AND 23 ",
            "AND full_short_url IN ",
            "<foreach collection='linkKeys' item='linkKey' open='(' separator=',' close=')'>",
            "#{linkKey}",
            "</foreach> ",
            "GROUP BY full_short_url",
            "</script>"
    })
    List<LinkClickStatsResponse> selectHourPvStatsByLinkKeys(@Param("linkKeys") List<String> linkKeys);
}
