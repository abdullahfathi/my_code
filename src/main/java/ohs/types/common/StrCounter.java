package ohs.types.common;

import ohs.types.Counter;

public class StrCounter extends Counter<String> {

	public StrCounter(Counter<String> queryWordCounts) {
		super(queryWordCounts);
	}

	public StrCounter() {
		super();
	}

}
