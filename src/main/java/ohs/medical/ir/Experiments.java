package ohs.medical.ir;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.LA;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;
import ohs.medical.ir.query.BaseQuery;
import ohs.medical.ir.query.QueryReader;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.common.IntCounter;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounter;
import ohs.types.common.StrCounterMap;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class Experiments {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		Experiments e = new Experiments();
		// e.searchByQLD();
		// e.searchByKLD();
		// e.searchByKLDFB();
		// e.searchByKldFbPriors();
		// e.searchByCBEEM();
		// e.searchSentsByQLD();
		// e.searchSentsByKLDFB();

		// e.searchByKldFbWordVectors();
		e.searchByKldFbWordVectorExp();
		// e.searchByKldFbWordVectorPrior();

		SearchResultEvaluator.main(args);

		System.out.println("process ends.");
	}

	private MedicalEnglishAnalyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

	private String[] queryFileNames = MIRPath.QueryFileNames;

	private String[] indexDirNames = MIRPath.IndexDirNames;

	private String[] resDirNames = MIRPath.ResultDirNames;

	private String[] docIdMapFileNames = MIRPath.DocIdMapFileNames;

	private String[] logDirNames = MIRPath.LogDirNames;

	private String[] relFileNames = MIRPath.RelevanceFileNames;

	private IndexSearcher[] iss = SearcherUtils.getIndexSearchers(indexDirNames);

	private String[] docPriorFileNames = MIRPath.DocPriorFileNames;

	public Experiments() throws Exception {

	}

	public double[] getVectorSum(IndexReader ir, Word2VecSearcher searcher, Counter<String> wcs) throws Exception {
		double[] ret = new double[searcher.getLayerSize()];

		double norm = 0;

		for (String word : wcs.keySet()) {
			double cnt = wcs.getCount(word);
			double tf = Math.log(cnt) + 1;
			double doc_freq = ir.docFreq(new Term(IndexFieldName.CONTENT, word));
			double num_docs = ir.maxDoc() + 1;
			double idf = Math.log((num_docs + 1) / doc_freq);
			double tfidf = tf * idf;
			wcs.setCount(word, tfidf);

			norm += (tfidf * tfidf);
		}

		norm = Math.sqrt(norm);
		wcs.scale(1f / norm);

		int num_words = 0;

		List<String> words = wcs.getSortedKeys();

		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			double tfidf = wcs.getCount(word);
			double[] v = searcher.getVector(word);
			if (v != null) {
				double sum = ArrayMath.addAfterScale(ret, v, 1, tfidf, ret);
				num_words++;
			}
		}

		ArrayMath.scale(ret, 1f / num_words, ret);

		return ret;
	}

	public void searchByCBEEM() throws Exception {
		System.out.println("search by CBEEM.");

		DenseVector[] docPriorData = new DenseVector[iss.length];

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
				docPriors = new DenseVector(iss[i].getIndexReader().maxDoc());
				double uniform_prior = 1f / docPriors.size();
				docPriors.setAll(uniform_prior);
			}
			docPriorData[i] = docPriors;
		}

		HyperParameter hp = new HyperParameter();

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			String outputFileName = resDirNames[i] + "cbeem.txt";
			CbeemDocumentSearcher cbeemSearcher = new CbeemDocumentSearcher(iss, docPriorData, hp, analyzer, false);
			cbeemSearcher.search(i, bqs, null, outputFileName, null);
		}

	}

	public void searchByKLD() throws Exception {
		System.out.println("search by KLD.");

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];

			String outputFileName = resDirNames[i] + "kld.txt";

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				System.out.println(bq);

				BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
				SparseVector docScores = SearcherUtils.search(lbq, is, 1000);
				docScores.normalizeAfterSummation();

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
				qlm.normalize();

				WordCountBox wcb = WordCountBox.getWordCountBox(is.getIndexReader(), docScores, wordIndexer);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb, qlm);

				SearcherUtils.write(writer, bq.getId(), docScores);
			}
			writer.close();
		}

	}

	public void searchByKLDFB() throws Exception {
		System.out.println("search by KLD FB.");

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			String outputFileName = resDirNames[i] + "kld-fb.txt";

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
				qlm.normalize();

				SparseVector eqlm = qlm.copy();
				SparseVector docScores = null;

				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(eqlm, wordIndexer));
				docScores = SearcherUtils.search(lbq, is, 1000);

				WordCountBox wcb = WordCountBox.getWordCountBox(ir, docScores, wordIndexer, IndexFieldName.CONTENT);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder();
				SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
				// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3, docScores);

				double rm_mixture = 0.5;

				eqlm = VectorMath.addAfterScale(qlm, rm, 1 - rm_mixture, rm_mixture);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb, eqlm);

				System.out.println(bq);
				System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm, wordIndexer));
				System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(eqlm, wordIndexer));

				SearcherUtils.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public void searchByKldFbPriors() throws Exception {
		System.out.println("search by KLD FB Priors.");

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			String outputFileName = resDirNames[i] + "kld-fb_prior.txt";

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
				qlm.normalize();

				SparseVector eqlm = qlm.copy();
				SparseVector docScores = null;

				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(eqlm, wordIndexer));
				docScores = SearcherUtils.search(lbq, is, 1000);

				WordCountBox wcb = WordCountBox.getWordCountBox(ir, docScores, wordIndexer, IndexFieldName.CONTENT);

				DocumentCentralityEstimator dce = new DocumentCentralityEstimator(wcb);
				SparseVector docPriors = dce.estimate();

				RelevanceModelBuilder rmb = new RelevanceModelBuilder();
				SparseVector rm = rmb.getRelevanceModel(wcb, docScores, docPriors);
				// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3, docScores);

				double rm_mixture = 0.5;

				eqlm = VectorMath.addAfterScale(qlm, rm, 1 - rm_mixture, rm_mixture);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb, eqlm);

				System.out.println(bq);
				System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm, wordIndexer));
				System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(eqlm, wordIndexer));

				SearcherUtils.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public StrCounter expand(IndexReader ir, Word2VecSearcher searcher, StrCounter wcs) throws IOException {

		StrCounter wwc = new StrCounter();
		double norm = 0;

		for (String word : wcs.keySet()) {
			double cnt = wcs.getCount(word);
			double tf = Math.log(cnt) + 1;
			double doc_freq = ir.docFreq(new Term(IndexFieldName.CONTENT, word));
			double num_docs = ir.maxDoc() + 1;
			double idf = Math.log((num_docs + 1) / doc_freq);
			double tfidf = tf * idf;
			wwc.setCount(word, tfidf);
			norm += (tfidf * tfidf);
		}

		norm = Math.sqrt(norm);
		wwc.scale(1f / norm);

		StrCounterMap cm = new StrCounterMap();
		double[] qwv = new double[searcher.getLayerSize()];

		for (String word : wwc.getSortedKeys()) {
			double[] v = searcher.getVector(word);
			if (v != null) {
				double tfidf = wwc.getCount(word);
				ArrayMath.addAfterScale(qwv, v, 1, tfidf, qwv);
			}
		}

		ArrayMath.unitVector(qwv, qwv);

		double[][] wvs = searcher.getVectors();
		StrCounter c = new StrCounter();
		for (int i = 0; i < wvs.length; i++) {
			String word = searcher.getWordIndexer().getObject(i);
			if (word.contains("<N")) {
				continue;
			}
			double sim = ArrayMath.dotProduct(qwv, wvs[i]);
			c.incrementCount(word, sim);

		}

		StrCounter ret = new StrCounter(wcs);

		List<String> words = c.getSortedKeys();
		for (int i = 0; i < words.size() && i < 5; i++) {
			ret.incrementCount(words.get(i), 1);
		}

		// System.out.println(wcs.toString());
		// System.out.println(c.toString());
		// System.out.println(ret.toString());
		// System.out.println();

		return ret;
	}

	public void searchByKldFbWordVectorExp() throws Exception {
		System.out.println("search by KLD FB Word Vector Exp.");

		Word2VecSearcher vSearcher = new Word2VecSearcher(Word2VecModel.fromSerFile("../../data/medical_ir/ohsumed/word2vec_model.ser.gz"));

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			String outputFileName = resDirNames[i] + "kld-fb_wv-exp.txt";

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs1 = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm1 = VectorUtils.toSparseVector(qwcs1, wordIndexer, true);
				qlm1.normalize();

				StrCounter qwcs2 = expand(ir, vSearcher, qwcs1);

				SparseVector qlm2 = VectorUtils.toSparseVector(qwcs2, wordIndexer, true);
				qlm2.normalize();

				int num_ret_docs = 1000;

				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(qlm2, wordIndexer));
				SparseVector docScores = SearcherUtils.search(lbq, is, num_ret_docs);

				WordCountBox wcb = WordCountBox.getWordCountBox(ir, docScores, wordIndexer, IndexFieldName.CONTENT);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				scorer.score(wcb, qlm1);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder();
				SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
				// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3, docScores);

				double mixture = 0.5;

				SparseVector qlm3 = VectorMath.addAfterScale(qlm1, rm, 1 - mixture, mixture);

				docScores = scorer.score(wcb, qlm3);

				docScores.normalizeAfterSummation();

				System.out.println(bq);
				System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm1, wordIndexer));
				System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(qlm2, wordIndexer));
				System.out.printf("QM3:\t%s\n", VectorUtils.toCounter(qlm3, wordIndexer));

				SearcherUtils.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public void searchByKldFbWordVectorPrior() throws Exception {
		System.out.println("search by KLD FB Word Vector Prior.");

		Word2VecSearcher vSearcher = new Word2VecSearcher(Word2VecModel.fromSerFile("../../data/medical_ir/ohsumed/word2vec_model.ser.gz"));
		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			String outputFileName = resDirNames[i] + "kld-fb_wv-priors.txt";

			// IOUtils.deleteFilesUnder(resDirNames[i]);

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
				qlm.normalize();

				SparseVector eqlm = qlm.copy();

				int num_ret_docs = 1000;

				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(eqlm, wordIndexer));
				SparseVector docScores = SearcherUtils.search(lbq, is, num_ret_docs);
				SparseVector docPriors = docScores.copy();

				WordCountBox wcb = WordCountBox.getWordCountBox(ir, docScores, wordIndexer, IndexFieldName.CONTENT);

				double[] qwv = getVectorSum(ir, vSearcher, qwcs);
				double[][] dwvs = new double[num_ret_docs][];

				for (int k = 0; k < num_ret_docs; k++) {
					int docId = docScores.indexAtLoc(k);
					SparseVector dwcs = wcb.getDocWordCounts().vectorAtRowLoc(k);
					double[] dwv = getVectorSum(ir, vSearcher, VectorUtils.toCounter(dwcs, wordIndexer));
					dwvs[k] = dwv;
				}

				double[][] sim_mat = ArrayUtils.newMatrix(dwvs.length, 0);

				for (int k = 0; k < dwvs.length; k++) {
					double[] dwv1 = dwvs[k];
					sim_mat[k][k] = 1;

					for (int l = k + 1; l < dwvs.length; l++) {
						double[] dwv2 = dwvs[l];
						double cosine = ArrayMath.cosine(dwv1, dwv2);
						sim_mat[k][l] = cosine;
						sim_mat[l][k] = cosine;
					}
				}

				for (int k = 0; k < sim_mat.length; k++) {
					double[] sim = sim_mat[k];
					int[] indexes = ArrayUtils.rankedIndexes(sim);

					for (int l = 10; l < sim.length; l++) {
						sim[indexes[l]] = 0;
					}
				}

				LA.transpose(sim_mat);

				ArrayMath.normalizeColumns(sim_mat);

				double[] cents = new double[sim_mat.length];
				ArrayUtils.setAll(cents, 1f / cents.length);

				ArrayMath.doRandomWalk(sim_mat, cents, 10, 0.00000001, 0.85);
				docPriors.setValues(cents);
				docPriors.summation();

				RelevanceModelBuilder rmb = new RelevanceModelBuilder();
				SparseVector rm = rmb.getRelevanceModel(wcb, docScores, docPriors);

				double rm_mixture = 0.5;

				eqlm = VectorMath.addAfterScale(qlm, rm, 1 - rm_mixture, rm_mixture);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb, eqlm);

				System.out.println(bq);
				System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm, wordIndexer));
				System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(eqlm, wordIndexer));

				SearcherUtils.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public void searchByKldFbWordVectors() throws Exception {
		System.out.println("search by KLD FB Word Vectors.");

		Word2VecSearcher vSearcher = new Word2VecSearcher(Word2VecModel.fromSerFile("../../data/medical_ir/ohsumed/word2vec_model.ser.gz"));

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			String outputFileName = resDirNames[i] + "kld-fb_wv.txt";

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
				qlm.normalize();

				SparseVector eqlm = qlm.copy();

				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(eqlm, wordIndexer));
				SparseVector docScores = SearcherUtils.search(lbq, is, 1000);

				WordCountBox wcb = WordCountBox.getWordCountBox(ir, docScores, wordIndexer, IndexFieldName.CONTENT);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder();
				SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
				// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3, docScores);

				double rm_mixture = 0.5;

				eqlm = VectorMath.addAfterScale(qlm, rm, 1 - rm_mixture, rm_mixture);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb, eqlm);

				SparseVector docScores2 = docScores.copy();
				double[] qwv = getVectorSum(ir, vSearcher, qwcs);

				for (int k = 0; k < docScores.size(); k++) {
					int docId = docScores.indexAtLoc(k);
					SparseVector dwcs = wcb.getDocWordCounts().vectorAtRowLoc(k);
					double[] dwv = getVectorSum(ir, vSearcher, VectorUtils.toCounter(dwcs, wordIndexer));
					double cosine = ArrayMath.cosine(qwv, dwv);
					docScores2.setAtLoc(k, cosine);
				}

				ArrayMath.multiply(docScores.values(), docScores2.values(), docScores.values());
				docScores.normalizeAfterSummation();

				System.out.println(bq);
				System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm, wordIndexer));
				System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(eqlm, wordIndexer));

				SearcherUtils.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public void searchByQLD() throws Exception {
		System.out.println("search by QLD.");

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];

			IOUtils.deleteFilesUnder(resDirNames[i]);

			String outFileName = resDirNames[i] + "qld.txt";

			TextFileWriter writer = new TextFileWriter(outFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
				SparseVector docScores = SearcherUtils.search(lbq, is, 1000);
				SearcherUtils.write(writer, bq.getId(), docScores);
			}
			writer.close();
		}
	}

	public void searchSentsByKldFb() throws Exception {
		System.out.println("search by KLD FB.");

		IndexSearcher[] siss = SearcherUtils.getIndexSearchers(MIRPath.SentIndexDirNames);

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher sis = siss[i];

			String outFileName = resDirNames[i].replace("result", "result_sent") + "kld-fb.txt";
			String logFileName = logDirNames[i].replace("log", "log_sent") + "kld-fb.txt";

			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docIdMapFileNames[i]);

			// IOUtils.deleteFilesUnder(resDirNames[i]);

			TextFileWriter resWriter = new TextFileWriter(outFileName);
			// TextFileWriter logWriter = new TextFileWriter(logFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
				SparseVector sentScores = SearcherUtils.search(lbq, sis, 1000);

				// logWriter.write(bq.toString());

				CounterMap<String, Integer> cm = new CounterMap<String, Integer>();

				for (int k = 0; k < sentScores.size(); k++) {
					int sentId = sentScores.indexAtLoc(k);
					double score = sentScores.valueAtLoc(k);
					Document doc = sis.doc(sentId);
					String sent = doc.get(IndexFieldName.CONTENT);
					String docId = doc.get(IndexFieldName.DOCUMENT_ID);
					// logWriter.write(String.format("\n%d\t%s\t%s\t%s", k + 1, did, score, sent));
					cm.incrementCount(docId, sentId, score);
				}
				// logWriter.write("\n\n");

				IntCounter c = new IntCounter();

				for (String docId : cm.keySet()) {
					String s = docIdMap.getKey(docId);
					if (s == null) {
						continue;
					}
					int indexId = Integer.parseInt(s);
					c.incrementCount(indexId, cm.getCounter(docId).max());
				}

				SparseVector docScores = VectorUtils.toSparseVector(c);

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
				qlm.normalize();

				SparseVector expQLM = qlm.copy();

				WordCountBox wcb1 = WordCountBox.getWordCountBox(siss[i].getIndexReader(), sentScores, wordIndexer, IndexFieldName.CONTENT);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder(10, 15, 20);
				SparseVector rm = rmb.getRelevanceModel(wcb1, sentScores);
				// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3, docScores);

				double mixture = 0.5;

				expQLM = VectorMath.addAfterScale(qlm, rm, 1 - mixture, mixture);

				WordCountBox wcb2 = WordCountBox.getWordCountBox(iss[i].getIndexReader(), docScores, wordIndexer, IndexFieldName.CONTENT);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb2, expQLM);

				SearcherUtils.write(resWriter, bq.getId(), docScores);
			}
			resWriter.close();
			// logWriter.close();
		}
	}

	public void searchSentsByQLD() throws Exception {
		System.out.println("search by QLD.");

		IndexSearcher[] siss = SearcherUtils.getIndexSearchers(MIRPath.SentIndexDirNames);

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher sis = siss[i];

			String outputFileName = resDirNames[i].replace("result", "result_sent") + "qld.txt";
			String logFileName = logDirNames[i].replace("log", "log_sent") + "gld.txt";

			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docIdMapFileNames[i]);

			// IOUtils.deleteFilesUnder(resDirNames[i]);

			TextFileWriter resWriter = new TextFileWriter(outputFileName);
			TextFileWriter logWriter = new TextFileWriter(logFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
				SparseVector sentScores = SearcherUtils.search(lbq, sis, 1000);

				logWriter.write(bq.toString());

				CounterMap<String, Integer> cm = new CounterMap<String, Integer>();

				for (int k = 0; k < sentScores.size(); k++) {
					int sentId = sentScores.indexAtLoc(k);
					double score = sentScores.valueAtLoc(k);
					Document doc = sis.doc(sentId);
					String sent = doc.get(IndexFieldName.CONTENT);
					String docId = doc.get(IndexFieldName.DOCUMENT_ID);
					logWriter.write(String.format("\n%d\t%s\t%s\t%s", k + 1, docId, score, sent));
					cm.incrementCount(docId, sentId, score);
				}
				logWriter.write("\n\n");

				IntCounter c = new IntCounter();

				for (String docId : cm.keySet()) {
					String s = docIdMap.getKey(docId);
					if (s == null) {
						continue;
					}
					int indexId = Integer.parseInt(s);
					c.incrementCount(indexId, cm.getCounter(docId).average());
				}

				SparseVector docScores = VectorUtils.toSparseVector(c);

				SearcherUtils.write(resWriter, bq.getId(), docScores);
			}
			resWriter.close();
			logWriter.close();
		}
	}

}
