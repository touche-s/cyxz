package com.cyxz.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.constant.CacheKeyConstants;
import com.cyxz.common.utils.TransactionUtils;
import com.cyxz.user.dto.UpdateProfileRequest;
import com.cyxz.user.entity.UserProfilePO;
import com.cyxz.user.mapper.UserProfileMapper;
import com.cyxz.user.service.FollowService;
import com.cyxz.user.service.UserProfileService;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户资料服务实现
 * <p>管理 user_profile 表的查询与更新。
 * <p>注册时不创建 profile，前端以 username 作为昵称降级展示；
 * 用户首次修改资料时懒加载创建记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper profileMapper;
    private final FollowService followService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 根据用户 ID 查询资料
     * <p>查不到返回 null，调用方（前端）以 username 作为昵称降级展示。
     *
     * @param userId 用户 ID
     * @return 用户资料视图，无资料时返回 null
     */
    @Override
    public UserProfileVO getByUserId(Long userId) {
        UserProfilePO po = profileMapper.selectById(userId);
        if (po == null) {
            return null;
        }
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(po, vo);
        if (po.getBirthday() != null) {
            vo.setBirthday(po.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        vo.setFollowingCount(followService.countFollowing(userId));
        vo.setFollowerCount(followService.countFollowers(userId));
        return vo;
    }

    /**
     * 批量查询用户资料
     * <p>根据用户 ID 列表一次查询，返回 userId → UserProfileVO 映射。
     * 不存在的用户不会出现在结果中。
     *
     * @param userIds 用户 ID 列表
     * @return userId → UserProfileVO 映射
     */
    @Override
    public Map<Long, UserProfileVO> batchGetUserProfiles(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 去重 + 限制最大 200
        Set<Long> distinctIds = userIds.stream().distinct().limit(200).collect(Collectors.toSet());
        List<UserProfilePO> profiles = profileMapper.selectList(
                new LambdaQueryWrapper<UserProfilePO>().in(UserProfilePO::getUserId, distinctIds)
        );
        Map<Long, UserProfileVO> result = new HashMap<>();
        for (UserProfilePO po : profiles) {
            UserProfileVO vo = toVO(po);
            result.put(po.getUserId(), vo);
        }
        return result;
    }

    /**
     * 修改用户资料
     * <p>仅更新传入的非 null 字段，不做全量覆盖。
     * <p>如果用户资料不存在（新用户首次修改），则懒加载创建记录。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求（字段可为 null）
     */
    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        UserProfilePO po = profileMapper.selectById(userId);
        if (po == null) {
            // 新用户首次修改资料，懒加载创建记录
            po = new UserProfilePO();
            po.setUserId(userId);
            po.setNickname(request.getNickname());
            po.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
            po.setGender(request.getGender() != null ? request.getGender() : 0);
            po.setBio(request.getBio() != null ? request.getBio() : "");
            if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
                po.setBirthday(LocalDate.parse(request.getBirthday()));
            }
            profileMapper.insert(po);
            log.info("懒加载创建用户资料: userId={}", userId);
        } else {
            if (request.getNickname() != null) {
                po.setNickname(request.getNickname());
            }
            if (request.getAvatar() != null) {
                po.setAvatar(request.getAvatar());
            }
            if (request.getGender() != null) {
                po.setGender(request.getGender());
            }
            if (request.getBio() != null) {
                po.setBio(request.getBio());
            }
            if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
                po.setBirthday(LocalDate.parse(request.getBirthday()));
            }
            profileMapper.updateById(po);
        }
        // 失效跨服务缓存（post 服务缓存了 user:profile:{userId}），避免改昵称/头像后帖子侧最长 1 小时显示旧资料
        TransactionUtils.afterCommit(() -> {
            try {
                stringRedisTemplate.delete(CacheKeyConstants.getUserProfileKey(userId));
            } catch (Exception e) {
                log.error("删除用户资料缓存失败，资料变更可能延迟生效: userId={}", userId, e);
            }
        });
    }

    /**
     * 将用户资料实体转换为视图对象
     * <p>统一处理 birthday 的 LocalDate → String(yyyy-MM-dd) 格式化。
     *
     * @param po 用户资料实体
     * @return 用户资料视图对象
     */
    private UserProfileVO toVO(UserProfilePO po) {
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(po, vo);
        if (po.getBirthday() != null) {
            vo.setBirthday(po.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return vo;
    }
}
