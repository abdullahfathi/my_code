package ohs.string.sim.func;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.optimization.GoalType;
import org.hamcrest.core.Is;

import ohs.entity.DataReader;
import ohs.entity.ENTPath;
import ohs.entity.data.struct.BilingualText;
import ohs.math.ArrayMath;
import ohs.types.Counter;
import ohs.utils.KoreanUtils;

public class NeedlemanWunsch {

	private class ScoreMatrix extends MemoMatrix {
		public ScoreMatrix(String s, String t) {
			super(s, t);
			best = -Double.MAX_VALUE;
		}

		public double compute(int i, int j) {
			if (i == 0)
				return -j * gap_cost;
			if (j == 0)
				return -i * gap_cost;

			char si = getSource().charAt(i - 1);
			char tj = getTarget().charAt(j - 1);

			double cost = 0;

			if (si == tj) {
				cost = match_cost;
			} else {
				cost = unmatch_cost;
			}

			double substitute_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + gap_cost;
			double insert_score = get(i, j - 1) + gap_cost;
			double[] scores = new double[] { substitute_score, delete_score, insert_score };
			int index = ArrayMath.argMax(scores);
			double ret = scores[index];

			if (ret > best) {
				best = ret;
				indexAtBest.set(i, j);
			}
			return ret;
		}
	}

	public static void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "MCCOHN", "COHEN" };
		String[] strs = { "CXOHEN", "COHEN" };
		// String[] strs = { "부산대학교 고분자공학과", "부산대학교 병원" };
		// strs = new String[] { "국민은행", "국민대학교 금속재료공학부" };

		String s = strs[0];
		String t = strs[1];

		NeedlemanWunsch sw = new NeedlemanWunsch();

		MemoMatrix m = sw.compute(s, t);

		Aligner al = new Aligner();
		AlignResult ar = al.align(m);

		System.out.println();
		System.out.println(m.toString());
		// System.out.println();
		// System.out.println(ar);

		// AlignResult ar = new Aligner().align(m);

		// System.out.println(ar.toString());

	}

	private Counter<Character> chWeights;

	private double match_cost;

	private double unmatch_cost;

	private double gap_cost;

	private boolean ignoreGap;

	public NeedlemanWunsch() {
		this(1, 0, -1, false);
	}

	public NeedlemanWunsch(double match_cost, double unmatch_cost, double gap_cost, boolean ignoreGap) {
		this.match_cost = match_cost;
		this.unmatch_cost = unmatch_cost;
		this.gap_cost = gap_cost;
		this.ignoreGap = ignoreGap;
	}

	public MemoMatrix compute(String s, String t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	public double getBestScore(String s, String t) {
		return compute(s, t).getBestScore();
	}

	public double getNormalizedScore(String s, String t) {
		double score = getBestScore(s, t);
		double max_match_score = match_cost * Math.min(s.length(), t.length());
		double ret = score / max_match_score;
		return ret;
	}

	public void setChWeight(Counter<Character> chWeights) {
		this.chWeights = chWeights;
	}

	public String toString() {
		return "[Needleman Wunsch]";
	}

}
