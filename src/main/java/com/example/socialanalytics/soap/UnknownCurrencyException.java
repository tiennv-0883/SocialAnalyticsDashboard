package com.example.socialanalytics.soap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class UnknownCurrencyException extends RuntimeException {

	public UnknownCurrencyException(String currency) {
		super("Unknown currency: " + currency);
	}

}
