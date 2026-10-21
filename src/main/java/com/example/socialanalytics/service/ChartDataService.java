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

@Service
@Transactional(readOnly = true)
public class ChartDataService {

	public static final int MAX_DAYS = 90;

	private static final List<String> SERIES = List.of("likes", "shares", "comments", "followers");

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
		Instant now = Instant.now();
		LocalDate today = LocalDate.ofInstant(now, zone);
		LocalDate firstDay = today.minusDays(days - 1L);
		Instant from = firstDay.atStartOfDay(zone).toInstant();
		Instant to = from.plusSeconds(days * 86_400L);

		Map<Integer, long[]> deltasByDay = new HashMap<>();
		Map<SocialPlatform, long[]> totals = new EnumMap<>(SocialPlatform.class);
		for (Object[] row : metricRepository.dailyDeltasByPlatform(from, to, platform == null ? "" : platform.name())) {
			int dayIndex = Math.max(0, (int) number(row[0]));
			long[] delta = { number(row[2]), number(row[3]), number(row[4]), number(row[5]), number(row[6]) };
			add(deltasByDay.computeIfAbsent(dayIndex, key -> new long[5]), delta);
			add(totals.computeIfAbsent(SocialPlatform.valueOf((String) row[1]), key -> new long[5]), delta);
		}

		List<String> labels = new ArrayList<>();
		Map<String, List<Long>> series = new LinkedHashMap<>();
		SERIES.forEach(name -> series.put(name, new ArrayList<>()));
		long[] running = new long[5];
		for (int i = 0; i < days; i++) {
			add(running, deltasByDay.getOrDefault(i, new long[5]));
			labels.add(firstDay.plusDays(i).toString());
			for (int s = 0; s < SERIES.size(); s++) {
				series.get(SERIES.get(s)).add(running[s]);
			}
		}
		Map<SocialPlatform, ChartData.Totals> byPlatform = new EnumMap<>(SocialPlatform.class);
		totals.forEach((key, value) -> byPlatform.put(key,
				new ChartData.Totals((int) value[4], value[0], value[1], value[2], value[3])));
		return new ChartData(platform, days, zone.getId(), labels, series, byPlatform, now);
	}

	private static void add(long[] target, long[] delta) {
		for (int i = 0; i < target.length; i++) {
			target[i] += delta[i];
		}
	}

	private static long number(Object value) {
		return value == null ? 0 : ((Number) value).longValue();
	}

}
