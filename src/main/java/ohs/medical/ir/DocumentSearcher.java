package ohs.medical.ir;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.medical.ir.query.BaseQuery;
import ohs.medical.ir.query.QueryReader;
import ohs.types.Indexer;
import ohs.types.common.StrCounter;
import ohs.types.common.StrCounterMap;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class DocumentSearcher {

	public static IndexSearcher getIndexSearcher(String indexDirName) throws Exception {
		System.out.printf("open an index at [%s]\n", indexDirName);
		IndexSearcher ret = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexDirName))));
		ret.setSimilarity(new LMDirichletSimilarity());
		// indexSearcher.setSimilarity(new BM25Similarity());
		// indexSearcher.setSimilarity(new DFRSimilarity(new BasicModelBE(), new
		// AfterEffectB(), new NormalizationH1()));
		return ret;
	}

	public static IndexSearcher[] getIndexSearchers(String[] indexDirNames) throws Exception {
		IndexSearcher[] ret = new IndexSearcher[indexDirNames.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = getIndexSearcher(indexDirNames[i]);
		}
		return ret;
	}

	public static QueryParser getQueryParser() throws Exception {
		QueryParser ret = new QueryParser(IndexFieldName.CONTENT, MedicalEnglishAnalyzer.getAnalyzer());
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String[] queryFileNames = MIRPath.QueryFileNames;
		String[] indexDirNames = MIRPath.IndexDirNames;
		String[] resultDirNames = MIRPath.ResultDirNames;

		// for (int i = 0; i < outputDirNames.length; i++) {
		// IOUtils.deleteFilesUnder(new File(outputDirNames[i]));
		// }

		QueryParser queryParser = getQueryParser();

		IndexSearcher[] indexSearchers = new IndexSearcher[indexDirNames.length];

		for (int i = 0; i < indexDirNames.length; i++) {
			indexSearchers[i] = getIndexSearcher(indexDirNames[i]);
		}

		DocumentSearcher ds = new DocumentSearcher(queryParser, indexSearchers);

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> baseQueries = new ArrayList<BaseQuery>();
			StrCounterMap relevanceData = new StrCounterMap();

			String indexDirName = indexDirNames[i];
			String resultDirName = resultDirNames[i];
			String resultFileName = resultDirName + "init.txt";

			if (i == 0) {
				baseQueries = QueryReader.readTrecCdsQueries(queryFileNames[i]);
			} else if (i == 1) {
				baseQueries = QueryReader.readClefEHealthQueries(queryFileNames[i]);
			} else if (i == 2) {
				baseQueries = QueryReader.readOhsumedQueries(queryFileNames[i]);
			}

			baseQueries = QueryReader.filter(baseQueries, relevanceData);
			ds.search(i, baseQueries, resultFileName);
		}

		SearchResultEvaluator e = new SearchResultEvaluator();
		e.evaluate();

		System.out.println("process ends.");
	}

	public static SparseVector search(Query query, IndexSearcher indexSearcher, int top_k) throws Exception {
		TopDocs topDocs = indexSearcher.search(query, top_k);
		int num_docs = topDocs.scoreDocs.length;

		SparseVector ret = new SparseVector(num_docs);
		for (int i = 0; i < topDocs.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = topDocs.scoreDocs[i];
			ret.incrementAtLoc(i, scoreDoc.doc, scoreDoc.score);
		}
		ret.sortByIndex();
		return ret;
	}

	public static SparseVector search(SparseVector queryModel, Indexer<String> wordIndexer, IndexSearcher indexSearcher, int top_k)
			throws Exception {
		Query q = AnalyzerUtils.getQuery((StrCounter) VectorUtils.toCounter(queryModel, wordIndexer));
		return search(q, indexSearcher, top_k);
	}

	private QueryParser queryParser;

	private IndexSearcher[] indexSearchers;

	public DocumentSearcher(QueryParser queryParser, IndexSearcher[] indexSearchers) {
		super();
		this.queryParser = queryParser;
		this.indexSearchers = indexSearchers;
	}

	public void search(int colId, List<BaseQuery> baseQueries, String resultFileName) throws Exception {
		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < baseQueries.size(); i++) {
			BaseQuery baseQuery = baseQueries.get(i);

			Query query = queryParser.parse(baseQuery.getSearchText());

			HyperParameter hyperParameter = new HyperParameter();
			hyperParameter.setTopK(1000);

			SparseVector docScores = search(query, indexSearchers[colId], hyperParameter.getTopK());

			write(writer, baseQuery.getId(), docScores);
		}
		writer.close();
	}

	public void write(TextFileWriter writer, String queryId, SparseVector docScores) {
		docScores.sortByValue();
		for (int i = 0; i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			double score = docScores.valueAtLoc(i);
			writer.write(queryId + "\t" + docId + "\t" + score + "\n");
		}
		docScores.sortByIndex();
	}

}
