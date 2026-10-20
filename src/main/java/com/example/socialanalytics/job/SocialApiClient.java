package com.example.socialanalytics.job;

import com.example.socialanalytics.entity.SocialPlatform;

public interface SocialApiClient {

	SocialPlatform platform();

	MetricSnapshot fetchMetrics(String externalId, MetricSnapshot previous);

}
