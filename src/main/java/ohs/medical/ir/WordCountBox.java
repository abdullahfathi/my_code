package ohs.medical.ir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ohs.lucene.common.IndexFieldName;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.DeepMap;
import ohs.types.Indexer;
import ohs.types.common.IntCounter;
import ohs.types.common.IntCounterMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

public class WordCountBox {

	public static Counter<String> getDocFreqs(IndexReader indexReader, String field, Counter<String> c) throws Exception {
		Counter<String> ret = new Counter<String>();
		for (String word : c.keySet()) {
			Term term = new Term(field, word);
			double df = indexReader.docFreq(term);
			ret.setCount(word, df);
		}
		return ret;
	}

	public static WordCountBox getWordCountBox(IndexReader indexReader, SparseVector docScores, Indexer<String> wordIndexer)
			throws Exception {
		return getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);
	}

	public static WordCountBox getWordCountBox(IndexReader indexReader, SparseVector docScores, Indexer<String> wordIndexer, String field)
			throws Exception {
		Set<Integer> fbWords = new HashSet<Integer>();

		IntCounterMap cm = new IntCounterMap();
		DeepMap<Integer, Integer, Integer> docWords = new DeepMap<Integer, Integer, Integer>();

		for (int j = 0; j < docScores.size(); j++) {
			int docId = docScores.indexAtLoc(j);
			double score = docScores.valueAtLoc(j);
			Document doc = indexReader.document(docId);

			Terms termVector = indexReader.getTermVector(docId, field);

			if (termVector == null) {
				continue;
			}

			TermsEnum termsEnum = null;
			termsEnum = termVector.iterator(termsEnum);

			BytesRef bytesRef = null;
			PostingsEnum postingsEnum = null;
			IntCounter wcs = new IntCounter();
			Map<Integer, Integer> words = new TreeMap<Integer, Integer>();

			while ((bytesRef = termsEnum.next()) != null) {
				postingsEnum = termsEnum.postings(null, postingsEnum, PostingsEnum.ALL);

				if (postingsEnum.nextDoc() != 0) {
					throw new AssertionError();
				}

				String word = bytesRef.utf8ToString();
				int w = wordIndexer.getIndex(word);
				int freq = postingsEnum.freq();
				wcs.incrementCount(w, freq);

				for (int k = 0; k < freq; k++) {
					final int position = postingsEnum.nextPosition();
					words.put(position, w);
				}
			}
			cm.setCounter(docId, wcs);
			docWords.put(docId, words);

			for (int w : wcs.keySet()) {
				fbWords.add(w);
			}
		}

		SparseMatrix docWordCounts = VectorUtils.toSpasreMatrix(cm);

		IntCounter c1 = new IntCounter();
		IntCounter c2 = new IntCounter();

		for (int w : fbWords) {
			String word = wordIndexer.getObject(w);
			Term term = new Term(field, word);
			double cnt = indexReader.totalTermFreq(term);
			double df = indexReader.docFreq(term);
			c1.setCount(w, cnt);
			c2.setCount(w, df);
		}

		SparseVector collWordCounts = VectorUtils.toSparseVector(c1);
		SparseVector docFreqs = VectorUtils.toSparseVector(c2);

		double cnt_sum_in_coll = indexReader.getSumTotalTermFreq(IndexFieldName.CONTENT);

		WordCountBox ret = new WordCountBox(docWordCounts, collWordCounts, cnt_sum_in_coll, docFreqs, indexReader.maxDoc(), docWords);

		return ret;
	}

	public static Counter<String> getWordCounts(IndexReader indexReader, String field, Counter<String> c) throws Exception {
		Counter<String> ret = new Counter<String>();
		for (String word : c.keySet()) {
			Term term = new Term(field, word);
			double cnt = indexReader.totalTermFreq(term);
			ret.setCount(word, cnt);
		}
		return ret;
	}

	private SparseMatrix docWordCounts;

	private SparseVector collWordCounts;

	private double cnt_sum_in_coll;

	private double num_docs_in_coll;

	private DeepMap<Integer, Integer, Integer> docWordLocs;

	private SparseMatrix wordToWordCounts;

	private SparseVector collDocFreqs;

	public WordCountBox(SparseMatrix docWordCounts, SparseVector collWordCounts, double cnt_sum_in_coll, SparseVector docFreqs,
			double num_docs_in_coll, DeepMap<Integer, Integer, Integer> docWordLocs) {
		super();
		this.docWordCounts = docWordCounts;
		this.collWordCounts = collWordCounts;
		this.cnt_sum_in_coll = cnt_sum_in_coll;
		this.collDocFreqs = docFreqs;
		this.num_docs_in_coll = num_docs_in_coll;
		this.docWordLocs = docWordLocs;
	}

	public void computeWordCooccurrences(int window_size) {

		IntCounterMap cm = new IntCounterMap();

		for (int docId : docWordLocs.keySet()) {
			Map<Integer, Integer> wordLocs = docWordLocs.get(docId);
			List<Integer> locs = new ArrayList<Integer>(wordLocs.values());

			for (int j = 0; j < locs.size(); j++) {
				int loc1 = locs.get(j);
				int w1 = wordLocs.get(loc1);

				for (int k = j + 1; k < window_size && k < locs.size(); k++) {
					int loc2 = locs.get(k);
					int w2 = wordLocs.get(k);
					cm.incrementCount(w1, w2, 1);
				}
			}
		}
		wordToWordCounts = VectorUtils.toSpasreMatrix(cm);
	}

	public SparseVector getCollDocFreqs() {
		return collDocFreqs;
	}

	public SparseVector getCollWordCounts() {
		return collWordCounts;
	}

	public double getCountSumInCollection() {
		return cnt_sum_in_coll;
	}

	public SparseMatrix getDocWordCounts() {
		return docWordCounts;
	}

	public DeepMap<Integer, Integer, Integer> getDocWordLocs() {
		return docWordLocs;
	}

	public double getNumDocsInCollection() {
		return num_docs_in_coll;
	}

	public void setBgWordCounts(SparseVector collWordCounts) {
		this.collWordCounts = collWordCounts;
	}

	public void setCountSumInCollection(double cnt_sum_in_col) {
		this.cnt_sum_in_coll = cnt_sum_in_col;
	}

	public void setDocWordCounts(SparseMatrix docWordCounts) {
		this.docWordCounts = docWordCounts;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		return sb.toString();
	}

}
