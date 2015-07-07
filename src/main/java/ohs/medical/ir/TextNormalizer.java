package ohs.medical.ir;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextNormalizer {

	public Pattern p = Pattern.compile("\\d+(-| )(years|year|yo) [\\p{Alpha}\\s]+");

	public TextNormalizer() {

	}

	public String normalize(String s) {
		Matcher m = p.matcher(s);

		String[] lines = s.split("\\.");

		for (int i = 0; i < lines.length; i++) {
			if (m.find()) {
				// StringBuffer sb = new StringBuffer();
				do {
					String g = m.group();
					System.out.println(s);
					// m.appendReplacement(sb, r);
				} while (m.find());
				// m.appendTail(sb);
			}
		}
		return s;
	}

}
