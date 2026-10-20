package com.example.socialanalytics.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialanalytics.dto.ChartData;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.repository.SocialMetricRepository;
import com.example.socialanalytics.repository.SocialMetricRepository.ChartPoint;

@Service
@Transactional(readOnly = true)
public class ChartDataService {

	public static final int MAX_DAYS = 90;

	private final SocialMetricRepository metricRepository;

	private final ZoneId zone;

	public ChartDataService(SocialMetricRepository metricRepository, @Value("${app.chart.zone:UTC}") String zone) {
		this.metricRepository = metricRepository;
		this.zone = ZoneId.of(zone);
	}

	public ChartData chartData(SocialPlatform platform, int days) {
		if (days < 1 || days > MAX_DAYS) {
			throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS);
		}
		LocalDate today = LocalDate.now(zone);
		LocalDate firstDay = today.minusDays(days - 1L);
		Instant from = firstDay.atStartOfDay(zone).toInstant();

		Map<Long, ChartPoint> latestByPost = new HashMap<>();
		metricRepository.findLatestChartPointsBefore(from, platform).forEach(point -> latestByPost.put(point.getPostId(), point));
		List<ChartPoint> points = metricRepository.findChartPointsSince(from, platform);

		List<String> labels = new ArrayList<>();
		Map<String, List<Long>> series = new LinkedHashMap<>();
		for (String name : List.of("likes", "shares", "comments", "followers")) {
			series.put(name, new ArrayList<>());
		}
		int next = 0;
		for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
			Instant endOfDay = day.plusDays(1).atStartOfDay(zone).toInstant();
			while (next < points.size() && points.get(next).getCollectedAt().isBefore(endOfDay)) {
				ChartPoint point = points.get(next++);
				latestByPost.put(point.getPostId(), point);
			}
			labels.add(day.toString());
			series.get("likes").add(latestByPost.values().stream().mapToLong(ChartPoint::getLikes).sum());
			series.get("shares").add(latestByPost.values().stream().mapToLong(ChartPoint::getShares).sum());
			series.get("comments").add(latestByPost.values().stream().mapToLong(ChartPoint::getComments).sum());
			series.get("followers").add(latestByPost.values().stream().mapToLong(ChartPoint::getFollowers).sum());
		}
		while (next < points.size()) {
			ChartPoint point = points.get(next++);
			latestByPost.put(point.getPostId(), point);
		}
		return new ChartData(platform, days, zone.getId(), labels, series, totalsByPlatform(latestByPost),
				Instant.now());
	}

	private static Map<SocialPlatform, ChartData.Totals> totalsByPlatform(Map<Long, ChartPoint> latestByPost) {
		Map<SocialPlatform, ChartData.Totals> totals = new EnumMap<>(SocialPlatform.class);
		for (ChartPoint point : latestByPost.values()) {
			ChartData.Totals current = totals.getOrDefault(point.getPlatform(), new ChartData.Totals(0, 0, 0, 0, 0));
			totals.put(point.getPlatform(),
					new ChartData.Totals(current.posts() + 1, current.likes() + point.getLikes(),
							current.shares() + point.getShares(), current.comments() + point.getComments(),
							current.followers() + point.getFollowers()));
		}
		return totals;
	}

}
