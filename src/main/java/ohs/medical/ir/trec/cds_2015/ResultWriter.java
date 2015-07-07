package ohs.medical.ir.trec.cds_2015;

import ohs.io.TextFileWriter;
import ohs.matrix.SparseVector;

public class ResultWriter {
	public static void write(TextFileWriter writer, String queryId, SparseVector docScores) {
		docScores.sortByValue();
		for (int i = 0; i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			double score = docScores.valueAtLoc(i);
			writer.write(queryId + "\t" + docId + "\t" + score + "\n");
		}
		docScores.sortByIndex();
	}
}
