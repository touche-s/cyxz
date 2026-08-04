package com.cyxz.circle.service.impl;

import com.cyxz.circle.dto.SectionConfigRequest;
import com.cyxz.circle.entity.CircleSectionPO;
import com.cyxz.circle.entity.SectionTemplatePO;
import com.cyxz.circle.mapper.CircleSectionMapper;
import com.cyxz.circle.mapper.SectionTemplateMapper;
import com.cyxz.circle.vo.CircleSectionVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.constant.CommonStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CircleSectionServiceImpl 单元测试
 * <p>覆盖圈子板块的查询、配置、默认板块获取、校验、初始化与批量取名称等场景。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CircleSectionServiceImpl 圈子板块服务")
class CircleSectionServiceImplTest {

    @Mock
    private CircleSectionMapper circleSectionMapper;

    @Mock
    private SectionTemplateMapper sectionTemplateMapper;

    @InjectMocks
    private CircleSectionServiceImpl circleSectionService;

    private static final Long CIRCLE_ID = 7L;

    private CircleSectionPO buildSection(Long id, Long templateId, int isDefault, int sortOrder) {
        CircleSectionPO po = new CircleSectionPO();
        po.setId(id);
        po.setCircleId(CIRCLE_ID);
        po.setTemplateId(templateId);
        po.setIsDefault(isDefault);
        po.setSortOrder(sortOrder);
        po.setStatus(CommonStatus.ACTIVE);
        return po;
    }

    private SectionTemplatePO buildTemplate(Long id, String name) {
        SectionTemplatePO po = new SectionTemplatePO();
        po.setId(id);
        po.setName(name);
        po.setApplicableType("ALL");
        po.setSortOrder(0);
        return po;
    }

    private SectionConfigRequest buildConfig(Long templateId) {
        SectionConfigRequest req = new SectionConfigRequest();
        req.setTemplateId(templateId);
        req.setIsDefault(0);
        req.setSortOrder(0);
        req.setStatus(CommonStatus.ACTIVE);
        return req;
    }

    // ==================== listByCircleId ====================

    @Nested
    @DisplayName("listByCircleId — 查询圈子已启用板块")
    class ListByCircleId {

        @Test
        @DisplayName("空列表返回 List.of()")
        void shouldReturnEmptyListWhenNoSections() {
            when(circleSectionMapper.selectEnabledByCircleId(CIRCLE_ID))
                    .thenReturn(List.of());

            List<CircleSectionVO> result = circleSectionService.listByCircleId(CIRCLE_ID);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(sectionTemplateMapper, never()).selectBatchIds(anyList());
        }

        @Test
        @DisplayName("正常返回含模板名称")
        void shouldReturnSectionsWithTemplateName() {
            CircleSectionPO section = buildSection(1L, 10L, 1, 0);
            SectionTemplatePO template = buildTemplate(10L, "图楼分享");
            when(circleSectionMapper.selectEnabledByCircleId(CIRCLE_ID))
                    .thenReturn(List.of(section));
            when(sectionTemplateMapper.selectBatchIds(List.of(10L)))
                    .thenReturn(List.of(template));

            List<CircleSectionVO> result = circleSectionService.listByCircleId(CIRCLE_ID);

            assertEquals(1, result.size());
            CircleSectionVO vo = result.get(0);
            assertEquals(1L, vo.getId());
            assertEquals(10L, vo.getTemplateId());
            assertEquals("图楼分享", vo.getName());
            assertEquals("ALL", vo.getApplicableType());
            assertEquals(1, vo.getIsDefault());
        }
    }

    // ==================== configureSections ====================

    @Nested
    @DisplayName("configureSections — 配置圈子板块")
    class ConfigureSections {

        @Test
        @DisplayName("模板不存在抛 PARAM_ERROR")
        void shouldThrowWhenTemplateNotFound() {
            when(sectionTemplateMapper.selectBatchIds(anyList()))
                    .thenReturn(List.of());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> circleSectionService.configureSections(CIRCLE_ID, List.of(buildConfig(99L))));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            verify(circleSectionMapper, never()).delete(any());
            verify(circleSectionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("正常配置：先删后插")
        void shouldDeleteAndInsertWhenTemplatesExist() {
            when(sectionTemplateMapper.selectBatchIds(anyList()))
                    .thenReturn(List.of(buildTemplate(1L, "板块A")));

            circleSectionService.configureSections(CIRCLE_ID, List.of(buildConfig(1L)));

            verify(circleSectionMapper).delete(any());
            verify(circleSectionMapper).insert(argThat(po ->
                    CIRCLE_ID.equals(po.getCircleId())
                            && po.getTemplateId() == 1L
                            && po.getStatus() == CommonStatus.ACTIVE));
        }
    }

    // ==================== getDefaultSection ====================

    @Nested
    @DisplayName("getDefaultSection — 获取默认板块")
    class GetDefaultSection {

        @Test
        @DisplayName("有默认板块直接返回")
        void shouldReturnDefaultSectionWhenExists() {
            CircleSectionPO section = buildSection(1L, 10L, 1, 0);
            SectionTemplatePO template = buildTemplate(10L, "默认板块");
            when(circleSectionMapper.selectDefaultByCircleId(CIRCLE_ID))
                    .thenReturn(section);
            when(sectionTemplateMapper.selectById(10L))
                    .thenReturn(template);

            CircleSectionVO vo = circleSectionService.getDefaultSection(CIRCLE_ID);

            assertEquals(1L, vo.getId());
            assertEquals(10L, vo.getTemplateId());
            assertEquals("默认板块", vo.getName());
            assertEquals(1, vo.getIsDefault());
        }

