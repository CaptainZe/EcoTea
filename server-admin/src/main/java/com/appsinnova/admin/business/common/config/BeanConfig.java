package com.appsinnova.admin.business.common.config;

import com.appsinnova.admin.business.common.utils.ApplicationContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理器默认线程数是1  需要根据实际情况额外配置
 */
@Configuration
@EnableAsync
public class BeanConfig {

    private static final int CORE_POOL_SIZE = 8;
    private static final int MAX_POOL_SIZE = 32;
    private static final int QUEUE_CAPACITY = 512;
    private static final int KEEP_ALIVE_SECONDS = 60;
    private static final String THREAD_PREFIX = "admin-executor-";
    private static final String THREAD_POOL_BEAN = "admin-pool";

    @Bean(THREAD_POOL_BEAN)
    public Executor apiExecutorPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_PREFIX);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        return executor;
    }

    @Bean
    public ApplicationContextUtil getApplicationContextUtil() {
        return new ApplicationContextUtil();
    }
}
