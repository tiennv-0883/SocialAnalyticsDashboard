package com.example.socialanalytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.socialanalytics.dto.MetricRequest;
import com.example.socialanalytics.dto.MetricResponse;
import com.example.socialanalytics.service.MetricService;

@WebMvcTest(MetricController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MetricControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MetricService metricService;

	private final MetricResponse response = new MetricResponse(5L, 1L, 10, 2, 3, 100, Instant.now());

	@Test
	void listFiltersByPost() throws Exception {
		when(metricService.findAll(eq(1L), any())).thenReturn(new PageImpl<>(List.of(response)));

		mockMvc.perform(get("/metrics").param("postId", "1").param("sort", "likes,desc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].likes").value(10));
	}

	@Test
	void createReturns201() throws Exception {
		when(metricService.create(any(MetricRequest.class))).thenReturn(response);

		mockMvc.perform(post("/metrics").contentType(MediaType.APPLICATION_JSON)
			.content("{\"postId\":1,\"likes\":10,\"shares\":2,\"comments\":3,\"followers\":100}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void createRejectsNegativeCounts() throws Exception {
		mockMvc.perform(post("/metrics").contentType(MediaType.APPLICATION_JSON)
			.content("{\"postId\":1,\"likes\":-1,\"shares\":0,\"comments\":0,\"followers\":0}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.likes").exists());
	}

}
