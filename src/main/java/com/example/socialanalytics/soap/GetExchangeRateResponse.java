package com.example.socialanalytics.soap;

import java.math.BigDecimal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetExchangeRateResponse", namespace = ExchangeRateSoap.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class GetExchangeRateResponse {

	@XmlElement(namespace = ExchangeRateSoap.NAMESPACE, required = true)
	private String from;

	@XmlElement(namespace = ExchangeRateSoap.NAMESPACE, required = true)
	private String to;

	@XmlElement(namespace = ExchangeRateSoap.NAMESPACE, required = true)
	private BigDecimal rate;

	@XmlElement(namespace = ExchangeRateSoap.NAMESPACE, required = true)
	private String date;

}
