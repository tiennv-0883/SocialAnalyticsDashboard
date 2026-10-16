package com.example.socialanalytics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.socialanalytics.dto.PostReportRow;
import com.example.socialanalytics.excel.ExcelMapper;
import com.example.socialanalytics.excel.ExcelMapper.RowResult;
import com.example.socialanalytics.excel.ExcelTestFiles;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.service.PostExcelService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExcelApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostRepository postRepository;

	@Test
	void importsValidRowsReportsBadOnesThenExports() throws Exception {
		byte[] excel = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "url", "title" },
				{ "FACEBOOK", "imp-fb-1", "https://fb.com/imp/1", "First" },
				{ "TWITTER", "imp-tw-1", "https://x.com/imp/1", "Second" },
				{ "FACEBOOK", "imp-fb-1", "https://fb.com/imp/1", "Duplicate of row 2" },
				{ "TWITTER", "imp-tw-2", "", "Missing url" },
				{ "INSTAGRAM", "imp-ig-1", "https://ig.com/1", "Unsupported platform" } });

		mockMvc.perform(multipart("/import-posts").file(new MockMultipartFile("file", "posts.xlsx", null, excel)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalRows").value(5))
			.andExpect(jsonPath("$.imported").value(2))
			.andExpect(jsonPath("$.failed").value(3))
			.andExpect(jsonPath("$.errors[0].row").value(4))
			.andExpect(jsonPath("$.errors[1].row").value(5))
			.andExpect(jsonPath("$.errors[1].message").value("url must not be blank"))
			.andExpect(jsonPath("$.errors[2].row").value(6));

		Long postId = postRepository.findAll().stream().filter(p -> p.getExternalId().equals("imp-fb-1")).findFirst()
			.orElseThrow().getId();
		mockMvc.perform(post("/metrics").contentType(MediaType.APPLICATION_JSON)
			.content("{\"postId\":%d,\"likes\":42,\"shares\":1,\"comments\":0,\"followers\":7}".formatted(postId)))
			.andExpect(status().isCreated());

		byte[] report = mockMvc.perform(get("/export-report")).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsByteArray();
		List<PostReportRow> rows = ExcelMapper.read(new ByteArrayInputStream(report), PostReportRow.class).stream()
			.map(RowResult::value)
			.toList();

		assertThat(rows).filteredOn(row -> row.getExternalId().equals("imp-fb-1")).singleElement()
			.satisfies(row -> assertThat(row.getLikes()).isEqualTo(42L));
		assertThat(rows).filteredOn(row -> row.getExternalId().equals("imp-tw-1")).singleElement()
			.satisfies(row -> assertThat(row.getLikes()).isNull());
	}

	@Test
	void rejectsFileOverRowLimit() throws Exception {
		Object[][] rows = new Object[PostExcelService.MAX_IMPORT_ROWS + 2][];
		rows[0] = new Object[] { "platform", "externalId", "url" };
		for (int i = 1; i < rows.length; i++) {
			rows[i] = new Object[] { "FACEBOOK", "limit-" + i, "u" };
		}

		mockMvc.perform(multipart("/import-posts")
			.file(new MockMultipartFile("file", "big.xlsx", null, ExcelTestFiles.workbook(rows))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Too many rows: at most 5000 data rows are allowed"));
		assertThat(postRepository.findAll()).noneMatch(post -> post.getExternalId().startsWith("limit-"));
	}

	@Test
	void rejectsFileMissingRequiredColumn() throws Exception {
		byte[] excel = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId" }, { "FACEBOOK", "no-url-1" } });

		mockMvc.perform(multipart("/import-posts").file(new MockMultipartFile("file", "posts.xlsx", null, excel)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(startsWith("Missing required columns: [url]")));
	}

	@Test
	void rejectsNonXlsxFile() throws Exception {
		mockMvc.perform(multipart("/import-posts").file(new MockMultipartFile("file", "posts.csv", null, "a,b".getBytes())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Only .xlsx files are supported"));
	}

}
