package ohs.medical.ir.trec.cds_2015;

import java.util.Collections;
import java.util.List;

import edu.stanford.nlp.trees.WordNetConnection;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.medical.ir.WordCountBox;
import ohs.types.CounterMap;
import ohs.types.DeepListMap;
import ohs.types.Indexer;
import ohs.types.ListMap;

public class PromximityRelevanceModelBuilder {

	private DeepListMap<Integer, Integer, Integer> docWordLocs;

	private int window_size = 2;

	public SparseVector score(WordCountBox wcb, SparseVector queryModel, Indexer<String> wordIndexer) {
		setDocWordLocs(wcb.getDocWords());

		SparseVector ret = new SparseVector(wcb.getDocWordCounts().rowSize());

		SparseMatrix docWordCounts = wcb.getDocWordCounts();

		for (int i = 0; i < docWordCounts.rowSize(); i++) {
			int docId = docWordCounts.indexAtRowLoc(i);
			List<Integer> words = wcb.getDocWords().get(docId);
			ListMap<Integer, Integer> wordLocs = docWordLocs.get(docId, false);

			CounterMap<String, String> cm = new CounterMap<String, String>();

			for (int j = 0; j < queryModel.size(); j++) {

				int w = queryModel.indexAtLoc(j);
				String word = wordIndexer.getObject(w);
				List<Integer> locs = wordLocs.get(w);

				StringBuffer sb = new StringBuffer();
				sb.append(String.format("QWord:\t%s\n", word));

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
						int cw = words.get(l);
						String cWord = wordIndexer.getObject(cw);
						if (l == loc) {
							sb.append(String.format("CWord at [%d, %d]:\t%s (#)\n", l, loc, cWord));
						} else {
							sb.append(String.format("CWord at [%d, %d]:\t%s\n", l, loc, cWord));
						}

						cm.incrementCount(word, cWord, 1);
					}
					sb.append("\n");
				}
				System.out.println(sb.toString());
			}

			System.out.println(cm.toString());
			System.out.println();

		}

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
