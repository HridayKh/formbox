package formbox.submission.internal;

interface SubmissionMetrics {
	String ANY_SUBMISSION = "submissions.stats.anySubmission";
	String SUCCESSFUL = "submissions.stats.successfull";
	String PAYLOAD_FIELD_COUNT = "submissions.stats.payloadFieldCount";
	String PAYLOAD_SIZE_BYTES = "submissions.stats.payloadSizeBytes";

	interface Failed {
		String FORM_NOT_FOUND = "submissions.statsFailed.formNotFound";
		String RATE_LIMIT_PASSED = "submissions.statsFailed.rateLimitPassed";
		String OUT_OF_SUBMISSIONS = "submissions.statsFailed.outOfSubmissions";
		String JSON_NOT_ALLOWED = "submissions.statsFailed.jsonNotAllowed";
		String HONEYPOT = "submissions.statsFailed.honeypot";
		String TURNSTILE = "submissions.statsFailed.turnstile";
		String FILES_NOT_ALLOWED = "submissions.statsFailed.filesNotAllowed";
		String INVALID_MIME_TYPES = "submissions.statsFailed.invalidMimeTypes";
		String INVALID_FIELDS = "submissions.statsFailed.invalidFields";
		String PARTS_READ_ERROR = "submissions.statsFailed.partsReadError";
	}
}
