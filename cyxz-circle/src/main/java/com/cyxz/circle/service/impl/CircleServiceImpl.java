package com.cyxz.circle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Override
    public List<CircleVO> listAll(Long currentUserId) {
        LambdaQueryWrapper<CirclePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CirclePO::getStatus, CommonStatus.ACTIVE);
        wrapper.orderByAsc(CirclePO::getSortOrder);
        List<CirclePO> circles = circleMapper.selectList(wrapper);
        return toVOList(circles, currentUserId);
    }

    @Override
    public CircleVO getById(Long circleId, Long currentUserId) {
        CirclePO po = circleMapper.selectById(circleId);
        if (po == null || po.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "圈子不存在");
        }
        return toVO(po, currentUserId);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinCircle(Long userId, Long circleId) {
        CirclePO circle = circleMapper.selectById(circleId);
        if (circle == null || circle.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "圈子不存在");
        }
        int rows = circleMemberMapper.upsertMember(circleId, userId);
        if (rows > 0) {
            circleMapper.updateMemberCount(circleId, 1);
            log.info("{}圈子: userId={}, circleId={}", rows == 1 ? "加入" : "恢复", userId, circleId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveCircle(Long userId, Long circleId) {
        int rows = circleMemberMapper.deactivateMember(circleId, userId);
        if (rows > 0) {
            circleMapper.updateMemberCount(circleId, -1);
            log.info("退出圈子: userId={}, circleId={}", userId, circleId);
        }
    }

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

    @Override
    public Map<Long, String> batchGetNames(Set<Long> circleIds) {
        if (circleIds == null || circleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return circleMapper.selectBatchIds(circleIds).stream()
                .collect(Collectors.toMap(CirclePO::getId, CirclePO::getName));
    }

    @Override
    public void updateCircle(Long circleId, String name, String intro, String avatar, String cover) {
        CirclePO po = circleMapper.selectById(circleId);
        if (po == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "圈子不存在");
        }
        if (StringUtils.hasText(name)) po.setName(name);
        if (intro != null) po.setIntro(intro);
        if (StringUtils.hasText(avatar)) po.setAvatar(avatar);
        if (StringUtils.hasText(cover)) po.setCover(cover);
        circleMapper.updateById(po);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCircle(Long circleId) {
        CirclePO po = circleMapper.selectById(circleId);
        if (po == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "圈子不存在");
        }
        po.setStatus(CommonStatus.DELETED);
        circleMapper.updateById(po);
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
