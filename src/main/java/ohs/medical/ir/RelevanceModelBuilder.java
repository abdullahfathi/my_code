package ohs.medical.ir;

import java.io.IOException;

import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;

public class RelevanceModelBuilder {

	public static int num_fb_docs = 5;

	public static double dirichlet_prior = 2000;

	public static SparseVector getRelevanceModel(WordCountBox wcb, SparseVector docScores) throws IOException {
		int num_fb_docs = 5;

		docScores.sortByValue();

		SparseVector ret = new SparseVector(wcb.getCollWordCounts().size());

		for (int j = 0; j < wcb.getCollWordCounts().size(); j++) {
			int w = wcb.getCollWordCounts().indexAtLoc(j);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double prob_w_in_coll = cnt_w_in_coll / wcb.getCountSumInCollection();

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
		ret.removeZeros();
		ret.normalize();
		return ret;
	}

}
