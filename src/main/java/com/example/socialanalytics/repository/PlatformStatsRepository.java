package com.example.socialanalytics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.socialanalytics.entity.PlatformStats;
import com.example.socialanalytics.entity.SocialPlatform;

public interface PlatformStatsRepository extends JpaRepository<PlatformStats, Long> {

	Optional<PlatformStats> findByPlatform(SocialPlatform platform);

	List<PlatformStats> findAllByOrderByPlatform();

}
