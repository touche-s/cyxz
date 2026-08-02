package com.cyxz.circle.service;

/**
 * 圈子计数刷库服务接口，从 post 服务拉取帖子数并落库
 */
public interface CircleCountFlushService {

    /**
     * 从 post 服务批量查询各圈子已发布帖子数，覆盖写入 circle.post_count
     *
     * @return 成功处理条数
     */
    int flushPostCounts();
}
