package ohs.medical.ir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.types.common.StrCounterMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class DataStats {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String[] queryFileNames = { MIRPath.TREC_CDS_QUERY_2014_FILE, MIRPath.CLEF_EHEALTH_QUERY_2014_FILE, MIRPath.OHSUMED_QUERY_FILE };
		String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR,
				MIRPath.WIKI_INDEX_DIR };

		String[] relevanceDataFileNames = { MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE, MIRPath.CLEF_EHEALTH_RELEVANCE_JUDGE_2014_FILE,
				MIRPath.OHSUMED_RELEVANCE_JUDGE_FILE };

		QueryParser queryParser = DocumentSearcher.getQueryParser();

		String[] collNames = { "TREC CDS 2014", "CLEF eHealth 2014", "OHSUMED", "Wikipedia" };
		String[] labels = { "#Docs", "Voc. Size", "Avg. Doc. Len", "#Queries", "Avg. Query Len.", "Query-Doc Pairs" };

		double[][] values = new double[labels.length][collNames.length];

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < indexDirNames.length; i++) {
			int num_queries = 0;
			int num_query_doc_pairs = 0;
			int num_docs = 0;
			long word_cnt_sum = 0;
			long num_words = 0;
			double avg_doc_len = 0;
			double avg_query_len = 0;

			IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(indexDirNames[i]);
			IndexReader indexReader = indexSearcher.getIndexReader();

			if (i < indexDirNames.length - 1) {
				File queryFile = new File(queryFileNames[i]);
				File relvFile = new File(relevanceDataFileNames[i]);

				List<BaseQuery> baseQueries = new ArrayList<BaseQuery>();
				StrCounterMap relevanceData = new StrCounterMap();

				if (i == 0) {
					baseQueries = QueryReader.readTrecCdsQueries(queryFileNames[i]);
					relevanceData = RelevanceReader.readTrecCdsRelevances(relevanceDataFileNames[i]);
				} else if (i == 1) {
					baseQueries = QueryReader.readClefEHealthQueries(queryFileNames[i]);
					relevanceData = RelevanceReader.readClefEHealthRelevances(relevanceDataFileNames[i]);
				} else if (i == 2) {
					baseQueries = QueryReader.readOhsumedQueries(queryFileNames[i]);
					relevanceData = RelevanceReader.readOhsumedRelevances(relevanceDataFileNames[i]);
				}

				baseQueries = QueryReader.filter(baseQueries, relevanceData);

				num_queries = baseQueries.size();
				num_query_doc_pairs = relevanceData.totalSize();

				for (int j = 0; j < baseQueries.size(); j++) {
					BaseQuery bq = baseQueries.get(j);
					List<String> words = AnalyzerUtils.getWords(bq.getSearchText(), analyzer);
					avg_query_len += words.size();
				}

				avg_query_len /= baseQueries.size();
			}

			num_docs = indexReader.maxDoc();
			word_cnt_sum = indexReader.getSumTotalTermFreq(IndexFieldName.CONTENT);
			avg_doc_len = word_cnt_sum / num_docs;

			Fields fields = MultiFields.getFields(indexReader);
			Terms terms = fields.terms(IndexFieldName.CONTENT);
			num_words = terms.size();

			values[0][i] = num_docs;
			values[1][i] = num_words;
			values[2][i] = avg_doc_len;
			values[3][i] = num_queries;
			values[4][i] = avg_query_len;
			values[5][i] = num_query_doc_pairs;

			// sb.append("Collection\t#Docs\tVoc\tAvg Doc Len\t#Queries\t#Query-Doc Pairs");
		}

		StringBuffer sb = new StringBuffer();
		// sb.append("Collection\t#Docs\tVoc\tAvg Doc Len\t#Queries\t#Avg Query Len\t#Query-Doc Pairs");

		for (int i = 0; i < collNames.length; i++) {
			sb.append(String.format("\t%s", collNames[i]));
		}

		for (int i = 0; i < labels.length; i++) {
			sb.append("\n");
			sb.append(labels[i]);
			for (int j = 0; j < collNames.length; j++) {
				sb.append(String.format("\t%f", values[i][j]));
			}
		}

		// sb.append(String.format("\n%s\t%d\t%d\t%f\t%d\t%f\t%d", collNames[i],
		// num_docs, num_words, avg_doc_len, num_queries,
		// avg_query_len,
		// num_query_doc_pairs));

		System.out.println(sb.toString());

		System.out.println("process ends.");
	}
}
