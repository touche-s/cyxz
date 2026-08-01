package com.cyxz.circle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.circle.dto.SectionConfigRequest;
import com.cyxz.circle.entity.CircleSectionPO;
import com.cyxz.circle.entity.SectionTemplatePO;
import com.cyxz.circle.mapper.CircleSectionMapper;
import com.cyxz.circle.mapper.SectionTemplateMapper;
import com.cyxz.circle.service.CircleSectionService;
import com.cyxz.circle.vo.CircleSectionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 圈子板块服务实现
 * <p>核心逻辑：
 * <ul>
 *   <li>板块模板（section_template）由管理员统一管理，保证全站命名一致</li>
 *   <li>圈子通过 circle_section 表按需启用模板中的板块</li>
 *   <li>每个圈子必须有一个默认板块（is_default=1），前端发布时预填</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleSectionServiceImpl implements CircleSectionService {

    private final CircleSectionMapper circleSectionMapper;
    private final SectionTemplateMapper sectionTemplateMapper;

    /**
     * 查询圈子已启用的板块列表（status=1），含模板名称
     * <p>前端发布页和圈子详情页的板块选择器均调用此接口
     */
    @Override
    public List<CircleSectionVO> listByCircleId(Long circleId) {
        List<CircleSectionPO> sections = circleSectionMapper.selectEnabledByCircleId(circleId);
        if (sections.isEmpty()) {
            return List.of();
        }
        // 批量查模板名称，避免 N+1
        List<Long> templateIds = sections.stream()
                .map(CircleSectionPO::getTemplateId)
                .collect(Collectors.toList());
        Map<Long, SectionTemplatePO> templateMap = sectionTemplateMapper.selectBatchIds(templateIds)
                .stream()
                .collect(Collectors.toMap(SectionTemplatePO::getId, t -> t));

        return sections.stream().map(section -> {
            CircleSectionVO vo = new CircleSectionVO();
            vo.setId(section.getId());
            vo.setCircleId(section.getCircleId());
            vo.setTemplateId(section.getTemplateId());
            vo.setIsDefault(section.getIsDefault());
            vo.setSortOrder(section.getSortOrder());
            vo.setStatus(section.getStatus());

            SectionTemplatePO template = templateMap.get(section.getTemplateId());
            if (template != null) {
                vo.setName(template.getName());
                vo.setApplicableType(template.getApplicableType());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 管理员配置圈子的板块（全量替换策略）
     * <p>先删后插，保证最终状态与传入的 configs 完全一致
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void configureSections(Long circleId, List<SectionConfigRequest> configs) {
        List<Long> templateIds = configs.stream()
                .map(SectionConfigRequest::getTemplateId)
                .collect(Collectors.toList());
        List<SectionTemplatePO> templates = sectionTemplateMapper.selectBatchIds(templateIds);
        if (templates.size() != templateIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部分板块模板不存在");
        }

        // 全量删除圈子现有的板块配置
        LambdaQueryWrapper<CircleSectionPO> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(CircleSectionPO::getCircleId, circleId);
        circleSectionMapper.delete(deleteWrapper);

        // 全部重新插入
        List<CircleSectionPO> newSections = new ArrayList<>();
        for (SectionConfigRequest config : configs) {
            CircleSectionPO section = new CircleSectionPO();
            section.setCircleId(circleId);
            section.setTemplateId(config.getTemplateId());
            section.setIsDefault(config.getIsDefault() != null ? config.getIsDefault() : 0);
            section.setSortOrder(config.getSortOrder() != null ? config.getSortOrder() : 0);
            section.setStatus(config.getStatus() != null ? config.getStatus() : CommonStatus.ACTIVE);
            newSections.add(section);
        }
        for (CircleSectionPO section : newSections) {
            circleSectionMapper.insert(section);
        }
    }

    /**
     * 获取圈子的默认板块
     * <p>如果没有已启用的默认板块，则取第一个 ALL 类型模板作为兜底
     */
    @Override
    public CircleSectionVO getDefaultSection(Long circleId) {
        CircleSectionPO section = circleSectionMapper.selectDefaultByCircleId(circleId);
        if (section == null) {
            section = fallbackDefault(circleId);
        }
        SectionTemplatePO template = sectionTemplateMapper.selectById(section.getTemplateId());

        CircleSectionVO vo = new CircleSectionVO();
        vo.setId(section.getId());
        vo.setCircleId(section.getCircleId());
        vo.setTemplateId(section.getTemplateId());
        vo.setIsDefault(section.getIsDefault());
        vo.setSortOrder(section.getSortOrder());
        vo.setStatus(section.getStatus());
        if (template != null) {
            vo.setName(template.getName());
            vo.setApplicableType(template.getApplicableType());
        }
        return vo;
    }

    /**
     * 校验板块是否属于指定圈子且已启用
     * <p>供 post 模块在发布/更新帖子时通过 Feign 调用来校验 sectionId 合法性
     */
    @Override
    public boolean validateSection(Long sectionId, Long circleId) {
        if (sectionId == null || circleId == null) {
            return false;
        }
        LambdaQueryWrapper<CircleSectionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleSectionPO::getId, sectionId)
                .eq(CircleSectionPO::getCircleId, circleId)
                .eq(CircleSectionPO::getStatus, CommonStatus.ACTIVE);
        return circleSectionMapper.selectCount(wrapper) > 0;
    }

    /**
     * 创建圈子时自动初始化默认板块
     * <p>取 applicable_type=ALL 的模板作为初始板块，首个设为默认
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultSections(Long circleId) {
        // 已有配置则不重复初始化
        LambdaQueryWrapper<CircleSectionPO> exists = new LambdaQueryWrapper<>();
        exists.eq(CircleSectionPO::getCircleId, circleId);
        if (circleSectionMapper.selectCount(exists) > 0) {
            return;
        }

        // 取所有 ALL 类型的模板
        LambdaQueryWrapper<SectionTemplatePO> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(SectionTemplatePO::getApplicableType, "ALL")
                .orderByAsc(SectionTemplatePO::getSortOrder);
        List<SectionTemplatePO> allTemplates = sectionTemplateMapper.selectList(templateWrapper);

        List<CircleSectionPO> sections = new ArrayList<>();
        for (int i = 0; i < allTemplates.size(); i++) {
            SectionTemplatePO template = allTemplates.get(i);
            CircleSectionPO section = new CircleSectionPO();
            section.setCircleId(circleId);
            section.setTemplateId(template.getId());
            section.setIsDefault(i == 0 ? 1 : 0);
            section.setSortOrder(i);
            section.setStatus(CommonStatus.ACTIVE);
            sections.add(section);
        }
        for (CircleSectionPO section : sections) {
            circleSectionMapper.insert(section);
        }
    }

    /**
     * 批量查询板块名称（内部 Feign 接口）
     * <p>post 模块展示帖子列表时通过 Feign 批量获取 sectionId → sectionName 映射
     */
    @Override
    public Map<Long, String> batchGetSectionNames(Set<Long> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CircleSectionPO> sections = circleSectionMapper.selectBatchIds(sectionIds);
        if (sections.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> templateIds = sections.stream()
                .map(CircleSectionPO::getTemplateId)
                .collect(Collectors.toSet());
        Map<Long, String> templateNameMap = sectionTemplateMapper.selectBatchIds(templateIds)
                .stream()
                .collect(Collectors.toMap(SectionTemplatePO::getId, SectionTemplatePO::getName));
        return sections.stream()
                .collect(Collectors.toMap(
                        CircleSectionPO::getId,
                        s -> templateNameMap.getOrDefault(s.getTemplateId(), "")
                ));
    }

    /**
     * 兜底默认板块：没有任何配置时，取第一个 ALL 类型模板作为默认
     */
    private CircleSectionPO fallbackDefault(Long circleId) {
        LambdaQueryWrapper<SectionTemplatePO> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(SectionTemplatePO::getApplicableType, "ALL")
                .orderByAsc(SectionTemplatePO::getSortOrder)
                .last("LIMIT 1");
        SectionTemplatePO template = sectionTemplateMapper.selectOne(templateWrapper);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "板块模板为空，请先初始化板块模板");
        }

        CircleSectionPO section = new CircleSectionPO();
        section.setCircleId(circleId);
        section.setTemplateId(template.getId());
        section.setIsDefault(1);
        section.setSortOrder(0);
        section.setStatus(CommonStatus.ACTIVE);
        circleSectionMapper.insert(section);
        return section;
    }
}
