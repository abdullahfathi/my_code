package ohs.math;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ohs.matrix.DenseVector;

public class ArrayUtils {

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void copy(double[] a, double[] b) {
		System.arraycopy(a, 0, b, 0, a.length);
	}

	public static void copy(double[] a, double[][] b) {
		if (sizeOfEntries(b) != a.length) {
			throw new IllegalArgumentException();
		}
	}

	public static void copy(double[] a, int[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = (int) a[i];
		}
	}

	public static void copy(double[][] a, double[] b) {
		if (sizeOfEntries(a) != b.length) {
			throw new IllegalArgumentException();
		}

		for (int i = 0, k = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[k++] = a[i][j];
			}
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void copy(double[][] a, double[][] b) {
		for (int i = 0; i < a.length; i++) {
			copy(a[i], b[i]);
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void copy(float[] a, float[] b) {
		System.arraycopy(a, 0, b, 0, a.length);
	}

	public static void copy(int[] a, int[] b) {
		System.arraycopy(a, 0, b, 0, a.length);
	}

	public static void copyAs(Collection<Double> x, double[] b) {
		int loc = 0;
		Iterator<Double> iter = x.iterator();
		while (iter.hasNext()) {
			b[loc++] = iter.next();
		}
	}

	public static void copyAs(Collection<Float> a, float[] b) {
		int loc = 0;
		Iterator<Float> iter = a.iterator();
		while (iter.hasNext()) {
			b[loc++] = iter.next();
		}
	}

	public static void copyAs(Collection<Integer> a, int[] b) {
		if (a.size() > 0) {
			int loc = 0;
			Iterator<Integer> iter = a.iterator();
			while (iter.hasNext()) {
				b[loc++] = iter.next();
			}
		}
	}

	public static void copyAs(double[] a, List<Double> b) {
		for (int i = 0; i < a.length; i++) {
			b.add(a[i]);
		}
	}

	public static void copyAs(double[][] a, int[][] b) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[i][j] = (int) a[i][j];
			}
		}
	}

	public static double[] copyAs(int[] a) {
		double[] ret = new double[a.length];
		copyAs(a, ret);
		return ret;
	}

	public static void copyAs(int[] a, double[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
		}
	}

	public static void copyAs(int[] a, Integer[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = new Integer(a[i]);
		}
	}

	public static void copyAs(int[] a, List<Integer> b) {
		for (int i = 0; i < a.length; i++) {
			b.add(a[i]);
		}
	}

	public static void copyAs(int[] a, Set<Integer> b) {
		for (int value : a) {
			b.add(value);
		}
	}

