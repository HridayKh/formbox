package formbox.notifs;

public enum EmailStatus {
	SENT(1), SOFT_BOUNCE(2), HARD_BOUNCE(3), DELIVERED(4), MARKED_AS_SPAM(5);

	private final int precedence;

	EmailStatus(int precedence) {
		this.precedence = precedence;
	}

	public boolean isAfter(EmailStatus other) {
		if (other == null) return true;
		return this.precedence > other.precedence;
	}
}