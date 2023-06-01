package com.lya79.mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@EnableWebMvc
public class CustomWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private CustomHandlerInterceptor messageInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(messageInterceptor).addPathPatterns("/**"); // 攔截全部請求
    }
}