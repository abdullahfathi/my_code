package ohs.medical.ir;

import java.util.List;

import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;

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

		int num_passages = 1;

		for (int i = 0; i < wcb.getDocWordCounts().rowSize(); i++) {
			int docId = wcb.getDocWordCounts().indexAtRowLoc(i);
			// SparseVector wordCounts = wcb.getDocWordCounts().rowAtLoc(i);

			List<Integer> words = wcb.getDocWords().get(docId);

			int num_words_in_passage = words.size() / num_passages;

			SparseVector[] psgWordCountData = new SparseVector[num_passages];

			for (int j = 0, loc = 0; j < num_passages; j++) {
				Counter<Integer> c = new Counter<Integer>();
				int temp_cnt = num_words_in_passage;

				for (int k = 0; k < num_words_in_passage && loc < words.size(); k++) {
					int w = words.get(loc++);
					c.incrementCount(w, 1);
				}

				psgWordCountData[j] = VectorUtils.toSparseVector(c);
			}

			SparseVector psgScores = new SparseVector(num_passages);

			for (int j = 0; j < num_passages; j++) {
				SparseVector psgWordCounts = psgWordCountData[j];

				double div_sum = 0;

				for (int k = 0; k < queryModel.size(); k++) {
					int w = queryModel.indexAtLoc(k);
					double prob_w_in_query = queryModel.valueAtLoc(k);
					double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
					double prob_w_in_coll = cnt_w_in_coll / wcb.getCountSumInCollection();

					double cnt_w_in_psg = psgWordCounts.valueAlways(w);
					double cnt_sum_in_psg = psgWordCounts.sum();
					double mixture_for_coll = dirichlet_prior / (cnt_sum_in_psg + dirichlet_prior);
					double prob_w_in_doc = cnt_w_in_psg / cnt_sum_in_psg;

					prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;

					if (prob_w_in_doc > 0) {
						double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_doc);
						div_sum += div;
					}
				}

				double approx_prob = Math.exp(-div_sum);
				psgScores.incrementAtLoc(j, j, approx_prob);
			}

			double max_score = psgScores.max();
			ret.incrementAtLoc(i, docId, max_score);
		}

		return ret;
	}

}
