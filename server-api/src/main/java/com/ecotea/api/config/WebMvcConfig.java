package com.ecotea.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final EcoTeaApiKeyInterceptor ecoTeaApiKeyInterceptor;
    private final EmployeeAuthInterceptor employeeAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ecoTeaApiKeyInterceptor)
                .addPathPatterns("/wx/mp/menu/**");
        registry.addInterceptor(employeeAuthInterceptor)
                .addPathPatterns("/employee/**")
                .excludePathPatterns("/employee/login", "/employee/wx-login");
    }
}
