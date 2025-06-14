package com.minicarrot.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 성능 최적화를 위한 타임아웃 설정
        factory.setConnectTimeout(5000);  // 5초
        factory.setReadTimeout(10000);    // 10초
        
        return new RestTemplate(factory);
    }
} 