package com.example.socialanalytics.job;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.example.socialanalytics.entity.SocialPlatform;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(JobProperties.class)
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

	@Bean
	public SocialApiClient facebookApiClient(@Value("${app.social-api.mock.latency-ms:0}") long latencyMs,
			@Value("${app.social-api.mock.failure-rate:0}") double failureRate) {
		return new MockSocialApiClient(SocialPlatform.FACEBOOK, Duration.ofMillis(latencyMs), failureRate);
	}

	@Bean
	public SocialApiClient twitterApiClient(@Value("${app.social-api.mock.latency-ms:0}") long latencyMs,
			@Value("${app.social-api.mock.failure-rate:0}") double failureRate) {
		return new MockSocialApiClient(SocialPlatform.TWITTER, Duration.ofMillis(latencyMs), failureRate);
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (Throwable ex, Method method, Object... params) -> log.error("Uncaught error in async {}.{}",
				method.getDeclaringClass().getSimpleName(), method.getName(), ex);
	}

}
