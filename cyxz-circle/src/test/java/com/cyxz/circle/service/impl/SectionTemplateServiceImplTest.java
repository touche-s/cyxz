package com.cyxz.circle.service.impl;

import com.cyxz.circle.dto.SectionTemplateRequest;
import com.cyxz.circle.entity.SectionTemplatePO;
import com.cyxz.circle.mapper.SectionTemplateMapper;
import com.cyxz.circle.vo.SectionTemplateVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SectionTemplateServiceImpl 单元测试
 * <p>覆盖模板的列表查询、创建、更新、删除等场景。
 * <p>注：listAll 内部使用 LambdaQueryWrapper，纯单测环境会触发 MybatisPlus lambda cache
 * 未初始化异常，相关用例以 try-catch 包裹执行。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SectionTemplateServiceImpl 板块模板服务")
class SectionTemplateServiceImplTest {

    @Mock
    private SectionTemplateMapper sectionTemplateMapper;

    @InjectMocks
    private SectionTemplateServiceImpl sectionTemplateService;

    private SectionTemplatePO buildTemplate(Long id, String name, String applicableType, int sortOrder) {
        SectionTemplatePO po = new SectionTemplatePO();
        po.setId(id);
        po.setName(name);
        po.setApplicableType(applicableType);
        po.setSortOrder(sortOrder);
        return po;
    }

    // ==================== listAll ====================

    @Nested
    @DisplayName("listAll — 查询全部模板")
    class ListAll {

        @Test
        @DisplayName("按 sortOrder 排序返回列表")
        void shouldOrderBySortOrder() {
            when(sectionTemplateMapper.selectList(any()))
                    .thenReturn(List.of(
                            buildTemplate(1L, "板块A", "ALL", 0),
                            buildTemplate(2L, "板块B", "ALL", 1)));

            List<SectionTemplateVO> result = sectionTemplateService.listAll();

            assertEquals(2, result.size());
            assertEquals("板块A", result.get(0).getName());
            assertEquals("板块B", result.get(1).getName());
        }
    }

    // ==================== create ====================

    @Nested
    @DisplayName("create — 创建模板")
    class Create {

        @Test
        @DisplayName("正常创建：未指定类型默认 ALL，排序默认 0")
        void shouldCreateWithDefaults() {
            SectionTemplateRequest dto = new SectionTemplateRequest();
            dto.setName("新板块");

            SectionTemplateVO vo = sectionTemplateService.create(dto);

            assertEquals("新板块", vo.getName());
            assertEquals("ALL", vo.getApplicableType());
            assertEquals(0, vo.getSortOrder());
            verify(sectionTemplateMapper).insert(argThat(po ->
                    "新板块".equals(po.getName())
                            && "ALL".equals(po.getApplicableType())
                            && po.getSortOrder() == 0));
        }
    }

    // ==================== update ====================

    @Nested
    @DisplayName("update — 更新模板")
    class Update {

        @Test
        @DisplayName("模板不存在抛 NOT_FOUND")
        void shouldThrowWhenTemplateNotFound() {
            when(sectionTemplateMapper.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> sectionTemplateService.update(1L, new SectionTemplateRequest()));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
            verify(sectionTemplateMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("正常更新")
        void shouldUpdateTemplate() {
            SectionTemplatePO existing = buildTemplate(1L, "旧名称", "ALL", 0);
            SectionTemplateRequest dto = new SectionTemplateRequest();
            dto.setName("新名称");
            dto.setApplicableType("NORMAL");
            dto.setSortOrder(5);
            when(sectionTemplateMapper.selectById(1L)).thenReturn(existing);

            SectionTemplateVO vo = sectionTemplateService.update(1L, dto);

            assertEquals("新名称", vo.getName());
            assertEquals("NORMAL", vo.getApplicableType());
            assertEquals(5, vo.getSortOrder());
            verify(sectionTemplateMapper).updateById(existing);
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("delete — 删除模板")
    class Delete {

        @Test
        @DisplayName("模板不存在抛 NOT_FOUND")
        void shouldThrowWhenTemplateNotFound() {
            when(sectionTemplateMapper.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> sectionTemplateService.delete(1L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
            verify(sectionTemplateMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("正常删除")
        void shouldDeleteTemplate() {
            when(sectionTemplateMapper.selectById(1L))
                    .thenReturn(buildTemplate(1L, "板块", "ALL", 0));

            sectionTemplateService.delete(1L);

            verify(sectionTemplateMapper).deleteById(1L);
        }
    }
}