	public static void copyAs(int[][] a, double[][] b) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[i][j] = a[i][j];
			}
		}
	}

	public static void copyAs(Integer[] a, int[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i].intValue();
		}
	}

	public static void copyColumn(double[] a, double[][] b, int b_col) {
		int[] dims = dimensions(b);

		for (int i = 0; i < dims[0]; i++) {
			b[i][b_col] = a[i];
		}
	}

	/**
	 * @param a
	 * @param a_col
	 * @param b
	 *            output
	 */
	public static void copyColumn(double[][] a, int a_col, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i][a_col];
		}
	}

	/**
	 * @param a
	 *            input
	 * @param a_col
	 *            column index of matrix a
	 * @param b
	 *            output
	 * @param b_col
	 *            column index of matrix b
	 */
	public static void copyColumn(double[][] a, int a_col, double[][] b, int b_col) {
		if (!ArrayChecker.isSameDimensions(a, b)) {
			throw new IllegalArgumentException();
		}

		int rowDim = a.length;
		int colDim = a[0].length;
		for (int i = 0; i < rowDim; i++) {
			b[i][b_col] = a[i][a_col];
		}
	}

	public static double[] copyOut(double[] x) {
		double[] ret = new double[x.length];
		copy(x, ret);
		return ret;
	}

	/**
	 * @param a
	 *            input
	 * @param start
	 * @param end
	 * @return
	 */
	public static double[] copyOut(double[] a, int start, int end) {
		int size = end - start;
		double[] ret = new double[size];
		System.arraycopy(a, start, ret, 0, size);
		return ret;
	}

	public static double[][] copyOut(double[][] x) {
		double[][] ret = new double[x.length][];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = copyOut(x[i]);
		}
		return ret;
	}

	public static float[] copyOut(float[] x) {
		float[] ret = new float[x.length];
		copy(x, ret);
		return ret;
	}

	/**
	 * @param x
	 * @return
	 */
	public static int[] copyOut(int[] x) {
		int[] ret = new int[x.length];
		copy(x, ret);
		return ret;
	}

	public static void copyRow(double[] a, double[][] b, int b_row) {
		int colDim = b[0].length;
		for (int i = 0; i < colDim; i++) {
			b[b_row][i] = a[i];
		}
	}

	public static void copyRow(double[][] a, int a_row, double[] b) {
		int colDim = a[0].length;
		for (int i = 0; i < colDim; i++) {
			b[i] = a[a_row][i];
		}
	}

	public static void copyRow(double[][] a, int a_row, double[][] b, int b_row) {
		int colDim = a[0].length;
		for (int i = 0; i < colDim; i++) {
			b[b_row][i] = a[a_row][i];
		}
	}

	public static int[] dimensions(double[][] a) {
		int[] ret = new int[2];
		ret[0] = a.length;
		ret[1] = a[0].length;
		return ret;
	}

	public static NumberFormat getDoubleNumberFormat(int num_fractions) {
		NumberFormat ret = NumberFormat.getInstance();
		ret.setMinimumFractionDigits(num_fractions);
		ret.setGroupingUsed(false);
		return ret;
	}

	public static NumberFormat getIntegerNumberFormat() {
		NumberFormat ret = NumberFormat.getInstance();
		ret.setMinimumFractionDigits(0);
		ret.setGroupingUsed(false);
		return ret;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		{
			double[][] x = { { Double.NaN, Double.POSITIVE_INFINITY }, { Double.NEGATIVE_INFINITY, 10.25 } };

			System.out.println(toString(x));
		}

		System.out.println("process ends.");
	}

	public static int maxColumnSize(int[][] x) {
		int ret = 0;
		for (int i = 0; i < x.length; i++) {
			if (x[i].length > ret) {
				ret = x[i].length;
			}
		}
		return ret;
	}

	public static double[] newArray(double... array) {
		double[] ret = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			ret[i] = array[i];
		}
		return ret;
	}

	public static double[] newArray(int size, double init) {
		double[] ret = new double[size];
		if (init != 0) {
			setAll(ret, init);
		}
		return ret;
	}

	public static double[][] newDiagonalMatrix(double[] a) {
		double[][] ret = newMatrix(a.length, 0);
		for (int i = 0; i < a.length; i++) {
			ret[i][i] = a[i];
		}
		return ret;
	}

	public static double[][] newDiagonalMatrix(int size, double init) {
		double[][] ret = newMatrix(size, 0);
		for (int i = 0; i < ret.length; i++) {
			ret[i][i] = init;
		}
		return ret;
	}

	public static double[][] newIdentityMatrix(int size, double init) {
		double[][] ret = new double[size][size];
		for (int i = 0; i < size; i++) {
			ret[i][i] = init;
		}
		return ret;
	}

	public static double[][] newMatrix(int size, double init) {
		return newMatrix(size, size, init);
	}

	public static double[][] newMatrix(int rowSize, int colSize, double init) {
		double[][] ret = new double[rowSize][colSize];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = newArray(colSize, init);
		}
		return ret;
	}

	public static int[] nonzeroIndexes(double[] x) {
		List<Integer> set = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				set.add(i);
			}
		}

		int[] ret = new int[set.size()];
		copyAs(set, ret);
		return ret;
	}

	public static int[] nonzeroIndexes(int[] x) {
		List<Integer> set = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				set.add(i);
			}
		}
		int[] ret = new int[set.size()];
		copyAs(set, ret);
		return ret;
	}

	public static void reverse(int[] x) {
		int mid = x.length / 2;
		for (int i = 0; i < mid; i++) {
			swap(x, i, x.length - 1 - i);
		}
	}

	public static void setAll(double[] a, double value) {
		Arrays.fill(a, value);
	}

	public static int sizeOfEntries(double[][] a) {
		int[] dims = dimensions(a);
		return dims[0] * dims[1];
	}

	public static int sizeOfNonzero(double[] x) {
		int ret = 0;

		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				ret++;
			}
		}
		return ret;
	}

	public static int sizeOfNonzero(int[] x) {
		int ret = 0;
		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				ret++;
			}
		}
		return ret;
	}

	public static void sort(double[] a, boolean descending) {
		Arrays.sort(a);

		if (descending) {
			int middle = a.length / 2;
			for (int i = 0; i < middle; i++) {
				int j = a.length - 1 - i;
				swap(a, i, j);
			}
		}
	}

	public static List<Integer>[] split(List<Integer> indexList, int numFolds) {
		double[] proportions = new double[numFolds];
		for (int i = 0; i < proportions.length; i++) {
			proportions[i] = 1f / proportions.length;
		}
		return splitInOrder(indexList, proportions);
	}

	public static List<Integer>[] splitInOrder(List<Integer> indexList, double[] proportions) {
		DenseVector fold_prop = new DenseVector(copyOut(proportions));
		fold_prop.normalizeAfterSummation();

		DenseVector fold_maxIndex = fold_prop.copy();

		for (int i = 1; i < fold_maxIndex.size(); i++) {
			double prevValue = fold_maxIndex.value(i - 1);
			fold_maxIndex.increment(i, prevValue);
		}

		List<Integer>[] ret = new List[fold_prop.size()];

		for (int i = 0; i < 3; i++) {
			Collections.shuffle(indexList);
		}

		for (int i = 0; i < fold_maxIndex.size(); i++) {
			ret[i] = new ArrayList<Integer>();
			double value = fold_maxIndex.value(i);
			double maxIndex = Math.rint(value * indexList.size());
			fold_maxIndex.set(i, maxIndex);
		}

		for (int i = 0, j = 0; i < indexList.size(); i++) {
			// This gives a slight bias toward putting an extra instance in the
			// last InstanceList.

			double maxIndex = fold_maxIndex.value(j);

			if (i >= maxIndex && j < ret.length) {
				j++;
			}

			ret[j].add(indexList.get(i));
		}
		return ret;
	}

	public static void swap(double[] x, int index1, int index2) {
		double value1 = x[index1];
		double value2 = x[index2];
		x[index1] = value2;
		x[index2] = value1;
	}

	public static void swap(int[] x, int index1, int index2) {
		int value1 = x[index1];
		int value2 = x[index2];
		x[index1] = value2;
		x[index2] = value1;
	}

	public static void swapColumns(double[][] x, int index1, int index2) {
		for (int i = 0; i < x.length; i++) {
			double temp = x[i][index1];
			x[i][index1] = x[i][index2];
			x[i][index2] = temp;
		}
	}

	public static void swapRows(double[][] x, int index1, int index2) {
		for (int i = 0; i < x[0].length; i++) {
			double temp = x[index1][i];
			x[index1][i] = x[index2][i];
			x[index2][i] = temp;
		}
	}

	public static String toString(double[] x) {
		return toString(x, x.length, false, false, getDoubleNumberFormat(4));
	}

	public static String toString(double[] x, int num_print, boolean sparse, boolean vertical, NumberFormat nf) {
		StringBuffer sb = new StringBuffer();

		String delim = "\t";

		if (vertical) {
			delim = "\n";
		}

		if (sparse) {
			for (int i = 0; i < x.length && i < num_print; i++) {
				sb.append(String.format("%s%d:%s", delim, i, nf.format(x[i])));
			}
		} else {
			for (int i = 0; i < x.length && i < num_print; i++) {
				sb.append(String.format("%s%s", delim, nf.format(x[i])));
			}
		}
		return sb.toString().trim();
	}

	public static String toString(double[][] x) {
		int num_rows = x.length;
		int num_cols = x[0].length;
		boolean sparse = false;

		return toString(x, num_rows, num_cols, sparse, getDoubleNumberFormat(4));
	}

	public static String toString(double[][] x, int num_print_rows, int num_print_cols, boolean sparse, NumberFormat nf) {

		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[Row size\t%d]\n", x.length));
		sb.append(String.format("[Col size\t%d]\n", x[0].length));

		if (sparse) {
			for (int i = 0; i < x.length && i < num_print_rows; i++) {
				StringBuffer sb2 = new StringBuffer();
				sb2.append(i);

				int num_nonzero_print_cols = 0;

				for (int j = 0; j < x[i].length && num_nonzero_print_cols < num_print_cols; j++) {
					Double v = new Double(x[i][j]);
					if (v != 0) {
						if (Double.isFinite(v) || Double.isInfinite(v) || Double.isNaN(v)) {
							sb2.append(String.format("\t%d:%s", j, v.toString()));
							num_nonzero_print_cols++;
						} else {
							sb2.append(String.format("\t%d:%s", j, nf.format(v.doubleValue())));
							num_nonzero_print_cols++;
						}
					}
				}

				if (num_nonzero_print_cols > 0) {
					sb.append(sb2.toString());
					sb.append("\n");
				}
			}
		} else {
			sb.append("#");
			for (int i = 0; i < x[0].length && i < num_print_cols; i++) {
				sb.append(String.format("\t%d", i));
			}
			sb.append("\n");

			for (int i = 0; i < x.length && i < num_print_rows; i++) {
				sb.append(i);
				for (int j = 0; j < x[i].length && j < num_print_cols; j++) {
					Double v = new Double(x[i][j]);
					if (!Double.isFinite(v)) {
						sb.append(String.format("\t%s", v.toString()));
					} else {
						sb.append(String.format("\t%s", nf.format(v.doubleValue())));
					}
				}
				sb.append("\n");
			}
		}

		return sb.toString().trim();
	}

	public List<Integer>[] splitInOrder(List<Integer> indexList, int[] counts) {
		List<Integer>[] ret = new List[counts.length];
		int idx = 0;
		for (int num = 0; num < counts.length; num++) {
			ret[num] = new ArrayList<Integer>();
			for (int i = 0; i < counts[num]; i++) {
				ret[num].add(indexList.get(idx)); // Transfer weights?
				idx++;
			}
		}
		return ret;
	}
}
