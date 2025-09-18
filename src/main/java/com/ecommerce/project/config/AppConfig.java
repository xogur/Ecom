package com.ecommerce.project.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    // application.yml 의 spring.redis.* 값을 주입
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.username:default}")
    private String username;

    @Value("${spring.redis.password:}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration();
        cfg.setHostName(host);
        cfg.setPort(port);
        // Redis 6+ ACL: 기본 사용자 이름은 보통 "default"
        cfg.setUsername(username);
        if (password != null && !password.isBlank()) {
            cfg.setPassword(password);
        }
        return new LettuceConnectionFactory(cfg);
    }

    // 문자열 용도로 가장 간단하고 편한 템플릿
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    // 필요시 Object 등으로 쓸 수 있는 템플릿 (여기선 String 직렬화 지정)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);

        StringRedisSerializer s = new StringRedisSerializer();
        t.setKeySerializer(s);
        t.setValueSerializer(s);
        t.setHashKeySerializer(s);
        t.setHashValueSerializer(s);

        t.afterPropertiesSet();
        return t;
    }
}
