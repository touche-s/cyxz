package com.cyxz.circle.service;

import com.cyxz.circle.dto.SectionConfigRequest;
import com.cyxz.circle.vo.CircleSectionVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 圈子板块业务接口
 * <p>板块采用"模板+关联"两表设计：
 * section_template 定义标准化的板块名称（所有圈子共享），
 * circle_section 记录各圈子启用了哪些模板及其个性化配置。
 * <p>圈子的板块列表 = 该圈子的 circle_section JOIN section_template
 */
public interface CircleSectionService {

    /**
     * 获取圈子已启用的板块列表，含模板名称
     * @param circleId 圈子 ID
     * @return 已启用的板块列表，按 sort_order 排序
     */
    List<CircleSectionVO> listByCircleId(Long circleId);

    /**
     * 管理员配置圈子的板块（全量替换策略）
     * <p>先删后插，保证最终状态与传入的 configs 完全一致。
     * 会校验所有 templateId 对应的模板是否存在。
     * @param circleId 圈子 ID
     * @param configs 板块配置列表，一个模板一条配置
     */
    void configureSections(Long circleId, List<SectionConfigRequest> configs);

    /**
     * 获取圈子的默认板块
     * <p>优先取 is_default=1 的板块，若无则取第一个已启用的板块
     * @param circleId 圈子 ID
     * @return 默认板块，无可用板块时返回 null
     */
    CircleSectionVO getDefaultSection(Long circleId);

    /**
     * 校验板块是否属于指定圈子
     * @param sectionId circle_section 主键（不是 templateId）
     * @param circleId 圈子 ID
     * @return true=板块存在且属于该圈子且已启用
     */
    boolean validateSection(Long sectionId, Long circleId);

    /**
     * 创建圈子时初始化默认板块
     * <p>自动分配 DEFAULT 类型的模板作为该圈子的板块
     * @param circleId 新创建的圈子 ID
     */
    void initDefaultSections(Long circleId);

    /**
     * 批量获取板块名称，用于帖子列表等场景的板块名展示
     * @param sectionIds circle_section 主键集合
     * @return sectionId → sectionName 的映射
     */
    Map<Long, String> batchGetSectionNames(Set<Long> sectionIds);
}
