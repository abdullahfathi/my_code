package ohs.ir.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.medical.ir.clef.ehealth_2014.EHPath;
import ohs.medical.ir.clef.ehealth_2014.RelevanceJudgementReader;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.common.StrCounterMap;

public class PerformanceEvaluator {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		StrCounterMap relevanceData = RelevanceJudgementReader.read(new File(EHPath.QUERY_2013_TEST_RELEVANCE_FILE));

		for (File file : new File(EHPath.OUTPUT_BASIC_DIR).listFiles()) {
			String retModel = IOUtils.removeExtension(file.getName());

			StrCounterMap resultData = new StrCounterMap();

			TextFileReader reader = new TextFileReader(file.getPath());
			while (reader.hasNext()) {
				String[] parts = reader.next().split("\t");
				String qId = parts[0];
				String docId = parts[1];
				int indexId = Integer.parseInt(parts[2]);
				double score = Double.parseDouble(parts[3]);
				resultData.incrementCount(qId, docId, score);
			}
			reader.close();

			PerformanceEvaluator eval = new PerformanceEvaluator();
			eval.evalute(resultData, relevanceData);

			System.out.printf("[%s]\n", retModel);
			System.out.printf("%s\n\n", eval.toString(true));
		}

		System.out.println("process ends.");
	}

	public static StrCounterMap readSearchResults(String fileName) {
		StrCounterMap ret = new StrCounterMap();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
			String qId = parts[0];
			String docId = parts[1];
			double score = Double.parseDouble(parts[2]);
			ret.incrementCount(qId, docId, score);
		}
		reader.close();
		return ret;
	}

	// public static final int[] top_n_for_each_eval = { 5, 10, 20 };

	public int[] top_n_for_each_eval = { 5, 10, 20 };

	public List<Performance> evalute(CounterMap<String, String>  resultData, CounterMap<String, String>  relevanceData) {
		List<Performance> ret = new ArrayList<Performance>();

		for (int top_n : top_n_for_each_eval) {
			CounterMap<MetricType, String> cm = new CounterMap<MetricType, String>();

			for (String qId : resultData.keySet()) {
				Counter<String> docScores = resultData.getCounter(qId);
				Counter<String> docRelevances = relevanceData.getCounter(qId);

				List<String> docIds = docScores.getSortedKeys();

				double num_relevant_in_result = Metrics.relevantAtN(docIds, docScores.size(), docRelevances);
				double num_relevant_in_judgements = Metrics.relevant(docRelevances);
				double num_relevant_at_n = Metrics.relevantAtN(docIds, top_n, docRelevances);
				double num_retrieved = docScores.size();

				cm.setCount(MetricType.RETRIEVED, qId, num_retrieved);
				cm.setCount(MetricType.RELEVANT_ALL, qId, num_relevant_in_judgements);
				cm.setCount(MetricType.RELEVANT_IN_RET, qId, num_relevant_in_result);
				cm.setCount(MetricType.RELEVANT_AT, qId, num_relevant_at_n);

				double precision = Metrics.precisionAtN(docIds, top_n, docRelevances);
				double ap = Metrics.averagePrecisionAtN(docIds, top_n, docRelevances);
				double ndcg = Metrics.normalizedDiscountedCumulativeGainAtN(docIds, top_n, docRelevances);

				cm.setCount(MetricType.PRECISION, qId, precision);
				cm.setCount(MetricType.AP, qId, ap);
				cm.setCount(MetricType.NDCG, qId, ndcg);
			}

			Performance eval = new Performance(top_n, cm);
			ret.add(eval);
		}

		return ret;
	}

	public void setTopNs(int[] topNs) {
		this.top_n_for_each_eval = topNs;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean showIndividuals) {
		StringBuffer ret = new StringBuffer();

		return ret.toString().trim();
	}
}
