package com.cyxz.circle.service.impl;

import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.circle.service.CircleCountFlushService;
import com.cyxz.common.utils.FeignResults;
import com.cyxz.post.feign.PostFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 圈子计数定时校验：从 post 服务批量查询已发布帖子数，覆盖写入 circle.post_count
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleCountFlushServiceImpl implements CircleCountFlushService {

    private final CircleMapper circleMapper;
    private final PostFeignClient postFeignClient;

    /**
     * 批量拉取各圈子已发布帖子数并覆盖写入 post_count，按圈子逐条更新
     */
    @Override
    public int flushPostCounts() {
        // 获取所有圈子 ID
        List<Long> allCircleIds = circleMapper.selectList(null).stream()
                .map(c -> c.getId())
                .collect(Collectors.toList());
        if (allCircleIds.isEmpty()) {
            return 0;
        }

        // 批量查帖子数
        Map<Long, Integer> counts = FeignResults.unwrapOrNull(postFeignClient.batchCountByCircle(Set.copyOf(allCircleIds)));
        if (counts == null) {
            log.warn("批量查询圈子帖子数失败");
            return 0;
        }

        int success = 0;
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            try {
                circleMapper.setPostCount(entry.getKey(), entry.getValue());
                success++;
            } catch (Exception e) {
                log.error("更新圈子帖子数失败: circleId={}, count={}", entry.getKey(), entry.getValue(), e);
            }
        }

        if (success > 0) {
            log.info("圈子帖子数定时校验完成: {} 个圈子", success);
        }
        return success;
    }
}
