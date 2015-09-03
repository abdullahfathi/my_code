package ohs.medical.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.common.StrCounter;
import ohs.types.common.StrCounterMap;
import ohs.utils.StrUtils;

import org.apache.lucene.analysis.Analyzer;

public class AbbreviationExpander {

	public static void main(String[] args) throws Exception {
		String[] collDirs = MIRPath.CollDirs;

		for (int i = 0; i < collDirs.length; i++) {
			String inputFileName = collDirs[i] + "abbrs_filter.txt";
			String outputFileName = collDirs[i] + "abbrs_cm.txt";
			StrCounterMap cm = readAbbreviationData(inputFileName);
			IOUtils.write(outputFileName, cm);
		}
	}

	public static StrCounterMap readAbbreviationData(String fileName) throws Exception {
		StrCounterMap ret = new StrCounterMap();
		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String shortForm = lines.get(0).split("\t")[1];
			StrCounter c = new StrCounter();

			for (int i = 1; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\t");
				String longForm = parts[0];

				if (longForm.toLowerCase().contains(shortForm.toLowerCase())) {
					continue;
				}

				double cnt = Double.parseDouble(parts[1]);

				List<String> words = AnalyzerUtils.getWords(longForm, analyzer);

				for (String word : words) {
					c.incrementCount(word, 1);
				}
				c.incrementAll(cnt);
			}

			ret.setCounter(shortForm, c);
		}
		reader.close();

		ret.normalize();

		// System.out.println(ret.toString());

		return ret;
	}

	private StrCounterMap abbrMap;

	public AbbreviationExpander(String fileName) throws Exception {
		abbrMap = readAbbreviationData(fileName);
	}

	public String expand(String searchText) {
		StrCounter ret = new StrCounter();
		double mixture = 0.5;

		List<String> words = StrUtils.split(searchText);
		CounterMap<String, String> cm = new CounterMap<String, String>();

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			sb.append(word);
			if (abbrMap.containsKey(word) && !cm.containsKey(word)) {
				Counter<String> c = abbrMap.getCounter(word);
				sb.append(" (");
				for (String w : c.getSortedKeys()) {
					sb.append(" " + w);
				}
				sb.append(" )");
			}
			sb.append(" ");
		}

		return sb.toString().trim();
	}

	public StrCounter expand(StrCounter qLM) {
		StrCounter ret = new StrCounter();
		double mixture = 0.5;

		for (String word : qLM.keySet()) {
			double prob = qLM.getCount(word);
			if (abbrMap.containsKey(word)) {
				Counter<String> c = abbrMap.getCounter(word);

				double prob_for_query_word = prob * (1 - mixture);
				double prob_for_abbr_word = prob * mixture;

				for (String w : c.keySet()) {
					double prob2 = c.getCount(w);
					ret.incrementCount(w, prob_for_abbr_word * prob2);
				}

				ret.incrementCount(word, prob_for_query_word);

				// System.out.println(tok);
				// System.out.println(abbrMap.getCounter(tok));
				// System.out.println();
			} else {
				ret.incrementCount(word, prob);
			}
		}

		double sum = ret.totalCount();

		return ret;
	}
}
