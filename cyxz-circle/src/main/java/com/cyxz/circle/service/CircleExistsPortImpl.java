package com.cyxz.circle.service;

import com.cyxz.circle.feign.CircleExistsPort;
import com.cyxz.circle.mapper.CircleMapper;
import com.cyxz.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 圈子存在性校验端口本地实现（circle 服务专用）
 * <p>circle 服务自己是圈子数据 owner，直接查本库即可，无需经 Feign 自环调用。
 * 其他服务由 {@code CircleFeignClient}（Feign 继承 {@link CircleExistsPort}）提供等价契约。
 */
@Service
@RequiredArgsConstructor
public class CircleExistsPortImpl implements CircleExistsPort {

    private final CircleMapper circleMapper;

    @Override
    public Result<Boolean> exists(Long circleId) {
        return Result.success(circleMapper.selectById(circleId) != null);
    }
}
