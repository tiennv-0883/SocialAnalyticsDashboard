package com.example.socialanalytics.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.socialanalytics.entity.PlatformStats;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelTestFiles;
import com.example.socialanalytics.repository.PlatformStatsRepository;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndFlowTests {

	private static final RequestPostProcessor ADMIN = oauth2Login();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private SocialMetricRepository metricRepository;

	@Autowired
	private PlatformStatsRepository statsRepository;

	@Test
	void importThenJobThenChartThenExport() throws Exception {
		long statsBefore = statsRepository.findByPlatform(SocialPlatform.FACEBOOK).map(PlatformStats::getImportedPosts).orElse(0L);
		byte[] excel = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "url", "title" },
				{ "FACEBOOK", "e2e-fb-1", "https://fb.com/e2e/1", "E2E 1" },
				{ "FACEBOOK", "e2e-fb-2", "https://fb.com/e2e/2", "E2E 2" },
				{ "TWITTER", "e2e-tw-1", "https://x.com/e2e/1", "E2E 3" } });

		mockMvc.perform(multipart("/import-posts").file(new MockMultipartFile("file", "e2e.xlsx", null, excel))
			.with(ADMIN).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imported").value(3));

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
				statsRepository.findByPlatform(SocialPlatform.FACEBOOK).orElseThrow().getImportedPosts())
			.isEqualTo(statsBefore + 2));

		mockMvc.perform(post("/jobs/update-metrics/run").with(ADMIN).with(csrf())).andExpect(status().isAccepted());
		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> mockMvc
			.perform(get("/jobs/update-metrics/last").with(ADMIN))
			.andExpect(jsonPath("$.trigger").value("MANUAL"))
			.andExpect(jsonPath("$.status").value("SUCCESS")));
		assertThat(metricRepository.count()).isGreaterThanOrEqualTo(postRepository.count());

		String chartJson = mockMvc.perform(get("/chart-data").param("days", "7").with(ADMIN))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		JsonNode chart = JsonMapper.builder().build().readTree(chartJson);
		assertThat(chart.get("series").get("followers").get(6).asLong()).isPositive();
		assertThat(chart.get("byPlatform").get("TWITTER").get("posts").asInt()).isPositive();

		byte[] metricsExport = mockMvc.perform(get("/export/metrics").with(ADMIN))
			.andExpect(status().isOk())
			.andExpect(header().string("X-Export-Truncated", "false"))
			.andReturn().getResponse().getContentAsByteArray();
		Sheet sheet = new XSSFWorkbook(new ByteArrayInputStream(metricsExport)).getSheetAt(0);
		assertThat(sheet.getLastRowNum()).isEqualTo((int) metricRepository.count());
		assertThat(sheet.getRow(0).getCell(sheet.getRow(0).getLastCellNum() - 1)).isNotNull();

		mockMvc.perform(get("/export/oauth-tokens").with(ADMIN)).andExpect(status().isNotFound());
		mockMvc.perform(get("/export").with(ADMIN))
			.andExpect(jsonPath("$").value(containsInAnyOrder(
					List.of("posts", "metrics", "job-runs", "import-logs", "platform-stats").toArray())));
	}

}
