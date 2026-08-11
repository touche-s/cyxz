package com.cyxz.circle.feign;

import com.cyxz.common.base.Result;
import com.cyxz.circle.feign.fallback.CircleFeignClientFallbackFactory;
import com.cyxz.circle.vo.PublishableResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

@FeignClient(name = "cyxz-circle", fallbackFactory = CircleFeignClientFallbackFactory.class)
public interface CircleFeignClient {

    @GetMapping("/circle/internal/{circleId}/publishable")
    Result<PublishableResult> checkPublishable(@PathVariable Long circleId, @RequestParam Long userId);

    @GetMapping("/circle/internal/batch-names")
    Result<Map<Long, String>> batchGetNames(@RequestParam Set<Long> circleIds);

    @GetMapping("/circle/internal/section/batch-names")
    Result<Map<Long, String>> batchGetSectionNames(@RequestParam Set<Long> sectionIds);
}
