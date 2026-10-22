package com.example.socialanalytics.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.example.socialanalytics.dto.ChartData;
import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelTestFiles;
import com.example.socialanalytics.job.MetricRetentionJob;
import com.example.socialanalytics.job.UpdateSocialMetricsJob;
import com.example.socialanalytics.repository.ImportLogRepository;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;
import com.example.socialanalytics.service.ChartDataService;
import com.example.socialanalytics.service.EntityExportService;
import com.example.socialanalytics.service.PostExcelService;

@SpringBootTest(properties = "app.jobs.metrics-retention.days=30")
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Import(MySqlIntegrationTests.Containers.class)
@DirtiesContext
class MySqlIntegrationTests {

	@TestConfiguration(proxyBeanMethods = false)
	static class Containers {

		@Bean
		@ServiceConnection
		MySQLContainer mysql() {
			return new MySQLContainer("mysql:8.4");
		}

	}

	@Autowired
	private MySQLContainer mysql;

	@Autowired
	private PostExcelService postExcelService;

	@Autowired
	private ImportLogRepository importLogRepository;

	@Autowired
	private UpdateSocialMetricsJob job;

	@Autowired
	private ChartDataService chartDataService;

	@Autowired
	private MetricRetentionJob retentionJob;

	@Autowired
	private EntityExportService exportService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private SocialMetricRepository metricRepository;

	@Test
	void mainFlowWorksOnRealMySql() throws Exception {
		assertThat(mysql.isRunning()).isTrue();
		byte[] excel = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "url", "content" },
				{ "FACEBOOK", "mysql-fb-1", "https://fb.com/m/1", "Nội dung tiếng Việt 😀" },
				{ "TWITTER", "mysql-tw-1", "https://x.com/m/1", null } });

		assertThat(postExcelService.importPosts(new MockMultipartFile("file", "mysql.xlsx", null, excel)).imported())
			.isEqualTo(2);
		await().atMost(Duration.ofSeconds(10)).until(() -> importLogRepository.count() == 1);

		JobRun run = job.run(JobRun.Trigger.MANUAL).orElseThrow();
		assertThat(run.getStatus()).isEqualTo(JobRun.Status.SUCCESS);
		assertThat(run.getUpdatedItems()).isEqualTo(2);

		ChartData chart = chartDataService.chartData(null, 7);
		assertThat(chart.series().get("followers").get(6)).isPositive();

		Post post = postRepository.findAll().get(0);
		SocialMetric old = new SocialMetric();
		old.setPost(post);
		old.setCollectedAt(Instant.now().minus(Duration.ofDays(60)));
		metricRepository.save(old);
		assertThat(retentionJob.purge()).isEqualTo(1);

		assertThat(exportService.export("metrics").rows()).isEqualTo((int) metricRepository.count());
		assertThat(postRepository.findAll()).anySatisfy(saved -> assertThat(saved.getContent()).contains("😀"));
	}

}
