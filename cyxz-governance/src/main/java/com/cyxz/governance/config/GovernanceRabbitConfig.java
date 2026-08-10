package com.cyxz.governance.config;

import com.cyxz.governance.api.constant.GovernanceConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 治理中心 RabbitMQ 配置（生产者侧）
 * <p>仅声明内容处置事件交换机，确保 governance 启动即可发布事件。
 * <p>主队列 / 死信队列由消费端（post / comment）通过 {@code AbstractDlxRabbitConfig} 声明并绑定，
 * 双方声明同一资源时 RabbitMQ 幂等校验，参数一致即可。
 */
@Configuration
public class GovernanceRabbitConfig {

    @Bean
    public TopicExchange governanceExchange() {
        return new TopicExchange(GovernanceConstants.EXCHANGE, true, false);
    }
}
