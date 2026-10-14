package com.example.socialanalytics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.socialanalytics.entity.SocialMetric;

public interface SocialMetricRepository extends JpaRepository<SocialMetric, Long> {

	Page<SocialMetric> findByPostId(Long postId, Pageable pageable);

	void deleteByPostId(Long postId);

}
