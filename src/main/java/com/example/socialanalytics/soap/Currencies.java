package com.example.socialanalytics.soap;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Currencies {

	private static final Pattern CODE = Pattern.compile("[A-Z]{3}");

	private Currencies() {
	}

	public static String normalize(String currency) {
		return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
	}

	public static boolean isValid(String normalized) {
		return CODE.matcher(normalized).matches();
	}

}
