package com.example.socialanalytics.soap;

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
@XmlRootElement(name = "GetExchangeRateRequest", namespace = ExchangeRateSoap.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class GetExchangeRateRequest {

	@XmlElement(namespace = ExchangeRateSoap.NAMESPACE, required = true)
	private String from;

	@XmlElement(namespace = ExchangeRateSoap.NAMESPACE, required = true)
	private String to;

}
