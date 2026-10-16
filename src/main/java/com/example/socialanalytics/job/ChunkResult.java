package com.example.socialanalytics.job;

import java.util.List;

public record ChunkResult(int updated, List<String> errors) {

	public int failed() {
		return errors.size();
	}

}
