package com.example.socialanalytics.soap;

public class ExchangeRateException extends RuntimeException {

	private final boolean clientError;

	public ExchangeRateException(String message, boolean clientError, Throwable cause) {
		super(message, cause);
		this.clientError = clientError;
	}

	public boolean isClientError() {
		return clientError;
	}

}
