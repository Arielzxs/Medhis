package com.neusoft.his.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.neusoft.his")
// 新增这一行，指定 Mapper 接口所在的包路径
@MapperScan("com.neusoft.his.dal.mapper")
public class HisApplication {
    public static void main(String[] args) {
        SpringApplication.run(HisApplication.class, args);
    }
}