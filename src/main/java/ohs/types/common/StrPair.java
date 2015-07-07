package ohs.types.common;

import ohs.types.Pair;

public class StrPair extends Pair<String, String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7125783010793193515L;

	public StrPair(String first, String second) {
		super(first, second);
	}

	public StrPair(String[] pair) {
		super(pair[0], pair[1]);
	}
	
	

}
