package ohs.types;

import java.util.ArrayList;
import java.util.List;

public class Vocabulary {

	private Indexer<String> wordIndexer;

	private Counter<Integer> wordCounts;

	public Vocabulary() {
		wordIndexer = new Indexer<String>();

		wordCounts = new Counter<Integer>();
	}

	public int getWordCount(int w) {
		return (int) wordCounts.getCount(w);
	}

	public int getIndexWithIncrement(String word) {
		int ret = wordIndexer.getIndex(word);
		wordCounts.incrementCount(ret, 1);
		return ret;
	}

	public int size() {
		return wordCounts.size();
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public Counter<Integer> getWordCounts() {
		return wordCounts;
	}

	public List<String> getWords() {
		List<String> ret = new ArrayList<String>();

		for (int w : wordCounts.getSortedKeys()) {
			ret.add(wordIndexer.getObject(w));
		}
		return ret;
	}

}
