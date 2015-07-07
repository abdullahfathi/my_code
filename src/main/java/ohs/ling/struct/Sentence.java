package ohs.ling.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sentence extends ArrayList<Token> {

	public String toString() {
		return toString(true);
	}

	public String toString(boolean vertical) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < size(); i++) {
			Token t = get(i);
			sb.append(i + "\t" + t.toString());
			if (i != size() - 1) {
				sb.append(vertical ? "\n" : " ");
			}
		}
		return sb.toString();
	}

}