        @Test
        @DisplayName("无默认板块走 fallbackDefault")
        void shouldFallbackWhenNoDefaultSection() {
            SectionTemplatePO template = buildTemplate(10L, "兜底板块");
            when(circleSectionMapper.selectDefaultByCircleId(CIRCLE_ID))
                    .thenReturn(null);
            when(sectionTemplateMapper.selectOne(any()))
                    .thenReturn(template);
            when(sectionTemplateMapper.selectById(10L))
                    .thenReturn(template);

            CircleSectionVO vo = circleSectionService.getDefaultSection(CIRCLE_ID);

            assertEquals(10L, vo.getTemplateId());
            assertEquals("兜底板块", vo.getName());
            assertEquals(1, vo.getIsDefault());
            verify(circleSectionMapper).insert(argThat(po ->
                    CIRCLE_ID.equals(po.getCircleId())
                            && po.getTemplateId() == 10L
                            && po.getIsDefault() == 1));
        }
    }

    // ==================== validateSection ====================

    @Nested
    @DisplayName("validateSection — 校验板块归属")
    class ValidateSection {

        @Test
        @DisplayName("sectionId 为 null 返回 false")
        void shouldReturnFalseWhenSectionIdNull() {
            boolean result = circleSectionService.validateSection(null, CIRCLE_ID);

            assertFalse(result);
            verify(circleSectionMapper, never()).selectCount(any());
        }

        @Test
        @DisplayName("circleId 为 null 返回 false")
        void shouldReturnFalseWhenCircleIdNull() {
            boolean result = circleSectionService.validateSection(1L, null);

            assertFalse(result);
            verify(circleSectionMapper, never()).selectCount(any());
        }

        @Test
        @DisplayName("校验通过：板块存在且已启用")
        void shouldReturnTrueWhenSectionValid() {
            when(circleSectionMapper.selectCount(any())).thenReturn(1L);

            boolean result = circleSectionService.validateSection(1L, CIRCLE_ID);

            assertTrue(result);
        }

        @Test
        @DisplayName("校验不通过：板块不存在")
        void shouldReturnFalseWhenSectionInvalid() {
            when(circleSectionMapper.selectCount(any())).thenReturn(0L);

            boolean result = circleSectionService.validateSection(1L, CIRCLE_ID);

            assertFalse(result);
        }
    }

    // ==================== initDefaultSections ====================

    @Nested
    @DisplayName("initDefaultSections — 初始化默认板块")
    class InitDefaultSections {

        @Test
        @DisplayName("已有配置时跳过初始化")
        void shouldSkipWhenAlreadyConfigured() {
            when(circleSectionMapper.selectCount(any())).thenReturn(1L);

            circleSectionService.initDefaultSections(CIRCLE_ID);

            verify(sectionTemplateMapper, never()).selectList(any());
            verify(circleSectionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("无配置时初始化 ALL 类型模板")
        void shouldInitAllTemplatesWhenNoConfig() {
            when(circleSectionMapper.selectCount(any())).thenReturn(0L);
            when(sectionTemplateMapper.selectList(any()))
                    .thenReturn(List.of(buildTemplate(10L, "板块A"), buildTemplate(20L, "板块B")));

            circleSectionService.initDefaultSections(CIRCLE_ID);

            verify(circleSectionMapper, times(2)).insert(any());
            verify(circleSectionMapper).insert(argThat(po ->
                    po.getTemplateId() == 10L && po.getIsDefault() == 1));
            verify(circleSectionMapper).insert(argThat(po ->
                    po.getTemplateId() == 20L && po.getIsDefault() == 0));
        }
    }

    // ==================== batchGetSectionNames ====================

    @Nested
    @DisplayName("batchGetSectionNames — 批量查询板块名称")
    class BatchGetSectionNames {

        @Test
        @DisplayName("空入参返回空 Map")
        void shouldReturnEmptyMapWhenIdsEmpty() {
            Map<Long, String> result = circleSectionService.batchGetSectionNames(Set.of());

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(circleSectionMapper, never()).selectBatchIds(anyList());
        }

        @Test
        @DisplayName("正常批量查询返回 sectionId → name 映射")
        void shouldReturnNameMapForSections() {
            CircleSectionPO s1 = buildSection(1L, 10L, 1, 0);
            CircleSectionPO s2 = buildSection(2L, 20L, 0, 1);
            SectionTemplatePO t1 = buildTemplate(10L, "图楼分享");
            SectionTemplatePO t2 = buildTemplate(20L, "作品讨论");
            when(circleSectionMapper.selectBatchIds(Set.of(1L, 2L)))
                    .thenReturn(List.of(s1, s2));
            when(sectionTemplateMapper.selectBatchIds(Set.of(10L, 20L)))
                    .thenReturn(List.of(t1, t2));

            Map<Long, String> result = circleSectionService.batchGetSectionNames(Set.of(1L, 2L));

            assertEquals(2, result.size());
            assertEquals("图楼分享", result.get(1L));
            assertEquals("作品讨论", result.get(2L));
        }
    }
}
