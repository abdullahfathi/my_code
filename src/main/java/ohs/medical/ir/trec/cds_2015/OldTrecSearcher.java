package ohs.medical.ir.trec.cds_2015;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.ir.eval.Performance;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.medical.ir.DocumentIdMapper;
import ohs.medical.ir.DocumentSearcher;
import ohs.medical.ir.HyperParameter;
import ohs.medical.ir.KLDivergenceScorer;
import ohs.medical.ir.MIRPath;
import ohs.medical.ir.RelevanceModelBuilder;
import ohs.medical.ir.WordCountBox;
import ohs.medical.ir.esa.ESA;
import ohs.medical.ir.query.BaseQuery;
import ohs.medical.ir.query.QueryReader;
import ohs.medical.ir.query.RelevanceReader;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounterMap;
import ohs.utils.KoreanUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class OldTrecSearcher {

	public static void evalute() throws Exception {
		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		CounterMap<String, String>  relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);

		List<File> files = IOUtils.getFilesUnder(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR);

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);

			if (file.getName().contains("_log")) {
				continue;
			}

			StrCounterMap res = PerformanceEvaluator.readSearchResults(file.getPath());
			CounterMap<String, String> resultData = DocumentIdMapper.mapIndexIdsToDocIds(res, docIdMap);

			PerformanceEvaluator eval = new PerformanceEvaluator();
			eval.setTopNs(new int[] { 10 });
			List<Performance> perfs = eval.evalute(resultData, relevanceData);

			System.out.println(file.getPath());
			sb.append(file.getPath());
			for (int j = 0; j < perfs.size(); j++) {
				sb.append("\n" + perfs.get(j).toString());
			}
			sb.append("\n\n");
		}

		IOUtils.write(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_PERFORMANCE_FILE, sb.toString().trim());
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		OldTrecSearcher tc = new OldTrecSearcher();
		// tc.searchByQLD();
		// tc.searchByKLD();
		// tc.searchByKLDSimpleFB();
		// tc.searchByKLDMultiFieldFB();
		tc.searchByKLDProximityFB();
		// tc.searchByCBEEM();
		// tc.searchByESA();
		// tc.searchByWiki();
		// tc.searchByAbbr();
		evalute();

		System.out.println("process ends.");
	}

	private List<BaseQuery> bqs;

	private IndexSearcher indexSearcher;

	private Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

	public OldTrecSearcher() throws Exception {
		bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		indexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);
	}

	public void searchByAbbr() throws Exception {
		System.out.println("search by Abbrs.");

		AbbrQueryExpander expander = new AbbrQueryExpander(analyzer, MIRPath.ABBREVIATION_FILTERED_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "abbrs.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

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

		HyperParameter hyperParameter = new HyperParameter();
		hyperParameter.setTopK(1000);
		hyperParameter.setMixtureForAllCollections(0.5);
		// hyperParameter.setNumFBDocs(10);
		// hyperParameter.setNumFBWords(10);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + String.format("cbeem.txt");
		// String logFileName = logDirName
		// + String.format("cbeem_%s.txt", hyperParameter.toString(true));

		System.out.printf("process for [%s].\n", resultFileName);

		TrecCbeemDocumentSearcher ds = new TrecCbeemDocumentSearcher(indexSearchers, docPriorData, hyperParameter, analyzer,
				wikiIndexSearcher, false);
		ds.search(0, bqs, null, resultFileName, null);

	}

	public void searchByESA() throws Exception {

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "esa.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		CounterMap<String, String>  relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);

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

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			System.out.println(bq);

			Counter<String> wordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

			SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
			docScores.normalizeAfterSummation();

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

	public void searchByKLDMultiFieldFB() throws Exception {
		System.out.println("search by KLD Multi-fields FB.");

		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		IndexReader indexReader = indexSearcher.getIndexReader();

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_fb_multi-field.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = new Indexer<String>();
			StringBuffer qBuf = new StringBuffer(bq.getSearchText());
			Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(qBuf.toString(), analyzer);

			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			SparseVector expQueryModel = queryModel.copy();
			SparseVector docScores = null;

			for (int j = 0; j < 1; j++) {
				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
				docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

				// SparseVector wikiScores = DocumentSearcher.search(lbq, wikiIndexSearcher, 50);

				WordCountBox wcb1 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.TITLE);
				WordCountBox wcb2 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.ABSTRACT);
				WordCountBox wcb3 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);
				// WordCountBox wcb4 = WordCountBox.getWordCountBox(wikiIndexSearcher.getIndexReader(), wikiScores, wordIndexer,
				// IndexFieldName.CONTENT);

				// KLDivergenceScorer kldScorer = new KLDivergenceScorer();
				// docScores = kldScorer.scoreDocuments(wcb3, expQueryModel);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder(10, 15, 2000);
				SparseVector rm1 = rmb.getRelevanceModel(wcb1, docScores);
				SparseVector rm2 = rmb.getRelevanceModel(wcb2, docScores);
				SparseVector rm3 = rmb.getRelevanceModel(wcb3, docScores);
				// SparseVector rm4 = rmb.getRelevanceModel(wcb4, wikiScores);

				double mixture = 0.5;

				double[] mixtures = { 50, 50, 50 };

				ArrayMath.normalize(mixtures);

				SparseVector rm = VectorMath.addAfterScale(new Vector[] { rm1, rm2, rm3 }, mixtures);
				rm.removeZeros();
				rm.normalize();

				expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture, mixture);
			}

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.scoreDocuments(wcb, expQueryModel);

			System.out.println(bq);
			System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(queryModel, wordIndexer));
			System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
			// System.out.printf("RM1:\t%s\n", VectorUtils.toCounter(rm1, wordIndexer));
			// System.out.printf("RM2:\t%s\n", VectorUtils.toCounter(rm2, wordIndexer));
			// System.out.printf("RM3:\t%s\n", VectorUtils.toCounter(rm3, wordIndexer));
			// System.out.printf("RM4:\t%s\n", VectorUtils.toCounter(rm4, wordIndexer));
			// System.out.printf("RM:\t%s\n", VectorUtils.toCounter(rm, wordIndexer));
			// System.out.println();

			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDProximityFB() throws Exception {
		System.out.println("search by KLD Proximity FB.");

		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		IndexReader indexReader = indexSearcher.getIndexReader();

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_fb_proximity.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = new Indexer<String>();
			StringBuffer qBuf = new StringBuffer(bq.getSearchText());
			Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(qBuf.toString(), analyzer);

			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			SparseVector expQueryModel = queryModel.copy();
			SparseVector docScores = null;

			for (int j = 0; j < 1; j++) {
				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
				docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

				// SparseVector wikiScores = DocumentSearcher.search(lbq, wikiIndexSearcher, 50);

				WordCountBox wcb1 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.TITLE);
				WordCountBox wcb2 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.ABSTRACT);
				WordCountBox wcb3 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);

				// WordCountBox wcb4 = WordCountBox.getWordCountBox(wikiIndexSearcher.getIndexReader(), wikiScores, wordIndexer,
				// IndexFieldName.CONTENT);

				// KLDivergenceScorer kldScorer = new KLDivergenceScorer();
				// docScores = kldScorer.scoreDocuments(wcb3, expQueryModel);

				ProximityRelevanceModelBuilder rmb = new ProximityRelevanceModelBuilder(wordIndexer, 10, 15, 2000, 3, false);
				rmb.computeWordProximities(wcb1, expQueryModel);
				SparseVector rm1 = rmb.getRelevanceModel(wcb1, docScores);

				rmb.computeWordProximities(wcb2, expQueryModel);
				SparseVector rm2 = rmb.getRelevanceModel(wcb2, docScores);

				rmb.computeWordProximities(wcb3, expQueryModel);
				SparseVector rm3 = rmb.getRelevanceModel(wcb3, docScores);

				// SparseVector rm4 = rmb.getRelevanceModel(wcb4, wikiScores);

				double mixture = 0.5;

				double[] mixtures = { 50, 50, 50 };

				ArrayMath.normalize(mixtures);

				SparseVector rm = VectorMath.addAfterScale(new Vector[] { rm1, rm2, rm3 }, mixtures);
				rm.removeZeros();
				rm.normalize();

				expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture, mixture);
			}

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.scoreDocuments(wcb, expQueryModel);

			System.out.println(bq);
			System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(queryModel, wordIndexer));
			System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
			// System.out.printf("RM1:\t%s\n", VectorUtils.toCounter(rm1, wordIndexer));
			// System.out.printf("RM2:\t%s\n", VectorUtils.toCounter(rm2, wordIndexer));
			// System.out.printf("RM3:\t%s\n", VectorUtils.toCounter(rm3, wordIndexer));
			// System.out.printf("RM4:\t%s\n", VectorUtils.toCounter(rm4, wordIndexer));
			// System.out.printf("RM:\t%s\n", VectorUtils.toCounter(rm, wordIndexer));
			// System.out.println();

			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDSimpleFB() throws Exception {
		System.out.println("search by KLD Simple FB.");

		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_fb_simple.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = new Indexer<String>();
			StringBuffer qBuf = new StringBuffer(bq.getSearchText());
			SparseVector docScores = null;

			Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(qBuf.toString(), analyzer);

			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
			docScores.normalize();

			WordCountBox wcb = WordCountBox
					.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer, IndexFieldName.ABSTRACT);

			RelevanceModelBuilder rmb = new RelevanceModelBuilder(5, 25, 2000);
			SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
			double mixture = 0.5;

			SparseVector expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture, mixture);

			qWordCounts = VectorUtils.toCounter(expQueryModel, wordIndexer);

			lbq = AnalyzerUtils.getQuery(qWordCounts);

			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
			docScores.normalize();

			wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer, IndexFieldName.ABSTRACT);

			System.out.println(bq);
			System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(queryModel, wordIndexer));
			System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
			System.out.println();

			lbq = AnalyzerUtils.getQuery(qWordCounts);

			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
			docScores.normalizeAfterSummation();

			wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.scoreDocuments(wcb, expQueryModel);

			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByQLD() throws Exception {
		System.out.println("search by QLD.");

		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "qld.txt");

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
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

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki.txt";
		String logFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki_log.txt";

		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		CounterMap<String, String>  relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);

		WikiQueryExpander expander = new WikiQueryExpander(wikiIndexSearcher, analyzer, true, 2000, 0.5, 5, 15);

		TextFileWriter writer = new TextFileWriter(resultFileName);
		TextFileWriter logWriter = new TextFileWriter(logFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			Indexer<String> wordIndexer = new Indexer<String>();

			Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);
			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			SparseVector expQueryModel = expander.expand(wordIndexer, queryModel);

			System.out.println(expander.getLogBuffer().toString());
			System.out.println();
			SparseVector docScores = DocumentSearcher.search(expQueryModel, wordIndexer, trecIndexSearcher, 1000);
			// KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			// WordCountBox wcb = WordCountBox.getWordCountBox(trecIndexSearcher.getIndexReader(), docScores, wordIndexer);
			//
			// docScores = kldScorer.scoreDocuments(wcb, expQueryModel);

			// logWriter.write(ex.getLogBuffer().toString() + "\n\n");
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
		CounterMap<String, String>  relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);
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
