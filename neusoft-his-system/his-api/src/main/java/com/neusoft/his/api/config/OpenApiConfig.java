package com.neusoft.his.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 文档配置。
 *
 * <p>系统使用 JWT Bearer Token 做接口鉴权，这里把统一的安全方案注册到
 * OpenAPI 文档中，前端或测试人员可在 Swagger UI 右上角 Authorize 处填入 token。</p>
 */
@Configuration
public class OpenApiConfig {
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI hisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("云医院 HIS 系统 API")
                        .version("1.0.0")
                        .description("覆盖患者建档、门诊挂号、医生工作站、药房、财务、统计分析与权限管理的后端接口文档。")
                        .contact(new Contact().name("Neusoft HIS Team")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}
