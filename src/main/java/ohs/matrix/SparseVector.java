package ohs.matrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.IOUtils;
import ohs.math.ArrayUtils;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.ListMap;

/**
 * @author Heung-Seon Oh
 * 
 */
public class SparseVector implements Vector {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6671749703272005320L;

	public static SparseVector read(String fileName) throws Exception {
		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);
		SparseVector ret = readStream(ois);
		ois.close();
		return ret;
	}

	public static ListMap<Integer, SparseVector> readIndexedList(String fileName) throws Exception {
		ListMap<Integer, SparseVector> ret = new ListMap<Integer, SparseVector>();
		List<SparseVector> dataList = readList(fileName);
		for (int i = 0; i < dataList.size(); i++) {
			SparseVector vector = dataList.get(i);
			ret.put(vector.label(), vector);
		}
		return ret;
	}

	public static List<SparseVector> readList(ObjectInputStream ois) throws Exception {
		List<SparseVector> ret = new ArrayList<SparseVector>();
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			SparseVector vector = readStream(ois);
			ret.add(vector);
		}
		return ret;
	}

	public static List<SparseVector> readList(String fileName) throws Exception {
		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);
		List<SparseVector> ret = readList(ois);
		ois.close();
		System.out.printf("read [%d] vectors from [%s].\n", ret.size(), fileName);
		return ret;
	}

	public static ListMap<Integer, SparseVector> readMap(String fileName) throws Exception {
		ListMap<Integer, SparseVector> ret = new ListMap<Integer, SparseVector>();
		for (SparseVector vector : readList(fileName)) {
			ret.put(vector.label(), vector);
		}
		return ret;
	}

	public static SparseVector readStream(ObjectInputStream ois) throws IOException {
		int size = ois.readInt();
		int[] indexes = new int[size];
		double[] values = new double[size];
		int label = ois.readInt();
		int dim = ois.readInt();

		double sum = 0;
		for (int i = 0; i < size; i++) {
			int index = ois.readInt();
			double value = ois.readDouble();
			indexes[i] = index;
			values[i] = value;
			sum += value;
		}
		SparseVector ret = new SparseVector(indexes, values, label, dim);
		ret.setSum(sum);
		ret.sortByIndex();
		return ret;
	}

	public static void write(ObjectOutputStream oos, List<SparseVector> vectors) throws Exception {
		oos.writeInt(vectors.size());
		for (int i = 0; i < vectors.size(); i++) {
			SparseVector vector = vectors.get(i);
			vector.write(oos);
		}
	}

	public static void write(String fileName, List<SparseVector> vectors) throws Exception {
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);
		write(oos, vectors);
		oos.close();
		System.out.printf("write [%d] vectors to [%s].\n", vectors.size(), fileName);
	}

	private int[] indexes;

	private double[] values;

	private int label;

	private double sum;

	private int dim;

	public SparseVector(Counter<Integer> counter, int label, int dim) {
		indexes = new int[counter.size()];
		values = new double[counter.size()];
		int loc = 0;

		for (Entry<Integer, Double> entry : counter.entrySet()) {
			int index = entry.getKey();
			double value = entry.getValue();
			setAtLoc(loc++, index, value);
			sum += value;
		}
		sortByIndex();
		this.label = label;
		this.dim = dim;
	}

	public SparseVector(int size) {
		this(new int[size], new double[size], -1, size);
	}

	public SparseVector(int size, int label) {
		this(new int[size], new double[size], label, size);
	}

	public SparseVector(int[] indexes) {
		this(indexes, new double[indexes.length], -1, indexes.length);
	}

	public SparseVector(int[] indexes, double[] values, int label) {
		this(indexes, values, label, indexes.length);
	}

	public SparseVector(int[] indexes, double[] values, int label, int dim) {
		this.indexes = indexes;
		this.values = values;
		this.label = label;
		this.sum = 0;
		this.dim = dim;

		// sortByIndex();
	}

	public SparseVector(int[] indexes, int label) {
		this(indexes, new double[indexes.length], label, indexes.length);
	}

	public SparseVector(String svmFeatStr) {
		String[] parts = svmFeatStr.split("[\\s]+");
		label = Integer.parseInt(parts[0]);
		indexes = new int[parts.length - 1];
		values = new double[parts.length - 1];
		sum = 0;

		for (int i = 1; i < parts.length; i++) {
			String[] toks = parts[i].split(":");
			int index = Integer.parseInt(toks[0]);
			double value = Double.parseDouble(toks[1]);
			indexes[i - 1] = index;
			values[i - 1] = value;
			sum += value;
		}
		// sortByIndex();
	}

	public int argMax() {
		return indexAtLoc(argMaxLoc());
	}

	public int argMaxLoc() {
		int loc = -1;
		double max = -Double.MAX_VALUE;

		for (int i = 0; i < size(); i++) {
			double value = valueAtLoc(i);
			if (value > max) {
				max = value;
				loc = i;
			}
		}
		return loc;
	}

	public int argMin() {
		return indexAtLoc(argMinLoc());
	}

	public int argMinLoc() {
		int loc = -1;
		double min = Double.MAX_VALUE;

		for (int i = 0; i < size(); i++) {
			double value = valueAtLoc(i);
			if (value < min) {
				min = value;
				loc = i;
			}
		}
		return loc;
	}

	public SparseVector copy() {
		int[] newIndexes = ArrayUtils.copy(indexes);
		double[] newValues = ArrayUtils.copy(values);
		SparseVector ret = new SparseVector(newIndexes, newValues, label, dim);
		ret.setSum(sum);
		return ret;
	}

	public int[] copyIndexes() {
		return ArrayUtils.copy(indexes);
	}

	public double[] copyValues() {
		return ArrayUtils.copy(values);
	}

	public int dim() {
		return dim;
	}

	public void increment(int index, double value) {
		int loc = location(index);
		if (loc > -1) {
			values[loc] += value;
			sum += value;
		}
	}

	public void incrementAll(double value) {
		for (int i = 0; i < values.length; i++) {
			values[i] += value;
			sum += value;
		}
	}

	public void incrementAtLoc(int loc, double value) {
		values[loc] += value;
		sum += value;
	}

	public void incrementAtLoc(int loc, int index, double value) {
		indexes[loc] = index;
		values[loc] += value;
		sum += value;
	}

	public int indexAtLoc(int loc) {
		return indexes[loc];
	}

	public int[] indexes() {
		return indexes;
	}

	public void keepAbove(double cutoff) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();

		for (int i = 0; i < values.length; i++) {
			if (values[i] < cutoff) {
				continue;
			}
			indexList.add(indexes[i]);
			valueList.add(values[i]);
		}

		indexes = new int[indexList.size()];
		values = new double[indexList.size()];
		sum = 0;
		for (int i = 0; i < indexList.size(); i++) {
			indexes[i] = indexList.get(i);
			values[i] = valueList.get(i);
			sum += values[i];
		}
	}

	public void keepTopN(int topN) {
		if (values.length > topN) {
			sortByValue();
			int[] newIndexes = new int[topN];
			double[] newValues = new double[topN];

			sum = 0;
			for (int i = 0; i < topN; i++) {
				newIndexes[i] = indexes[i];
				newValues[i] = values[i];
				sum += newValues[i];
			}

			indexes = newIndexes;
			values = newValues;
			sortByIndex();
		}
	}

	public int label() {
		return label;
	}

	/**
	 * 
	 * it should be called after calling sortByIndex
	 * 
	 * @param index
	 * @return
	 */
	public int location(int index) {
		return Arrays.binarySearch(indexes, index);
	}

	public double max() {
		return valueAtLoc(argMaxLoc());
	}

	public double min() {
		return valueAtLoc(argMinLoc());
	}

	public void normalize() {
		double new_sum = 0;
		for (int i = 0; i < values.length; i++) {
			values[i] /= sum;
			new_sum += values[i];
		}
		sum = new_sum;
	}

	public void normalizeAfterSummation() {
		summation();
		normalize();
	}

	public void normalizeByL2Norm() {
		double norm = 0;
		for (int i = 0; i < values.length; i++) {
			norm += (values[i] * values[i]);
		}
		norm = Math.sqrt(norm);
		scale(1f / norm);
	}

	public double prob(int index) {
		double ret = 0;
		int loc = location(index);
		if (loc > -1) {
			throw new IllegalArgumentException("not found");
		}
		return ret;
	}

	public double probAlways(int index) {
		double ret = 0;
		int loc = location(index);
		if (loc > -1) {
			ret = probAtLoc(loc);
		}
		return ret;
	}

	public double probAtLoc(int loc) {
		return values[loc] / sum;
	}

	public void prune(final Set<Integer> toRemove) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();
		sum = 0;
		for (int i = 0; i < size(); i++) {
			int index = indexAtLoc(i);
			double value = valueAtLoc(i);

			if (toRemove.contains(index)) {
				continue;
			}
			indexList.add(index);
			valueList.add(value);
			sum += value;
		}

		indexes = new int[indexList.size()];
		values = new double[valueList.size()];

		ArrayUtils.copyAs(indexList, indexes);
		ArrayUtils.copyAs(valueList, values);
	}

	public void pruneExcept(final Set<Integer> toKeep) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();
		sum = 0;

		for (int i = 0; i < size(); i++) {
			int index = indexAtLoc(i);
			double value = valueAtLoc(i);

			if (!toKeep.contains(index)) {
				continue;
			}
			indexList.add(index);
			valueList.add(value);
			sum += value;
		}

		indexes = new int[indexList.size()];
		values = new double[valueList.size()];

		ArrayUtils.copyAs(indexList, indexes);
		ArrayUtils.copyAs(valueList, values);
	}

	private int qPartition(int low, int high, boolean sortByIndex) {
		// First element
		// int pivot = a[low];

		// Middle element
		// int middle = (low + high) / 2;

		int i = low - 1;
		int j = high + 1;

		if (sortByIndex) {
			// ascending order
			int randomIndex = (int) (Math.random() * (high - low)) + low;
			int pivotValue = indexes[randomIndex];

			while (i < j) {
				i++;
				while (indexes[i] < pivotValue) {
					i++;
				}

				j--;
				while (indexes[j] > pivotValue) {
					j--;
				}

				if (i < j) {
					swap(i, j);
				}
			}
		} else {
			// descending order
			int randomIndex = (int) (Math.random() * (high - low)) + low;
			double pivotValue = values[randomIndex];

			while (i < j) {
				i++;
				while (values[i] > pivotValue) {
					i++;
				}

				j--;
				while (values[j] < pivotValue) {
					j--;
				}

				if (i < j) {
					swap(i, j);
				}
			}
		}
		return j;
	}

	private void qSort(int low, int high, boolean sortByIndex) {
		if (low >= high)
			return;
		int p = qPartition(low, high, sortByIndex);
		qSort(low, p, sortByIndex);
		qSort(p + 1, high, sortByIndex);
	}

	private void quicksort(boolean sortByIndex) {
		qSort(0, indexes.length - 1, sortByIndex);
	}

	public int[] rankedIndexes() {
		sortByValue();
		int[] ret = new int[size()];
		for (int i = 0; i < size(); i++) {
			ret[i] = indexAtLoc(i);
		}
		sortByIndex();
		return ret;
	}

	public SparseVector ranking() {
		return ranking(false);
	}

	public SparseVector ranking(boolean ascending) {
		SparseVector ret = copy();
		ret.sortByValue();

		for (int i = 0; i < ret.size(); i++) {
			int index = ret.indexAtLoc(i);
			double rank = i + 1;
			if (ascending) {
				rank = ret.size() - i;
			}
			ret.setAtLoc(i, index, rank);
		}
		ret.sortByIndex();
		return ret;
	}

	public void removeZeros() {
		sum = 0;
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();

		for (int i = 0; i < indexes.length; i++) {
			int index = indexes[i];
			double value = values[i];
			if (value == 0) {
				continue;
			}

			sum += values[i];
			indexList.add(index);
			valueList.add(value);
		}

		indexes = new int[indexList.size()];
		values = new double[valueList.size()];

		ArrayUtils.copyAs(indexList, indexes);
		ArrayUtils.copyAs(valueList, values);
	}

	public void reset() {
		Arrays.fill(indexes, 0);
		Arrays.fill(values, 0);
		sum = 0;
	}

	public void reverse() {
		int middle = indexes.length / 2;
		for (int i = 0; i < middle; i++) {
			int left = i;
			int right = indexes.length - i - 1;
			swap(left, right);
		}
	}

	public void scale(double factor) {
		sum = 0;
		for (int i = 0; i < size(); i++) {
			values[i] = values[i] * factor;
			sum += values[i];
		}
	}

	public void scale(int index, double factor) {
		int loc = location(index);
		if (loc > -1) {
			scaleAtLoc(loc, factor);
		}
	}

	public void scaleAtLoc(int loc, double factor) {
		double newValue = values[loc] * factor;
		double diff = newValue - values[loc];
		values[loc] = newValue;
		sum += diff;
	}

	public void set(int index, double value) {
		int loc = location(index);
		if (loc > -1) {
			setAtLoc(loc, value);
		}
	}

	public void setAll(double value) {
		Arrays.fill(values, value);
		sum = (values.length * value);
	}

	public void setAtLoc(int loc, double value) {
		values[loc] = value;
	}

	public void setAtLoc(int loc, int index, double value) {
		indexes[loc] = index;
		values[loc] = value;
	}

	public void setDim(int dim) {
		this.dim = dim;
	}

	public void setIndexes(int[] indexes) {
		this.indexes = indexes;
	}

	public void setLabel(int label) {
		this.label = label;
	}

	public void setSum(double sum) {
		this.sum = sum;
	}

	public void setValues(double[] values) {
		this.values = values;
	}

	public int size() {
		return indexes.length;
	}

	public int sizeOfNonzero() {
		int ret = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] != 0) {
				ret++;
			}
		}
		return ret;
	}

	public void sortByIndex() {
		quicksort(true);
	}

	public void sortByValue() {
		sortByValue(true);
	}

	public void sortByValue(boolean descending) {
		quicksort(false);
		if (!descending) {
			reverse();
		}
	}

	public double sum() {
		return sum;
	}

	public void summation() {
		sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
	}

	private void swap(int i, int j) {
		int temp1 = indexes[i];
		int temp2 = indexes[j];
		indexes[i] = temp2;
		indexes[j] = temp1;

		double temp3 = values[i];
		double temp4 = values[j];
		values[i] = temp4;
		values[j] = temp3;
	}

	public DenseVector toDenseVector() {
		DenseVector ret = new DenseVector(dim, label);
		for (int i = 0; i < indexes.length; i++) {
			int index = indexes[i];
			double value = values[i];
			ret.increment(index, value);
		}
		return ret;
	}

	public String toString() {
		return toString(false, 20, null, null);
	}

	public String toString(boolean vertical, int numKeys, Indexer<String> labelIndexer, Indexer<String> featIndexer) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);
		nf.setGroupingUsed(false);

		StringBuffer sb = new StringBuffer();
		if (labelIndexer == null) {
			sb.append(String.format("%d (%d/%d, %s) ->", label, size(), dim(), nf.format(sum)));
		} else {
			sb.append(String.format("%s (%d/%d, %s) ->", labelIndexer.getObject(label), size(), dim(), nf.format(sum)));
		}

		// sortByValue();

		if (vertical) {
			sb.append("\n");
		}

		for (int i = 0; i < indexes.length && i < numKeys; i++) {
			int index = indexes[i];
			double value = values[i];

			if (featIndexer == null) {
				sb.append(String.format(" %d:%s", index, nf.format(value)));
			} else {
				sb.append(String.format(" %s:%s", featIndexer.getObject(index), nf.format(value)));
			}

			if (vertical) {
				sb.append("\n");
			}
		}

		// sortByIndex();

		return sb.toString().trim();
	}

	public String toSvmString() {
		StringBuffer sb = new StringBuffer();
		sb.append(label);
		for (int i = 0; i < size(); i++) {
			sb.append(String.format(" %d:%s", indexes[i], values[i] + ""));
			if (i != size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	public double value(int index) {
		int loc = location(index);
		if (loc < 0) {
			// System.out.println(toString(true, size(), null, null));
			throw new IllegalArgumentException("not found");
		}
		return valueAtLoc(loc);
	}

	public double valueAlways(int index) {
		double ret = 0;
		int loc = location(index);
		if (loc > -1) {
			ret = valueAtLoc(loc);
		}
		return ret;
	}

	public double valueAtLoc(int loc) {
		return values[loc];
	}

	public double[] values() {
		return values;
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(size());
		oos.writeInt(label());
		oos.writeInt(dim());

		for (int i = 0; i < size(); i++) {
			int index = indexAtLoc(i);
			double value = valueAtLoc(i);
			oos.writeInt(index);
			oos.writeDouble(value);
		}
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);
		write(oos);
		oos.close();
	}
}
