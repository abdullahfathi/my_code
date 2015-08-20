package ohs.ir.eval;

import java.text.NumberFormat;
import java.util.Set;
import java.util.TreeSet;

import ohs.types.CounterMap;

public class Performance {
	private int total_relevant_in_ret;

	private int total_relevant;

	private int total_retrieved;

	private int top_n;

	private double map;

	private double ndcg;

	private double precision;

	private int total_relevant_at_n;

	private CounterMap<MetricType, String> metric_query_value;

	public Performance(int top_n, CounterMap<MetricType, String> metric_query_value) {
		this.top_n = top_n;
		this.metric_query_value = metric_query_value;
		compute();
	}

	private void compute() {
		total_retrieved = (int) metric_query_value.getCounter(MetricType.RETRIEVED).totalCount();
		total_relevant = (int) metric_query_value.getCounter(MetricType.RELEVANT_ALL).totalCount();
		total_relevant_in_ret = (int) metric_query_value.getCounter(MetricType.RELEVANT_IN_RET).totalCount();
		total_relevant_at_n = (int) metric_query_value.getCounter(MetricType.RELEVANT_AT).totalCount();
		map = metric_query_value.getCounter(MetricType.AP).average();
		ndcg = metric_query_value.getCounter(MetricType.NDCG).average();
		precision = metric_query_value.getCounter(MetricType.PRECISION).average();
	}

	public double getMAP() {
		return map;
	}

	public CounterMap<MetricType, String> getMetricQueryValue() {
		return metric_query_value;
	}

	public double getNDCG() {
		return ndcg;
	}

	public double getPrecisionAtN() {
		return precision;
	}

	public int getTopN() {
		return top_n;
	}

	public int getTotalCorrect() {
		return total_relevant_in_ret;
	}

	public int getTotalCorrectAtN() {
		return total_relevant_at_n;
	}

	public int getTotalRelevant() {
		return total_relevant;
	}

	public int getTotalRetrieved() {
		return total_retrieved;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean showIndividuals) {
		StringBuffer ret = new StringBuffer();

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(5);
		nf.setGroupingUsed(false);

		Set<String> queryIds = new TreeSet<String>(metric_query_value.getCounter(MetricType.RETRIEVED).keySet());

		ret.append(String.format("[Performance for Top-%d]\n", top_n));
		ret.append(String.format("Queries:\t%d\n", queryIds.size()));
		ret.append(String.format("Relevant:\t%d\n", total_relevant));
		ret.append(String.format("Retrieved:\t%d\n", total_retrieved));
		ret.append(String.format("Relevant in Retrieved:\t%d\n", total_relevant_in_ret));
		ret.append(String.format("Relevant@%d:\t%d\n", top_n, total_relevant_at_n));
		ret.append(String.format("P@%d:\t%s\n", top_n, nf.format(precision)));
		ret.append(String.format("MAP@%d:\t%s\n", top_n, nf.format(map)));
		ret.append(String.format("NDCG@%d:\t%s\n", top_n, nf.format(ndcg)));

		if (showIndividuals) {
			MetricType[] mts = { MetricType.RETRIEVED, MetricType.RELEVANT_ALL, MetricType.RELEVANT_IN_RET, MetricType.RELEVANT_AT,
					MetricType.PRECISION, MetricType.AP, MetricType.NDCG };

			ret.append("\n[Individual Performances]\n");
			ret.append("Id");
			for (MetricType mt : mts) {
				ret.append(String.format("\t%s", mt));
			}
			ret.append("\n");

			for (String qId : queryIds) {
				ret.append(qId);
				for (int i = 0; i < mts.length; i++) {
					MetricType mt = mts[i];
					double value = metric_query_value.getCount(mt, qId);
					if (i < 4) {
						ret.append("\t" + (int) value);
					} else {
						ret.append("\t" + nf.format(value));
					}
				}
				ret.append("\n");
			}
		}

		return ret.toString().trim();
	}
}
