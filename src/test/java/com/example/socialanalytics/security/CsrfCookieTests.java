package com.example.socialanalytics.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


@SpringBootTest(properties = "test.context=csrf-cookie")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfCookieTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void xsrfCookieIsIssuedEvenOnPagesWithoutForms() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andExpect(cookie().httpOnly("XSRF-TOKEN", false));
	}

}
