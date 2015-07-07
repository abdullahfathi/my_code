package ohs.string.sim.func;

public enum MatchType {
	MATCH("="), UNMATCH("!="), SIMILAR(":=");

	private String symbol;

	private MatchType(String symbol) {
		this.symbol = symbol;
	}

	String getSymbol() {
		return symbol;
	}
}