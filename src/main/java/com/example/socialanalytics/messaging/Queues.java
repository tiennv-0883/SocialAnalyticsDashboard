package com.example.socialanalytics.messaging;

public final class Queues {

	public static final String IMPORT_COMPLETED = "social.import.completed";

	public static final String IMPORT_COMPLETED_DLQ = IMPORT_COMPLETED + ".DLQ";

	private Queues() {
	}

}
