package ohs.medical.ir;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounter;
import ohs.types.common.StrCounterMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

public class ConceptRelevanceCollector {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ConceptRelevanceCollector c = new ConceptRelevanceCollector();
		c.collect();

		System.out.println("process ends.");
	}

	public void collect() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR };

		String[] relDataFileNames = MIRPath.RelevanceDataFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		String[] queryDocFileNames = { MIRPath.TREC_CDS_QUERY_DOC_FILE, MIRPath.CLEF_EHEALTH_QUERY_DOC_FILE, MIRPath.OHSUMED_QUERY_DOC_FILE };

		IndexSearcher[] indexSearchers = DocumentSearcher.getIndexSearchers(indexDirNames);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		IndexSearcher wikiSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = new ArrayList<BaseQuery>();
			StrCounterMap queryRels = new StrCounterMap();

			// if (!queryFileNames[i].contains("ohsumed")) {
			// continue;
			// }

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
			}

			IndexSearcher indexSearcher = indexSearchers[i];

			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);

			List<SparseVector> docRelData = DocumentIdMapper.mapDocIdsToIndexIds(bqs, queryRels, docIdMap);
			List<SparseVector> queryConceptData = new ArrayList<SparseVector>();

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				Counter<String> c = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);
				SparseVector queryConceptWeights = DocumentSearcher.search(AnalyzerUtils.getQuery(c), wikiSearcher, 100);
				queryConceptData.add(queryConceptWeights);

				SparseVector docRels = docRelData.get(j);
				List<SparseVector> docConceptData = new ArrayList<SparseVector>();

				for (int k = 0; k < docRels.size(); k++) {
					int docId = docRels.indexAtLoc(k);
					double rel = docRels.valueAtLoc(k);

					Document doc = indexSearcher.getIndexReader().document(docId);

					Terms terms = indexSearcher.getIndexReader().getTermVector(docId, IndexFieldName.CONTENT);

					if (terms == null) {
						continue;
					}

					TermsEnum termsEnum = terms.iterator(null);

					BytesRef bytesRef = null;
					PostingsEnum postingsEnum = null;
					StrCounter cc = new StrCounter();

					while ((bytesRef = termsEnum.next()) != null) {
						postingsEnum = termsEnum.postings(null, postingsEnum, PostingsEnum.ALL);

						if (postingsEnum.nextDoc() != 0) {
							throw new AssertionError();
						}

						String word = bytesRef.utf8ToString();

						if (word.equals("null")) {
							continue;
						}
						int freq = postingsEnum.freq();
						cc.incrementCount(word, freq);

						// for (int k = 0; k < freq; k++) {
						// final int position = postingsEnum.nextPosition();
						// }
					}

					SparseVector docConceptWeights = DocumentSearcher.search(AnalyzerUtils.getQuery(cc), wikiSearcher, 100);
					docConceptData.add(docConceptWeights);
				}
				
				
				
				
			}

			IndexReader indexReader = indexSearchers[i].getIndexReader();

			// List<CounterMap<Integer, String>> dcms = setWordCountBoxes(indexReader, docRelevances);

			if (bqs.size() != docRelData.size()) {
				throw new Exception();
			}

			TextFileWriter writer = new TextFileWriter(queryDocFileNames[i]);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				StrCounter qwc = qcs.get(j);
				SparseVector docRels = docRelData.get(j);

				CounterMap<Integer, String> dcm = new CounterMap<Integer, String>();

				for (int k = 0; k < docRels.size(); k++) {
					int docId = docRels.indexAtLoc(k);
					double relevance = docRels.valueAtLoc(k);
					Document doc = indexReader.document(docId);

					Terms terms = indexReader.getTermVector(docId, IndexFieldName.CONTENT);

					if (terms == null) {
						continue;
					}

					TermsEnum termsEnum = terms.iterator(null);

					BytesRef bytesRef = null;
					PostingsEnum postingsEnum = null;
					StrCounter c = new StrCounter();

					while ((bytesRef = termsEnum.next()) != null) {
						postingsEnum = termsEnum.postings(null, postingsEnum, PostingsEnum.ALL);

						if (postingsEnum.nextDoc() != 0) {
							throw new AssertionError();
						}

						String word = bytesRef.utf8ToString();

						if (word.equals("null")) {
							continue;
						}
						int freq = postingsEnum.freq();
						c.incrementCount(word, freq);

						// for (int k = 0; k < freq; k++) {
						// final int position = postingsEnum.nextPosition();
						// }
					}
					dcm.setCounter(docId, c);
				}

				writer.write(String.format("#Query\t%d\t%s\n", j + 1, qwc.toStringSortedByValues(true, false, qwc.size())));

				List<Integer> docIds = new ArrayList<Integer>(dcm.keySet());
				Collections.sort(docIds);

				for (int k = 0; k < docIds.size(); k++) {
					int docId = docIds.get(k);
					Counter<String> c = dcm.getCounter(docId);
					double relevance = docRels.valueAlways(docId);
					writer.write(String.format("%d\t%d\t%s\n", docId, (int) relevance, c.toStringSortedByValues(true, false, c.size())));
				}
				writer.write("\n");
			}
		}
	}

}
