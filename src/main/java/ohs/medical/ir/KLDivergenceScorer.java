package ohs.medical.ir;

import ohs.matrix.SparseVector;

public class KLDivergenceScorer {

	private double dirichlet_prior;

	private boolean makeLog;

	private StringBuffer logBuf;

	public KLDivergenceScorer() {
		this(2000, false);
	}

	public KLDivergenceScorer(double dirichlet_prior, boolean makeLog) {
		this.dirichlet_prior = dirichlet_prior;
		this.makeLog = makeLog;
	}

	public String getLog() {
		return logBuf.toString();
	}

	public SparseVector scoreDocuments(WordCountBox wcb, SparseVector queryModel) {
		SparseVector ret = new SparseVector(wcb.getDocWordCounts().rowSize());

		for (int i = 0; i < queryModel.size(); i++) {
			int w = queryModel.indexAtLoc(i);
			double prob_w_in_query = queryModel.valueAtLoc(i);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double prob_w_in_coll = cnt_w_in_coll / wcb.getCountSumInCollection();

			for (int j = 0; j < wcb.getDocWordCounts().rowSize(); j++) {
				int docId = wcb.getDocWordCounts().indexAtRowLoc(j);
				SparseVector wordCounts = wcb.getDocWordCounts().rowAtLoc(j);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;

				prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;

				if (prob_w_in_doc > 0) {
					double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_doc);
					ret.incrementAtLoc(j, docId, div);
				}
			}
		}

		for (int i = 0; i < ret.size(); i++) {
			double sum_div = ret.valueAtLoc(i);
			double approx_prob = Math.exp(-sum_div);
			ret.setAtLoc(i, approx_prob);
		}
		ret.summation();

		return ret;
	}

	public SparseVector scoreDocuments2(WordCountBox wcb, SparseVector queryModel) {
		SparseVector ret = new SparseVector(wcb.getDocWordCounts().rowSize());

		for (int i = 0; i < queryModel.size(); i++) {
			int w = queryModel.indexAtLoc(i);
			double prob_w_in_query = queryModel.valueAtLoc(i);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double df_w_in_coll = wcb.getCollDocFreqs().valueAlways(w);
			double prob_w_in_coll = df_w_in_coll / wcb.getCountSumInCollection();

			for (int j = 0; j < wcb.getDocWordCounts().rowSize(); j++) {
				int docId = wcb.getDocWordCounts().indexAtRowLoc(j);
				SparseVector wordCounts = wcb.getDocWordCounts().rowAtLoc(j);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = 0.8;
				double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;

				prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;

				if (prob_w_in_doc > 0) {
					double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_doc);
					ret.incrementAtLoc(j, docId, div);
				}
			}
		}

		for (int i = 0; i < ret.size(); i++) {
			double sum_div = ret.valueAtLoc(i);
			double approx_prob = Math.exp(-sum_div);
			ret.setAtLoc(i, approx_prob);
		}
		ret.summation();

		return ret;
	}
}
