package formbox.notifs;

public enum EmailStatus {
	SENT(1), DELIVERED(2), SOFT_BOUNCE(3), HARD_BOUNCE(4), MARKED_AS_SPAM(5);

	private final int precedence;

	EmailStatus(int precedence) {
		this.precedence = precedence;
	}

	public boolean isAfter(EmailStatus other) {
		if (other == null) return true;
		return this.precedence > other.precedence;
	}
}