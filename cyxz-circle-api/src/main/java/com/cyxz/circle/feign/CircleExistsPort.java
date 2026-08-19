package com.cyxz.circle.feign;

import com.cyxz.common.base.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 圈子存在性校验端口
 * <p>抽象「圈子是否真实存在」的读取入口，屏蔽「本地 Mapper 直查」与「Feign 调用 circle 服务」两种实现：
 * <ul>
 *   <li>circle 服务：提供本地实现（{@code CircleExistsPortImpl}），基于自身 Mapper 直查本库</li>
 *   <li>其他服务：{@code CircleFeignClient} 继承本接口，Feign 调用 {@code /circle/internal/{circleId}/exists}</li>
 * </ul>
 * <p>方法上的 Spring MVC 注解同时作为 Feign 契约（SpringMvcContract 支持接口继承），
 * 返回 {@link Result} 与项目现有 internal 接口契约保持一致。
 */
public interface CircleExistsPort {

    /**
     * 校验圈子是否真实存在（供 {@code @circlePerm} 校验，防传不存在的 ID 绕过）
     *
     * @param circleId 圈子 ID
     * @return true=存在
     */
    @GetMapping("/circle/internal/{circleId}/exists")
    Result<Boolean> exists(@PathVariable Long circleId);
}
