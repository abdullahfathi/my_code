package ohs.string.sim.search.ppss;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.types.Counter;
import ohs.types.common.IntPair;

/**
 * @author Heung-Seon Oh
 * 
 *         A class of determining global gram orders
 */
public class GramSorter implements Serializable {

	private class PositionComparator implements Comparator<IntPair>, Serializable {

		@Override
		public int compare(IntPair o1, IntPair o2) {
			return o1.getFirst() - o2.getFirst();
		}
	}

	private Counter<String> gramWeights;

	private Map<String, Integer> gramOrders;

	private boolean isAscendingOrder = true;

	public GramSorter() {
	}

	public Map<String, Integer> getGramOrders() {
		return gramOrders;
	}

	public Counter<String> getGramWeights() {
		return gramWeights;
	}

	// private void computeGramWeights(Gram[][] allGrams, boolean useTFIDF) {
	// gramWeights = new Counter<String>();
	//
	// if (useTFIDF) {
	// Map<String, Integer> gramFirstLocs = new HashMap<String, Integer>();
	// Counter<String> gramDFs = new Counter<String>();
	//
	// for (int i = 0; i < allGrams.length; i++) {
	// Gram[] grams = allGrams[i];
	// Set<String> gramSet = new HashSet<String>();
	//
	// for (int j = 0; j < grams.length; j++) {
	// Gram gram = grams[j];
	// String g = gram.getString();
	// gramWeights.incrementCount(g, 1);
	// gramSet.add(g);
	// }
	//
	// for (String g : gramSet) {
	// gramDFs.incrementCount(g, 1);
	// }
	// }
	//
	// for (String g : gramWeights.keySet()) {
	// // double tf = Math.log(gramWeights.getCount(g)) + 1;
	// double tf = gramWeights.getCount(g);
	// double df = gramDFs.getCount(g);
	// double num_grams = allGrams.length;
	// double idf = Math.log(((num_grams + 1) / df));
	// double tfidf = tf * idf;
	// gramWeights.setCount(g, idf);
	// }
	//
	// } else {
	// Map<String, Integer> gramFirstLocs = new HashMap<String, Integer>();
	// int gram_cnt = 0;
	//
	// for (int i = 0; i < allGrams.length; i++) {
	// Gram[] grams = allGrams[i];
	//
	// for (int j = 0; j < grams.length; j++) {
	// Gram gram = grams[j];
	// String g = gram.getString();
	// gramWeights.incrementCount(g, 1);
	// gram_cnt++;
	// if (!gramFirstLocs.containsKey(g)) {
	// gramFirstLocs.put(g, gram_cnt);
	// }
	// }
	// }
	//
	// for (String g : gramFirstLocs.keySet()) {
	// int id = gramFirstLocs.get(g);
	// double pos_weight = 1f * id / (gram_cnt + 1);
	// gramWeights.incrementCount(g, pos_weight);
	// }
	// }
	// }

	/**
	 * @param grams
	 * 
	 *            Reorder the grams according to global gram orders.
	 * */
	public void order(Gram[] grams) {
		Set<Integer> unknown = new HashSet<Integer>();
		List<IntPair> orderLocs = new ArrayList<IntPair>();

		for (int i = 0; i < grams.length; i++) {
			Gram gram = grams[i];
			Integer order = gramOrders.get(gram.getString());
			if (order == null) {
				unknown.add(i);
				order = -unknown.size();
			}
			orderLocs.add(new IntPair(order, i));
		}

		Collections.sort(orderLocs, new PositionComparator());

		Gram[] tempGrams = new Gram[grams.length];

		for (int i = 0; i < grams.length; i++) {
			tempGrams[i] = grams[i];
			grams[i] = null;
		}

		for (int i = 0; i < orderLocs.size(); i++) {
			int loc = orderLocs.get(i).getSecond();
			grams[i] = tempGrams[loc];
		}
	}

	public void setGramWeights(Counter<String> gramWeights) {
		this.gramWeights = gramWeights;
	}

	public void setIsAscendingOrder(boolean isAscendingOrder) {
		this.isAscendingOrder = isAscendingOrder;
	}

	public void sortGrams() {
		// if (allGrams != null) {
		// computeGramWeights(allGrams, useTFIDF);
		// }

		List<String> grams = gramWeights.getSortedKeys();

		if (isAscendingOrder) {
			Collections.reverse(grams);
		}

		gramOrders = new HashMap<String, Integer>();

		for (int i = 0; i < grams.size(); i++) {
			gramOrders.put(grams.get(i), i);
		}
	}
}
