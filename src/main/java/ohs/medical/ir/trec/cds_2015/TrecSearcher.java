package ohs.medical.ir.trec.cds_2015;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.ir.eval.Performance;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;
import ohs.medical.ir.BaseQuery;
import ohs.medical.ir.DocumentIdMapper;
import ohs.medical.ir.DocumentSearcher;
import ohs.medical.ir.HyperParameter;
import ohs.medical.ir.KLDivergenceScorer;
import ohs.medical.ir.MIRPath;
import ohs.medical.ir.QueryReader;
import ohs.medical.ir.RelevanceModelBuilder;
import ohs.medical.ir.RelevanceReader;
import ohs.medical.ir.WordCountBox;
import ohs.medical.ir.esa.ESA;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounterMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class TrecSearcher {

	public static void evalute() {
		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		StrCounterMap relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);

		List<File> files = IOUtils.getFilesUnder(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR);

		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);

			if (file.getName().contains("_log")) {
				continue;
			}

			StrCounterMap res = PerformanceEvaluator.readSearchResults(file.getPath());
			StrCounterMap resultData = DocumentIdMapper.mapIndexIdsToDocIds(res, docIdMap);

			PerformanceEvaluator eval = new PerformanceEvaluator();
			eval.setTopNs(new int[] { 10 });
			List<Performance> perfs = eval.evalute(resultData, relevanceData);

			System.out.println(file.getPath());
			for (int j = 0; j < perfs.size(); j++) {
				System.out.println(perfs.get(j).toString());
				System.out.println();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		TrecSearcher tc = new TrecSearcher();
		// tc.searchByQLD();
		// tc.searchByKLD();
		// tc.searchByKLDFB();
		tc.searchByCBEEM();
		// tc.searchByESA();
		// tc.searchByWiki();
		// tc.searchByAbbr();
		evalute();

		System.out.println("process ends.");
	}

	public void searchByAbbr() throws Exception {
		System.out.println("search by Abbrs.");

		IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		AbbrQueryExpander expander = new AbbrQueryExpander(analyzer, MIRPath.ABBREVIATION_FILTERED_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "abbrs.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			String q = bq.getSearchText();
			Counter<String> wWordCounts = expander.expand(q);

			System.out.println(q);

			Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(q, analyzer);
			Query lq = AnalyzerUtils.getQuery(qWordCounts);
			SparseVector docScores = DocumentSearcher.search(lq, indexSearcher, 1000);
			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByCBEEM() throws Exception {
		System.out.println("search by CBEEM.");

		String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR };

		String[] docPriorFileNames = MIRPath.DocPriorFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		IndexSearcher[] indexSearchers = DocumentSearcher.getIndexSearchers(indexDirNames);
		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		DenseVector[] docPriorData = new DenseVector[indexSearchers.length];

		for (int i = 0; i < indexDirNames.length; i++) {
			File inputFile = new File(docPriorFileNames[i]);
			DenseVector docPriors = null;
			if (inputFile.exists()) {
				docPriors = DenseVector.read(inputFile.getPath());
				double uniform_prior = 1f / docPriors.size();
				for (int j = 0; j < docPriors.size(); j++) {
					if (docPriors.value(j) == 0) {
						docPriors.set(j, uniform_prior);
					}
				}
			} else {
				docPriors = new DenseVector(indexSearchers[i].getIndexReader().maxDoc());
				double uniform_prior = 1f / docPriors.size();
				docPriors.setAll(uniform_prior);
			}
			docPriorData[i] = docPriors;
		}

		MedicalEnglishAnalyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		HyperParameter hyperParameter = new HyperParameter();
		hyperParameter.setTopK(1000);
		hyperParameter.setMixtureForAllCollections(0.5);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + String.format("cbeem.txt");
		// String logFileName = logDirName
		// + String.format("cbeem_%s.txt", hyperParameter.toString(true));

		System.out.printf("process for [%s].\n", resultFileName);

		TrecCbeemDocumentSearcher ds = new TrecCbeemDocumentSearcher(indexSearchers, docPriorData, hyperParameter, analyzer,
				wikiIndexSearcher, false);
		ds.search(0, bqs, null, resultFileName, null);

		// SearchResultEvaluator e = new SearchResultEvaluator();
		// e.evaluate();
	}

	public void searchByESA() throws Exception {

		QueryParser queryParser = DocumentSearcher.getQueryParser();

		IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "esa.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		StrCounterMap relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);
		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		ESA esa = new ESA(analyzer);
		esa.read(MIRPath.ICD10_ESA_FILE);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			List<String> words = AnalyzerUtils.getWords(bq.getSearchText(), analyzer);

			SparseVector qcws = esa.getConceptVector(bq.getSearchText());

			// List<String> concepts = conceptWeights.getSortedKeys();
			// Set<String> wordSet = new HashSet<String>();
			// for (int j = 0; j < concepts.size() && j < 5; j++) {
			// String concept = concepts.get(j);
			// List<String> ws = AnalyzerUtils.analyze(concept, analyzer);
			// wordSet.addAll(ws);
			// }
			//
			// words.addAll(wordSet);

			Query lq = AnalyzerUtils.getQuery(words);
			bq.setLuceneQuery(lq);

			SparseVector docScores1 = DocumentSearcher.search(bq.getLuceneQuery(), indexSearcher, 100);
			SparseVector docCosines = docScores1.copy();

			for (int j = 0; j < docScores1.size(); j++) {
				int docId = docScores1.indexAtLoc(j);
				double score = docScores1.valueAtLoc(j);

				Document doc = indexSearcher.doc(docId);
				String text = doc.get(IndexFieldName.CONTENT);

				SparseVector dcws = esa.getConceptVector(text);
				Counter<String> cws = VectorUtils.toCounter(dcws, esa.getConceptIndexer());
				double cosine = VectorMath.cosine(qcws, dcws, false);

				docCosines.setAtLoc(j, docId, cosine);
			}

			docCosines.normalizeAfterSummation();

			double mixture = 0.5;

			for (int j = 0; j < docScores1.size(); j++) {
				int docId = docScores1.indexAtLoc(j);
				double score = docScores1.valueAtLoc(j);
				double cosine = docCosines.valueAlways(docId);
				// double new_score = (1 - mixture) * score + mixture * cosine;
				double new_score = score * Math.exp(cosine);
				writer.write(bq.getId() + "\t" + docId + "\t" + new_score + "\n");
			}

		}

		writer.close();
	}

	public void searchByKLD() throws Exception {
		System.out.println("search by KLD.");

		IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			// System.out.println(bq);

			Counter<String> wordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

			SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

			Indexer<String> wordIndexer = new Indexer<String>();
			SparseVector queryModel = VectorUtils.toSparseVector(wordCounts, wordIndexer, true);
			queryModel.normalize();

			WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.scoreDocuments(wcb, queryModel);

			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDFB() throws Exception {
		System.out.println("search by KLD FB.");

		IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_fb.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			// System.out.println(bq);

			Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
			SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
			docScores.sortByValue();

			Counter<String> fbWordCounts = new Counter<String>();

			for (int j = 0; j < docScores.size() && j < 20; j++) {
				int docId = docScores.indexAtLoc(j);
				double score = docScores.valueAtLoc(j);
				Document doc = indexSearcher.getIndexReader().document(docId);
				String title = doc.get(IndexFieldName.TITLE);
				String abs = doc.get(IndexFieldName.ABSTRACT);
				fbWordCounts.incrementAll(AnalyzerUtils.getWordCounts(title, analyzer));
				// fbWordCounts.incrementAll(AnalyzerUtils.getWordCounts(abs, analyzer));
			}

			List<String> words = fbWordCounts.getSortedKeys();

			for (int j = 0; j < words.size() && j < 10; j++) {
				String word = words.get(j);
				double cnt = fbWordCounts.getCount(word);
				qWordCounts.incrementCount(word, cnt);
			}

			docScores.sortByIndex();

			Indexer<String> wordIndexer = new Indexer<String>();
			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

			// RelevanceModelBuilder rmb = new RelevanceModelBuilder();
			//
			// SparseVector relevanceModel = rmb.getRelevanceModel(wcb, docScores);
			//
			// double mixture = 0.5;
			//
			// SparseVector expQueryModel = VectorMath.addAfterScale(queryModel, relevanceModel, 1 - mixture, mixture);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.scoreDocuments(wcb, queryModel);

			// lbq = AnalyzerUtils.getQuery(qWordCounts);
			// docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByQLD() throws Exception {
		System.out.println("search by QLD.");

		IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "qld.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			System.out.println(bq);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
			SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByWiki() throws Exception {
		System.out.println("serach by wiki.");

		IndexSearcher trecIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);
		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki.txt";
		String logFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki_log.txt";

		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		StrCounterMap relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);

		WikiQueryExpander ex = new WikiQueryExpander(wikiIndexSearcher, analyzer, true, 2000, 0.5, 20, 100);

		TextFileWriter writer = new TextFileWriter(resultFileName);
		TextFileWriter logWriter = new TextFileWriter(logFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			String q = ex.expand(bq);
			BooleanQuery lbq = AnalyzerUtils.getQuery(q, analyzer);
			SparseVector docScores = DocumentSearcher.search(lbq, trecIndexSearcher, 1000);
			logWriter.write(ex.getLogBuffer().toString() + "\n\n");
			ResultWriter.write(writer, bq.getId(), docScores);
		}

		writer.close();
		logWriter.close();
	}

	public void searchByWiki2() throws Exception {

		QueryParser queryParser = DocumentSearcher.getQueryParser();

		IndexSearcher trecIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);
		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		StrCounterMap relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);
		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			Counter<String> qws = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);
			bq.setLuceneQuery(AnalyzerUtils.getQuery(bq.getSearchText(), analyzer));

			SparseVector queryConceptScores = DocumentSearcher.search(bq.getLuceneQuery(), wikiIndexSearcher, 100);
			SparseVector docScores = DocumentSearcher.search(bq.getLuceneQuery(), trecIndexSearcher, 1000);
			List<SparseVector> docConceptScoreData = new ArrayList<SparseVector>();

			System.out.println(bq.toString());

			for (int j = 0; j < docScores.size(); j++) {
				int docId = docScores.indexAtLoc(j);
				double score = docScores.valueAtLoc(j);

				Document doc = trecIndexSearcher.doc(docId);
				String text = doc.get(IndexFieldName.CONTENT);

				Counter<String> docWordCounts = AnalyzerUtils.getWordCounts(text.replace("<NL>", "\n"), analyzer);

				// for (String w : docWordCounts.keySet()) {
				// double cnt = docWordCounts.getCount(w);
				// Term termInstance = new Term(IndexFieldName.CONTENT, w);
				// // long termFreq = trecIndexSearcher.getIndexReader().totalTermFreq(termInstance);
				// double doc_freq = trecIndexSearcher.getIndexReader().docFreq(termInstance);
				// double num_docs = trecIndexSearcher.getIndexReader().maxDoc();
				// double idf = Math.log(1f * (1 + num_docs) / doc_freq);
				// double tf = Math.log(cnt);
				// double tfidf = tf * idf;
				// docWordCounts.setCount(w, tfidf);
				// }

				docWordCounts.keepTopNKeys(qws.size());

				BooleanQuery dbq = AnalyzerUtils.getQuery(docWordCounts);
				SparseVector docConceptScores = DocumentSearcher.search(dbq, wikiIndexSearcher, 100);
				docConceptScoreData.add(docConceptScores);

				StringBuffer sb = new StringBuffer();

				for (int k = 0; k < docConceptScores.size(); k++) {
					int cid = docConceptScores.indexAtLoc(k);
					double conceptScore = docConceptScores.valueAtLoc(k);
					Document article = wikiIndexSearcher.doc(cid);
					String concept = article.get(IndexFieldName.TITLE);
					sb.append(String.format("%d\t%s\t%s", k + 1, concept, conceptScore));

					if (k != docConceptScores.size() - 1) {
						sb.append("\n");
					}
				}

				// System.out.println(text.replace("<NL>", "\n"));
				// System.out.println(docWordCounts.toString());
				// System.out.println(sb.toString());
				// System.out.println();

			}

			double mixture = 0.5;

			for (int j = 0; j < docScores.size(); j++) {
				int docId = docScores.indexAtLoc(j);
				double score = docScores.valueAtLoc(j);

				SparseVector docConceptWeights = docConceptScoreData.get(j);
				double cosine = VectorMath.cosine(queryConceptScores, docConceptWeights, false);
				// double new_score = (1 - mixture) * score + mixture * cosine;
				double new_score = score * Math.exp(cosine);
				writer.write(bq.getId() + "\t" + docId + "\t" + new_score + "\n");
			}

		}

		writer.close();
	}
}
