package com.example.socialanalytics.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findByPlatform(SocialPlatform platform, Pageable pageable);

	boolean existsByPlatformAndExternalId(SocialPlatform platform, String externalId);

	interface IdAndPlatform {

		Long getId();

		SocialPlatform getPlatform();

	}

	@Query("select p.id as id, p.platform as platform from Post p order by p.id")
	List<IdAndPlatform> findAllIdsAndPlatforms();

}
