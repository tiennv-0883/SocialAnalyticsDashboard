package com.example.socialanalytics.soap;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExchangeRateService {

	private final ExchangeRateClient client;

	private final Duration cacheTtl;

	private final Map<String, ExchangeRate> cache = new ConcurrentHashMap<>();

	public ExchangeRateService(ExchangeRateClient client, @Value("${app.exchange-rate.cache-ttl:10m}") Duration cacheTtl) {
		this.client = client;
		this.cacheTtl = cacheTtl;
	}

	public ExchangeRate getRate(String from, String to) {
		String key = from.toUpperCase(Locale.ROOT) + "/" + to.toUpperCase(Locale.ROOT);
		ExchangeRate cached = cache.get(key);
		if (cached != null && cached.fetchedAt().plus(cacheTtl).isAfter(Instant.now())) {
			return cached;
		}
		ExchangeRate fresh = client.getExchangeRate(from, to);
		cache.put(key, fresh);
		return fresh;
	}

	public Optional<ExchangeRate> findRate(String from, String to) {
		try {
			return Optional.of(getRate(from, to));
		}
		catch (ExchangeRateException ex) {
			log.warn("Exchange rate {}->{} not available: {}", from, to, ex.getMessage());
			return Optional.empty();
		}
	}

}
