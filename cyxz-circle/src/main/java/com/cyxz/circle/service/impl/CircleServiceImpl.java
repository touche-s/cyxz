package com.cyxz.circle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.audit.api.constant.AuditConstants;
import com.cyxz.audit.api.event.AuditEvent;
import com.cyxz.auth.feign.AuthFeignClient;
import com.cyxz.auth.feign.dto.CircleRoleRequest;
import com.cyxz.auth.feign.vo.CircleMemberVO;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.common.constant.AnalyticsConstants;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.constant.CommonStatus;
import com.cyxz.common.constant.PageConstants;
import com.cyxz.common.event.AnalyticsEvent;
import com.cyxz.common.utils.TransactionUtils;
import com.cyxz.circle.constant.CircleRoleConstants;
import com.cyxz.circle.entity.CircleMemberPO;
import com.cyxz.circle.entity.CirclePO;
import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.circle.mapper.CircleMemberMapper;
import com.cyxz.circle.service.CircleSectionService;
import com.cyxz.circle.service.CircleService;
import com.cyxz.circle.vo.CircleVO;
import com.cyxz.circle.vo.MemberVO;
import com.cyxz.circle.vo.PublishableResult;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 圈子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleServiceImpl implements CircleService {

    /** 圈子状态：启用 */
    private static final int STATUS_ENABLED = 1;

    /** 圈子状态：禁用 */
    private static final int STATUS_DISABLED = 0;

    private final CircleMapper circleMapper;
    private final CircleMemberMapper circleMemberMapper;
    private final CircleSectionService circleSectionService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final AuthFeignClient authFeignClient;
    private final UserFeignClient userFeignClient;

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
     * 加入圈子，幂等处理成员关系并维护 member_count，同时分配 CIRCLE_MEMBER 角色
     * <p>事务跨 cyxz_circle（circle_member/circle）与 cyxz_auth（sys_user_role）两库，
     * 依赖同一 MySQL 实例保证原子性；如未来拆库需改用 MQ 最终一致性。
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
        // 分配圈子成员角色（幂等），经 auth 服务写入 sys_user_role
        assignCircleRole(userId, CircleRoleConstants.CIRCLE_MEMBER_ROLE_ID, circleId);
        invalidateCirclePermissionCache(userId, circleId);
        // 发布统计事件与新加入成员数，放到事务提交后执行（避免从CircleJoinApplicationService调用时重复计数）
        TransactionUtils.afterCommit(() -> {
            try {
                AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .metric(AnalyticsConstants.METRIC_NEW_JOIN)
                        .value(1)
                        .statDate(LocalDate.now())
                        .build();
                rabbitTemplate.convertAndSend(AnalyticsConstants.EXCHANGE, AnalyticsConstants.ROUTING_KEY, analyticsEvent);
            } catch (Exception e) {
                log.error("发布统计事件失败: metric={}", AnalyticsConstants.METRIC_NEW_JOIN, e);
            }
        });
    }

    /**
     * 退出圈子，软删成员关系并递减 member_count，同时撤销 CIRCLE_MEMBER 角色
     * <p>事务跨 cyxz_circle（circle_member/circle）与 cyxz_auth（sys_user_role）两库，
     * 依赖同一 MySQL 实例保证原子性；如未来拆库需改用 MQ 最终一致性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveCircle(Long userId, Long circleId) {
        int rows = circleMemberMapper.deactivateMember(circleId, userId);
        if (rows > 0) {
            circleMapper.updateMemberCount(circleId, -1);
            log.info("退出圈子: userId={}, circleId={}", userId, circleId);
        }
        // 撤销圈子成员角色（不影响圈主/管理员角色），经 auth 服务写入 sys_user_role
        removeCircleRole(userId, CircleRoleConstants.CIRCLE_MEMBER_ROLE_ID, circleId);
        invalidateCirclePermissionCache(userId, circleId);
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
     * 查询用户管理的圈子（圈主或圈子管理员），用于圈子管理后台选择器
     */
    @Override
    public List<CircleVO> listManagedCircles(Long userId) {
        // 角色归属 auth（sys_user_role），先查管理的圈子 ID 再批量查圈子
        Result<List<Long>> result = authFeignClient.selectManagedCircleIds(userId);
        if (result == null || !result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
            return Collections.emptyList();
        }
        List<CirclePO> circles = circleMapper.selectBatchIds(result.getData());
        return circles.stream()
                .map(c -> convertToVO(c, Collections.emptySet()))
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
     * 创建圈子并初始化默认板块，同时将创建者设为圈主（写 owner_id + 分配 CIRCLE_OWNER 角色）
     * <p>事务跨 cyxz_circle（circle/circle_section）与 cyxz_auth（sys_user_role）两库，
     * 依赖同一 MySQL 实例保证原子性；如未来拆库需改用 MQ 最终一致性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CircleVO createCircle(String name, String intro, String avatar, String cover, Long ownerId) {
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
        po.setOwnerId(ownerId);
        circleMapper.insert(po);
        circleSectionService.initDefaultSections(po.getId());

        // 分配圈主角色给创建者（幂等），权限校验以 sys_user_role 为准；经 auth 服务写入
        if (ownerId != null) {
            assignCircleRole(ownerId, CircleRoleConstants.CIRCLE_OWNER_ROLE_ID, po.getId());
        }
        // 发布审计事件与统计事件，放到事务提交后执行
        final Long circleId = po.getId();
        TransactionUtils.afterCommit(() -> {
            try {
                AuditEvent auditEvent = AuditEvent.builder()
                        .operatorId(ownerId)
                        .operatorName(null)
                        .action(AuditConstants.ACTION_CIRCLE_APPROVE)
                        .targetType("CIRCLE")
                        .targetId(circleId)
                        .detail(null)
                        .ip(null)
                        .createTime(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(AuditConstants.EXCHANGE, AuditConstants.ROUTING_KEY, auditEvent);
            } catch (Exception e) {
                log.error("发布审计事件失败: action={}, targetId={}", AuditConstants.ACTION_CIRCLE_APPROVE, circleId, e);
            }
            try {
                AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .metric(AnalyticsConstants.METRIC_NEW_CIRCLE)
                        .value(1)
                        .statDate(LocalDate.now())
                        .build();
                rabbitTemplate.convertAndSend(AnalyticsConstants.EXCHANGE, AnalyticsConstants.ROUTING_KEY, analyticsEvent);
            } catch (Exception e) {
                log.error("发布统计事件失败: metric={}", AnalyticsConstants.METRIC_NEW_CIRCLE, e);
            }
        });
        log.info("创建圈子并指定圈主: circleId={}, ownerId={}", po.getId(), ownerId);
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
        UpdateWrapper<CircleMemberPO> memberWrapper = new UpdateWrapper<>();
        memberWrapper.eq("circle_id", circleId)
                .eq("status", CommonStatus.ACTIVE)
                .set("status", CommonStatus.DELETED);
        circleMemberMapper.update(null, memberWrapper);

        log.info("删除圈子并级联清理: circleId={}, 成员关系已软删", circleId);
    }

    /**
     * 更新圈子状态（启用/禁用）
     */
    @Override
    public void updateStatus(Long circleId, Integer status) {
        // 圈子状态：1=启用 0=禁用（与 CommonStatus 数值一致但语义不同，独立常量更直观）
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 必须为 0（禁用）或 1（启用）");
        }
        CirclePO po = circleMapper.selectById(circleId);
        if (po == null) {
            throw new BusinessException(ErrorCode.CIRCLE_NOT_FOUND);
        }
        po.setStatus(status);
        circleMapper.updateById(po);
        log.info("更新圈子状态: circleId={}, status={}", circleId, status);
    }

    /**
     * 管理员查询全量圈子列表（含禁用状态），用于平台管理后台
     */
    @Override
    public List<CircleVO> listAllForAdmin() {
        LambdaQueryWrapper<CirclePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CirclePO::getSortOrder);
        List<CirclePO> circles = circleMapper.selectList(wrapper);
        return circles.stream()
                .map(c -> convertToVO(c, Collections.emptySet()))
                .collect(Collectors.toList());
    }

    /**
     * 查询圈子成员列表（含角色信息），按圈主→管理员→成员排序
     * <p>角色信息来自 auth（sys_user_role + sys_role），昵称/头像来自 user（user_profile），
     * 经 Feign 分别查询后合并（避免跨库关联）。
     */
    @Override
    public List<MemberVO> listMembers(Long circleId) {
        Result<List<CircleMemberVO>> roleResult = authFeignClient.listCircleMembers(circleId);
        if (roleResult == null || !roleResult.isSuccess() || roleResult.getData() == null) {
            return Collections.emptyList();
        }
        List<CircleMemberVO> roles = roleResult.getData();
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量补昵称/头像（user 服务）
        List<Long> userIds = roles.stream().map(CircleMemberVO::getUserId).collect(Collectors.toList());
        Result<Map<Long, UserProfileVO>> profileResult = userFeignClient.batchGetUserProfiles(userIds);
        Map<Long, UserProfileVO> profiles = profileResult != null && profileResult.isSuccess()
                && profileResult.getData() != null ? profileResult.getData() : Collections.emptyMap();

        List<MemberVO> members = new ArrayList<>();
        for (CircleMemberVO role : roles) {
            MemberVO vo = new MemberVO();
            vo.setUserId(role.getUserId());
            vo.setUsername(role.getUsername());
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleLabel(role.getRoleLabel());
            vo.setJoinTime(role.getJoinTime());
            UserProfileVO profile = profiles.get(role.getUserId());
            vo.setNickname(profile != null ? profile.getNickname() : null);
            vo.setAvatar(profile != null ? profile.getAvatar() : null);
            members.add(vo);
        }
        return members;
    }

    /**
     * 任命圈子管理员，仅圈主可操作，目标用户必须是圈子成员
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appointAdmin(Long circleId, Long userId) {
        // 校验目标用户是圈子成员（角色归属 auth）
        List<Long> roles = getUserCircleRoleIds(userId, circleId);
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_CIRCLE_MEMBER);
        }
        // 已拥有管理员或圈主角色的无需重复分配
        if (roles.contains(CircleRoleConstants.CIRCLE_ADMIN_ROLE_ID) ||
            roles.contains(CircleRoleConstants.CIRCLE_OWNER_ROLE_ID)) {
            return;
        }
        assignCircleRole(userId, CircleRoleConstants.CIRCLE_ADMIN_ROLE_ID, circleId);
        invalidateCirclePermissionCache(userId, circleId);
        log.info("任命圈子管理员: circleId={}, userId={}", circleId, userId);
    }

    /**
     * 撤销圈子管理员，降级为普通成员
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAdmin(Long circleId, Long userId) {
        List<Long> roles = getUserCircleRoleIds(userId, circleId);
        if (!roles.contains(CircleRoleConstants.CIRCLE_ADMIN_ROLE_ID)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户不是圈子管理员");
        }
        removeCircleRole(userId, CircleRoleConstants.CIRCLE_ADMIN_ROLE_ID, circleId);
        invalidateCirclePermissionCache(userId, circleId);
        log.info("撤销圈子管理员: circleId={}, userId={}", circleId, userId);
    }

    /**
     * 移除圈子成员，撤销该用户在该圈子中的所有角色，并递减成员数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(Long circleId, Long userId) {
        List<Long> roles = getUserCircleRoleIds(userId, circleId);
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_CIRCLE_MEMBER);
        }
        // 撤销该用户在该圈子中的所有角色（CIRCLE_OWNER/CIRCLE_ADMIN/CIRCLE_MEMBER），经 auth 服务写入
        for (Long roleId : roles) {
            removeCircleRole(userId, roleId, circleId);
        }
        // 更新成员关系表
        int rows = circleMemberMapper.deactivateMember(circleId, userId);
        if (rows > 0) {
            circleMapper.updateMemberCount(circleId, -1);
        }
        invalidateCirclePermissionCache(userId, circleId);
        log.info("移除圈子成员: circleId={}, userId={}, 撤销角色={}", circleId, userId, roles);
    }

    /**
     * 失效用户在指定圈子的权限缓存（Cache-Aside 旁路删除）
     * <p>角色分配/撤销后调用，Redis 异常不阻塞业务事务，等待 TTL 自然过期。
     * <p>缓存删除放到事务提交后执行，避免 T1 删缓存→T2 读旧值回写→T1 提交导致的缓存-DB 不一致。
     */
    private void invalidateCirclePermissionCache(Long userId, Long circleId) {
        TransactionUtils.afterCommit(() -> {
            try {
                stringRedisTemplate.delete(CacheKeyConstants.getAuthCircleKey(userId, circleId));
            } catch (Exception e) {
                log.warn("圈子权限缓存失效失败，等待 TTL 自然过期: userId={}, circleId={}", userId, circleId, e);
            }
        });
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

    /**
     * 经 auth 服务分配圈子角色，失败抛出业务异常触发本地事务回滚
     */
    private void assignCircleRole(Long userId, long roleId, Long circleId) {
        CircleRoleRequest request = new CircleRoleRequest();
        request.setUserId(userId);
        request.setRoleId(roleId);
        request.setCircleId(circleId);
        Result<Void> result = authFeignClient.assignCircleRole(request);
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(ErrorCode.FAIL, "圈子角色分配失败，请稍后重试");
        }
    }

    /**
     * 经 auth 服务撤销圈子角色，失败抛出业务异常触发本地事务回滚
     */
    private void removeCircleRole(Long userId, long roleId, Long circleId) {
        CircleRoleRequest request = new CircleRoleRequest();
        request.setUserId(userId);
        request.setRoleId(roleId);
        request.setCircleId(circleId);
        Result<Void> result = authFeignClient.removeCircleRole(request);
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(ErrorCode.FAIL, "圈子角色撤销失败，请稍后重试");
        }
    }

    /**
     * 查询用户在圈子中的角色 ID 列表（经 auth 服务）
     */
    private List<Long> getUserCircleRoleIds(Long userId, Long circleId) {
        Result<List<Long>> result = authFeignClient.selectUserRoleIdsInCircle(userId, circleId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return Collections.emptyList();
        }
        return result.getData();
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
        vo.setStatus(po.getStatus());
        vo.setJoined(joinedIds.contains(po.getId()));
        return vo;
    }
}
