package com.example.socialanalytics.soap;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.core.env.Environment;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExchangeRateClient {

	private final WebServiceTemplate webServiceTemplate;

	private final Environment environment;

	public ExchangeRateClient(Environment environment) {
		this.environment = environment;
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(GetExchangeRateRequest.class, GetExchangeRateResponse.class);
		HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
		sender.setConnectionTimeout(Duration.ofSeconds(2));
		sender.setReadTimeout(Duration.ofSeconds(3));
		this.webServiceTemplate = new WebServiceTemplate(marshaller);
		this.webServiceTemplate.setMessageSender(sender);
	}

	public ExchangeRate getExchangeRate(String from, String to) {
		String url = environment.getRequiredProperty("app.exchange-rate.url");
		try {
			GetExchangeRateResponse response = (GetExchangeRateResponse) webServiceTemplate
				.marshalSendAndReceive(url, new GetExchangeRateRequest(Currencies.normalize(from), Currencies.normalize(to)));
			log.debug("SOAP GetExchangeRate {}->{} = {}", response.getFrom(), response.getTo(), response.getRate());
			return new ExchangeRate(response.getFrom(), response.getTo(), response.getRate(),
					LocalDate.parse(response.getDate()), Instant.now());
		}
		catch (SoapFaultClientException ex) {
			throw new ExchangeRateException(ex.getFaultStringOrReason(), true, ex);
		}
		catch (WebServiceClientException ex) {
			throw new ExchangeRateException("Exchange rate service unavailable: " + ex.getMessage(), false, ex);
		}
	}

}
