package com.example.socialanalytics.job;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({ JobProperties.class, RetentionProperties.class })
public class AsyncConfig implements AsyncConfigurer {

	public static final String METRICS_EXECUTOR = "metricsTaskExecutor";

	@Bean(name = METRICS_EXECUTOR)
	public ThreadPoolTaskExecutor metricsTaskExecutor(JobProperties properties) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("metrics-");
		executor.setCorePoolSize(properties.corePoolSize());
		executor.setMaxPoolSize(properties.maxPoolSize());
		executor.setQueueCapacity(properties.queueCapacity());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (Throwable ex, Method method, Object... params) -> log.error("Uncaught error in async {}.{}",
				method.getDeclaringClass().getSimpleName(), method.getName(), ex);
	}

}
