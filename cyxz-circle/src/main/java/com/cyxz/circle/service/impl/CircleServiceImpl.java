package com.cyxz.circle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.circle.entity.CircleMemberPO;
import com.cyxz.circle.entity.CirclePO;
import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.circle.mapper.CircleMemberMapper;
import com.cyxz.circle.service.CircleSectionService;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.PublishableResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 圈子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleServiceImpl implements CircleService {

    private final CircleMapper circleMapper;
    private final CircleMemberMapper circleMemberMapper;
    private final CircleSectionService circleSectionService;

    /**
     * 查询全量启用圈子列表并回填当前用户加入状态
     */
    @Override
    public List<CircleVO> listAll(Long currentUserId) {
        LambdaQueryWrapper<CirclePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePO::getStatus, CommonStatus.ACTIVE);
        wrapper.orderByAsc(CirclePO::getSortOrder);
        List<CirclePO> circles = circleMapper.selectList(wrapper);
        return toVOList(circles, currentUserId);
    }

    /**
     * 根据圈子 ID 查询详情，校验圈子存在且启用后回填用户加入状态
     */
    @Override
    public CircleVO getById(Long circleId, Long currentUserId) {
        CirclePO po = circleMapper.selectById(circleId);
        if (po == null || po.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CIRCLE_NOT_FOUND);
        }
        return toVO(po, currentUserId);
    }

    /**
     * 分页查询热门圈子，按成员数降序并回填用户加入状态
     */
    @Override
    public PageResult<CircleVO> listHot(int page, int size, Long currentUserId) {
        Page<CirclePO> pageParam = PageConstants.pageOf(page, size);
        LambdaQueryWrapper<CirclePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePO::getStatus, CommonStatus.ACTIVE);
        wrapper.orderByDesc(CirclePO::getMemberCount);
        Page<CirclePO> result = circleMapper.selectPage(pageParam, wrapper);
        List<CircleVO> vos = toVOList(result.getRecords(), currentUserId);
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 加入圈子，幂等处理成员关系并维护 member_count
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinCircle(Long userId, Long circleId) {
        CirclePO circle = circleMapper.selectById(circleId);
        if (circle == null || circle.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CIRCLE_NOT_FOUND);
        }
        int rows = circleMemberMapper.upsertMember(circleId, userId);
        if (rows > 0) {
            circleMapper.updateMemberCount(circleId, 1);
            log.info("{}圈子: userId={}, circleId={}", rows == 1 ? "加入" : "恢复", userId, circleId);
        }
    }

    /**
     * 退出圈子，软删成员关系并递减 member_count
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveCircle(Long userId, Long circleId) {
        int rows = circleMemberMapper.deactivateMember(circleId, userId);
        if (rows > 0) {
            circleMapper.updateMemberCount(circleId, -1);
            log.info("退出圈子: userId={}, circleId={}", userId, circleId);
        }
    }

    /**
     * 查询当前用户已加入的启用圈子，VO 中 joined 固定为 true
     */
    @Override
    public List<CircleVO> listJoined(Long userId) {
        Set<Long> joinedIds = circleMemberMapper.selectJoinedCircleIds(userId);
        if (joinedIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CirclePO> circles = circleMapper.selectBatchIds(joinedIds);
        return circles.stream()
                .filter(c -> c.getStatus() == CommonStatus.ACTIVE)
                .map(c -> convertToVO(c, joinedIds))
                .collect(Collectors.toList());
    }

    /**
     * 校验是否可在指定圈子发布，聚合圈子存在性、启用状态与成员关系
     */
    @Override
    public PublishableResult checkPublishable(Long circleId, Long userId) {
        PublishableResult result = new PublishableResult();
        CirclePO circle = circleMapper.selectById(circleId);
        if (circle == null) {
            return result;
        }
        result.setExists(true);
        result.setEnabled(circle.getStatus() == CommonStatus.ACTIVE);

        LambdaQueryWrapper<CircleMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleMemberPO::getCircleId, circleId)
                .eq(CircleMemberPO::getUserId, userId)
                .eq(CircleMemberPO::getStatus, CommonStatus.ACTIVE);
        result.setJoined(circleMemberMapper.selectCount(wrapper) > 0);
        result.setPublishable(result.isExists() && result.isEnabled() && result.isJoined());
        return result;
    }

    /**
     * 批量查询圈子 ID 到名称的映射
     */
    @Override
    public Map<Long, String> batchGetNames(Set<Long> circleIds) {
        if (circleIds == null || circleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return circleMapper.selectBatchIds(circleIds).stream()
                .collect(Collectors.toMap(CirclePO::getId, CirclePO::getName));
    }

    /**
     * 局部更新圈子资料，仅更新非空字段
     */
    @Override
    public void updateCircle(Long circleId, String name, String intro, String avatar, String cover) {
        CirclePO po = circleMapper.selectById(circleId);
        if (po == null) {
            throw new BusinessException(ErrorCode.CIRCLE_NOT_FOUND);
        }
        if (StringUtils.hasText(name)) po.setName(name);
        if (intro != null) po.setIntro(intro);
        if (StringUtils.hasText(avatar)) po.setAvatar(avatar);
        if (StringUtils.hasText(cover)) po.setCover(cover);
        circleMapper.updateById(po);
    }

    /**
     * 创建圈子并初始化默认板块
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CircleVO createCircle(String name, String intro, String avatar, String cover) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "圈子名称不能为空");
        }
        CirclePO po = new CirclePO();
        po.setName(name);
        po.setSlug(name.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-"));
        po.setIntro(intro != null ? intro : "");
        po.setAvatar(avatar);
        po.setCover(cover);
        po.setStatus(CommonStatus.ACTIVE);
        po.setSortOrder(0);
        po.setPostCount(0);
        po.setMemberCount(0);
        circleMapper.insert(po);
        circleSectionService.initDefaultSections(po.getId());
        return toVO(po, null);
    }

    /**
     * 软删圈子并级联软删其下成员关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCircle(Long circleId) {
        CirclePO po = circleMapper.selectById(circleId);
        if (po == null) {
            throw new BusinessException(ErrorCode.CIRCLE_NOT_FOUND);
        }
        po.setStatus(CommonStatus.DELETED);
        circleMapper.updateById(po);

        // 级联软删成员关系，避免残留孤儿数据
        LambdaUpdateWrapper<CircleMemberPO> memberWrapper = new LambdaUpdateWrapper<>();
        memberWrapper.eq(CircleMemberPO::getCircleId, circleId)
                .eq(CircleMemberPO::getStatus, CommonStatus.ACTIVE)
                .set(CircleMemberPO::getStatus, CommonStatus.DELETED);
        circleMemberMapper.update(null, memberWrapper);

        log.info("删除圈子并级联清理: circleId={}, 成员关系已软删", circleId);
    }

    private List<CircleVO> toVOList(List<CirclePO> circles, Long currentUserId) {
        if (circles.isEmpty()) return Collections.emptyList();
        Set<Long> joinedIds = currentUserId != null
                ? circleMemberMapper.selectJoinedCircleIds(currentUserId)
                : Collections.emptySet();
        return circles.stream()
                .map(c -> convertToVO(c, joinedIds))
                .collect(Collectors.toList());
    }

    private CircleVO toVO(CirclePO po, Long currentUserId) {
        Set<Long> joinedIds = currentUserId != null
                ? circleMemberMapper.selectJoinedCircleIds(currentUserId)
                : Collections.emptySet();
        return convertToVO(po, joinedIds);
    }

    private CircleVO convertToVO(CirclePO po, Set<Long> joinedIds) {
        CircleVO vo = new CircleVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setSlug(po.getSlug());
        vo.setIntro(po.getIntro());
        vo.setAvatar(po.getAvatar());
        vo.setCover(po.getCover());
        vo.setPostCount(po.getPostCount());
        vo.setMemberCount(po.getMemberCount());
        vo.setJoined(joinedIds.contains(po.getId()));
        return vo;
    }
}
