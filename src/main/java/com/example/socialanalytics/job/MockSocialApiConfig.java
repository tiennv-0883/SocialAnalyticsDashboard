package com.example.socialanalytics.job;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.socialanalytics.entity.SocialPlatform;

@Configuration
@ConditionalOnProperty(prefix = "app.social-api.mock", name = "enabled", havingValue = "true")
public class MockSocialApiConfig {

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

}
