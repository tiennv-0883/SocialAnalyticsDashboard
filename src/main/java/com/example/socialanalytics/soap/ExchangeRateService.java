package com.example.socialanalytics.soap;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExchangeRateService {

	static final int MAX_CACHE_ENTRIES = 200;

	private record CacheEntry(ExchangeRate rate, ExchangeRateException error, Instant expiresAt) {
	}

	private final ExchangeRateClient client;

	private final Duration cacheTtl;

	private final Duration errorCacheTtl;

	private final Clock clock;

	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	@Autowired
	public ExchangeRateService(ExchangeRateClient client, @Value("${app.exchange-rate.cache-ttl:10m}") Duration cacheTtl,
			@Value("${app.exchange-rate.error-cache-ttl:30s}") Duration errorCacheTtl) {
		this(client, cacheTtl, errorCacheTtl, Clock.systemUTC());
	}

	ExchangeRateService(ExchangeRateClient client, Duration cacheTtl, Duration errorCacheTtl, Clock clock) {
		this.client = client;
		this.cacheTtl = cacheTtl;
		this.errorCacheTtl = errorCacheTtl;
		this.clock = clock;
	}

	public ExchangeRate getRate(String from, String to) {
		String normalizedFrom = Currencies.normalize(from);
		String normalizedTo = Currencies.normalize(to);
		if (!Currencies.isValid(normalizedFrom) || !Currencies.isValid(normalizedTo)) {
			throw new ExchangeRateException("Currency codes must be 3 letters (e.g. USD, VND)", true, null);
		}
		String key = normalizedFrom + "/" + normalizedTo;
		Instant now = clock.instant();
		CacheEntry cached = cache.get(key);
		if (cached != null && cached.expiresAt().isAfter(now)) {
			if (cached.error() != null) {
				throw cached.error();
			}
			return cached.rate();
		}
		try {
			ExchangeRate fresh = client.getExchangeRate(normalizedFrom, normalizedTo);
			put(key, new CacheEntry(fresh, null, now.plus(cacheTtl)), now);
			return fresh;
		}
		catch (ExchangeRateException ex) {
			put(key, new CacheEntry(null, ex, now.plus(errorCacheTtl)), now);
			throw ex;
		}
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

	private void put(String key, CacheEntry entry, Instant now) {
		if (cache.size() >= MAX_CACHE_ENTRIES) {
			cache.values().removeIf(existing -> !existing.expiresAt().isAfter(now));
			if (cache.size() >= MAX_CACHE_ENTRIES) {
				cache.clear();
			}
		}
		cache.put(key, entry);
	}

}
