package ohs.medical.ir;

import java.io.File;
import java.util.List;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.eval.Performance;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.medical.ir.query.BaseQuery;
import ohs.medical.ir.query.QueryReader;
import ohs.medical.ir.query.RelevanceReader;
import ohs.medical.ir.trec.cds_2015.ProximityRelevanceModelBuilder;
import ohs.medical.ir.trec.cds_2015.TrecCbeemDocumentSearcher;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.common.StrBidMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class Experiments {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		Experiments exp = new Experiments();
		// exp.searchByQLD();
		// exp.searchByKLD();
		// exp.searchByKLDFB();
		exp.searchByCBEEM();
		// exp.searchByKLDPLM();
		// exp.searchByKLDPassage();
		// exp.searchByKLDProximityFB();
		// exp.searchByKLDMultiFieldFB();
		// exp.searchByKLDMultiFieldsProximityFB();
		// exp.searchByCBEEM();
		exp.evalute();
		exp.summarize();
		// format();

		System.out.println("process ends.");
	}

	private Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

	private String[] QueryFileNames = MIRPath.QueryFileNames;

	private String[] IndexDirNames = MIRPath.IndexDirNames;

	private String[] RelevanceFileNames = MIRPath.RelevanceFileNames;

	private String[] OutputDirNames = MIRPath.OutputDirNames;

	private String[] DocIdMapFileNames = MIRPath.DocIdMapFileNames;

	public Experiments() throws Exception {
	}

	public void evalute() throws Exception {

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < OutputDirNames.length; i++) {
			String outoputDirName = OutputDirNames[i];
			String docIdMapFileName = DocIdMapFileNames[i];
			String relFileName = RelevanceFileNames[i];
			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docIdMapFileName);
			CounterMap<String, String> relData = RelevanceReader.readRelevances(relFileName);

			List<File> files = IOUtils.getFilesUnder(outoputDirName + "/temp-res");

			for (int j = 0; j < files.size(); j++) {
				File file = files.get(j);

				if (file.getName().contains("_log")) {
					continue;
				}

				CounterMap<String, String> resultData = PerformanceEvaluator.readSearchResults(file.getPath());
				resultData = DocumentIdMapper.mapIndexIdsToDocIds(resultData, docIdMap);

				PerformanceEvaluator eval = new PerformanceEvaluator();
				eval.setTopNs(new int[] { 10 });
				List<Performance> perfs = eval.evalute(resultData, relData);

				System.out.println(file.getPath());
				sb.append(file.getPath());
				for (int k = 0; k < perfs.size(); k++) {
					sb.append("\n" + perfs.get(k).toString());
				}
				sb.append("\n\n");
			}
		}

		IOUtils.write(MIRPath.PERFORMANCE_FILE, sb.toString().trim());

	}

	public void summarize() {
		TextFileReader reader = new TextFileReader(MIRPath.PERFORMANCE_FILE);
		TextFileWriter writer = new TextFileWriter(MIRPath.PERFORMANCE_SUMMARY_FILE);

		writer.write("FileName\tCollection\tModelName\tRelevant\tRetrieved\tRelInRet\tRelevantAt\tP\tMap\tNDCG\n");

		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			File file = new File(lines.get(0));

			File parent = file.getParentFile().getParentFile().getParentFile();
			File parent2 = parent.getParentFile();

			String collName = parent.getPath();
			int idx = collName.indexOf(parent2.getPath());
			collName = collName.substring(idx + parent2.getPath().length() + 1);

			String fileName = file.getName();
			fileName = IOUtils.removeExtension(fileName);

			int num_fb_iters = 0;
			double mixture_for_fb_model = 0;
			double[] mixtures_for_field_rms = new double[] { 0, 0, 0 };
			int num_fb_docs = 0;
			int num_fb_words = 0;

			String modelName = fileName;

			if (fileName.equals("qld") || fileName.equals("kld") || fileName.equals("cbeem") || fileName.equals("kld-fb")) {

			} else {
				String[] toks = fileName.split("_");
				modelName = toks[0];
				num_fb_iters = Integer.parseInt(toks[1]);
				mixture_for_fb_model = Double.parseDouble(toks[2]);
				mixtures_for_field_rms[0] = Double.parseDouble(toks[3]);
				mixtures_for_field_rms[1] = Double.parseDouble(toks[4]);
				mixtures_for_field_rms[2] = Double.parseDouble(toks[5]);
				num_fb_docs = Integer.parseInt(toks[6]);
				num_fb_words = Integer.parseInt(toks[7]);
			}

			int num_relevant = Integer.parseInt(lines.get(3).split("\t")[1]);
			int num_retrieved = Integer.parseInt(lines.get(4).split("\t")[1]);
			int num_relevant_in_retrieved = Integer.parseInt(lines.get(5).split("\t")[1]);
			int num_relevant_at = Integer.parseInt(lines.get(6).split("\t")[1]);
			double p = Double.parseDouble(lines.get(7).split("\t")[1]);
			double map = Double.parseDouble(lines.get(8).split("\t")[1]);
			double ndcg = Double.parseDouble(lines.get(9).split("\t")[1]);

			String output = String.format("%s\t%s\t%s\t%d\t%d\t%d\t%d\t%f\t%f\t%f",

			file.getPath(), collName, modelName, num_relevant, num_retrieved, num_relevant_in_retrieved, num_relevant_at, p, map, ndcg);

			writer.write(output + "\n");

		}
		reader.close();
		writer.close();
	}

	public void run(int num_fb_iters, double mixture_for_fb_model, double[] mixtures_for_field_rms, int num_fb_docs, int num_fb_words)
			throws Exception {
		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		String outputFileName = null;

		{
			StringBuffer sb = new StringBuffer("kld_fb");
			sb.append(String.format("_%d", num_fb_iters));
			sb.append(String.format("_%s", mixture_for_fb_model + ""));

			for (int i = 0; i < mixtures_for_field_rms.length; i++) {
				sb.append(String.format("_%d", (int) mixtures_for_field_rms[i]));
			}

			sb.append(String.format("_%d", num_fb_docs));
			sb.append(String.format("_%d", num_fb_words));
			sb.append(".txt");
			outputFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + sb.toString();
		}

		System.out.println(outputFileName);

		mixtures_for_field_rms = ArrayUtils.copy(mixtures_for_field_rms);
		ArrayMath.normalize(mixtures_for_field_rms);

		TextFileWriter writer = new TextFileWriter(outputFileName);

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

				RelevanceModelBuilder rmb = new RelevanceModelBuilder(num_fb_docs, num_fb_words, 2000);
				SparseVector rm1 = rmb.getRelevanceModel(wcb1, docScores);
				SparseVector rm2 = rmb.getRelevanceModel(wcb2, docScores);
				SparseVector rm3 = rmb.getRelevanceModel(wcb3, docScores);
				// SparseVector rm4 = rmb.getRelevanceModel(wcb4, wikiScores);

				SparseVector rm = VectorMath.addAfterScale(new Vector[] { rm1, rm2, rm3 }, mixtures_for_field_rms);
				rm.removeZeros();
				rm.normalize();

				expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture_for_fb_model, mixture_for_fb_model);
			}

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.score(wcb, expQueryModel);

			// System.out.println(bq);
			// System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(queryModel, wordIndexer));
			// System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
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

	public void searchByCBEEM() throws Exception {
		System.out.println("search by CBEEM.");

		// String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR };

		String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR,
				MIRPath.TREC_GENOMICS_INDEX_DIR };

		IndexSearcher[] indexSearchers = DocumentSearcher.getIndexSearchers(indexDirNames);

		DenseVector[] docPriorData = new DenseVector[indexSearchers.length];

		for (int i = 0; i < indexDirNames.length; i++) {
			DenseVector docPriors = new DenseVector(indexSearchers[i].getIndexReader().maxDoc());
			double uniform_prior = 1f / docPriors.size();
			docPriors.setAll(uniform_prior);
			docPriorData[i] = docPriors;
		}

		HyperParameter hyperParameter = new HyperParameter();
		hyperParameter.setTopK(1000);
		hyperParameter.setMixtureForAllCollections(0.5);
		// hyperParameter.setNumFBDocs(10);
		// hyperParameter.setNumFBWords(10);

		// String logFileName = logDirName
		// + String.format("cbeem_%s.txt", hyperParameter.toString(true));

		for (int i = 0; i < QueryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(QueryFileNames[i]);
			IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(IndexDirNames[i]);

			String resDirName = OutputDirNames[i];
			String resFileName = resDirName + "temp-res/cbeem.txt";

			CbeemDocumentSearcher ds = new CbeemDocumentSearcher(indexSearchers, docPriorData, hyperParameter, analyzer, false);
			ds.search(i, bqs, null, resFileName, null);
		}

	}

	public void searchByKLD() throws Exception {
		System.out.println("search by KLD.");

		for (int i = 0; i < QueryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(QueryFileNames[i]);
			IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(IndexDirNames[i]);

			String resDirName = OutputDirNames[i];

			resDirName = resDirName + "temp-res/kld.txt";

			TextFileWriter writer = new TextFileWriter(resDirName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

				SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
				docScores.normalizeAfterSummation();

				Indexer<String> wordIndexer = new Indexer<String>();
				SparseVector qLM = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
				qLM.normalize();

				WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb, qLM);

				ResultWriter.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public void searchByKLDFB() throws Exception {
		System.out.println("search by KLD FB.");

		double mixture_for_rm = 0.5;

		for (int i = 0; i < QueryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(QueryFileNames[i]);
			IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(IndexDirNames[i]);

			String resDirName = OutputDirNames[i];

			resDirName = resDirName + "temp-res/kld-fb.txt";

			TextFileWriter writer = new TextFileWriter(resDirName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

				SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
				docScores.normalizeAfterSummation();

				Indexer<String> wordIndexer = new Indexer<String>();
				SparseVector qLM = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
				qLM.normalize();

				WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder(10, 15, 20);
				SparseVector rm = rmb.getRelevanceModel(wcb, docScores);

				SparseVector expQLM = VectorMath.addAfterScale(qLM, rm, 1 - mixture_for_rm, mixture_for_rm);

				KLDivergenceScorer kldScorer = new KLDivergenceScorer();
				docScores = kldScorer.score(wcb, expQLM);

				// System.out.println(bq);
				// System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qLM, wordIndexer));
				// System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(expQLM, wordIndexer));

				ResultWriter.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public void searchByKLDMultiFieldFB() throws Exception {
		System.out.println("search by KLD Multi-fields FB.");

		// double[][] mixture_for_field_rms = { { 0, 0, 100 }, { 0, 100, 0 }, { 100, 0, 0 }, { 50, 50, 50 }, { 50, 30, 20 }, { 20, 30, 50 }
		// };
		// int[] num_fb_iters = { 1, 2 };
		// double[] mixture_for_fb_model = { 0.5 };
		// int[] num_fb_docs = { 5, 10, 15 };
		// int[] num_fb_words = { 10, 15, 20 };

		double[][] mixture_for_field_rms = { { 50, 30, 20 } };
		int[] num_fb_iters = { 1 };
		double[] mixture_for_fb_model = { 0.5 };
		int[] num_fb_docs = { 15 };
		int[] num_fb_words = { 10 };

		for (int l1 = 0; l1 < num_fb_iters.length; l1++) {
			for (int l2 = 0; l2 < mixture_for_fb_model.length; l2++) {
				for (int l3 = 0; l3 < mixture_for_field_rms.length; l3++) {
					for (int l4 = 0; l4 < num_fb_docs.length; l4++) {
						for (int l5 = 0; l5 < num_fb_docs.length; l5++) {
							run(num_fb_iters[l1], mixture_for_fb_model[l2], mixture_for_field_rms[l3], num_fb_docs[l4], num_fb_words[l5]);
						}
					}
				}
			}
		}
	}

	public void searchByKLDMultiFieldsProximityFB() throws Exception {
		System.out.println("search by KLD Multi-Fields Proximity FB.");

		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

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
				rmb.computeWordProximities(expQueryModel, docScores, wcb1);
				SparseVector rm1 = rmb.getRelevanceModel(wcb1, docScores);

				rmb.computeWordProximities(expQueryModel, docScores, wcb2);
				SparseVector rm2 = rmb.getRelevanceModel(wcb2, docScores);

				rmb.computeWordProximities(expQueryModel, docScores, wcb3);
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
			docScores = kldScorer.score(wcb, expQueryModel);

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

	public void searchByKLDPassage() throws Exception {
		System.out.println("search by KLD.");

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_passage.txt";

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
			docScores = scorer.scoreByPassages(wcb, queryModel);
			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDPLM() throws Exception {
		System.out.println("search by KLD PLM.");

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_plm.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			System.out.println(bq);

			Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

			SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
			docScores.normalizeAfterSummation();

			Indexer<String> wordIndexer = new Indexer<String>();
			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.scoreByPLMs(wcb, queryModel);
			ResultWriter.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDProximityFB() throws Exception {
		System.out.println("search by KLD Proximity FB.");

		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

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

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

			// SparseVector wikiScores = DocumentSearcher.search(lbq, wikiIndexSearcher, 50);

			// WordCountBox wcb1 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.TITLE);
			// WordCountBox wcb2 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.ABSTRACT);
			WordCountBox wcb3 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);

			ProximityRelevanceModelBuilder rmb = new ProximityRelevanceModelBuilder(wordIndexer, 10, 15, 2000, 1, false);
			rmb.computeWordProximities(expQueryModel, docScores, wcb3);
			SparseVector rm = rmb.getRelevanceModel(wcb3, docScores);

			// SparseVector rm4 = rmb.getRelevanceModel(wcb4, wikiScores);

			double mixture = 0.5;
			expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture, mixture);

			lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, IndexFieldName.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.score(wcb, expQueryModel);

			System.out.println(bq);
			System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(queryModel, wordIndexer));
			System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
			// System.out.println(rmb.getLogBuffer().toString());
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

	public void searchByQLD() throws Exception {
		System.out.println("search by QLD.");

		for (int i = 0; i < QueryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(QueryFileNames[i]);
			IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(IndexDirNames[i]);

			String resDirName = OutputDirNames[i];

			resDirName = resDirName + "temp-res/qld.txt";

			TextFileWriter writer = new TextFileWriter(resDirName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
				SparseVector docScores = DocumentSearcher.search(lbq, indexSearcher, 1000);
				ResultWriter.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}

	}
}
