package com.example.socialanalytics.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PostMetricApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void postAndMetricCrudFlow() throws Exception {
		MvcResult created = mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content("""
				{"platform":"FACEBOOK","externalId":"fb-1","url":"https://facebook.com/p/1","title":"Hello"}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andReturn();
		Number postId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content("""
				{"platform":"FACEBOOK","externalId":"fb-1","url":"https://facebook.com/p/1"}
				"""))
			.andExpect(status().isConflict());

		mockMvc.perform(post("/metrics").contentType(MediaType.APPLICATION_JSON).content("""
				{"postId":%d,"likes":10,"shares":2,"comments":3,"followers":100}
				""".formatted(postId.longValue())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.likes").value(10));

		mockMvc.perform(get("/metrics").param("postId", postId.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1));

		mockMvc.perform(delete("/posts/{id}", postId)).andExpect(status().isNoContent());
		mockMvc.perform(get("/posts/{id}", postId)).andExpect(status().isNotFound());
	}

	@Test
	void rejectsInvalidPost() throws Exception {
		mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.platform").exists());
	}

	@Test
	void rejectsUnknownSortProperty() throws Exception {
		mockMvc.perform(get("/metrics").param("sort", "[\"string\"]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("Invalid sort property")));

		mockMvc.perform(get("/posts").param("sort", "createdAt,desc")).andExpect(status().isOk());
	}

	@Test
	void exposesOpenApiDocs() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/posts']").exists());
	}

}
