package com.example.socialanalytics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByProviderAndProviderUserId(SocialPlatform provider, String providerUserId);

}
