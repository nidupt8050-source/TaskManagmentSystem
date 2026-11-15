package com.smarttask.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns(
                        "/dashboard",
                        "/tasks", "/tasks/**",
                        "/aims", "/aims/**",
                        "/goals", "/goals/**",
                        "/objectives", "/objectives/**",
                        "/reports", "/reports/**"
                )
                .excludePathPatterns("/css/**", "/js/**", "/images/**");
    }
}
