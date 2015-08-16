package ohs.medical.ir;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.medical.ir.query.BaseQuery;
import ohs.medical.ir.query.QueryReader;
import ohs.medical.ir.query.RelevanceReader;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounterMap;
import ohs.utils.TermWeighting;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class RelevanceCollector {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		RelevanceCollector c = new RelevanceCollector();
		c.collect();

		System.out.println("process ends.");
	}

	public void collect() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] relDataFileNames = MIRPath.RelevanceDataFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		String[] queryDocFileNames = MIRPath.QueryDocFileNames;

		IndexSearcher[] indexSearchers = DocumentSearcher.getIndexSearchers(indexDirNames);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = new ArrayList<BaseQuery>();
			CounterMap<String, String> queryRels = new CounterMap<String, String>();

			File queryFile = new File(queryFileNames[i]);
			File relvFile = new File(relDataFileNames[i]);

			if (i == 0) {
				bqs = QueryReader.readTrecCdsQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readTrecCdsRelevances(relDataFileNames[i]);
			} else if (i == 1) {
				bqs = QueryReader.readClefEHealthQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readClefEHealthRelevances(relDataFileNames[i]);
			} else if (i == 2) {
				bqs = QueryReader.readOhsumedQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readOhsumedRelevances(relDataFileNames[i]);
			} else if (i == 3) {
				bqs = QueryReader.readTrecGenomicsQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readTrecGenomicsRelevances(relDataFileNames[i]);
			}

			List<Counter<String>> qs = new ArrayList<Counter<String>>();

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				qs.add(AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer));
			}

			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);

			// queryRelevances = RelevanceReader.filter(queryRelevances, docIdMap);

			// baseQueries = QueryReader.filter(baseQueries, queryRelevances);

			List<SparseVector> docRelData = DocumentIdMapper.mapDocIdsToIndexIds(bqs, queryRels, docIdMap);

			IndexReader indexReader = indexSearchers[i].getIndexReader();

			if (bqs.size() != docRelData.size()) {
				throw new Exception();
			}

			TextFileWriter writer = new TextFileWriter(queryDocFileNames[i]);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				SparseVector docRels = docRelData.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();

				Counter<String> qwcs = qs.get(j);

				SparseVector q = VectorUtils.toSparseVector(qs.get(j), wordIndexer, true);

				{
					SparseVector docFreqs = VectorUtils.toSparseVector(
							WordCountBox.getDocFreqs(indexReader, IndexFieldName.CONTENT, qs.get(j)), wordIndexer, true);
					computeTFIDFs(q, docFreqs, indexReader.maxDoc());

				}

				WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docRels, wordIndexer);
				SparseMatrix sm = wcb.getDocWordCounts();
				SparseVector docFreqs = wcb.getCollDocFreqs();

				for (int k = 0; k < sm.rowSize(); k++) {
					int docId = sm.indexAtRowLoc(k);
					SparseVector sv = sm.vectorAtRowLoc(k);
					computeTFIDFs(sv, docFreqs, wcb.getNumDocsInCollection());
				}

				writer.write(String.format("#Query\t%d\t%s\n", j + 1, toString(VectorUtils.toCounter(q, wordIndexer))));

				docRels.sortByValue();

				for (int k = 0; k < docRels.size(); k++) {
					int docId = docRels.indexAtLoc(k);
					double rel = docRels.valueAtLoc(k);
					SparseVector sv = sm.rowAlways(docId);

					if (sv.size() == 0) {
						continue;
					}

					writer.write(String.format("%d\t%d\t%s\n", docId, (int) rel, toString(VectorUtils.toCounter(sv, wordIndexer))));
				}
				writer.write("\n");
			}
		}
	}

	private void computeTFIDFs(SparseVector wcs, SparseVector docFreqs, double num_docs) {
		double norm = 0;
		for (int i = 0; i < wcs.size(); i++) {
			int w = wcs.indexAtLoc(i);
			double cnt = wcs.valueAtLoc(i);
			double doc_freq = docFreqs.valueAlways(w);
			double tf = Math.log(cnt) + 1;
			double idf = Math.log((num_docs + 1) / doc_freq);
			double tfidf = tf * idf;
			wcs.setAtLoc(i, tfidf);

			norm += (tfidf * tfidf);
		}
		norm = Math.sqrt(norm);
		wcs.scale(1f / norm);
	}

	public String toString(Counter<String> c) {
		StringBuffer sb = new StringBuffer();
		List<String> keys = c.getSortedKeys();
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			double value = c.getCount(key);
			sb.append(String.format("%s:%f", key, value));
			if (i != keys.size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

}
