package com.example.socialanalytics.soap;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExchangeRate(String from, String to, BigDecimal rate, LocalDate date, Instant fetchedAt) {
}
