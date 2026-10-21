package com.example.socialanalytics.soap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class ExchangeRateEndpoint {

	static final Map<String, BigDecimal> UNITS_PER_USD = Map.of("USD", BigDecimal.ONE, "VND", new BigDecimal("25350"),
			"EUR", new BigDecimal("0.92"), "JPY", new BigDecimal("149.50"), "SGD", new BigDecimal("1.34"));

	@PayloadRoot(namespace = ExchangeRateSoap.NAMESPACE, localPart = "GetExchangeRateRequest")
	@ResponsePayload
	public GetExchangeRateResponse getExchangeRate(@RequestPayload GetExchangeRateRequest request) {
		String from = normalize(request.getFrom());
		String to = normalize(request.getTo());
		BigDecimal rate = unitsPerUsd(to).divide(unitsPerUsd(from), 6, RoundingMode.HALF_UP).stripTrailingZeros();
		return new GetExchangeRateResponse(from, to, rate, LocalDate.now().toString());
	}

	private static BigDecimal unitsPerUsd(String currency) {
		BigDecimal units = UNITS_PER_USD.get(currency);
		if (units == null) {
			throw new UnknownCurrencyException(currency);
		}
		return units;
	}

	private static String normalize(String currency) {
		return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
	}

}
