package com.example.socialanalytics.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;

public interface SocialMetricRepository extends JpaRepository<SocialMetric, Long> {

	Page<SocialMetric> findByPostId(Long postId, Pageable pageable);

	void deleteByPostId(Long postId);

	Optional<SocialMetric> findTopByPostIdOrderByCollectedAtDescIdDesc(Long postId);

	@Query("""
			select m.id from SocialMetric m
			where m.collectedAt < :cutoff
			and m.collectedAt < (select max(m2.collectedAt) from SocialMetric m2 where m2.post = m.post)
			order by m.id
			""")
	List<Long> findPurgeableIds(Instant cutoff, Pageable pageable);

	interface ChartPoint {

		Long getPostId();

		SocialPlatform getPlatform();

		Instant getCollectedAt();

		long getLikes();

		long getShares();

		long getComments();

		long getFollowers();

	}

	@Query("""
			select m.post.id as postId, m.post.platform as platform, m.collectedAt as collectedAt, m.likes as likes,
			m.shares as shares, m.comments as comments, m.followers as followers
			from SocialMetric m
			where m.collectedAt >= :from and (:platform is null or m.post.platform = :platform)
			order by m.collectedAt, m.id
			""")
	List<ChartPoint> findChartPointsSince(Instant from, SocialPlatform platform);

	@Query("""
			select m.post.id as postId, m.post.platform as platform, m.collectedAt as collectedAt, m.likes as likes,
			m.shares as shares, m.comments as comments, m.followers as followers
			from SocialMetric m
			where (:platform is null or m.post.platform = :platform)
			and m.collectedAt = (select max(m2.collectedAt) from SocialMetric m2
				where m2.post = m.post and m2.collectedAt < :before)
			order by m.collectedAt, m.id
			""")
	List<ChartPoint> findLatestChartPointsBefore(Instant before, SocialPlatform platform);

	@Query("""
			select m from SocialMetric m join fetch m.post
			where m.collectedAt = (select max(m2.collectedAt) from SocialMetric m2 where m2.post = m.post)
			""")
	List<SocialMetric> findLatestPerPost();

}
