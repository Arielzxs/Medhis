package com.neusoft.his.api.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 序列化配置。
 *
 * <p>MyBatis-Plus 的 ASSIGN_ID 会生成超过 JavaScript 安全整数范围的 Long。
 * 后端按字符串输出 Long，避免前端拿到 id 后精度丢失，导致充值、退费、挂号等接口按错误 id 查询。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }
}
