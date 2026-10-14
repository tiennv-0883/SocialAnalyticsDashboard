package com.example.socialanalytics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findByPlatform(SocialPlatform platform, Pageable pageable);

	boolean existsByPlatformAndExternalId(SocialPlatform platform, String externalId);

}
