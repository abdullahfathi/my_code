package ohs.medical.ir.trec.cds_2015;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.matrix.SparseVector;
import ohs.medical.ir.DocumentIdMapper;
import ohs.medical.ir.DocumentSearcher;
import ohs.medical.ir.MIRPath;
import ohs.medical.ir.query.BaseQuery;
import ohs.medical.ir.query.QueryReader;
import ohs.medical.ir.query.RelevanceReader;
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

		String[] relDataFileNames = MIRPath.RelevanceFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		String[] queryDocFileNames = { MIRPath.TREC_CDS_CONCEPT_QUERY_DOC_FILE, MIRPath.CLEF_EHEALTH_CONCEPT_QUERY_DOC_FILE,
				MIRPath.OHSUMED_CONCEPT_QUERY_DOC_FILE };

		IndexSearcher[] indexSearchers = DocumentSearcher.getIndexSearchers(indexDirNames);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		IndexSearcher wikiSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = new ArrayList<BaseQuery>();
			CounterMap<String, String> queryRels = new CounterMap<String, String>();

			// if (!queryFileNames[i].contains("ohsumed")) {
			// continue;
			// }

			String queryFileName = queryFileNames[i];
			String relvFileName = relDataFileNames[i];

			if (i == 0) {
				bqs = QueryReader.readTrecCdsQueries(queryFileName);
				queryRels = RelevanceReader.readTrecCdsRelevances(relvFileName);
			} else if (i == 1) {
				bqs = QueryReader.readClefEHealthQueries(queryFileName);
				queryRels = RelevanceReader.readClefEHealthRelevances(relvFileName);
			} else if (i == 2) {
				bqs = QueryReader.readOhsumedQueries(queryFileName);
				queryRels = RelevanceReader.readOhsumedRelevances(relvFileName);
			}

			IndexSearcher indexSearcher = indexSearchers[i];

			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);

			List<SparseVector> docRelData = DocumentIdMapper.mapDocIdsToIndexIds(bqs, queryRels, docIdMap);

			TextFileWriter writer = new TextFileWriter(queryDocFileNames[i]);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				Counter<String> c = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);
				SparseVector queryConceptWeights = DocumentSearcher.search(AnalyzerUtils.getQuery(c), wikiSearcher, 100);

				writer.write(String.format("Q_%d:\t", j + 1) + getString(queryConceptWeights) + "\n");

				SparseVector docRels = docRelData.get(j);
				docRels.sortByValue();

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

					cc.keepTopNKeys(50);

					SparseVector docConceptWeights = DocumentSearcher.search(AnalyzerUtils.getQuery(cc), wikiSearcher, 100);
					writer.write(rel + "\t" + getString(docConceptWeights) + "\n");
				}
				writer.write("\n");
			}
			writer.close();
		}
	}

	private Counter<String> getConcepts(IndexReader reader, SparseVector conceptWeights) {
		Counter<String> ret = new Counter<String>();

		return ret;
	}

	private String getString(SparseVector sv) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sv.size(); i++) {
			int cid = sv.indexAtLoc(i);
			double score = sv.valueAtLoc(i);
			sb.append(String.format("%s:%f", cid, score));

			if (i != sv.size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}
}
