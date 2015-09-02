package ohs.medical.ir;

import java.util.List;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.types.Counter;
import ohs.types.common.StrCounter;
import ohs.types.common.StrCounterMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

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

		System.out.println(ret.toString());

		return ret;
	}
}
