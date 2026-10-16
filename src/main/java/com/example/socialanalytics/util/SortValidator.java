package com.example.socialanalytics.util;

import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.example.socialanalytics.exception.InvalidSortException;

public final class SortValidator {

	private SortValidator() {
	}

	public static Pageable validate(Pageable pageable, Set<String> allowedProperties) {
		pageable.getSort().forEach(order -> {
			if (!allowedProperties.contains(order.getProperty())) {
				throw new InvalidSortException("Invalid sort property '" + order.getProperty()
						+ "'. Allowed: " + allowedProperties.stream().sorted().toList());
			}
		});
		return pageable;
	}

}
