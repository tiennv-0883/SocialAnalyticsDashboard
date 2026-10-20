package com.example.socialanalytics.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.socialanalytics.dto.ChartData;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.service.ChartDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chart", description = "Aggregated data for the dashboard charts")
@RestController
@RequiredArgsConstructor
public class ChartController {

	private final ChartDataService chartDataService;

	@Operation(summary = "Daily totals (likes, shares, comments, followers) and totals per platform")
	@GetMapping("/chart-data")
	public ChartData chartData(@RequestParam(required = false) SocialPlatform platform,
			@RequestParam(defaultValue = "14") int days) {
		try {
			return chartDataService.chartData(platform, days);
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}
	}

}
