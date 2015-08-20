package ohs.medical.ir;

import java.io.IOException;
import java.util.List;

import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.common.IntCounter;
import ohs.types.common.IntPair;

public class RelevanceModelBuilder {

	private int num_fb_docs;

	private int num_fb_words;

	private double dirichlet_prior;

	public RelevanceModelBuilder() {
		this(5, 20, 2000);
	}

	public RelevanceModelBuilder(int num_fb_docs, int num_fb_words, double dirichlet_prior) {
		this.num_fb_docs = num_fb_docs;
		this.num_fb_words = num_fb_words;
		this.dirichlet_prior = dirichlet_prior;
	}

	public SparseVector getPositionalRelevanceModel(SparseVector qLM, WordCountBox wcb, SparseVector docScores) {
		double pi = Math.PI;
		double sigma = 175;
		double mixture_for_coll = 0.5;

		int fb_type = 1;

		IntCounter fbCounts = new IntCounter();

		for (int i = 0; i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			double doc_score = docScores.valueAtLoc(i);
			List<Integer> words = wcb.getDocWords().get(docId);
			List<IntPair> locWords = PLMUtils.getQueryLocsInDocument(qLM, words);

			double real_doc_len = locWords.size();
			double len_norm = Math.sqrt(2 * pi) * sigma;
			double pos_score_sum = 0;

			double[] posScores = new double[words.size()];

			for (int j = 0; j < words.size(); j++) {
				IntCounter c = new IntCounter();

				for (int qw : qLM.indexes()) {
					c.incrementCount(qw, 0);
				}

				for (int k = 0; k < locWords.size(); k++) {
					int pos = locWords.get(k).getFirst();
					int w = locWords.get(k).getSecond();

					double dis = (pos - j) / sigma;
					double pr = Math.exp(-dis * dis / 2.0) / len_norm;
					c.incrementCount(w, pr);
				}

				SparseVector plm = VectorUtils.toSparseVector(c);

				for (int k = 0; k < plm.size(); k++) {
					int w = plm.indexAtLoc(k);
					double prob_w_in_doc = plm.valueAtLoc(k);
					double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
					double coll_cnt_sum = wcb.getCollectionCountSum();
					double prob_w_in_coll = cnt_w_in_coll / coll_cnt_sum;
					prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
					plm.setAtLoc(k, prob_w_in_doc);
				}
				plm.summation();

				double div_sum = 0;

				for (int k = 0; k < qLM.size(); k++) {
					int w = qLM.indexAtLoc(k);
					double prob_w_in_query = qLM.valueAtLoc(k);
					double prob_w_in_doc = plm.valueAlways(w);

					if (prob_w_in_doc > 0) {
						double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_doc);
						div_sum += div;
					}
				}

				double approx_prob = Math.exp(-div_sum);
				posScores[j] = approx_prob;
				pos_score_sum += approx_prob;
			}

			for (int j = 0; j < posScores.length; j++) {
				int w = words.get(j);
				double pos_score = posScores[j];

				if (fb_type == 1) {
					pos_score /= real_doc_len;
				} else if (fb_type == 2) {
					pos_score = pos_score * doc_score / pos_score_sum;
				}

				if (pos_score > 0) {
					fbCounts.incrementCount(w, pos_score);
				}
			}
		}

		SparseVector ret = VectorUtils.toSparseVector(fbCounts);
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}

	public SparseVector getRelevanceModel(WordCountBox wcb, SparseVector docScores) throws IOException {
		docScores.sortByValue();

		SparseVector ret = new SparseVector(wcb.getCollWordCounts().size());

		for (int j = 0; j < wcb.getCollWordCounts().size(); j++) {
			int w = wcb.getCollWordCounts().indexAtLoc(j);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double prob_w_in_coll = cnt_w_in_coll / wcb.getCollectionCountSum();

			for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
				int docId = docScores.indexAtLoc(k);
				double doc_weight = docScores.valueAtLoc(k);

				SparseVector wordCounts = wcb.getDocWordCounts().rowAlways(docId);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
				double doc_prior = 1;
				double prob_w_in_fb_model = doc_weight * prob_w_in_doc * doc_prior;

				if (prob_w_in_fb_model > 0) {
					ret.incrementAtLoc(j, w, prob_w_in_fb_model);
				}
			}
		}
		docScores.sortByIndex();
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}
}
