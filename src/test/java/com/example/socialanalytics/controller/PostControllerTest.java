package com.example.socialanalytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.socialanalytics.dto.PostRequest;
import com.example.socialanalytics.dto.PostResponse;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.exception.DuplicateResourceException;
import com.example.socialanalytics.exception.ResourceNotFoundException;
import com.example.socialanalytics.service.PostService;

@WebMvcTest(PostController.class)
@ActiveProfiles("test")
class PostControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostService postService;

	private final PostResponse response = new PostResponse(1L, SocialPlatform.FACEBOOK, "fb-1", "https://fb.com/p/1",
			"Title", null, Instant.now(), Instant.now());

	@Test
	void listReturnsPage() throws Exception {
		when(postService.findAll(eq(SocialPlatform.FACEBOOK), any())).thenReturn(new PageImpl<>(List.of(response)));

		mockMvc.perform(get("/posts").param("platform", "FACEBOOK"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].externalId").value("fb-1"));
	}

	@Test
	void getReturns404WhenMissing() throws Exception {
		when(postService.findById(99L)).thenThrow(new ResourceNotFoundException("Post", 99L));

		mockMvc.perform(get("/posts/99")).andExpect(status().isNotFound());
	}

	@Test
	void createReturns201() throws Exception {
		when(postService.create(any(PostRequest.class))).thenReturn(response);

		mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON)
			.content("{\"platform\":\"FACEBOOK\",\"externalId\":\"fb-1\",\"url\":\"https://fb.com/p/1\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void createReturns409OnDuplicate() throws Exception {
		when(postService.create(any(PostRequest.class))).thenThrow(new DuplicateResourceException("exists"));

		mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON)
			.content("{\"platform\":\"FACEBOOK\",\"externalId\":\"fb-1\",\"url\":\"https://fb.com/p/1\"}"))
			.andExpect(status().isConflict());
	}

	@Test
	void createReturns400WhenInvalid() throws Exception {
		mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.platform").exists())
			.andExpect(jsonPath("$.errors.externalId").exists());
		verifyNoInteractions(postService);
	}

	@Test
	void listRejectsUnknownSort() throws Exception {
		mockMvc.perform(get("/posts").param("sort", "collectedAt")).andExpect(status().isBadRequest());
		verifyNoInteractions(postService);
	}

	@Test
	void deleteReturns204() throws Exception {
		mockMvc.perform(delete("/posts/1")).andExpect(status().isNoContent());
		verify(postService).delete(1L);
	}

}
