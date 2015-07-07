package ohs.string.sim.func;

import java.text.NumberFormat;

import ohs.ling.struct.Token;
import ohs.math.ArrayUtils;
import ohs.types.common.IntPair;

/**
 * 
 * A modified version of MemoMatrix in SecondString
 * 
 * 
 * @author ohs
 */
public abstract class MemoMatrix {

	protected double[][] values;

	protected boolean[][] computed;

	protected String s;

	protected String t;

	protected IntPair indexAtBest;

	protected double best;

	MemoMatrix(String s, String t) {
		this.s = s;
		this.t = t;
		values = new double[s.length() + 1][t.length() + 1];
		computed = new boolean[s.length() + 1][t.length() + 1];
		indexAtBest = new IntPair(-1, -1);
		best = Double.NEGATIVE_INFINITY;
	}

	abstract double compute(int i, int j);

	public double get(int i, int j) {
		if (!computed[i][j]) {
			values[i][j] = compute(i, j);
			computed[i][j] = true;
		}
		return values[i][j];
	}

	public double getBestScore() {
		return get(indexAtBest.getFirst(), indexAtBest.getSecond());
	}

	public IntPair getIndexAtBest() {
		return indexAtBest;
	}

	String getSource() {
		return s;
	}

	String getTarget() {
		return t;
	}

	public void setIndexAtBest(IntPair indexAtBest) {
		this.indexAtBest = indexAtBest;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("<S/T>");
		for (int i = 0; i < t.length(); i++)
			sb.append("\t" + t.charAt(i));
		sb.append("\n");

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);

		for (int i = 1; i <= s.length(); i++) {
			sb.append(s.charAt(i - 1));
			for (int j = 1; j <= t.length(); j++) {
				double v = get(i, j);
				sb.append("\t" + nf.format(v));
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
