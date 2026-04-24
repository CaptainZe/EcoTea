package com.appsinnova.admin.business.common.utils;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextUtil implements ApplicationContextAware {

    @Getter
    private static ApplicationContext context = null;

    public ApplicationContextUtil() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    public static Object getBean(Class<?> classType) {
        if (context == null) {
            return null;
        }
        return context.getBean(classType);
    }
}
