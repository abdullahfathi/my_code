package ohs.medical.ir.clef.ehealth_2014;

import java.io.File;

import ohs.io.TextFileReader;
import ohs.types.common.StrCounterMap;

public class RelevanceJudgementReader {

	public static void main(String[] args) {
		System.out.println("process begins.");

		read(new File(EHPath.QUERY_2014_TRAIN_RELEVANCE_FILE));

		System.out.println("process ends.");
	}

	public static StrCounterMap read(File inputFile) {
		// boolean is_2013_queries = false;
		//
		// if (inputFile.getPath().contains("2013ehealth")) {
		// is_2013_queries = true;
		// }
		StrCounterMap ret = new StrCounterMap();
		TextFileReader reader = new TextFileReader(inputFile);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split(" ");

			String qId = parts[0];
			String docId = parts[2];
			double relevance = Double.parseDouble(parts[3]);

			// if (is_2013_queries) {
			// qId = qId.substring(5);
			// }

			ret.setCount(qId, docId, relevance);
		}
		reader.close();
		return ret;
	}

}
