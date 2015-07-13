package ohs.medical.ir.trec.cds_2015;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.medical.ir.WordCountBox;
import ohs.types.CounterMap;
import ohs.types.DeepListMap;
import ohs.types.Indexer;
import ohs.types.ListMap;

public class PromximityRelevanceModelBuilder {

	public static double hal(int i, int j, int window_size) {
		return window_size - (j - i) + 1;
	}

	private DeepListMap<Integer, Integer, Integer> docWordLocs;

	private int window_size;

	private int num_fb_docs;

	private int num_fb_words;

	private double dirichlet_prior;

	private Map<Integer, SparseMatrix> wordProximities;

	private Indexer<String> wordIndexer;

	private boolean makeLog;

	private StringBuffer logBuf;

	public PromximityRelevanceModelBuilder(Indexer<String> wordIndexer) {
		this(wordIndexer, 5, 20, 2000, 2, false);
	}

	public PromximityRelevanceModelBuilder(Indexer<String> wordIndexer, int num_fb_docs, int num_fb_words, int dirichlet_prior,
			int window_size, boolean makeLog) {
		this.wordIndexer = wordIndexer;
		this.num_fb_docs = num_fb_docs;
		this.num_fb_words = num_fb_words;
		this.dirichlet_prior = dirichlet_prior;
		this.window_size = window_size;
		this.makeLog = makeLog;

	}

	public void computeWordProximities(WordCountBox wcb, SparseVector queryModel) {
		setDocWordLocs(wcb.getDocWords());

		wordProximities = new HashMap<Integer, SparseMatrix>();

		logBuf = new StringBuffer();

		SparseMatrix docWordCounts = wcb.getDocWordCounts();

		for (int i = 0; i < docWordCounts.rowSize(); i++) {
			int docId = docWordCounts.indexAtRowLoc(i);
			List<Integer> words = wcb.getDocWords().get(docId);
			ListMap<Integer, Integer> wordLocs = docWordLocs.get(docId, false);

			// CounterMap<String, String> cm2 = new CounterMap<String, String>();

			CounterMap<Integer, Integer> cm1 = new CounterMap<Integer, Integer>();

			for (int j = 0; j < queryModel.size(); j++) {

				int qw = queryModel.indexAtLoc(j);
				List<Integer> locs = wordLocs.get(qw);

				if (makeLog) {
					StringBuffer sb = new StringBuffer();
					String word = wordIndexer.getObject(qw);
					sb.append(String.format("QWord:\t%s\n", word));
				}

				for (int k = 0; k < locs.size(); k++) {
					int loc = locs.get(k);

					int start = loc - window_size;
					int end = loc + window_size;

					if (start < 0) {
						start = 0;
					}

					if (end > words.size() - 1) {
						end = words.size() - 1;
					}

					for (int l = start; l <= end; l++) {

						if (k == l) {
							continue;
						}

						int cw = words.get(l);

						if (makeLog) {
							String cWord = wordIndexer.getObject(cw);
							if (l == loc) {
								logBuf.append(String.format("CWord at [%d, %d]:\t%s (#)\n", l, loc, cWord));
							} else {
								logBuf.append(String.format("CWord at [%d, %d]:\t%s\n", l, loc, cWord));
							}
						}

						double score = window_size - Math.abs(l - loc) + 1;
						// cm2.incrementCount(word, cWord, 1);
						// cm1.incrementCount(qw, cw, score);
						cm1.incrementCount(qw, cw, 1);
					}

					if (makeLog) {
						logBuf.append("\n");
					}
				}

				if (makeLog) {
					logBuf.append("\n");
				}
			}

			cm1.normalize();

			SparseMatrix sm = VectorUtils.toSpasreMatrix(cm1.invert());
			wordProximities.put(docId, sm);
		}
	}

	public StringBuffer getLogBuf() {
		return logBuf;
	}

	public SparseVector getRelevanceModel(WordCountBox wcb, SparseVector docScores) throws IOException {

		docScores.sortByValue();

		SparseVector ret = new SparseVector(wcb.getCollWordCounts().size());

		for (int j = 0; j < wcb.getCollWordCounts().size(); j++) {
			int qw = wcb.getCollWordCounts().indexAtLoc(j);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(qw);
			double prob_w_in_coll = cnt_w_in_coll / wcb.getCountSumInCollection();

			for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
				int docId = docScores.indexAtLoc(k);
				SparseMatrix prox = wordProximities.get(docId);
				SparseVector cws = prox.rowAlways(qw);

				// if (cws.size() > 0) {
				// System.out.println(wordIndexer.getObject(qw));
				// System.out.println(VectorUtils.toCounter(cws, wordIndexer));
				// System.out.println();
				// }

				double doc_weight = docScores.valueAtLoc(k);

				SparseVector wordCounts = wcb.getDocWordCounts().rowAlways(docId);
				double cnt_w_in_doc = wordCounts.valueAlways(qw);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
				double doc_prior = 1;
				double word_weight = cws.sum();
				double prob_w_in_fb_model = doc_weight * prob_w_in_doc * doc_prior * Math.exp(word_weight);

				if (prob_w_in_fb_model > 0) {
					ret.incrementAtLoc(j, qw, prob_w_in_fb_model);
				}
			}
		}
		docScores.sortByIndex();
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}

	private void setDocWordLocs(ListMap<Integer, Integer> docWords) {
		docWordLocs = new DeepListMap<Integer, Integer, Integer>();

		for (int docId : docWords.keySet()) {
			List<Integer> words = docWords.get(docId);
			ListMap<Integer, Integer> wordLocs = new ListMap<Integer, Integer>();

			for (int i = 0; i < words.size(); i++) {
				int w = words.get(i);
				wordLocs.put(w, i);
			}

			for (int w : wordLocs.keySet()) {
				List<Integer> locs = wordLocs.get(w);
				Collections.sort(locs);
			}

			docWordLocs.put(docId, wordLocs);
		}
	}
}
