package com.contentgenius.user;

import com.contentgenius.user.config.JwtAuthenticationFilter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.contentgenius.user", "com.contentgenius.common"})
@MapperScan("com.contentgenius.user.mapper")
@EnableConfigurationProperties({JwtAuthenticationFilter.class})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
