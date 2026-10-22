package com.example.socialanalytics.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.socialanalytics.soap.ExchangeRate;
import com.example.socialanalytics.soap.ExchangeRateException;
import com.example.socialanalytics.soap.ExchangeRateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Exchange rate", description = "Rates read from the SOAP web service (Spring-WS client)")
@RestController
@RequiredArgsConstructor
public class ExchangeRateController {

	private final ExchangeRateService exchangeRateService;

	@Operation(summary = "Exchange rate between two currencies (USD, VND, EUR, JPY, SGD)")
	@GetMapping("/exchange-rate")
	public ExchangeRate exchangeRate(@RequestParam(defaultValue = "USD") String from,
			@RequestParam(defaultValue = "VND") String to) {
		try {
			return exchangeRateService.getRate(from, to);
		}
		catch (ExchangeRateException ex) {
			throw new ResponseStatusException(ex.isClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE,
					ex.getMessage());
		}
	}

}
