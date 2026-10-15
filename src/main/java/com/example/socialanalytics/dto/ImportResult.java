package com.example.socialanalytics.dto;

import java.util.List;

public record ImportResult(int totalRows, int imported, int failed, List<RowError> errors) {

	public record RowError(int row, String message) {
	}

}
