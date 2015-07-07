package ohs.medical.ir;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.DeepMap;
import ohs.types.Indexer;
import ohs.types.common.IntArrayList;
import ohs.types.common.IntCounter;
import ohs.types.common.IntCounterMap;
import ohs.types.common.IntHashMap;
import ohs.types.common.IntHashSet;
import ohs.types.common.IntIndexedList;
import ohs.types.common.IntIndexedMap;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounterMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class CBEEMDocumentSearcher {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] resultDirNames = MIRPath.ResultDirNames;

		String[] logDirNames = MIRPath.LogDirNames;

		String[] docPriorFileNames = MIRPath.DocPriorFileNames;

		String[] relevanceDataFileNames = MIRPath.RelevanceDataFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		IndexSearcher[] indexSearchers = DocumentSearcher.getIndexSearchers(indexDirNames);

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

		for (int i = 0; i < logDirNames.length; i++) {
			File dirFile = new File(logDirNames[i]);
			for (File file : dirFile.listFiles()) {
				if (!file.getName().startsWith("init")) {
					file.delete();
				}
			}
		}

		for (int i = 0; i < resultDirNames.length; i++) {
			File dirFile = new File(resultDirNames[i]);
			for (File file : dirFile.listFiles()) {
				if (!file.getName().startsWith("init")) {
					file.delete();
				}
			}
		}

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();
		List<List<BaseQuery>> queryData = new ArrayList<List<BaseQuery>>();
		List<List<SparseVector>> relevanceData = new ArrayList<List<SparseVector>>();

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> baseQueries = new ArrayList<BaseQuery>();
			StrCounterMap queryRelevances = new StrCounterMap();

			if (i == 0) {
				baseQueries = QueryReader.readTrecCdsQueries(queryFileNames[i]);
				queryRelevances = RelevanceReader.readTrecCdsRelevances(relevanceDataFileNames[i]);
			} else if (i == 1) {
				baseQueries = QueryReader.readClefEHealthQueries(queryFileNames[i]);
				queryRelevances = RelevanceReader.readClefEHealthRelevances(relevanceDataFileNames[i]);
			} else if (i == 2) {
				baseQueries = QueryReader.readOhsumedQueries(queryFileNames[i]);
				queryRelevances = RelevanceReader.readOhsumedRelevances(relevanceDataFileNames[i]);
			}

			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);

			queryRelevances = RelevanceReader.filter(queryRelevances, docIdMap);

			baseQueries = QueryReader.filter(baseQueries, queryRelevances);

			queryData.add(baseQueries);

			List<SparseVector> docRelevances = DocumentIdMapper.mapDocIdsToIndexIds(baseQueries, queryRelevances, docIdMap);

			relevanceData.add(docRelevances);
		}

		// boolean[] runExps = { true, false, false, false, false, false };
		boolean[] runExps = { true, true, true, true, true, true };

		for (int run_id = 0; run_id < runExps.length; run_id++) {
			if (!runExps[run_id]) {
				continue;
			}

			int[] top_k = null;

			int[] top_k_in_wiki = null;

			int[] num_fb_docs = null;

			int[] num_fb_words = null;

			double[] dirichlet_prior = null;

			double[] mixture_for_all_colls = null;

			boolean[] useDocPrior = null;

			boolean[] useDoubleScoring = null;

			boolean[] useWiki = null;

			double[] mixture_for_fb_model = null;

			boolean[] smoothCollMixtures = null;

			boolean[] adjustNumbers = null;

			if (run_id == 0) {
				top_k = new int[] { 100 };
				top_k_in_wiki = new int[] { 10 };
				num_fb_docs = new int[] { 5 };
				num_fb_words = new int[] { 25 };
				dirichlet_prior = new double[] { 2000 };
				mixture_for_all_colls = new double[] { 0.0, 0.5 };
				useDocPrior = new boolean[] { false, true };
				useDoubleScoring = new boolean[] { false };
				useWiki = new boolean[] { false };
				mixture_for_fb_model = new double[] { 0.5 };
				smoothCollMixtures = new boolean[] { false };
				adjustNumbers = new boolean[] { true, false };
			} else if (run_id == 1) {
				top_k = new int[] { 100 };
				top_k_in_wiki = new int[] { 10 };
				num_fb_docs = new int[] { 5, 10, 25, 50 };
				num_fb_words = new int[] { 5, 10, 25, 50, 100 };
				dirichlet_prior = new double[] { 2000 };
				mixture_for_all_colls = new double[] { 0.5 };
				useDocPrior = new boolean[] { false };
				useDoubleScoring = new boolean[] { false };
				useWiki = new boolean[] { false };
				mixture_for_fb_model = new double[] { 0.5 };
				smoothCollMixtures = new boolean[] { false };
				adjustNumbers = new boolean[] { false };
			} else if (run_id == 2) {
				top_k = new int[] { 100 };
				top_k_in_wiki = new int[] { 10 };
				num_fb_docs = new int[] { 5 };
				num_fb_words = new int[] { 25 };
				dirichlet_prior = new double[] { 2000 };
				mixture_for_all_colls = new double[] { 0, 0.25, 0.5, 0.75, 1.0 };
				useDocPrior = new boolean[] { false };
				useDoubleScoring = new boolean[] { false };
				useWiki = new boolean[] { false };
				mixture_for_fb_model = new double[] { 0.5 };
				smoothCollMixtures = new boolean[] { false };
				adjustNumbers = new boolean[] { false };
			} else if (run_id == 3) {
				top_k = new int[] { 100 };
				top_k_in_wiki = new int[] { 10 };
				num_fb_docs = new int[] { 5 };
				num_fb_words = new int[] { 25 };
				dirichlet_prior = new double[] { 2000 };
				mixture_for_all_colls = new double[] { 0.5 };
				useDocPrior = new boolean[] { false };
				useDoubleScoring = new boolean[] { false };
				useWiki = new boolean[] { false };
				mixture_for_fb_model = new double[] { 0, 0.25, 0.5, 0.75, 1.0 };
				smoothCollMixtures = new boolean[] { false };
				adjustNumbers = new boolean[] { false };
			} else if (run_id == 4) {
				top_k = new int[] { 200, 300, 400, 500 };
				top_k_in_wiki = new int[] { 10 };
				num_fb_docs = new int[] { 5 };
				num_fb_words = new int[] { 25 };
				dirichlet_prior = new double[] { 2000 };
				mixture_for_all_colls = new double[] { 0.5 };
				useDocPrior = new boolean[] { false };
				useDoubleScoring = new boolean[] { false };
				useWiki = new boolean[] { false };
				mixture_for_fb_model = new double[] { 0.5 };
				smoothCollMixtures = new boolean[] { false };
				adjustNumbers = new boolean[] { false };
			} else if (run_id == 5) {
				top_k = new int[] { 100, 200, 300, 400, 500 };
				top_k_in_wiki = new int[] { 10 };
				num_fb_docs = new int[] { 5 };
				num_fb_words = new int[] { 25 };
				dirichlet_prior = new double[] { 2000 };
				mixture_for_all_colls = new double[] { 0.5 };
				useDocPrior = new boolean[] { false };
				useDoubleScoring = new boolean[] { false };
				useWiki = new boolean[] { true };
				mixture_for_fb_model = new double[] { 0.5 };
				smoothCollMixtures = new boolean[] { false };
				adjustNumbers = new boolean[] { false };
			}

			for (int h1 = 0; h1 < top_k.length; h1++) {
				for (int h2 = 0; h2 < top_k_in_wiki.length; h2++) {
					for (int h3 = 0; h3 < num_fb_docs.length; h3++) {
						for (int h4 = 0; h4 < num_fb_words.length; h4++) {
							for (int h5 = 0; h5 < mixture_for_all_colls.length; h5++) {
								for (int h6 = 0; h6 < useDocPrior.length; h6++) {
									for (int h7 = 0; h7 < useDoubleScoring.length; h7++) {
										for (int h8 = 0; h8 < useWiki.length; h8++) {
											for (int h9 = 0; h9 < mixture_for_fb_model.length; h9++) {
												for (int h10 = 0; h10 < smoothCollMixtures.length; h10++) {
													for (int h11 = 0; h11 < adjustNumbers.length; h11++) {
														HyperParameter hyperParameter = new HyperParameter();
														hyperParameter.setTopK(top_k[h1]);
														hyperParameter.setTopKInWiki(top_k_in_wiki[h2]);
														hyperParameter.setNumFBDocs(num_fb_docs[h3]);
														hyperParameter.setNumFBWords(num_fb_words[h4]);
														hyperParameter.setMixtureForAllCollections(mixture_for_all_colls[h5]);
														hyperParameter.setUseDocPrior(useDocPrior[h6]);
														hyperParameter.setUseDoubleScoring(useDoubleScoring[h7]);
														hyperParameter.setUseWiki(useWiki[h8]);
														hyperParameter.setMixtureForFeedbackModel(mixture_for_fb_model[h9]);
														hyperParameter.setSmoothCollMixtures(smoothCollMixtures[h10]);
														hyperParameter.setAdjustNumbers(adjustNumbers[h11]);

														for (int q = 0; q < queryData.size(); q++) {
															List<BaseQuery> baseQueries = queryData.get(q);
															List<SparseVector> queryDocRelevances = relevanceData.get(q);

															String resultDirName = resultDirNames[q];
															String logDirName = logDirNames[q];

															String resultFileName = resultDirName
																	+ String.format("cbeem_%s.txt", hyperParameter.toString(true));
															String logFileName = logDirName
																	+ String.format("cbeem_%s.txt", hyperParameter.toString(true));

															System.out.printf("process for [%s].\n", resultFileName);

															CBEEMDocumentSearcher ds = new CBEEMDocumentSearcher(indexSearchers,
																	docPriorData, hyperParameter, analyzer);
															ds.search(q, baseQueries, queryDocRelevances, resultFileName, logFileName);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		SearchResultEvaluator e = new SearchResultEvaluator();
		e.evaluate();

		System.out.println("process ends.");
	}

	private TextFileWriter logWriter;

	private IndexSearcher[] indexSearchers;

	private int num_colls;

	private DenseVector[] docPriorData;

	private HyperParameter hyperParam;

	private StringBuffer logBuf;

	private Indexer<String> wordIndexer;

	private Indexer<String> categoryIndexer;

	private SparseVector[] docScoreData;

	private BaseQuery baseQuery;

	private WordCountBox[] docWordCountBoxes;

	private SparseMatrix[] wordCocountData;

	private IntHashSet numberWords;

	private SparseVector queryModel;

	private SparseVector newQueryModel;

	private IntArrayList queryWords;

	private Analyzer analyzer;

	public CBEEMDocumentSearcher(IndexSearcher[] indexSearchers, DenseVector[] docPriorData, HyperParameter hyperParameter,
			Analyzer analyzer) {
		super();
		this.indexSearchers = indexSearchers;
		this.docPriorData = docPriorData;

		num_colls = hyperParameter.isUseWiki() ? indexSearchers.length : indexSearchers.length - 1;

		this.hyperParam = hyperParameter;
		this.analyzer = analyzer;
	}

	private SparseVector combineModels(SparseVector[] models, double[] mixtures) {
		IntCounter counter = new IntCounter();
		for (int i = 0; i < models.length; i++) {
			SparseVector model = models[i];
			double mixture = mixtures[i];

			for (int j = 0; j < model.size(); j++) {
				int w = model.indexAtLoc(j);
				double prob = model.valueAtLoc(j);

				String word = wordIndexer.getObject(w);
				double new_prob = mixture * prob;
				if (new_prob > 0) {
					counter.incrementCount(w, new_prob);
				}
			}
		}
		SparseVector ret = VectorUtils.toSparseVector(counter);
		ret.normalize();
		return ret;
	}

	private SparseVector[] computeRelevanceModels(boolean isNegative) throws IOException {
		double[] cnt_sum_in_each_coll = getCollWordCountSums();
		double cnt_sum_in_all_colls = ArrayMath.sum(cnt_sum_in_each_coll);

		int num_fb_docs = hyperParam.getNumFBDocs();
		double dirichlet_prior = hyperParam.getDirichletPrior();
		double mixture_for_all_colls = hyperParam.getMixtureForAllCollections();
		boolean useDocPrior = hyperParam.isUseDocPrior();

		SparseVector[] ret = new SparseVector[num_colls];

		for (int i = 0; i < num_colls; i++) {
			SparseVector docScores = docScoreData[i];
			docScores.sortByValue();

			if (isNegative) {
				docScores.reverse();
			}

			SparseMatrix docWordCountBox = docWordCountBoxes[i].getDocWordCounts();
			SparseVector collWordCounts = docWordCountBoxes[i].getCollWordCounts();
			DenseVector docPriors = docPriorData[i];

			SparseVector rm = new SparseVector(collWordCounts.size());

			for (int j = 0; j < collWordCounts.size(); j++) {
				int w = collWordCounts.indexAtLoc(j);
				boolean hasNumber = numberWords.contains(w);

				double[] cnt_w_in_each_coll = new double[num_colls];
				double cnt_w_in_all_colls = 0;

				for (int k = 0; k < num_colls; k++) {
					cnt_w_in_each_coll[k] = docWordCountBoxes[k].getCollWordCounts().valueAlways(w);
					cnt_w_in_all_colls += cnt_w_in_each_coll[k];
				}

				double cnt_w_in_coll = cnt_w_in_each_coll[i];
				double cnt_sum_in_coll = cnt_sum_in_each_coll[i];

				double prob_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;
				double prob_w_in_all_colls = cnt_w_in_all_colls / cnt_sum_in_all_colls;

				for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
					int docId = docScores.indexAtLoc(k);
					double doc_weight = docScores.valueAtLoc(k);

					SparseVector wordCounts = docWordCountBox.rowAlways(docId);
					double cnt_w_in_doc = wordCounts.valueAlways(w);
					double cnt_sum_in_doc = wordCounts.sum();
					double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
					double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;

					// double prob_w_in_doc = (cnt_w_in_doc + dirichlet_prior *
					// prob_w_in_coll) / (cnt_sum_in_doc + dirichlet_prior);
					prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
					prob_w_in_doc = (1 - mixture_for_all_colls) * prob_w_in_doc + mixture_for_all_colls * prob_w_in_all_colls;

					double doc_prior = useDocPrior ? cnt_sum_in_doc : 1;
					double prob_w_in_fb_model = doc_weight * prob_w_in_doc * doc_prior;

					if (prob_w_in_fb_model > 0) {
						rm.incrementAtLoc(j, w, prob_w_in_fb_model);
					}
				}
			}
			docScores.sortByIndex();

			rm.removeZeros();
			rm.normalize();

			ret[i] = rm;
		}
		return ret;
	}

	private double[] getCollWordCountSums() {
		double[] ret = new double[docWordCountBoxes.length];
		for (int i = 0; i < docWordCountBoxes.length; i++) {
			ret[i] = docWordCountBoxes[i].getCountSumInCollection();
		}
		return ret;
	}

	private String getRankingComparisonLog(SparseVector docScores, SparseVector newDocScores, SparseVector docRelevances) {
		StringBuffer sb = new StringBuffer();

		sb.append(String.format("Relevant docs:\t%d", docRelevances.size()));
		sb.append(String.format("\nRelevant docs at top-%d:\t%d", docScores.size(), getRelevant(docScores, docRelevances, docScores.size())));
		sb.append(String.format("\nRelevant docs at top-20:"));
		sb.append(String.format("\nRanking-1:\t%d", getRelevant(docScores, docRelevances, 20)));
		sb.append(String.format("\nRanking-2:\t%d", getRelevant(newDocScores, docRelevances, 20)));
		sb.append("\nDocID\tRelevance\tRank1\tRank2\tChange\tEffect");

		SparseVector ranking1 = docScores.ranking();
		SparseVector ranking2 = newDocScores.ranking();

		docScores.sortByValue();
		newDocScores.sortByValue();

		int num_poss = 0;
		int num_negs = 0;

		for (int i = 0; i < newDocScores.size() && i < 20; i++) {
			int docId = newDocScores.indexAtLoc(i);
			int rank2 = i + 1;
			int rank1 = (int) ranking1.valueAlways(docId);
			int change = rank1 - rank2;
			double relevance = docRelevances.valueAlways(docId);

			String effect = "";

			if (relevance > 0) {
				if (change > 0) {
					effect = "POS";
					num_poss++;
				} else if (change < 0) {
					effect = "NEG";
					num_negs++;
				}
			}

			sb.append(String.format("\n%d\t%d\t%d\t%d\t%d\t%s", docId, (int) relevance, rank1, rank2, change, effect));
		}

		sb.append(String.format("\nPOSs:\t%d", num_poss));
		sb.append(String.format("\nNEGs:\t%d", num_negs));
		sb.append("\nRanks of relevant docs:");
		sb.append("\nRanking-1:");

		for (int i = 0; i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			int rank = i + 1;
			if (docRelevances.valueAlways(docId) > 0) {
				sb.append(String.format("\t%d", rank));
			}
		}

		sb.append("\nRanking-2:");

		for (int i = 0; i < newDocScores.size(); i++) {
			int docId = newDocScores.indexAtLoc(i);
			int rank = i + 1;
			if (docRelevances.valueAlways(docId) > 0) {
				sb.append(String.format("\t%d", rank));
			}
		}
		sb.append("\n");

		docScores.sortByIndex();
		newDocScores.sortByIndex();

		return sb.toString();
	}

	private int getRelevant(SparseVector docScores, SparseVector docRelevances, int n) {
		int ret = 0;

		docScores.sortByValue();

		for (int i = 0; i < n && i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			double relevance = docRelevances.valueAlways(docId);
			if (relevance > 0) {
				ret++;
			}
		}
		docScores.sortByIndex();
		return ret;
	}

	private SparseVector scoreDocuments(SparseVector queryModel, int targetId) {
		double[] cnt_sum_in_each_coll = getCollWordCountSums();
		double cnt_sum_in_all_colls = ArrayMath.sum(cnt_sum_in_each_coll);

		SparseMatrix docWordCountBox = docWordCountBoxes[targetId].getDocWordCounts();
		SparseVector collWordCounts = docWordCountBoxes[targetId].getCollWordCounts();

		double dirichlet_prior = hyperParam.getDirichletPrior();
		double mixture_for_all_colls = hyperParam.getMixtureForAllCollections();

		SparseVector ret = new SparseVector(docWordCountBox.rowSize());

		for (int i = 0; i < queryModel.size(); i++) {
			int w = queryModel.indexAtLoc(i);
			double prob_w_in_query = queryModel.valueAtLoc(i);
			boolean hasNumber = numberWords.contains(w);

			double[] cnt_w_in_each_coll = new double[num_colls];
			double cnt_w_in_all_colls = 0;

			for (int j = 0; j < num_colls; j++) {
				cnt_w_in_each_coll[j] = docWordCountBoxes[j].getCollWordCounts().valueAlways(w);
				cnt_w_in_all_colls += cnt_w_in_each_coll[j];
			}

			double cnt_w_in_coll = cnt_w_in_each_coll[targetId];
			double cnt_sum_in_coll = cnt_sum_in_each_coll[targetId];

			double prob_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;
			double prob_w_in_all_colls = cnt_w_in_all_colls / cnt_sum_in_all_colls;

			for (int j = 0; j < docWordCountBox.rowSize(); j++) {
				int docId = docWordCountBox.indexAtRowLoc(j);
				SparseVector wordCounts = docWordCountBox.rowAtLoc(j);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				// double prob_w_in_doc = (cnt_w_in_doc + dirichlet_prior *
				// prob_w_in_coll) / (cnt_sum_in_doc + dirichlet_prior);

				prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
				prob_w_in_doc = (1 - mixture_for_all_colls) * prob_w_in_doc + mixture_for_all_colls * prob_w_in_all_colls;

				if (prob_w_in_doc > 0) {
					double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_doc);
					ret.incrementAtLoc(j, docId, div);
				}
			}
		}

		for (int i = 0; i < ret.size(); i++) {
			double sum_div = ret.valueAtLoc(i);
			double approx_prob = Math.exp(-sum_div);
			ret.setAtLoc(i, approx_prob);
		}
		ret.summation();

		return ret;
	}

	private SparseVector search(int targetId, BaseQuery baseQuery, SparseVector docRels) throws Exception {
		wordIndexer = new Indexer<String>();
		numberWords = new IntHashSet();

		categoryIndexer = new Indexer<String>();
		docScoreData = new SparseVector[num_colls];
		this.baseQuery = baseQuery;

		setupQuery();

		for (int i = 0; i < num_colls; i++) {
			docScoreData[i] = DocumentSearcher.search(baseQuery.getLuceneQuery(), indexSearchers[i], hyperParam.getTopK());
		}

		setWordCountBoxes();

		for (int i = 0; i < wordIndexer.size(); i++) {
			String word = wordIndexer.getObject(i);
			if (word.contains("NU")) {
				numberWords.add(i);
			}
		}

		if (hyperParam.isAdjustNumbers()) {
			List<Integer> qws = baseQuery.getQueryWords();
			double num_numbers = 0;
			IntCounter counter = new IntCounter();
			for (int w : qws) {
				counter.incrementCount(w, 1);
				if (numberWords.contains(w)) {
					num_numbers++;
				}
			}

			if (num_numbers > 0) {
				double discount_factor = 1f / num_numbers;
				for (int w : counter.keySet()) {
					double count = counter.getCount(w);
					if (numberWords.contains(w)) {
						double new_count = discount_factor * count;
						counter.setCount(w, new_count);
					}
				}

				newQueryModel = VectorUtils.toSparseVector(counter);
				newQueryModel.normalize();
			}
		}

		// setWordCocountData(2, hyperParam.getTopK());

		if (hyperParam.isUseDoubleScoring()) {
			for (int i = 0; i < num_colls; i++) {
				docScoreData[i] = scoreDocuments(newQueryModel, i);
			}
		}

		SparseVector[] rms = computeRelevanceModels(false);
		// SparseVector[] collNegRMs = computeRelevanceModels(true);

		double[] mixture_for_each_coll_rm = new double[num_colls];

		double score_in_target_coll = 0;
		double score_sum_except_target_coll = 0;

		for (int i = 0; i < num_colls; i++) {
			double coll_prior = 0;
			SparseVector docScores = docScoreData[i];
			docScores.sortByValue();

			double num_docs = 0;
			for (int j = 0; j < docScores.size() && j < hyperParam.getNumFBDocs(); j++) {
				coll_prior += docScores.valueAtLoc(j);
				num_docs++;
			}

			coll_prior /= num_docs;

			docScores.sortByIndex();

			mixture_for_each_coll_rm[i] = coll_prior;

			if (i == targetId) {
				score_in_target_coll = coll_prior;
			} else {
				score_sum_except_target_coll += coll_prior;
			}
		}

		if (hyperParam.isSmoothCollectionMixtures()) {
			double avg = ArrayMath.mean(mixture_for_each_coll_rm);
			// mixture_for_each_coll_rm[targetId] += (0.5 * avg);
			mixture_for_each_coll_rm[targetId] += (avg);
		}

		ArrayMath.normalize(mixture_for_each_coll_rm, mixture_for_each_coll_rm);

		SparseVector cbeem = combineModels(rms, mixture_for_each_coll_rm);
		cbeem.keepTopN(hyperParam.getNumFBWords());
		cbeem.normalize();

		double[] mixture_for_each_qm = { 1 - hyperParam.getMixtureForFeedbackModel(), hyperParam.getMixtureForFeedbackModel() };
		ArrayMath.normalize(mixture_for_each_qm);

		SparseVector newQM = combineModels(new SparseVector[] { newQueryModel, cbeem }, mixture_for_each_qm);

		SparseVector ret = scoreDocuments(newQM, targetId);

		logBuf.append(baseQuery.toString() + "\n");
		logBuf.append(String.format("QM1:\t%s\n", VectorUtils.toCounter(newQueryModel, wordIndexer).toString()));
		logBuf.append(String.format("QM2:\t%s\n", VectorUtils.toCounter(newQM, wordIndexer).toString()));

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);

		for (int i = 0; i < rms.length; i++) {
			SparseVector rm = rms[i];
			double mixture = mixture_for_each_coll_rm[i];
			logBuf.append(String.format("RM%d (%s):\t%s\n", i + 1, nf.format(mixture), VectorUtils.toCounter(rm, wordIndexer).toString()));
		}

		// logBuff.append(sb.toString());

		logBuf.append(String.format("RMM:\t%s\n\n", VectorUtils.toCounter(cbeem, wordIndexer).toString()));
		if (docRels != null) {
			logBuf.append(getRankingComparisonLog(docScoreData[targetId], ret, docRels));
		}
		logBuf.append("\n");

		// List<Integer> queryWords = new
		// ArrayList<Integer>(baseQuery.getWordLocMap().values());

		// for (int i = 0; i < queryWords.size(); i++) {
		// int w = queryWords.get(i);
		// String word = wordIndexer.getObject(w);
		//
		// logBuff.append(String.format("%dth:\t%s", i + 1, word));
		//
		// for (int j = 0; j < num_colls; j++) {
		// SparseMatrix docWordCocounts = wordCocountData[j];
		// SparseVector cocounts = docWordCocounts.rowAlways(w);
		//
		// if (cocounts == null) {
		// continue;
		// }
		//
		// cocounts.sortByValue();
		//
		// logBuff.append(String.format("\n\t\t%dth coll:", j + 1));
		//
		// for (int k = 0; k < cocounts.size() && k < 5; k++) {
		// int w2 = cocounts.indexAtLoc(k);
		// double cocount = cocounts.valueAtLoc(k);
		// String word2 = wordIndexer.getObject(w2);
		//
		// logBuff.append(String.format("\t%s:%d", word2, (int) cocount));
		// }
		// }
		// logBuff.append("\n");
		// }

		return ret;
	}

	public void search(int colId, List<BaseQuery> baseQueries, List<SparseVector> queryDocRels, String resultFileName, String logFileName)
			throws Exception {
		if (logFileName != null) {
			logWriter = new TextFileWriter(logFileName);
		}

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < baseQueries.size(); i++) {
			BaseQuery baseQuery = baseQueries.get(i);
			SparseVector docRels = null;

			if (queryDocRels != null) {
				docRels = queryDocRels.get(i);
			}

			logBuf = new StringBuffer();

			SparseVector docScores = search(colId, baseQuery, docRels);

			int num_relevant_at_k = getRelevant(docScores, docRels, 20);

			write(writer, baseQuery.getId(), docScores);

			if (writer != null) {
				logWriter.write(logBuf.toString().trim() + "\n\n");
			}
		}

		writer.close();

		if (logWriter != null) {
			logWriter.close();
		}
	}

	private void setupQuery() throws Exception {
		List<String> words = AnalyzerUtils.getWords(baseQuery.getSearchText(), analyzer);

		baseQuery.setLuceneQuery(AnalyzerUtils.getQuery(words));

		queryWords = new IntArrayList();
		IntCounter counter = new IntCounter();

		for (int i = 0; i < words.size(); i++) {
			int w = wordIndexer.getIndex(words.get(i));
			counter.incrementCount(w, 1);
			queryWords.add(w);
		}

		queryModel = VectorUtils.toSparseVector(counter);
		queryModel.normalize();

		newQueryModel = queryModel.copy();
	}

	private void setWordCocountData(int window_size, int top_k) {
		SparseMatrix[] ret = new SparseMatrix[num_colls];

		for (int i = 0; i < num_colls; i++) {
			WordCountBox docWordCountBox = docWordCountBoxes[i];
			SparseVector docScores = docScoreData[i];

			DeepMap<Integer, Integer, Integer> docWords = docWordCountBox.getDocWordLocs();
			IntCounterMap counterMap = new IntCounterMap();

			for (int docId : docWords.keySet()) {
				Map<Integer, Integer> wordLocs = docWords.get(docId);
				List<Integer> words = new ArrayList<Integer>(wordLocs.values());

				for (int j = 0; j < words.size(); j++) {
					int w1 = words.get(j);

					for (int k = j + 1; k < window_size && k < words.size(); k++) {
						int w2 = words.get(k);
						counterMap.incrementCount(w1, w2, 1);
					}
				}
			}
			ret[i] = VectorUtils.toSpasreMatrix(counterMap);
		}
		wordCocountData = ret;
	}

	private void setWordCountBoxes() throws Exception {
		Set<Integer> wordsInFB = new HashSet<Integer>();
		List<IntCounterMap> wordCountData = new ArrayList<IntCounterMap>();
		List<DeepMap<Integer, Integer, Integer>> wordData = new ArrayList<DeepMap<Integer, Integer, Integer>>();

		for (int i = 0; i < num_colls; i++) {
			SparseVector docScores = docScoreData[i];
			IndexReader indexReader = indexSearchers[i].getIndexReader();

			IntCounterMap docWordCountMap = new IntCounterMap();
			DeepMap<Integer, Integer, Integer> docWords = new DeepMap<Integer, Integer, Integer>();

			for (int j = 0; j < docScores.size(); j++) {
				int docId = docScores.indexAtLoc(j);
				double score = docScores.valueAtLoc(j);
				Document doc = indexReader.document(docId);

				Terms termVector = indexReader.getTermVector(docId, IndexFieldName.CONTENT);

				if (termVector == null) {
					continue;
				}

				TermsEnum termsEnum = null;
				termsEnum = termVector.iterator(termsEnum);

				BytesRef bytesRef = null;
				PostingsEnum postingsEnum = null;
				IntCounter wordCounts = new IntCounter();
				Map<Integer, Integer> words = new TreeMap<Integer, Integer>();

				while ((bytesRef = termsEnum.next()) != null) {
					postingsEnum = termsEnum.postings(null, postingsEnum, PostingsEnum.ALL);

					if (postingsEnum.nextDoc() != 0) {
						throw new AssertionError();
					}

					String word = bytesRef.utf8ToString();
					int w = wordIndexer.getIndex(word);
					int freq = postingsEnum.freq();
					wordCounts.incrementCount(w, freq);

					for (int k = 0; k < freq; k++) {
						final int position = postingsEnum.nextPosition();
						words.put(position, w);
					}
				}
				docWordCountMap.setCounter(docId, wordCounts);
				docWords.put(docId, words);

				for (int w : wordCounts.keySet()) {
					wordsInFB.add(w);
				}
			}
			wordCountData.add(docWordCountMap);
			wordData.add(docWords);
		}

		List<IntCounter> collWordCountData = new ArrayList<IntCounter>();
		double[] cnt_sum_in_each_coll = new double[num_colls];
		double[] num_docs_in_each_coll = new double[num_colls];

		for (int i = 0; i < num_colls; i++) {
			IndexReader indexReader = indexSearchers[i].getIndexReader();
			IntCounter counter = new IntCounter();
			for (int w : wordsInFB) {
				String word = wordIndexer.getObject(w);
				Term termInstance = new Term(IndexFieldName.CONTENT, word);
				double count = indexReader.totalTermFreq(termInstance);
				counter.setCount(w, count);
			}
			collWordCountData.add(counter);

			cnt_sum_in_each_coll[i] = indexReader.getSumTotalTermFreq(IndexFieldName.CONTENT);
			num_docs_in_each_coll[i] = indexReader.maxDoc();
		}

		WordCountBox[] ret = new WordCountBox[num_colls];

		for (int i = 0; i < num_colls; i++) {
			IntCounterMap counterMap = wordCountData.get(i);
			IntCounter counter = collWordCountData.get(i);
			DeepMap<Integer, Integer, Integer> docWords = wordData.get(i);
			ret[i] = new WordCountBox(VectorUtils.toSpasreMatrix(counterMap), VectorUtils.toSparseVector(counter),
					VectorUtils.toSpasreMatrix(new CounterMap<Integer, Integer>()), cnt_sum_in_each_coll[i], num_docs_in_each_coll[i],
					docWords);
		}
		docWordCountBoxes = ret;
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
