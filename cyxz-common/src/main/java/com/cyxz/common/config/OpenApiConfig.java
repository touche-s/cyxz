package com.cyxz.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置
 * <p>各微服务通过 component-scan 自动加载，依据 {@code spring.application.name}
 * 生成服务级 API 文档元信息。{@link ConditionalOnClass} 确保未引入 knife4j 的模块（如 gateway）跳过加载。
 */
@Configuration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfig {

    @Value("${spring.application.name:cyxz-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " 接口文档")
                        .description("次元小站 — " + applicationName + " 服务接口")
                        .version("1.0.0")
                        .contact(new Contact().name("cyxz"))
                        .license(new License().name("MIT")));
    }
}
