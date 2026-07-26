package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.post.entity.CircleMemberPO;
import com.cyxz.post.entity.CirclePO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.CircleMapper;
import com.cyxz.post.mapper.CircleMemberMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.CircleService;
import com.cyxz.post.vo.CircleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CircleServiceImpl implements CircleService {

    private final CircleMapper circleMapper;
    private final CircleMemberMapper circleMemberMapper;
    private final PostMapper postMapper;

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
        LambdaQueryWrapper<CircleMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleMemberPO::getCircleId, circleId)
                .eq(CircleMemberPO::getUserId, userId);
        CircleMemberPO member = circleMemberMapper.selectOne(wrapper);

        if (member == null) {
            member = new CircleMemberPO();
            member.setCircleId(circleId);
            member.setUserId(userId);
            member.setStatus(CommonStatus.ACTIVE);
            circleMemberMapper.insert(member);
        } else if (member.getStatus() != CommonStatus.ACTIVE) {
            member.setStatus(CommonStatus.ACTIVE);
            circleMemberMapper.updateById(member);
        } else {
            return;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveCircle(Long userId, Long circleId) {
        LambdaQueryWrapper<CircleMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CircleMemberPO::getCircleId, circleId)
                .eq(CircleMemberPO::getUserId, userId)
                .eq(CircleMemberPO::getStatus, CommonStatus.ACTIVE);
        CircleMemberPO member = circleMemberMapper.selectOne(wrapper);
        if (member == null) {
            return;
        }
        member.setStatus(CommonStatus.DELETED);
        circleMemberMapper.updateById(member);
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
                .map(c -> toVO(c, userId))
                .collect(Collectors.toList());
    }

    @Override
    public void recountStats() {
        List<CirclePO> circles = circleMapper.selectList(new LambdaQueryWrapper<CirclePO>()
                .eq(CirclePO::getStatus, CommonStatus.ACTIVE));
        if (circles.isEmpty()) return;

        for (CirclePO circle : circles) {
            long cnt = postMapper.selectCount(new LambdaQueryWrapper<PostPO>()
                    .eq(PostPO::getCircleId, circle.getId())
                    .eq(PostPO::getStatus, CommonStatus.ACTIVE));
            circleMapper.setPostCount(circle.getId(), (int) cnt);

            long mbr = circleMemberMapper.selectCount(new LambdaQueryWrapper<CircleMemberPO>()
                    .eq(CircleMemberPO::getCircleId, circle.getId())
                    .eq(CircleMemberPO::getStatus, CommonStatus.ACTIVE));
            circleMapper.setMemberCount(circle.getId(), (int) mbr);
        }
        log.debug("recountCircleStats done: {} circles", circles.size());
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
