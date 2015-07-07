package ohs.string.sim.search.ppss;

import java.io.Serializable;

import ohs.math.ArrayMath;
import ohs.string.sim.search.ppss.Gram.Type;

/**
 * A class of verifying grams outlined at Algorithm 4.
 * 
 * @author Heung-Seon Oh
 */
public class StringVerifier implements Serializable {

	private int q;

	private int tau;

	private double[][] M;

	private double num_errors;

	private double ed;

	public StringVerifier(int q, int tau) {
		this.q = q;
		this.tau = tau;
	}

	public double computeEditDistance(String s, String t, boolean findMinValue) {
		int n = s.length();
		int m = t.length();
		int ed[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		int min_i = 1;
		int min_j = 1;
		int min = Integer.MAX_VALUE;

		if (n == 0)
			return 1.0;
		if (m == 0)
			return 1.0;

		ed = new int[n + 1][m + 1];

		for (i = 0; i <= n; i++)
			ed[i][0] = i;

		for (j = 0; j <= m; j++)
			ed[0][j] = j;

		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);
				cost = (s_i == t_j) ? 0 : 1;
				ed[i][j] = ArrayMath.min(new int[] { ed[i - 1][j] + 1, ed[i][j - 1] + 1, ed[i - 1][j - 1] + cost });

				if (ed[i][j] < min) {
					min_i = i;
					min_j = j;
					min = ed[i][j];
				}
			}
		}

		int ret = findMinValue ? min : ed[n][m];

		// System.out.println(show(s, t, ed) + "\n\n");

		return ret;
	}

	public double getEditDistance() {
		return ed;
	}

	public double getNumErrors() {
		return num_errors;
	}

	public String show(String s, String t, int[][] d) {
		StringBuffer sb = new StringBuffer();
		sb.append("@");

		for (int i = 0; i < d[0].length; i++) {
			if (i == 0) {
				sb.append(" #");
			} else {
				sb.append(" " + t.charAt(i - 1));
			}
		}

		for (int i = 0; i < d.length; i++) {
			sb.append("\n");
			if (i == 0) {
				sb.append("#");
			} else {
				sb.append(s.charAt(i - 1));
			}

			for (int j = 0; j < d[i].length; j++) {
				sb.append(" " + (int) d[i][j]);
			}
		}

		return sb.toString();
	}

	public boolean verify(String s, Gram[] grams, String r) {
		num_errors = 0;
		ed = 0;

		for (int i = 0; i < grams.length; i++) {
			Gram gram = grams[i];
			if (gram.getType() != Type.PIVOT) {
				continue;
			}

			String g = gram.getString();

			int start = gram.getStart() - tau;
			int end = gram.getStart() + q - 1 + tau;

			if (start < 0) {
				start = gram.getStart();
			}

			if (end > r.length()) {
				end = r.length();
			}

			if (start >= end) {
				continue;
			}

			String sub_r = r.substring(start, end);
			num_errors += computeEditDistance(g, sub_r, true);

			if (num_errors > tau) {
				return false;
			}
		}

		ed = computeEditDistance(s, r, false);

		if (ed <= tau) {
			return true;
		} else {
			return false;
		}
	}
}
