package com.neusoft.his.api.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.neusoft.his.common.api.PageSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页配置。
 *
 * <p>项目中的库存、账单、患者、处方等查询都使用 MyBatis-Plus 的 selectPage。
 * 必须注册分页插件后，SQL 才会真正追加 LIMIT，避免接口一次返回全表数据。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(PageSupport.MAX_SIZE);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
