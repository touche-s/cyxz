package com.cyxz.circle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.circle.dto.SectionTemplateRequest;
import com.cyxz.circle.entity.SectionTemplatePO;
import com.cyxz.circle.mapper.SectionTemplateMapper;
import com.cyxz.circle.service.SectionTemplateService;
import com.cyxz.circle.vo.SectionTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 板块模板服务实现，负责模板的增删改查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectionTemplateServiceImpl implements SectionTemplateService {

    private final SectionTemplateMapper sectionTemplateMapper;

    /**
     * 查询全部模板，按 sort_order 升序
     */
    @Override
    public List<SectionTemplateVO> listAll() {
        LambdaQueryWrapper<SectionTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SectionTemplatePO::getSortOrder);
        List<SectionTemplatePO> templates = sectionTemplateMapper.selectList(wrapper);
        return templates.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 创建板块模板，未指定类型时默认 ALL，未指定排序时默认 0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SectionTemplateVO create(SectionTemplateRequest dto) {
        SectionTemplatePO po = new SectionTemplatePO();
        BeanUtils.copyProperties(dto, po);
        // 未指定类型默认通用模板
        if (!StringUtils.hasText(po.getApplicableType())) {
            po.setApplicableType("ALL");
        }
        // 排序默认 0
        if (po.getSortOrder() == null) {
            po.setSortOrder(0);
        }
        sectionTemplateMapper.insert(po);
        return toVO(po);
    }

    /**
     * 更新板块模板，模板不存在时抛出业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SectionTemplateVO update(Long id, SectionTemplateRequest dto) {
        SectionTemplatePO po = sectionTemplateMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.SECTION_TEMPLATE_NOT_FOUND);
        }
        BeanUtils.copyProperties(dto, po, "id", "createTime");
        sectionTemplateMapper.updateById(po);
        return toVO(po);
    }

    /**
     * 删除板块模板，模板不存在时抛出业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SectionTemplatePO po = sectionTemplateMapper.selectById(id);
        if (po == null) {
            throw new BusinessException(ErrorCode.SECTION_TEMPLATE_NOT_FOUND);
        }
        sectionTemplateMapper.deleteById(id);
    }

    private SectionTemplateVO toVO(SectionTemplatePO po) {
        SectionTemplateVO vo = new SectionTemplateVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }
}
