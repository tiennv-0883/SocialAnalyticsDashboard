package com.example.socialanalytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.socialanalytics.dto.ChartData;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;

@DataJpaTest
@ActiveProfiles("test")
@Import(ChartDataService.class)
class ChartDataServiceTest {

	@Autowired
	private ChartDataService service;

	@Autowired
	private TestEntityManager em;

	private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

	@BeforeEach
	void data() {
		Post facebook = post(SocialPlatform.FACEBOOK, "fb-1");
		metric(facebook, today.minusDays(10), 9, 100, 1_000);
		metric(facebook, today.minusDays(1), 8, 140, 1_100);
		metric(facebook, today.minusDays(1), 20, 150, 1_200);
		Post twitter = post(SocialPlatform.TWITTER, "tw-1");
		metric(twitter, today, 0, 20, 50);
		em.flush();
	}

	@Test
	void sumsLastSnapshotOfEachPostPerDayAndCarriesValuesForward() {
		ChartData data = service.chartData(null, 3);

		assertThat(data.labels()).containsExactly(today.minusDays(2).toString(), today.minusDays(1).toString(),
				today.toString());
		assertThat(data.series().get("likes")).containsExactly(100L, 150L, 170L);
		assertThat(data.series().get("followers")).containsExactly(1_000L, 1_200L, 1_250L);
		assertThat(data.byPlatform().get(SocialPlatform.FACEBOOK).likes()).isEqualTo(150);
		assertThat(data.byPlatform().get(SocialPlatform.TWITTER).posts()).isEqualTo(1);
	}

	@Test
	void filtersByPlatform() {
		ChartData data = service.chartData(SocialPlatform.TWITTER, 3);

		assertThat(data.series().get("likes")).containsExactly(0L, 0L, 20L);
		assertThat(data.byPlatform()).containsOnlyKeys(SocialPlatform.TWITTER);
	}

	@Test
	void rejectsOutOfRangeDays() {
		assertThatThrownBy(() -> service.chartData(null, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.chartData(null, ChartDataService.MAX_DAYS + 1))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private Post post(SocialPlatform platform, String externalId) {
		Post post = new Post();
		post.setPlatform(platform);
		post.setExternalId(externalId);
		post.setUrl("https://example.com/" + externalId);
		return em.persist(post);
	}

	private void metric(Post post, LocalDate day, int hour, long likes, long followers) {
		SocialMetric metric = new SocialMetric();
		metric.setPost(post);
		metric.setLikes(likes);
		metric.setFollowers(followers);
		metric.setCollectedAt(day.atTime(LocalTime.of(hour, 0)).toInstant(ZoneOffset.UTC));
		em.persist(metric);
	}

}
