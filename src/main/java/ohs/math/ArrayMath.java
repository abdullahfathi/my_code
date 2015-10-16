package ohs.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.stat.inference.TTestImpl;

/**
 * @author Heung-Seon Oh
 * 
 * 
 */
public class ArrayMath {
	public static final double LOGTOLERANCE = 30.0;

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void add(double[] a, double b, double[] c) {
		if (!ArrayChecker.isSameDimension(a, c)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] + b;
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void add(double[] a, double[] b, double[] c) {
		if (!ArrayChecker.isSameDimension(a, b, c)) {
			throw new IllegalArgumentException();
		}
		
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] + b[i];
		}
	}

	/**
	 * @param a
	 * @param b
	 * @param coef_a
	 * @param coef_b
	 * @param c
	 *            output
	 */
	public static void addAfterScale(double[] a, double b, double coef_a, double coef_b, double[] c) {
		if (!ArrayChecker.isSameDimension(a, c)) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < a.length; i++) {
			c[i] = coef_a * a[i] + coef_b * b;
		}
	}

	/**
	 * @param a
	 * @param b
	 * @param coef_a
	 * @param coef_b
	 * @param c
	 *            output
	 */
	public static void addAfterScale(double[] a, double[] b, double coef_a, double coef_b, double[] c) {
		if (!ArrayChecker.isSameDimension(a, b, c)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			c[i] = coef_a * a[i] + coef_b * b[i];
		}
	}

	public static int argMax(double[] x) {
		return argMax(x, 0, x.length);
	}

	public static int argMax(double[] x, int start, int end) {
		int ret = argMinMax(x, start, end)[1];
		return ret;
	}

	public static int[] argMax(double[][] x) {
		int[] ret = { -1, -1 };
		double max = -Double.MAX_VALUE;

		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[i].length; j++) {
				if (x[i][j] > max) {
					max = x[i][j];
					ret[0] = i;
					ret[1] = j;
				}
			}
		}
		return ret;
	}

	public static int argMax(int[] x) {
		int ret = -1;
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < x.length; i++) {
			if (x[i] > max) {
				ret = i;
				max = x[i];
			}
		}
		return ret;
	}

	public static int argMaxAtColumn(double[][] x, int j) {
		int ret = -1;
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < x.length; i++) {
			if (x[i][j] > max) {
				max = x[i][j];
				ret = i;
			}
		}
		return ret;
	}

	public static int argMaxAtRow(double[][] x, int i) {
		return argMax(x[i]);
	}

	public static int argMin(double[] x) {
		return argMin(x, 0, x.length);
	}

	public static int argMin(double[] a, int start, int end) {
		return argMinMax(a, start, end)[0];
	}

	public static int[] argMin(double[][] x) {
		int[] ret = { -1, -1 };
		double min = Double.MAX_VALUE;

		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[i].length; j++) {
				if (x[i][j] < min) {
					min = x[i][j];
					ret[0] = i;
					ret[1] = j;
				}
			}
		}
		return ret;
	}

	public static int argMin(int[] x) {
		return argMinMax(x)[0];
	}

	public static int argMinAtColumn(double[][] x, int j) {
		int ret = -1;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < x.length; i++) {
			if (x[i][j] < min) {
				min = x[i][j];
				ret = i;
			}
		}
		return ret;
	}

	public static int argMinAtRow(double[][] x, int i) {
		return argMin(x[i]);
	}

	public static int[] argMinMax(double[] a) {
		return argMinMax(a, 0, a.length);
	}

	public static int[] argMinMax(double[] a, int start, int end) {
		int min_i = -1;
		int max_i = -1;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;

		for (int i = start; i < a.length && i < end; i++) {
			if (a[i] < min) {
				min_i = i;
				min = a[i];
			}

			if (a[i] > max) {
				max_i = i;
				max = a[i];
			}
		}
		return new int[] { min_i, max_i };
	}

	public static int[] argMinMax(int[] a) {
		return argMinMax(a, 0, a.length);
	}

	public static int[] argMinMax(int[] a, int start, int end) {
		int min_i = -1;
		int max_i = -1;
		int min = Integer.MAX_VALUE;
		int max = -Integer.MAX_VALUE;

		for (int i = start; i < a.length && i < end; i++) {
			if (a[i] < min) {
				min_i = i;
				min = a[i];
			}

			if (a[i] > max) {
				max_i = i;
				max = a[i];
			}
		}
		return new int[] { min_i, max_i };
	}

	public static double[] basicStatistics(double[] x) {
		double[] ret = new double[3];
		double mean = mean(x);
		double variance = variance(x, mean);
		double stdDeviation = Math.sqrt(variance);
		ret[0] = mean;
		ret[1] = variance;
		ret[2] = stdDeviation;
		return ret;
	}

	public static int[] between(double[] x, double min, double max) {
		List<Integer> set = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			double value = x[i];
			if (value > min && value < max) {
				set.add(i);
			}
		}

		int[] ret = new int[set.size()];
		ArrayUtils.copyAs(set, ret);
		return ret;
	}

	public static double correlationKendall(double[] a, double[] b) {
		double numConcordant = 0;
		double numDiscordant = 0;

		for (int i = 0; i < a.length; i++) {
			double rank1 = a[i];
			double rank2 = b[i];

			if (rank1 == rank2) {
				numConcordant++;
			} else {
				numDiscordant++;
			}
		}

		double n = a.length;
		double ret = (numConcordant - numDiscordant) / (0.5 * n * (n - 1));
		return ret;
	}

	public static double correlationPearson(double[] a, double[] b) {
		double[] basicStats1 = basicStatistics(a);
		double[] basicStats2 = basicStatistics(b);
		double mean1 = basicStats1[0];
		double mean2 = basicStats2[0];
		double stdDeviation1 = basicStats1[2];
		double stdDeviation2 = basicStats2[2];
		double covariance = covariance(a, b, mean1, mean2);
		double ret = covariance / (stdDeviation1 * stdDeviation2);
		return ret;
	}

	public static double correlationSpearman(double[] a, double[] b) {
		double diffSum = 0;
		for (int i = 0; i < a.length; i++) {
			double rank1 = a[i];
			double rank2 = b[i];
			double diff = (rank1 - rank2);
			diffSum += diff * diff;
		}
		double n = a.length;
		double ret = 1 - (6 * diffSum) / (n * ((n * n) - 1));
		return ret;
	}

	public static double cosine(double dotProduct, double norm1, double norm2) {
		double ret = 0;
		if (norm1 > 0 && norm2 > 0) {
			ret = dotProduct / (norm1 * norm2);
		}

		if (ret > 1) {
			ret = 1;
		} else if (ret < 0) {
			ret = 0;
		}
		return ret;
	}

	public static double cosine(double[] a, double[] b) {
		double norm1 = 0;
		double norm2 = 0;
		double dotProduct = 0;

		for (int i = 0; i < a.length; i++) {
			dotProduct += a[i] * b[i];
			norm1 += a[i] * a[i];
			norm2 += b[i] * b[i];
		}

		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);

		return cosine(dotProduct, norm1, norm2);
	}

	public static double covariance(double[] a, double[] b) {
		double mean_of_a = mean(a);
		double mean_of_b = mean(b);
		return covariance(a, b, mean_of_a, mean_of_b);
	}

	public static double covariance(double[] a, double[] b, double mean_of_a, double mean_of_b) {
		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			ret += (a[i] - mean_of_a) * (b[i] - mean_of_b);
		}
		double n = a.length - 1; // unbiased estimator.
		ret /= n;
		return ret;
	}

	public static void cumulate(double[] in, double[] out) {
		double sum = 0;
		for (int i = 0; i < in.length; i++) {
			sum += in[i];
			out[i] = sum;
		}
	}

	public static void distribute(double[] in, double sum, double[] out) {
		normalize(in);
		for (int i = 0; i < in.length; i++) {
			out[i] = in[i] * sum;
		}
	}

	/**
	 * @param trans_probs
	 *            Column-normalized transition probabilities
	 * @param init_cents
	 * @param max_iter
	 * @param min_distance
	 * @param damping_factor
	 * @return
	 */
	public static double[] doRandomWalk(double[][] trans_probs, double[] init_cents, int max_iter, double min_distance,
			double damping_factor) {
		int num_docs = trans_probs.length;

		double[] cents = new double[num_docs];
		double uniform_cent = 1f / num_docs;

		double[] old_cents = ArrayUtils.copy(init_cents);
		double old_dist = Double.MAX_VALUE;

		for (int i = 0; i < max_iter; i++) {
			LA.product(trans_probs, old_cents, cents);

			for (int j = 0; j < cents.length; j++) {
				cents[j] = damping_factor * uniform_cent + (1 - damping_factor) * cents[j];
			}

			normalize(cents, cents);

			double dist = euclideanDistance(old_cents, cents);

			System.out.printf("%d: %s - %s = %s\n", i + 1, old_dist, dist, old_dist - dist);

			if (dist > old_dist || dist < min_distance) {
				break;
			}
			old_dist = dist;
			ArrayUtils.copy(cents, old_cents);
		}
		return cents;
	}

	public static double[] doRandomWalk(double[][] trans_probs, int max_iter, double min_distance, double damping_factor) {
		int num_docs = trans_probs.length;
		double[] init_cents = ArrayUtils.newArray(num_docs, 1f / num_docs);
		return doRandomWalk(trans_probs, init_cents, max_iter, min_distance, damping_factor);
	}

	public static double entropy(double[] a) {
		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i] > 0 && a[i] < 1) {
				ret += a[i] * FuncMath.log2(a[i]);
			}
		}
		return -ret;
	}

	public static double euclideanDistance(double[] a, double[] b) {
		return Math.sqrt(euclideanDistanceSquared(a, b));
	}

	public static double euclideanDistanceSquared(double[] a, double[] b) {
		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			double diff = a[i] - b[i];
			ret += diff * diff;
		}
		return ret;
	}

	/**
	 * @param a
	 *            input
	 * @param normalizeByMax
	 * @param b
	 *            output
	 */
	public static void exponentiate(double[] a, boolean normalizeByMax, double[] b) {

		if (normalizeByMax) {
			double max = max(a);
			double sum = 0;
			for (int i = 0; i < a.length; i++) {
				sum += (b[i] = Math.exp(a[i] - max));
			}
			scale(b, 1f / sum, b);
		} else {
			for (int i = 0; i < a.length; i++) {
				b[i] = Math.exp(a[i]);
			}
		}
	}

	public static double geometricMean(double[] x) {
		double logSum = 0;
		for (int i = 0; i < x.length; i++) {
			logSum += Math.log(x[i]);
		}
		return Math.exp(logSum / x.length);
	}

	public static double jensenShannonDivergence(double[] a, double[] b) {
		assert(a.length == b.length);
		double[] average = new double[a.length];
		for (int i = 0; i < a.length; ++i) {
			average[i] += (a[i] + b[i]) / 2;
		}
		return (KLDivergence(a, average) + KLDivergence(b, average)) / 2;
	}

	/**
	 * 
	 * See the example in table 8.2 in Introduction to Information Retrieval by Manning et al.
	 * 
	 * 
	 * @param judges1
	 * @param judges2
	 * @return
	 */
	public static double kappa(boolean[] judges1, boolean[] judges2) {

		// | Yes | No
		// -------------------------
		// Yes| |
		// --------------------------
		// No | |
		// --------------------------
		double[][] m = new double[2][2];

		for (int i = 0; i < judges1.length; i++) {
			boolean judge1 = judges1[i];
			boolean judge2 = judges2[i];

			if (judge1 && judge2) {
				m[0][0]++;
			} else if (!judge1 && !judge2) {
				m[1][1]++;
			} else if (judge1 && !judge2) {
				m[0][1]++;
			} else if (!judge1 && judge2) {
				m[1][0]++;
			}
		}

		return kappa(m);
	}

	public static double kappa(double[][] x) {
		double n = sum(x);
		double probA = (x[0][0] + x[1][1]) / n;
		double probNonrelevant = ((x[1][0] + x[1][1]) + (x[0][1] + x[1][1])) / (2 * n);
		double probRelevant = ((x[0][0] + x[0][1]) + (x[0][0] + x[1][0])) / (2 * n);
		double probE = probNonrelevant * probNonrelevant + probRelevant * probRelevant;
		double ret = (probA - probE) / (1 - probE);
		return ret;
	}

	/**
	 * Maths.java in mallet
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static double KLDivergence(double[] a, double[] b) {
		double ret = 0;

		for (int i = 0; i < a.length; ++i) {
			if (a[i] == 0) {
				continue;
			}
			if (b[i] == 0) {
				return Double.POSITIVE_INFINITY;
			}
			ret += a[i] * Math.log(a[i] / b[i]);
		}
		return ret * FuncMath.LOG_2_OF_E; // moved this division out of the
											// loop
											// -DM
	}

	/**
	 * http://introcs.cs.princeton.edu/java/97data/LinearRegression.java.html
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static double[] linearRegression(double[] x, double[] y) {
		int n = x.length;

		// first pass: read in data, compute xbar and ybar
		double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;

		for (int i = 0; i < n; i++) {
			sumx += x[i];
			sumx2 += x[i] * x[i];
			sumy += y[i];
		}

		double xbar = sumx / n;
		double ybar = sumy / n;

		// second pass: compute summary statistics
		double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
		for (int i = 0; i < n; i++) {
			xxbar += (x[i] - xbar) * (x[i] - xbar);
			yybar += (y[i] - ybar) * (y[i] - ybar);
			xybar += (x[i] - xbar) * (y[i] - ybar);
		}
		double beta1 = xybar / xxbar;
		double beta0 = ybar - beta1 * xbar;

		// print results
		System.out.println("y   = " + beta1 + " * x + " + beta0);

		boolean print = false;

		if (print) {
			// analyze results
			int df = n - 2;
			double rss = 0.0; // residual sum of squares
			double ssr = 0.0; // regression sum of squares
			for (int i = 0; i < n; i++) {
				double fit = beta1 * x[i] + beta0;
				rss += (fit - y[i]) * (fit - y[i]);
				ssr += (fit - ybar) * (fit - ybar);
			}
			double R2 = ssr / yybar;
			double svar = rss / df;
			double svar1 = svar / xxbar;
			double svar0 = svar / n + xbar * xbar * svar1;
			System.out.println("R^2                 = " + R2);
			System.out.println("std error of beta_1 = " + Math.sqrt(svar1));
			System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
			svar0 = svar * sumx2 / (n * xxbar);
			System.out.println("std error of beta_0 = " + Math.sqrt(svar0));

			System.out.println("SSTO = " + yybar);
			System.out.println("SSE  = " + rss);
			System.out.println("SSR  = " + ssr);
		}

		double[] ret = new double[2];
		ret[0] = beta0;
		ret[1] = beta1;
		return ret;
	}

	public static double[] log(double[] a) {
		double[] ret = new double[a.length];
		log(a, ret);
		return ret;
	}

	/**
	 * @param a
	 * @param b
	 *            output
	 */
	public static void log(double[] a, double[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = Math.log(a[i]);
		}
	}

	public static double logSum(double[] x) {
		double ret = 0;
		for (int i = 0; i < x.length; i++) {
			if (x[i] > 0) {
				ret += Math.log(x[i]);
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		{
			double[] probs = new double[100];
			int[] samples = new int[probs.length];

			random(probs, 0, 1);

			cumulate(probs, probs);

			sample(probs, samples);

			System.out.println();
		}

		{
			double[] x1 = { 3, 4, 5, 6, 7 };
			double[] x2 = { 3, 4, 5, 6, 7 };

			System.out.printf("-> %s\n", correlationPearson(x1, x2));
		}

		{
			double[] x1 = { 1, 2, 3, 4, 5 };
			double[] x2 = { 5, 4, 3, 2, 1 };
			System.out.printf("-> %s\n", correlationSpearman(x1, x2));
		}

		{
			double[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
			double[] x2 = { 1, 6, 8, 7, 10, 9, 3, 5, 2, 4 };
			System.out.printf("-> %s\n", correlationPearson(x1, x2));
		}

		{
			double[] x = { 1, 1 };
			normalize(x);

			System.out.println(entropy(x));
		}

		{
			double[][] m = { { 20, 5 }, { 10, 15 } };
			System.out.println(kappa(m));
		}

		{
			double[][] m = { { 300, 20 }, { 10, 70 } };
			System.out.println(kappa(m));
		}

		{
			double[][] m = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

			System.out.println(ArrayUtils.toString(sumColumns(m)));
		}

		{
			double[][] m = { { 1, 1, 0 }, { 1, 0, 0 }, { 0, 1, 1 } };
			normalizeColumns(m);

			System.out.println(ArrayUtils.toString(m));

			double[] b = doRandomWalk(m, 100, 0.0000001, 0);

			System.out.println(ArrayUtils.toString(b));
		}

		{
			double[] vs = new double[3];

			for (int i = 0; i < vs.length; i++) {
				vs[i] = i + 1;
			}

			normalize(vs);

			System.out.println(product(vs));

			double[] log_vs = log(vs);

			double log_sum1 = sum(log_vs);
			System.out.println(Math.exp(log_sum1));
			System.out.println(sumLogProb(log_vs));

			double log_sum2 = sumLogProb2(log_vs);
			// System.out.println(sum(vs));
			// System.out.println(sumLogProb(vs));
			System.out.println(Math.exp(log_sum2));
		}

		{
			TTestImpl tt = new TTestImpl();

			double[] x1 = { 1, 2, 3, 4, 5 };
			double[] x2 = { 1, 2, 4, 4, 2 };

			double[] stats1 = basicStatistics(x1);
			double[] stats2 = basicStatistics(x2);

			System.out.println(ArrayUtils.toString(stats1));
			System.out.println(ArrayUtils.toString(stats2));

			System.out.println(correlationPearson(x1, x2));

			double p = tt.pairedTTest(x1, x2);

			System.out.println(p);
		}

		{
			double[] a = new double[] { 1, 1, 1, 1 };
			double[] b = new double[] { 1, 1, 1, 0.25 };

			normalize(a);
			normalize(b);

			System.out.println(ArrayUtils.toString(a));
			System.out.println(ArrayUtils.toString(b));

		}

		{
			double num_words = 6056;
			double num_pages = num_words / 275;
			double max_cost_for_page = 13900;
			double min_cost_for_page = 9000;
			double max_cost_for_paper = num_pages * max_cost_for_page;
			double min_cost_for_paper = num_pages * min_cost_for_page;

			System.out.println(max_cost_for_paper);
			System.out.println(min_cost_for_paper);
		}

		{
			double[] a = { 1, 2, 3, 4, 5, 6 };
			double[] b = { 1, 2, 3, 4, 5, 6 };
			// b = new double[] { 6, 2, 3, 4, 5, 6 };

			normalize(a);
			normalize(b);

			double div_sum = 0;

			for (int i = 0; i < a.length; i++) {
				double v = a[i] * Math.log(a[i] / b[i]);
				div_sum += v;
			}

			double approx_prob = Math.exp(-div_sum);

			System.out.println(approx_prob);
		}

		System.out.println("process ends.");
	}

	public static double max(double[] x) {
		return x[argMax(x)];
	}

	public static double max(double[][] x) {
		int[] index = argMax(x);
		int row = index[0];
		int col = index[1];

		double ret = 0;
		if (row > 0 && col > 0) {
			ret = x[row][col];
		}
		return ret;
	}

	public static int max(int[] x) {
		return x[argMax(x)];
	}

	public static double maxAtColumn(double[][] x, int j) {
		return x[argMaxAtColumn(x, j)][j];
	}

	public static double maxAtRow(double[][] x, int i) {
		return x[i][argMaxAtRow(x, i)];
	}

	public static double mean(double[] x) {
		return sum(x) / x.length;
	}

	public static double min(double[] x) {
		return x[argMin(x, 0, x.length)];
	}

	public static double min(double[] x, int start, int end) {
		return x[argMin(x, start, end)];
	}

	public static double min(double[][] x) {
		int[] index = argMin(x);
		int row = index[0];
		int col = index[1];

		double ret = 0;
		if (row > 0 && col > 0) {
			ret = x[row][col];
		}
		return ret;
	}

	public static int min(int[] x) {
		return x[argMin(x)];
	}

	public static double minAtColumn(double[][] x, int j) {
		return x[argMinAtRow(x, j)][j];
	}

	public static double minAtRow(double[][] x, int i) {
		return x[i][argMinAtColumn(x, i)];
	}

	public static double[] minMax(double[] a) {
		int[] index = argMinMax(a);
		return new double[] { a[index[0]], a[index[1]] };
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void multiply(double[] a, double[] b, double[] c) {
		if (!ArrayChecker.isSameDimension(a, b, c)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] * b[i];
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void multiply(double[][] a, double[][] b, double[][] c) {
		if (!ArrayChecker.isSameDimensions(a, b, c)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				c[i][j] = a[i][j] * b[i][j];
			}
		}
	}

	/**
	 * @param a
	 *            input / output
	 */
	public static void normalize(double[] a) {
		normalize(a, a);
	}

	/**
	 * @param a
	 *            input / output
	 * @param high
	 * @param low
	 */
	public static void normalize(double[] a, double high, double low) {
		double[] minMax = minMax(a);
		double min = minMax[0];
		double max = minMax[1];

		for (int i = 0; i < a.length; i++) {
			double value = a[i];
			double newValue = low + ((high - low) / (max - min)) * (value - min);
			a[i] = newValue;
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void normalize(double[] a, double[] b) {
		scale(a, 1f / sum(a), b);
	}

	public static void normalizeByL2Norm(double[] a, double[] b) {
		scale(a, 1f / normL2(a), b);
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void normalizeBySigmoid(double[] a, double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = FuncMath.sigmoid(a[i]);
			sum += b[i];
		}
		scale(b, 1f / sum, b);
	}

	/**
	 * @param a
	 *            input / output
	 */
	public static void normalizeColumns(double[][] a) {
		double[] columnSums = sumColumns(a);

		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				double sum = columnSums[j];
				if (sum > 0) {
					a[i][j] /= sum;
				}
			}
		}
	}

	/**
	 * SloppyMath.java in Stanford
	 * 
	 * @param x
	 *            log values
	 */
	public static void normalizeLogProbs(double[] x) {
		double logSum = sumLogProb(x);
		if (Double.isNaN(logSum)) {
			throw new RuntimeException("Bad log-sum");
		}
		if (logSum == 0.0)
			return;
		for (int i = 0; i < x.length; i++) {
			x[i] -= logSum;
		}
	}

	public static void normalizeRows(double[][] a) {
		for (int i = 0; i < a.length; i++) {
			normalize(a[i]);
		}
	}

	/**
	 * 
	 * [Definition]
	 * 
	 * The length ( or norm ) of v is the nonnegative scalar |v| defined by
	 * 
	 * |v| = Sqrt(InnerProduct(v,v))
	 * 
	 * 
	 * @param x
	 * @return
	 */
	public static double normL2(double[] x) {
		return Math.sqrt(LA.dotProduct(x, x));
	}

	public static int[] over(double[] x, double cutoff, boolean includeCutoff) {
		List<Integer> set = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			double value = x[i];

			if (value > cutoff) {
				set.add(i);
			}

			if (includeCutoff && value == cutoff) {
				set.add(i);
			}
		}

		int[] ret = new int[set.size()];
		ArrayUtils.copyAs(set, ret);
		return ret;
	}

	public static double product(double[] a) {
		double ret = 1;
		for (int i = 0; i < a.length; i++) {
			ret *= a[i];
		}
		return ret;
	}

	public static void random(double[] x, double min, double max) {
		Random random = new Random();
		double range = max - min;

		for (int i = 0; i < x.length; i++) {
			x[i] = range * random.nextDouble() + min;
		}
	}

	public static void random(double[][] x, double min, double max) {
		for (int i = 0; i < x.length; i++) {
			random(x[i], min, max);
		}
	}

	public static void random(int[] x, int min, int max) {
		Random random = new Random();
		double range = max - min + 1;
		for (int i = 0; i < x.length; i++) {
			x[i] = (int) (range * random.nextDouble()) + min;
		}
	}

	public static void sample(double[] cumulated, int[] samples) {
		Random random = new Random();
		double sum = cumulated[cumulated.length - 1];

		for (int i = 0; i < samples.length; i++) {
			double rv = sum * random.nextDouble();
			for (int j = 0; j < cumulated.length; j++) {
				if (rv <= cumulated[j]) {
					samples[i] = j;
					break;
				}
			}
		}
	}

	public static void sample(int[] indexes, double[] values, int[] samples, boolean cumulate) {
		if (!ArrayChecker.isSameDimension(indexes, values) || !ArrayChecker.isSameDimension(indexes, samples)) {
			throw new IllegalArgumentException();
		}

		double[] temp = values;

		if (cumulate) {
			temp = new double[values.length];
			double sum = 0;
			for (int i = 0; i < values.length; i++) {
				sum += values[i];
				temp[i] = sum;
			}
		}

		Random random = new Random();
		double sum = temp[temp.length - 1];

		for (int i = 0; i < samples.length; i++) {
			double rv = sum * random.nextDouble();
			for (int j = 0; j < temp.length; j++) {
				if (rv <= temp[j]) {
					samples[i] = indexes[i];
					break;
				}
			}
		}
	}

	/**
	 * @param a
	 *            input
	 * @param coef
	 * @return
	 */
	public static double[] scale(double[] a, double coef) {
		double[] ret = new double[a.length];
		scale(a, coef, ret);
		return ret;
	}

	/**
	 * @param a
	 *            input
	 * @param coef
	 * @param b
	 *            output
	 */
	public static void scale(double[] a, double coef, double[] b) {
		if (!ArrayChecker.isSameDimension(a, b)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			b[i] = a[i] * coef;
		}
	}

	public static void simpleLinearRegression() {
		int MAXN = 1000;
		int n = 0;
		double[] x = new double[MAXN];
		double[] y = new double[MAXN];

		// first pass: read in data, compute xbar and ybar
		double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
		// while (!StdIn.isEmpty()) {
		// x[n] = StdIn.readDouble();
		// y[n] = StdIn.readDouble();
		sumx += x[n];
		sumx2 += x[n] * x[n];
		sumy += y[n];
		n++;
		// }
		double xbar = sumx / n;
		double ybar = sumy / n;

		// second pass: compute summary statistics
		double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
		for (int i = 0; i < n; i++) {
			xxbar += (x[i] - xbar) * (x[i] - xbar);
			yybar += (y[i] - ybar) * (y[i] - ybar);
			xybar += (x[i] - xbar) * (y[i] - ybar);
		}
		double beta1 = xybar / xxbar;
		double beta0 = ybar - beta1 * xbar;

		// print results
		System.out.println("y   = " + beta1 + " * x + " + beta0);

		// analyze results
		int df = n - 2;
		double rss = 0.0; // residual sum of squares
		double ssr = 0.0; // regression sum of squares
		for (int i = 0; i < n; i++) {
			double fit = beta1 * x[i] + beta0;
			rss += (fit - y[i]) * (fit - y[i]);
			ssr += (fit - ybar) * (fit - ybar);
		}
		double R2 = ssr / yybar;
		double svar = rss / df;
		double svar1 = svar / xxbar;
		double svar0 = svar / n + xbar * xbar * svar1;
		System.out.println("R^2                 = " + R2);
		System.out.println("std error of beta_1 = " + Math.sqrt(svar1));
		System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
		svar0 = svar * sumx2 / (n * xxbar);
		System.out.println("std error of beta_0 = " + Math.sqrt(svar0));

		System.out.println("SSTO = " + yybar);
		System.out.println("SSE  = " + rss);
		System.out.println("SSR  = " + ssr);
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void substract(double[] a, double[] b, double[] c) {

		if (!ArrayChecker.isSameDimension(a, b, c)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] - b[i];
		}
	}

	/**
	 * Maths.java in mallet
	 * 
	 * Returns the difference of two doubles expressed in log space, that is,
	 * 
	 * <pre>
	 *    sumLogProb = log (e^a - e^b)
	 *               = log e^a(1 - e^(b-a))
	 *               = a + log (1 - e^(b-a))
	 * </pre>
	 * 
	 * By exponentiating <tt>b-a</tt>, we obtain better numerical precision than we would if we calculated <tt>e^a</tt> or <tt>e^b</tt>
	 * directly.
	 * <p>
	 * Returns <tt>NaN</tt> if b > a (so that log(e^a - e^b) is undefined).
	 */
	public static double subtractLogProb(double a, double b) {
		if (b == Double.NEGATIVE_INFINITY)
			return a;
		else
			return a + Math.log(1 - Math.exp(b - a));
	}

	public static double sum(double[] x) {
		double ret = 0;
		for (int i = 0; i < x.length; i++) {
			ret += x[i];
		}
		return ret;
	}

	public static double sum(double[][] x) {
		double ret = 0;
		for (int i = 0; i < x.length; i++) {
			ret += sum(x[i]);
		}
		return ret;
	}

	public static double[] sumColumns(double[][] a) {
		double[] ret = new double[a[0].length];
		sumColumns(a, ret);
		return ret;
	}

	/**
	 * @param a
	 * @param b
	 *            output
	 */
	public static void sumColumns(double[][] a, double[] b) {
		if (!ArrayChecker.isSameColumnDimension(a, b)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[j] += a[i][j];
			}
		}
	}

	/**
	 * Maths.java in mallet
	 * 
	 * Returns the sum of two doubles expressed in log space, that is,
	 * 
	 * <pre>
	 *    sumLogProb = log (e^a + e^b)
	 *               = log e^a(1 + e^(b-a))
	 *               = a + log (1 + e^(b-a))
	 * </pre>
	 * 
	 * By exponentiating <tt>b-a</tt>, we obtain better numerical precision than we would if we calculated <tt>e^a</tt> or <tt>e^b</tt>
	 * directly.
	 * <P>
	 * Note: This function is just like {@link cc.mallet.fst.Transducer#sumNegLogProb sumNegLogProb} in <TT>Transducer</TT>, except that the
	 * logs aren't negated.
	 */
	public static double sumLogProb(double a, double b) {
		if (a == Double.NEGATIVE_INFINITY)
			return b;
		else if (b == Double.NEGATIVE_INFINITY)
			return a;
		else if (b < a)
			return a + Math.log(1 + Math.exp(b - a));
		else
			return b + Math.log(1 + Math.exp(a - b));
	}

	/**
	 * Below from Stanford NLP package, SloppyMath.java
	 * 
	 * Sums an array of numbers log(x1)...log(xn). This saves some of the unnecessary calls to Math.log in the two-argument version.
	 * <p>
	 * Note that this implementation IGNORES elements of the x array that are more than LOGTOLERANCE (currently 30.0) less than the maximum
	 * element.
	 * <p>
	 * Cursory testing makes me wonder if this is actually much faster than repeated use of the 2-argument version, however -cas.
	 * 
	 * @param x
	 *            An array log(x1), log(x2), ..., log(xn)
	 * @return log(x1+x2+...+xn)
	 */
	public static double sumLogProb(double[] x) {
		double max = Double.NEGATIVE_INFINITY;
		int len = x.length;
		int maxIndex = 0;

		for (int i = 0; i < len; i++) {
			if (x[i] > max) {
				max = x[i];
				maxIndex = i;
			}
		}

		boolean anyAdded = false;
		double intermediate = 0.0;
		double cutoff = max - LOGTOLERANCE;

		for (int i = 0; i < maxIndex; i++) {
			if (x[i] >= cutoff) {
				anyAdded = true;
				intermediate += Math.exp(x[i] - max);
			}
		}
		for (int i = maxIndex + 1; i < len; i++) {
			if (x[i] >= cutoff) {
				anyAdded = true;
				intermediate += Math.exp(x[i] - max);
			}
		}

		if (anyAdded) {
			return max + Math.log(1.0 + intermediate);
		} else {
			return max;
		}
	}

	/**
	 * 
	 * http://lingpipe-blog.com/category/lingpipe-news/page/4/
	 * 
	 * @param a
	 * @return
	 */
	public static double sumLogProb2(double[] a) {
		double ret = 0;
		double max = max(a);
		double sum = 0;

		for (int i = 0; i < a.length; ++i)
			if (a[i] != Double.NEGATIVE_INFINITY)
				sum += java.lang.Math.exp(a[i] - max);
		double logSum = Math.log(sum);
		return max + logSum;
	}

	public static double[] sumRows(double[][] a) {
		double[] ret = new double[a.length];
		sumRows(a, ret);
		return ret;
	}

	/**
	 * @param a
	 * @param b
	 *            output
	 */
	public static void sumRows(double[][] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			b[i] = sum(a[i]);
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void unitVector(double[] a, double[] b) {
		scale(a, 1f / normL2(a), b);
	}

	public static double variance(double[] x) {
		return variance(x, mean(x));
	}

	public static double variance(double[] x, double mean) {
		double ret = 0;
		for (int i = 0; i < x.length; i++) {
			double diff = x[i] - mean;
			ret += diff * diff;
		}
		double n = x.length - 1;// unbiased estimator.
		ret /= n;
		return ret;
	}

	public double sumAfterLogProb(double[] x) {
		return sumLogProb(log(x));
	}
}
