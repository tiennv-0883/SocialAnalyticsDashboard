package com.example.socialanalytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI socialAnalyticsOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Social Analytics Dashboard API")
				.description("Aggregate and track engagement (likes, shares, followers) from Facebook & Twitter")
				.version("v1"));
	}

}
