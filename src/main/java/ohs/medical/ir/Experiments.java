package ohs.medical.ir;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Searcher.Match;
import com.medallia.word2vec.Searcher.UnknownWordException;
import com.medallia.word2vec.Word2VecModel;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
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

	public double[] getVectorSum(Searcher searcher, Counter<String> docFreqs, String s) {
		double[] ret = new double[searcher.getModel().getLayerSize()];

		String[] words = s.toLowerCase().split("[\\s\\W]+");
		Counter<String> wcs = new Counter<String>();

		for (String word : words) {
			wcs.incrementCount(word, 1);
		}

		double norm = 0;

		for (String word : wcs.keySet()) {
			double cnt = wcs.getCount(word);
			double tf = Math.log(cnt) + 1;
			double doc_freq = docFreqs.getCount(word);
			double num_docs = docFreqs.getCount("#docs#") + 1;
			double idf = Math.log((num_docs + 1) / doc_freq);
			double tfidf = tf * idf;
			wcs.setCount(word, tfidf);

			norm += (tfidf * tfidf);
		}

		norm = Math.sqrt(norm);

		wcs.scale(1f / norm);

		wcs.keepTopNKeys(20);

		int num_words = 0;

		for (String word : wcs.keySet()) {
			double tfidf = wcs.getCount(word);

			double[] v = searcher.getVector(word);
			if (v.length > 0) {
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

	public void expand(Word2VecSearcher searcher, Set<String> stopwords, String s) {

		String[] words = s.toLowerCase().split("[\\s\\W]+");
		Counter<String> ret = new Counter<>();

		StrCounterMap cm = new StrCounterMap();

		for (String word : words) {
			if (stopwords.contains(word)) {
				continue;
			}
			Counter<String> wordScores = searcher.search(word);
			cm.setCounter(word, wordScores);
		}
		
		System.out.println();
	}

	public void searchByKldFbWordVectorExp() throws Exception {
		System.out.println("search by KLD FB Word Vector Exp.");

		// Searcher vSearcher = Word2VecModel.fromSerFile("../../data/medical_ir/ohsumed/word2vec_model.ser.gz").forSearch();
		Word2VecSearcher vSearcher = new Word2VecSearcher(Word2VecModel.fromSerFile("../../data/medical_ir/ohsumed/word2vec_model.ser.gz"));
		String[] docFreqFileNames = MIRPath.TaskDirs;

		Set<String> stopwords = IOUtils.readSet(MIRPath.STOPWORD_INQUERY_FILE);

		for (int i = 0; i < docFreqFileNames.length; i++) {
			docFreqFileNames[i] += "vocab2.txt";
		}

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			String outputFileName = resDirNames[i] + "kld-fb_wv-exp.txt";

			Counter<String> docFreqs = IOUtils.readCounter(docFreqFileNames[i]);

			// IOUtils.deleteFilesUnder(resDirNames[i]);

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();
				StrCounter qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

				SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
				qlm.normalize();

				expand(vSearcher, stopwords, bq.getSearchText());

				SparseVector eqlm = qlm.copy();

				int num_ret_docs = 1000;

				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(eqlm, wordIndexer));
				SparseVector docScores = SearcherUtils.search(lbq, is, num_ret_docs);

				WordCountBox wcb = WordCountBox.getWordCountBox(ir, docScores, wordIndexer, IndexFieldName.CONTENT);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder();
				SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
				// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3, docScores);

				double mixture = 0.5;

				eqlm = VectorMath.addAfterScale(qlm, rm, 1 - mixture, mixture);

				KLDivergenceScorer scorer = new KLDivergenceScorer();
				docScores = scorer.score(wcb, eqlm);

				docScores.normalizeAfterSummation();

				System.out.println(bq);
				System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm, wordIndexer));
				System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(eqlm, wordIndexer));

				SearcherUtils.write(writer, bq.getId(), docScores);
			}

			writer.close();
		}
	}

	public void searchByKldFbWordVectorPrior() throws Exception {
		System.out.println("search by KLD FB Word Vector Prior.");

		Searcher vSearcher = Word2VecModel.fromSerFile("../../data/medical_ir/ohsumed/word2vec_model.ser.gz").forSearch();
		String[] docFreqFileNames = MIRPath.TaskDirs;

		for (int i = 0; i < docFreqFileNames.length; i++) {
			docFreqFileNames[i] += "vocab2.txt";
		}
		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			String outputFileName = resDirNames[i] + "kld-fb_wv-priors.txt";

			Counter<String> docFreqs = IOUtils.readCounter(docFreqFileNames[i]);

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

				double[] qwv = getVectorSum(vSearcher, docFreqs, bq.getSearchText());
				double[][] dwvs = new double[num_ret_docs][];

				for (int k = 0; k < num_ret_docs; k++) {
					int docId = docScores.indexAtLoc(k);
					Document doc = is.doc(docId);
					String content = doc.get(IndexFieldName.CONTENT);
					double[] dwv = getVectorSum(vSearcher, docFreqs, content);
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

				ArrayMath.normalizeColumns(sim_mat);

				double[] cents = new double[sim_mat.length];
				ArrayUtils.setAll(cents, 1f / cents.length);

				ArrayMath.doRandomWalk(sim_mat, cents, 10, 0.0000001, 0.85);

				docPriors.setValues(cents);
				docPriors.summation();

				WordCountBox wcb = WordCountBox.getWordCountBox(ir, docScores, wordIndexer, IndexFieldName.CONTENT);

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

		Searcher vSearcher = Word2VecModel.fromSerFile("../../data/medical_ir/trec_cds/word2vec_model.ser.gz").forSearch();

		String[] docFreqFileNames = MIRPath.TaskDirs;

		for (int i = 0; i < docFreqFileNames.length; i++) {
			docFreqFileNames[i] += "vocab2.txt";
		}

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];
			IndexReader ir = is.getIndexReader();

			Counter<String> docFreqs = IOUtils.readCounter(docFreqFileNames[i]);

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
				double[] qwv = getVectorSum(vSearcher, docFreqs, bq.getSearchText());

				for (int k = 0; k < docScores.size(); k++) {
					int docId = docScores.indexAtLoc(k);
					Document doc = is.doc(docId);
					String content = doc.get(IndexFieldName.CONTENT);
					double[] dwv = getVectorSum(vSearcher, docFreqs, content);
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

	public void searchSentsByKLDFB() throws Exception {
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
