package com.cyxz.circle.service;

import com.cyxz.circle.dto.SectionTemplateRequest;
import com.cyxz.circle.vo.SectionTemplateVO;

import java.util.List;

/**
 * 板块模板业务接口
 * <p>模板由管理员统一管理，不直接面向普通用户。
 * 模板创建后，各圈子管理员在圈子配置中选择启用哪些模板作为圈内板块。
 */
public interface SectionTemplateService {

    /**
     * 获取所有模板列表，按 sort_order 升序
     */
    List<SectionTemplateVO> listAll();

    /**
     * 创建板块模板
     * @param dto 模板信息，applicableType 不填默认 ALL
     * @return 创建后的模板 VO
     */
    SectionTemplateVO create(SectionTemplateRequest dto);

    /**
     * 更新板块模板
     * @param id 模板 ID
     * @param dto 更新内容
     * @return 更新后的模板 VO
     * @throws BusinessException 模板不存在时
     */
    SectionTemplateVO update(Long id, SectionTemplateRequest dto);

    /**
     * 删除板块模板
     * <p>注意：删除模板不会自动级联删除已关联该模板的 circle_section 记录，
     * 因为模板被删除后圈子配置可能仍引用该模板 ID。
     * @param id 模板 ID
     * @throws BusinessException 模板不存在时
     */
    void delete(Long id);
}
