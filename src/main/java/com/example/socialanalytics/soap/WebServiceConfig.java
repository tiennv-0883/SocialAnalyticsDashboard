package com.example.socialanalytics.soap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@Configuration
public class WebServiceConfig {

	@Bean
	public XsdSchema exchangeRateSchema() {
		return new SimpleXsdSchema(new ClassPathResource("xsd/exchange-rate.xsd"));
	}

	@Bean(name = ExchangeRateSoap.WSDL_NAME)
	public DefaultWsdl11Definition exchangeRatesWsdl(XsdSchema exchangeRateSchema) {
		DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
		definition.setPortTypeName("ExchangeRatesPort");
		definition.setLocationUri("/soap");
		definition.setTargetNamespace(ExchangeRateSoap.NAMESPACE);
		definition.setSchema(exchangeRateSchema);
		return definition;
	}

}
