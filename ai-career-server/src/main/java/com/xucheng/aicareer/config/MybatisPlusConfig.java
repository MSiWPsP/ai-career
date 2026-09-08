package com.xucheng.aicareer.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.xucheng.aicareer.mapper")
public class MybatisPlusConfig {
}
