package com.example.socialanalytics.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class JobConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(PropertiesConfig.class)
		.withPropertyValues("app.jobs.update-metrics.cron=0 0 * * * *", "app.jobs.update-metrics.chunk-size=20",
				"app.jobs.update-metrics.core-pool-size=4", "app.jobs.update-metrics.max-pool-size=8",
				"app.jobs.update-metrics.queue-capacity=100", "app.jobs.metrics-retention.cron=0 0 3 * * *",
				"app.jobs.metrics-retention.days=90", "app.jobs.metrics-retention.batch-size=1000");

	@Test
	void validConfigurationStarts() {
		runner.run(context -> assertThat(context).hasNotFailed()
			.getBean(JobProperties.class)
			.extracting(JobProperties::chunkSize)
			.isEqualTo(20));
	}

	@Test
	void zeroChunkSizeFailsAtStartup() {
		runner.withPropertyValues("app.jobs.update-metrics.chunk-size=0")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("chunkSize"));
	}

	@Test
	void zeroRetentionDaysFailsAtStartup() {
		runner.withPropertyValues("app.jobs.metrics-retention.days=0")
			.run(context -> assertThat(context).hasFailed().getFailure().rootCause().hasMessageContaining("days"));
	}

	@Test
	void mockApiClientsOnlyWhenEnabled() {
		ApplicationContextRunner mockRunner = new ApplicationContextRunner()
			.withUserConfiguration(MockSocialApiConfig.class);

		mockRunner.run(context -> assertThat(context).doesNotHaveBean(SocialApiClient.class));
		mockRunner.withPropertyValues("app.social-api.mock.enabled=false")
			.run(context -> assertThat(context).doesNotHaveBean(SocialApiClient.class));
		mockRunner.withPropertyValues("app.social-api.mock.enabled=true")
			.run(context -> assertThat(context.getBeansOfType(SocialApiClient.class)).hasSize(2));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ JobProperties.class, RetentionProperties.class })
	static class PropertiesConfig {

	}

}
