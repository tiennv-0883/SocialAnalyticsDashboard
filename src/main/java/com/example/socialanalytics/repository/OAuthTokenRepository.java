package com.example.socialanalytics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.socialanalytics.entity.OAuthToken;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

	Optional<OAuthToken> findByRegistrationIdAndPrincipalName(String registrationId, String principalName);

	void deleteByRegistrationIdAndPrincipalName(String registrationId, String principalName);

}
