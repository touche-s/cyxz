package com.cyxz.circle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyxz.circle.entity.CircleSectionPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 圈子板块 Mapper，对应 circle_section 表，维护圈子启用的板块及其排序
 */
public interface CircleSectionMapper extends BaseMapper<CircleSectionPO> {

    /**
     * 查询圈子已启用的板块，JOIN 模板表获取排序信息
     * <p>先按圈子配置的 sort_order，再按模板的 sort_order 排序，
     * 保证同圈内板块展示顺序可控
     * @param circleId 圈子 ID
     * @return 已启用板块列表（按排序规则升序）
     */
    @Select("SELECT cs.* FROM circle_section cs " +
            "JOIN section_template st ON cs.template_id = st.id " +
            "WHERE cs.circle_id = #{circleId} AND cs.status = 1 " +
            "ORDER BY cs.sort_order ASC, st.sort_order ASC")
    List<CircleSectionPO> selectEnabledByCircleId(@Param("circleId") Long circleId);

    /**
     * 查询圈子的默认板块
     * <p>取 is_default=1 且状态启用的第一条。LIMIT 1 防止多默认板块的边缘情况
     * @param circleId 圈子 ID
     * @return 默认板块，不存在时返回 null
     */
    @Select("SELECT cs.* FROM circle_section cs " +
            "JOIN section_template st ON cs.template_id = st.id " +
            "WHERE cs.circle_id = #{circleId} AND cs.status = 1 AND cs.is_default = 1 " +
            "LIMIT 1")
    CircleSectionPO selectDefaultByCircleId(@Param("circleId") Long circleId);
}
