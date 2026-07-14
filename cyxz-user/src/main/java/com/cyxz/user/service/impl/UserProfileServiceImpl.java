package com.cyxz.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.user.dto.UpdateProfileRequest;
import com.cyxz.user.entity.UserProfilePO;
import com.cyxz.user.mapper.UserProfileMapper;
import com.cyxz.user.service.UserProfileService;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户资料服务实现
 * <p>管理 user_profile 表的查询、更新与初始化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper profileMapper;

    /**
     * 根据用户 ID 查询资料
     * <p>查不到则抛出 USER_NOT_FOUND 异常。
     *
     * @param userId 用户 ID
     * @return 用户资料视图
     */
    @Override
    public UserProfileVO getByUserId(Long userId) {
        UserProfilePO po = profileMapper.selectById(userId);
        if (po == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(po, vo);
        if (po.getBirthday() != null) {
            vo.setBirthday(po.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
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
    public Map<Long, UserProfileVO> batchGetByUserIds(List<Long> userIds) {
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
            UserProfileVO vo = new UserProfileVO();
            BeanUtils.copyProperties(po, vo);
            if (po.getBirthday() != null) {
                vo.setBirthday(po.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
            result.put(po.getUserId(), vo);
        }
        return result;
    }

    /**
     * 修改用户资料
     * <p>仅更新传入的非 null 字段，不做全量覆盖。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求（字段可为 null）
     */
    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        UserProfilePO po = profileMapper.selectById(userId);
        if (po == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
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

    /**
     * 创建默认资料
     * <p>新用户注册时调用，默认昵称为注册用户名。
     *
     * @param userId   用户 ID
     * @param username 注册用户名
     */
    @Override
    public void initDefaultProfile(Long userId, String username) {
        // 幂等：如果资料已存在则跳过
        UserProfilePO exist = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfilePO>().eq(UserProfilePO::getUserId, userId)
        );
        if (exist != null) {
            log.debug("用户资料已存在，跳过初始化: userId={}", userId);
            return;
        }
        UserProfilePO po = new UserProfilePO();
        po.setUserId(userId);
        po.setNickname(username);
        po.setAvatar("");
        po.setGender(0);
        po.setBio("");
        profileMapper.insert(po);
        log.info("创建默认用户资料: userId={}, username={}", userId, username);
    }

    @Override
    public UserProfileVO getOrInitMyProfile(Long userId) {
        UserProfilePO po = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfilePO>().eq(UserProfilePO::getUserId, userId)
        );
        if (po == null) {
            initDefaultProfile(userId, "用户" + userId);
            po = profileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfilePO>().eq(UserProfilePO::getUserId, userId)
            );
        }
        if (po == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(po, vo);
        if (po.getBirthday() != null) {
            vo.setBirthday(po.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return vo;
    }
}
