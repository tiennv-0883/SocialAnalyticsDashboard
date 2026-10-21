package com.example.socialanalytics.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExchangeRateSoapTests {

	@LocalServerPort
	private int port;

	@Autowired
	private ExchangeRateClient client;

	@Autowired
	private ExchangeRateService service;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void clientCallsSoapEndpointOverHttp() {
		ExchangeRate rate = client.getExchangeRate("usd", "VND");

		assertThat(rate.from()).isEqualTo("USD");
		assertThat(rate.to()).isEqualTo("VND");
		assertThat(rate.rate()).isEqualByComparingTo("25350");
		assertThat(client.getExchangeRate("EUR", "USD").rate()).isEqualByComparingTo(new BigDecimal("1.086957"));
	}

	@Test
	void unknownCurrencyIsSoapClientFault() {
		assertThatThrownBy(() -> client.getExchangeRate("USD", "XYZ")).isInstanceOf(ExchangeRateException.class)
			.hasMessageContaining("Unknown currency: XYZ")
			.satisfies(ex -> assertThat(((ExchangeRateException) ex).isClientError()).isTrue());
	}

	@Test
	void serviceCachesRates() {
		ExchangeRate first = service.getRate("USD", "JPY");

		assertThat(service.getRate("usd", "jpy")).isSameAs(first);
	}

	@Test
	void wsdlIsPublished() throws Exception {
		HttpResponse<String> response = HttpClient.newHttpClient()
			.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/soap/exchangeRates.wsdl")).build(),
					HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("GetExchangeRate").contains("ExchangeRatesPort")
			.contains("http://localhost:" + port + "/soap");
	}

	@Test
	void restEndpointUsesSoapClient() throws Exception {
		mockMvc.perform(get("/exchange-rate").with(oauth2Login()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.from").value("USD"))
			.andExpect(jsonPath("$.to").value("VND"))
			.andExpect(jsonPath("$.rate").value(25350));
		mockMvc.perform(get("/exchange-rate").param("to", "ABC").with(oauth2Login()))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/exchange-rate")).andExpect(status().isUnauthorized());
	}

}
