package ohs.medical.ir.clef.ehealth_2015;

import java.io.File;
import java.util.List;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.matrix.SparseVector;
import ohs.medical.ir.BaseQuery;
import ohs.medical.ir.DocumentSearcher;
import ohs.medical.ir.MIRPath;
import ohs.medical.ir.QueryReader;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.IO;

import edu.stanford.nlp.ling.CoreAnnotations.DocSourceTypeAnnotation;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class InitialSearch {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		search();

		System.out.println("process ends.");
	}

	public static void search() throws Exception {

		QueryParser queryParser = DocumentSearcher.getQueryParser();

		IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.CLEF_EHEALTH_INDEX_DIR);

		List<BaseQuery> baseQueries = QueryReader.readClefEHealthQueries(MIRPath.CLEF_EHEALTH_QUERY_2015_FILE);

		IOUtils.deleteFilesUnder(new File(MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_INIT_DIR));

		String resultFileName = MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_INIT_DIR + "lm_dirichlet.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < baseQueries.size(); i++) {
			BaseQuery baseQuery = baseQueries.get(i);
			Query luceneQuery = queryParser.parse(baseQuery.getSearchText());
			baseQuery.setLuceneQuery(luceneQuery);

			SparseVector docScores = DocumentSearcher.search(baseQuery.getLuceneQuery(), indexSearcher, 1000);
			docScores.sortByValue();

			for (int j = 0; j < docScores.size(); j++) {
				int docId = docScores.indexAtLoc(j);
				double score = docScores.valueAtLoc(j);
				writer.write(baseQuery.getId() + "\t" + docId + "\t" + score + "\n");
			}
		}

		writer.close();
	}
}
