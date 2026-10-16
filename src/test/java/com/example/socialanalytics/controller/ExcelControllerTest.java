package com.example.socialanalytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.socialanalytics.dto.ImportResult;
import com.example.socialanalytics.excel.ExcelProcessingException;
import com.example.socialanalytics.service.PostExcelService;

@WebMvcTest(ExcelController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ExcelControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostExcelService postExcelService;

	private final MockMultipartFile file = new MockMultipartFile("file", "posts.xlsx",
			ExcelController.XLSX.toString(), new byte[] { 1 });

	@Test
	void importReturnsResult() throws Exception {
		when(postExcelService.importPosts(any()))
			.thenReturn(new ImportResult(2, 1, 1, List.of(new ImportResult.RowError(3, "url must not be blank"))));

		mockMvc.perform(multipart("/import-posts").file(file))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imported").value(1))
			.andExpect(jsonPath("$.errors[0].row").value(3));
	}

	@Test
	void importReturns400ForBadFile() throws Exception {
		when(postExcelService.importPosts(any())).thenThrow(new ExcelProcessingException("Only .xlsx files are supported"));

		mockMvc.perform(multipart("/import-posts").file(file))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Only .xlsx files are supported"));
	}

	@Test
	void exportReturnsXlsxAttachment() throws Exception {
		when(postExcelService.exportReport()).thenReturn(new byte[] { 1, 2, 3 });

		mockMvc.perform(get("/export-report"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(ExcelController.XLSX))
			.andExpect(header().string("Content-Disposition", Matchers.startsWith("attachment; filename=\"social-report-")))
			.andExpect(content().bytes(new byte[] { 1, 2, 3 }));
	}

}
