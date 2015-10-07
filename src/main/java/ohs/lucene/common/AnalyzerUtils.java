package ohs.lucene.common;

import java.util.ArrayList;
import java.util.List;

import ohs.types.Indexer;
import ohs.types.common.StrCounter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

public class AnalyzerUtils {

	public static BooleanQuery getQuery(List<String> words) throws Exception {
		return getQuery(words, IndexFieldName.CONTENT);
	}

	public static BooleanQuery getQuery(List<String> words, String field) throws Exception {
		BooleanQuery ret = new BooleanQuery();
		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			TermQuery tq = new TermQuery(new Term(field, word));
			ret.add(tq, Occur.SHOULD);
		}
		return ret;
	}

	public static BooleanQuery getQuery(StrCounter wordCounts) throws Exception {
		return getQuery(wordCounts, IndexFieldName.CONTENT);
	}

	public static BooleanQuery getQuery(StrCounter wordCounts, String field) throws Exception {
		BooleanQuery ret = new BooleanQuery();
		List<String> words = wordCounts.getSortedKeys();
		for (int i = 0; i < words.size() && i < BooleanQuery.getMaxClauseCount(); i++) {
			String word = words.get(i);
			double cnt = wordCounts.getCount(word);
			TermQuery tq = new TermQuery(new Term(field, word));
			tq.setBoost((float) cnt);
			ret.add(tq, Occur.SHOULD);
		}
		return ret;
	}

	public static BooleanQuery getQuery(String text, Analyzer analyzer) throws Exception {
		return getQuery(text, analyzer, IndexFieldName.CONTENT);
	}

	public static BooleanQuery getQuery(String text, Analyzer analyzer, String field) throws Exception {
		BooleanQuery ret = new BooleanQuery();

		StrCounter c = getWordCounts(text, analyzer);
		List<String> words = c.getSortedKeys();

		for (int i = 0; i < words.size() && i < BooleanQuery.getMaxClauseCount(); i++) {
			String word = words.get(i);
			double cnt = c.getProbability(word);
			TermQuery tq = new TermQuery(new Term(field, word));
			tq.setBoost((float) cnt);
			ret.add(tq, Occur.SHOULD);
		}
		return ret;
	}

	public static StrCounter getWordCounts(String text, Analyzer analyzer) throws Exception {
		StrCounter ret = new StrCounter();

		TokenStream ts = analyzer.tokenStream(IndexFieldName.CONTENT, text);
		CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
		ts.reset();

		while (ts.incrementToken()) {
			String word = attr.toString();
			ret.incrementCount(word, 1);
		}
		ts.end();
		ts.close();
		return ret;
	}

	public static List<Integer> getWordIndexes(List<String> words, Indexer<String> wordIndexer) {
		List<Integer> ret = new ArrayList<Integer>();

		for (String word : words) {
			int w = wordIndexer.indexOf(word);
			ret.add(w);
		}

		return ret;
	}

	public static List<String> getWords(String text, Analyzer analyzer) throws Exception {
		TokenStream ts = analyzer.tokenStream(IndexFieldName.CONTENT, text);
		CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
		ts.reset();

		List<String> ret = new ArrayList<String>();
		while (ts.incrementToken()) {
			String word = attr.toString();
			ret.add(word);
		}
		ts.end();
		ts.close();
		return ret;
	}
}
