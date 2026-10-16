package com.example.socialanalytics.dto;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
		@NotNull SocialPlatform platform,
		@NotBlank @Size(max = 100) String externalId,
		@NotBlank @Size(max = 500) String url,
		@Size(max = 255) String title,
		@Size(max = Post.CONTENT_MAX_LENGTH) String content) {
}
