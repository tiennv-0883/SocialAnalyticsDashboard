package com.example.socialanalytics.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ExchangeRateServiceTest {

	private final ExchangeRateClient client = mock(ExchangeRateClient.class);

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-22T08:00:00Z"));

	private final ExchangeRateService service = new ExchangeRateService(client, Duration.ofMinutes(10),
			Duration.ofSeconds(30), clock);

	@Test
	void normalizesCodesSoVariantsShareOneCacheEntry() {
		when(client.getExchangeRate("USD", "VND")).thenReturn(rate("USD", "VND"));

		service.getRate(" usd", "vnd ");
		service.getRate("USD", "VND");

		verify(client, times(1)).getExchangeRate("USD", "VND");
	}

	@Test
	void rejectsMalformedCodesWithoutCallingSoap() {
		assertThatThrownBy(() -> service.getRate("US", "VND")).isInstanceOf(ExchangeRateException.class)
			.satisfies(ex -> assertThat(((ExchangeRateException) ex).isClientError()).isTrue());
		assertThatThrownBy(() -> service.getRate("USD", "V1D")).isInstanceOf(ExchangeRateException.class);
		verify(client, never()).getExchangeRate(any(), any());
	}

	@Test
	void failureIsCachedBrieflySoCallersDoNotWaitForTimeoutEachTime() {
		when(client.getExchangeRate("USD", "VND"))
			.thenThrow(new ExchangeRateException("Exchange rate service unavailable: timeout", false, null))
			.thenReturn(rate("USD", "VND"));

		assertThat(service.findRate("USD", "VND")).isEmpty();
		clock.advance(Duration.ofSeconds(20));
		assertThat(service.findRate("USD", "VND")).isEmpty();
		verify(client, times(1)).getExchangeRate("USD", "VND");

		clock.advance(Duration.ofSeconds(11));
		assertThat(service.findRate("USD", "VND")).isPresent();
		verify(client, times(2)).getExchangeRate("USD", "VND");
	}

	@Test
	void successfulRateExpiresAfterTtl() {
		when(client.getExchangeRate("USD", "JPY")).thenReturn(rate("USD", "JPY"));

		service.getRate("USD", "JPY");
		clock.advance(Duration.ofMinutes(9));
		service.getRate("USD", "JPY");
		clock.advance(Duration.ofMinutes(2));
		service.getRate("USD", "JPY");

		verify(client, times(2)).getExchangeRate("USD", "JPY");
	}

	@Test
	void cacheSizeIsBounded() {
		when(client.getExchangeRate(any(), any())).thenAnswer(invocation -> rate(invocation.getArgument(0),
				invocation.getArgument(1)));

		for (int i = 0; i < ExchangeRateService.MAX_CACHE_ENTRIES + 50; i++) {
			String code = "" + (char) ('A' + i / 676) + (char) ('A' + i / 26 % 26) + (char) ('A' + i % 26);
			service.getRate("USD", code);
		}

		service.getRate("USD", "AAA");
		verify(client, times(2)).getExchangeRate("USD", "AAA");
	}

	private ExchangeRate rate(String from, String to) {
		return new ExchangeRate(from, to, BigDecimal.TEN, LocalDate.of(2026, 10, 22), clock.instant());
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

	}

}
