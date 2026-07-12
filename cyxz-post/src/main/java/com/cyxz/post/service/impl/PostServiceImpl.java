package com.cyxz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyxz.common.base.BusinessException;
import com.cyxz.common.base.ErrorCode;
import com.cyxz.common.base.PageResult;
import com.cyxz.common.base.Result;
import com.cyxz.user.feign.UserFeignClient;
import com.cyxz.post.dto.CreatePostRequest;
import com.cyxz.post.dto.UpdatePostRequest;
import com.cyxz.post.entity.CategoryPO;
import com.cyxz.post.entity.PostPO;
import com.cyxz.post.mapper.CategoryMapper;
import com.cyxz.post.mapper.PostMapper;
import com.cyxz.post.service.PostService;
import com.cyxz.post.vo.PostVO;
import com.cyxz.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final UserFeignClient userFeignClient;

    /**
     * 创建帖子
     * <p>将前端传入的标题、正文、图片、标签等信息持久化到 post 表。
     * 图片和标签列表以逗号分隔的字符串形式存储。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建帖子请求
     * @return 新创建的帖子 ID
     */
    @Override
    public Long createPost(Long userId, CreatePostRequest request) {
        PostPO po = new PostPO();
        po.setUserId(userId);
        po.setCategoryId(request.getCategoryId());
        po.setTitle(request.getTitle());
        po.setContent(request.getContent());
        po.setCover(request.getCover());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            po.setImages(String.join(",", request.getImages()));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            po.setTags(String.join(",", request.getTags()));
        }
        po.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        po.setLikes(0);
        po.setComments(0);
        po.setViews(0);
        po.setCollections(0);
        postMapper.insert(po);
        log.info("创建帖子成功: postId={}, userId={}", po.getId(), userId);
        return po.getId();
    }

    /**
     * 更新帖子
     * <p>仅更新非 null 字段，不做全量覆盖。
     * 校验帖子归属权，非作者本人无权修改。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新帖子请求（须包含帖子 ID）
     */
    @Override
    public void updatePost(Long userId, UpdatePostRequest request) {
        PostPO po = postMapper.selectById(request.getId());
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (request.getCategoryId() != null) {
            po.setCategoryId(request.getCategoryId());
        }
        if (StringUtils.hasText(request.getTitle())) {
            po.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            po.setContent(request.getContent());
        }
        if (request.getCover() != null) {
            po.setCover(request.getCover());
        }
        if (request.getImages() != null) {
            po.setImages(String.join(",", request.getImages()));
        }
        if (request.getTags() != null) {
            po.setTags(String.join(",", request.getTags()));
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
        postMapper.updateById(po);
        log.info("更新帖子成功: postId={}, userId={}", po.getId(), userId);
    }

    /**
     * 删除帖子（软删除）
     * <p>仅将帖子状态改为 2（已删除），不物理删除数据，可在回收站恢复。
     */
    @Override
    public void deletePost(Long userId, Long postId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!po.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        po.setStatus(2);
        postMapper.updateById(po);
        log.info("软删除帖子成功: postId={}, userId={}", postId, userId);
    }

    /**
     * 根据 ID 查询帖子详情
     * <p>已发布帖子所有人可查看，草稿和已删除帖子仅作者本人可查看。
     *
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID（可为 null）
     * @return 帖子视图对象（含作者信息、分类名称）
     */
    @Override
    public PostVO getById(Long postId, Long currentUserId) {
        PostPO po = postMapper.selectById(postId);
        if (po == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        // 已删除：仅作者本人可查看
        if (po.getStatus() == 2 && !po.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        // 草稿：仅作者本人可查看
        if (po.getStatus() == 0 && !po.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        Map<Long, UserProfileVO> userMap = batchGetUsers(List.of(po));
        Map<Long, CategoryPO> categoryMap = batchGetCategories(List.of(po));
        return convertToVO(po, userMap, categoryMap);
    }

    /**
     * 分页查询帖子列表（仅已发布）
     * <p>按创建时间倒序排列，可按分类筛选。
     *
     * @param categoryId 分类 ID（可为 null，null 时查全部分类）
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @return 帖子视图列表
     */
    @Override
    public PageResult<PostVO> listPosts(Long categoryId, int page, int size) {
        Page<PostPO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(PostPO::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        Map<Long, UserProfileVO> userMap = batchGetUsers(result.getRecords());
        Map<Long, CategoryPO> categoryMap = batchGetCategories(result.getRecords());

        List<PostVO> vos = result.getRecords().stream()
                .map(po -> convertToVO(po, userMap, categoryMap))
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 查询用户的帖子列表
     * <p>包含草稿、已发布和已删除，按创建时间倒序。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 帖子视图列表
     */
    @Override
    public PageResult<PostVO> listByUserId(Long userId, int page, int size) {
        Page<PostPO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PostPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostPO::getUserId, userId);
        wrapper.orderByDesc(PostPO::getCreateTime);
        Page<PostPO> result = postMapper.selectPage(pageParam, wrapper);

        Map<Long, UserProfileVO> userMap = batchGetUsers(result.getRecords());
        Map<Long, CategoryPO> categoryMap = batchGetCategories(result.getRecords());

        List<PostVO> vos = result.getRecords().stream()
                .map(po -> convertToVO(po, userMap, categoryMap))
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    /**
     * 将帖子实体转换为视图对象
     * <p>作者信息和分类名称均从预查 map 中获取，避免 N+1 查询。
     *
     * @param po          帖子实体
     * @param userMap     预查的用户信息映射
     * @param categoryMap 预查的分类信息映射
     * @return 帖子视图对象
     */
    private PostVO convertToVO(PostPO po, Map<Long, UserProfileVO> userMap,
                                Map<Long, CategoryPO> categoryMap) {
        PostVO vo = new PostVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setCategoryId(po.getCategoryId());
        vo.setTitle(po.getTitle());
        vo.setContent(po.getContent());
        vo.setCover(po.getCover());
        if (StringUtils.hasText(po.getImages())) {
            vo.setImages(List.of(po.getImages().split(",")));
        } else {
            vo.setImages(Collections.emptyList());
        }
        if (StringUtils.hasText(po.getTags())) {
            vo.setTags(List.of(po.getTags().split(",")));
        } else {
            vo.setTags(Collections.emptyList());
        }
        vo.setStatus(po.getStatus());
        vo.setLikes(po.getLikes());
        vo.setComments(po.getComments());
        vo.setViews(po.getViews());
        vo.setCollections(po.getCollections());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());

        UserProfileVO author = userMap.get(po.getUserId());
        if (author != null) {
            vo.setAuthorName(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }

        if (po.getCategoryId() != null) {
            CategoryPO category = categoryMap.get(po.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        return vo;
    }

    /**
     * 批量查询用户信息
     */
    private Map<Long, UserProfileVO> batchGetUsers(List<PostPO> posts) {
        Set<Long> userIds = posts.stream().map(PostPO::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Result<Map<Long, UserProfileVO>> result = userFeignClient.batchGetByIds(new ArrayList<>(userIds));
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("批量查询用户信息失败", e);
        }
        return Collections.emptyMap();
    }

    /**
     * 批量查询分类信息
     */
    private Map<Long, CategoryPO> batchGetCategories(List<PostPO> posts) {
        Set<Long> categoryIds = posts.stream()
                .map(PostPO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CategoryPO> categories = categoryMapper.selectBatchIds(categoryIds);
        return categories.stream()
                .collect(Collectors.toMap(CategoryPO::getId, Function.identity()));
    }
}
