package com.example.socialanalytics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.socialanalytics.entity.SocialMetric;

public interface SocialMetricRepository extends JpaRepository<SocialMetric, Long> {

	Page<SocialMetric> findByPostId(Long postId, Pageable pageable);

	void deleteByPostId(Long postId);

	Optional<SocialMetric> findTopByPostIdOrderByCollectedAtDescIdDesc(Long postId);

	@Query("""
			select m from SocialMetric m join fetch m.post
			where m.collectedAt = (select max(m2.collectedAt) from SocialMetric m2 where m2.post = m.post)
			""")
	List<SocialMetric> findLatestPerPost();

}
