package ohs.medical.ir.query;

import ohs.io.TextFileReader;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounterMap;

public class RelevanceReader {
	public static CounterMap<String, String> filter(CounterMap<String, String> relevanceData, StrBidMap docIdMap) {
		StrCounterMap ret = new StrCounterMap();

		int num_pairs = 0;
		int num_result_pairs = 0;

		for (String queryId : relevanceData.keySet()) {
			Counter<String> docRelevances = relevanceData.getCounter(queryId);
			Counter<String> counter = new Counter<String>();

			num_pairs += docRelevances.size();

			for (String docId : docRelevances.keySet()) {
				double score = docRelevances.getCount(docId);
				String indexId = docIdMap.getKey(docId);
				if (indexId == null || score == 0) {
					continue;
				}
				counter.setCount(docId, score);
				num_result_pairs++;
			}

			if (counter.size() == 0) {
				continue;
			}

			ret.setCounter(queryId, counter);
		}
		System.out.printf("[%d] queries -> [%d] queries\n", relevanceData.keySet().size(), ret.keySet().size());
		System.out.printf("[%d] pairs -> [%d] pairs\n", num_pairs, num_result_pairs);
		return ret;
	}

	/**
	 * d definitely relevant -> 2 p possibly relevant -> 1 n not relevant -> 0
	 * 
	 * @param judgement
	 * @return
	 */
	private static double getRelevance(String judgement) {
		double ret = 0;
		if (judgement.equals("d")) {
			ret = 2;
		} else if (judgement.equals("p")) {
			ret = 1;
		}
		return ret;
	}

	public static CounterMap<String, String> readClefEHealthRelevances(String fileName) {
		StrCounterMap ret = new StrCounterMap();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split(" ");

			String qId = parts[0];
			String docId = parts[2];
			double relevance = Double.parseDouble(parts[3]);

			// if (relevance > 0) {
			ret.setCount(qId, docId, relevance);
			// }
		}
		reader.close();
		return ret;
	}

	public static CounterMap<String, String> readOhsumedRelevances(String fileName) {
		StrCounterMap ret = new StrCounterMap();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");

			String qId = parts[0];
			String doc_ui = parts[1];
			String doc_i = parts[2];
			String judgement1 = parts[3];
			String judgement2 = parts.length > 4 ? parts[4] : "";
			String judgement3 = parts.length > 5 ? parts[5] : "";

			Counter<String> counter = new Counter<String>();

			if (judgement1.length() > 0) {
				counter.incrementCount(judgement1, 1);
			}

			if (judgement2.length() > 0) {
				counter.incrementCount(judgement2, 1);
			}

			if (judgement3.length() > 0) {
				counter.incrementCount(judgement3, 1);
			}

			if (counter.size() == 1) {
				double relevance = getRelevance(counter.argMax());
				// if (relevance > 0) {
				ret.setCount(qId, doc_ui, relevance);
				// }
			} else {

			}
		}
		reader.close();
		return ret;
	}

	public static CounterMap<String, String> readTrecCdsRelevances(String fileName) {
		StrCounterMap ret = new StrCounterMap();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");

			String qId = parts[0];
			String docId = parts[2];
			double relevance = Double.parseDouble(parts[3]);
			// if (relevance > 0) {
			ret.setCount(qId, docId, relevance);
			// }
		}
		reader.close();
		return ret;
	}
}
