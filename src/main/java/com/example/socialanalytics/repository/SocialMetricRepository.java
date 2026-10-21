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

	@Query(nativeQuery = true, value = """
			WITH src AS (
				SELECT m.id, m.post_id, p.platform, m.collected_at, m.likes, m.shares, m.comments, m.followers,
					FLOOR(TIMESTAMPDIFF(SECOND, :from, m.collected_at) / 86400) AS bucket_day
				FROM social_metrics m JOIN posts p ON p.id = m.post_id
				WHERE m.collected_at >= :from AND m.collected_at < :to AND (:platform = '' OR p.platform = :platform)
				UNION ALL
				SELECT m.id, m.post_id, p.platform, m.collected_at, m.likes, m.shares, m.comments, m.followers,
					-1 AS bucket_day
				FROM social_metrics m JOIN posts p ON p.id = m.post_id
				WHERE (:platform = '' OR p.platform = :platform)
					AND m.collected_at = (SELECT MAX(m2.collected_at) FROM social_metrics m2
						WHERE m2.post_id = m.post_id AND m2.collected_at < :from)
			),
			ranked AS (
				SELECT src.*, ROW_NUMBER() OVER (PARTITION BY post_id, bucket_day ORDER BY collected_at DESC, id DESC) AS rn
				FROM src
			),
			deltas AS (
				SELECT bucket_day, platform,
					likes - COALESCE(LAG(likes) OVER w, 0) AS d_likes,
					shares - COALESCE(LAG(shares) OVER w, 0) AS d_shares,
					comments - COALESCE(LAG(comments) OVER w, 0) AS d_comments,
					followers - COALESCE(LAG(followers) OVER w, 0) AS d_followers,
					CASE WHEN LAG(likes) OVER w IS NULL THEN 1 ELSE 0 END AS new_posts
				FROM ranked
				WHERE rn = 1
				WINDOW w AS (PARTITION BY post_id ORDER BY bucket_day)
			)
			SELECT bucket_day, platform, SUM(d_likes), SUM(d_shares), SUM(d_comments), SUM(d_followers), SUM(new_posts)
			FROM deltas
			GROUP BY bucket_day, platform
			ORDER BY bucket_day, platform
			""")
	List<Object[]> dailyDeltasByPlatform(Instant from, Instant to, String platform);

	@Query("""
			select m from SocialMetric m join fetch m.post
			where m.collectedAt = (select max(m2.collectedAt) from SocialMetric m2 where m2.post = m.post)
			""")
	List<SocialMetric> findLatestPerPost();

}
