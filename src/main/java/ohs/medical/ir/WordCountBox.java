package ohs.medical.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.lucene.common.IndexFieldName;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.common.IntCounterMap;
import ohs.types.common.StrCounter;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

public class WordCountBox {

	public static StrCounter getDocFreqs(IndexReader indexReader, String field, Collection<String> c) throws Exception {
		StrCounter ret = new StrCounter();
		for (String word : c) {
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

		CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();
		ListMap<Integer, Integer> docWords = new ListMap<Integer, Integer>();

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
			Counter<Integer> wcs = new Counter<Integer>();
			Map<Integer, Integer> locWords = new HashMap<Integer, Integer>();

			while ((bytesRef = termsEnum.next()) != null) {
				postingsEnum = termsEnum.postings(null, postingsEnum, PostingsEnum.ALL);

				if (postingsEnum.nextDoc() != 0) {
					throw new AssertionError();
				}

				String word = bytesRef.utf8ToString();
				if (word.startsWith("#")) {
					continue;
				}
				int w = wordIndexer.getIndex(word);
				int freq = postingsEnum.freq();
				wcs.incrementCount(w, freq);

				for (int k = 0; k < freq; k++) {
					final int position = postingsEnum.nextPosition();
					locWords.put(position, w);
				}
			}
			cm.setCounter(docId, wcs);

			List<Integer> locs = new ArrayList<Integer>(locWords.keySet());
			Collections.sort(locs);

			List<Integer> words = new ArrayList<Integer>();

			for (int loc : locs) {
				words.add(locWords.get(loc));
			}

			docWords.set(docId, words);

			for (int w : wcs.keySet()) {
				fbWords.add(w);
			}
		}

		SparseMatrix docWordCounts = VectorUtils.toSpasreMatrix(cm);

		Counter<Integer> c1 = new Counter<Integer>();
		Counter<Integer> c2 = new Counter<Integer>();

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
		ret.setWordIndexer(wordIndexer);
		return ret;
	}

	public static StrCounter getWordCounts(IndexReader indexReader, String field, StrCounter c) throws Exception {
		StrCounter ret = new StrCounter();
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

	private ListMap<Integer, Integer> docWords;

	private SparseMatrix wordToWordCounts;

	private SparseVector collDocFreqs;

	private Indexer<String> wordIndexer;

	public WordCountBox(SparseMatrix docWordCounts, SparseVector collWordCounts, double cnt_sum_in_coll, SparseVector docFreqs,
			double num_docs_in_coll, ListMap<Integer, Integer> docWords) {
		super();
		this.docWordCounts = docWordCounts;
		this.collWordCounts = collWordCounts;
		this.cnt_sum_in_coll = cnt_sum_in_coll;
		this.collDocFreqs = docFreqs;
		this.num_docs_in_coll = num_docs_in_coll;
		this.docWords = docWords;
	}

	public void computeWordCooccurrences(int window_size) {

		IntCounterMap cm = new IntCounterMap();

		for (int docId : docWords.keySet()) {
			List<Integer> words = docWords.get(docId);

			for (int j = 0; j < words.size(); j++) {
				int w1 = words.get(j);
				for (int k = j + 1; k < window_size && k < words.size(); k++) {
					int w2 = words.get(k);
					cm.incrementCount(w1, w2, 1);
				}
			}
		}
		wordToWordCounts = VectorUtils.toSpasreMatrix(cm);
	}

	public SparseVector getCollDocFreqs() {
		return collDocFreqs;
	}

	public double getCollectionCountSum() {
		return cnt_sum_in_coll;
	}

	public SparseVector getCollWordCounts() {
		return collWordCounts;
	}

	public SparseMatrix getDocWordCounts() {
		return docWordCounts;
	}

	public ListMap<Integer, Integer> getDocWords() {
		return docWords;
	}

	public double getNumDocsInCollection() {
		return num_docs_in_coll;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
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

	public void setWordIndexer(Indexer<String> wordIndexer) {
		this.wordIndexer = wordIndexer;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		return sb.toString();
	}

}
