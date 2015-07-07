package ohs.string.sim.func;

import ohs.ling.struct.Token;
import ohs.math.ArrayMath;
import ohs.types.common.IntPair;

public class SmithWaterman {
	private class ScoreMatrix extends MemoMatrix {
		public ScoreMatrix(String s, String t) {
			super(s, t);
			best = -Double.MAX_VALUE;
		}

		public double compute(int i, int j) {
			if (i == 0)
				return 0;
			if (j == 0)
				return 0;

			char si = getSource().charAt(i - 1);
			char tj = getTarget().charAt(j - 1);

			double cost = si == tj ? match_cost : unmatch_cost;
			double substitute_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + gap_cost;
			double insert_score = get(i, j - 1) + gap_cost;
			double[] scores = new double[] { 0, substitute_score, delete_score, insert_score };
			int index = ArrayMath.argMax(scores);
			double ret = scores[index];

			if (ret > best) {
				best = ret;
				indexAtBest.set(i, j);
			}
			return ret;
		}
	}

	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "MCCOHN", "COHEN" };
		// String[] strs = { "MCCOHN", "COHEN" };
		String[] strs = { "부산대학교 고분자공학과", "부산대학교 병원" };
		String s = strs[0];
		String t = strs[1];

		SmithWaterman sw = new SmithWaterman();
		MemoMatrix m = sw.compute(s, t);

		Aligner al = new Aligner();
		AlignResult ar = al.align(m);

		System.out.println(m);
		// System.out.println(m.getBestScore());
		System.out.println();
		System.out.println(ar);

		// AlignResult ar = new Aligner().align(m);

		// System.out.println(ar.toString());

	}

	private double match_cost;

	private double unmatch_cost;

	private double gap_cost;

	public SmithWaterman() {
		this(2, -1, -1);
	}

	public SmithWaterman(double match_cost, double unmatch_cost, double gap_cost) {
		this.match_cost = match_cost;
		this.unmatch_cost = unmatch_cost;
		this.gap_cost = gap_cost;
	}

	public MemoMatrix compute(String s, String t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	public double getNormalizedScore(String s, String t) {
		double score = getBestScore(s, t);
		double max_match_score = match_cost * Math.min(s.length(), t.length());
		double ret = score / max_match_score;
		return ret;
	}

	public double getBestScore(String s, String t) {
		return compute(s, t).getBestScore();
	}

	public String toString() {
		return "[Smith Waterman]";
	}

}
