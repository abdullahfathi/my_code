package ohs.medical.ir;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;

import ohs.io.TextFileWriter;
import ohs.ir.eval.RankComparator;
import ohs.lucene.common.AnalyzerUtils;
import ohs.math.ArrayMath;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.medical.ir.query.BaseQuery;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.common.StrCounter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class CbeemDocumentSearcher {

	private TextFileWriter logWriter;

	private IndexSearcher[] indexSearchers;

	private Analyzer analyzer;

	private int num_colls;

	private HyperParameter hyperParam;

	private StringBuffer logBuf;

	private Indexer<String> wordIndexer;

	private SparseVector[] docScoreData;

	private WordCountBox[] wcbs;

	private DenseVector[] collDocPriors;

	private BaseQuery bq;

	private boolean makeLog = false;

	public CbeemDocumentSearcher(IndexSearcher[] indexSearchers, DenseVector[] docPriorData, HyperParameter hyperParameter,
			Analyzer analyzer, boolean makeLog) throws Exception {
		super();
		this.indexSearchers = indexSearchers;
		this.collDocPriors = docPriorData;
		this.hyperParam = hyperParameter;
		this.analyzer = analyzer;
		this.makeLog = makeLog;

		num_colls = indexSearchers.length;
	}

	private SparseVector[] getRelevanceModels() throws IOException {
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

			SparseMatrix docWordCounts = wcbs[i].getDocWordCounts();
			SparseVector collWordCounts = wcbs[i].getCollWordCounts();
			DenseVector docPriors = collDocPriors[i];

			SparseVector rm = new SparseVector(collWordCounts.size());

			for (int j = 0; j < collWordCounts.size(); j++) {
				int w = collWordCounts.indexAtLoc(j);

				double[] cnt_w_in_each_coll = new double[num_colls];
				double cnt_w_in_all_colls = 0;

				for (int k = 0; k < num_colls; k++) {
					cnt_w_in_each_coll[k] = wcbs[k].getCollWordCounts().valueAlways(w);
					cnt_w_in_all_colls += cnt_w_in_each_coll[k];
				}

				double cnt_w_in_coll = cnt_w_in_each_coll[i];
				double cnt_sum_in_coll = cnt_sum_in_each_coll[i];

				double pr_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;
				double pr_w_in_all_colls = cnt_w_in_all_colls / cnt_sum_in_all_colls;

				for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
					int docId = docScores.indexAtLoc(k);
					double doc_weight = docScores.valueAtLoc(k);

					SparseVector wordCounts = docWordCounts.rowAlways(docId);
					double cnt_w_in_doc = wordCounts.valueAlways(w);
					double cnt_sum_in_doc = wordCounts.sum();
					double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
					double pr_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;

					// double pr_w_in_doc = (cnt_w_in_doc + dirichlet_prior *
					// pr_w_in_coll) / (cnt_sum_in_doc + dirichlet_prior);
					pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;
					pr_w_in_doc = (1 - mixture_for_all_colls) * pr_w_in_doc + mixture_for_all_colls * pr_w_in_all_colls;

					double doc_prior = useDocPrior ? cnt_sum_in_doc : 1;
					double pr_w_in_fb_model = doc_weight * pr_w_in_doc * doc_prior;

					if (pr_w_in_fb_model > 0) {
						rm.incrementAtLoc(j, w, pr_w_in_fb_model);
					}

					// double log1 = Math.log(doc_weight);
					// double log2 = Math.log(pr_w_in_doc);
					// double log3 = Math.log(doc_prior);
					// double log_sum = log1 + log2 + log3;
					//
					// rm2.incrementAtLoc(j, w, log_sum);
				}
			}
			docScores.sortByIndex();
			rm.normalize();

			// double[] log_probs = rm2.values();
			// double log_pr_sum = ArrayMath.sumLogProb(log_probs);
			//
			// ArrayMath.add(log_probs, -log_pr_sum, log_probs);
			// ArrayMath.exponentiate(log_probs, false, log_probs);
			//
			// rm2.summation();
			//
			// System.out.println(VectorUtils.toCounter(rm, wordIndexer));
			// System.out.println(VectorUtils.toCounter(rm2, wordIndexer));
			// System.out.println();

			ret[i] = rm;
		}
		return ret;
	}

	private double[] getCollWordCountSums() {
		double[] ret = new double[num_colls];
		for (int i = 0; i < num_colls; i++) {
			ret[i] = wcbs[i].getCollectionCountSum();
		}
		return ret;
	}

	private SparseVector score(int colId, SparseVector qLM) {
		double[] cnt_sum_in_each_coll = getCollWordCountSums();
		double cnt_sum_in_all_colls = ArrayMath.sum(cnt_sum_in_each_coll);

		SparseMatrix docWordCountBox = wcbs[colId].getDocWordCounts();
		SparseVector collWordCounts = wcbs[colId].getCollWordCounts();

		double dirichlet_prior = hyperParam.getDirichletPrior();
		double mixture_for_all_colls = hyperParam.getMixtureForAllCollections();

		SparseVector ret = new SparseVector(docWordCountBox.rowSize());

		for (int i = 0; i < qLM.size(); i++) {
			int w = qLM.indexAtLoc(i);
			double pr_w_in_query = qLM.valueAtLoc(i);

			double[] cnt_w_in_each_coll = new double[num_colls];
			double cnt_w_in_all_colls = 0;

			for (int j = 0; j < num_colls; j++) {
				cnt_w_in_each_coll[j] = wcbs[j].getCollWordCounts().valueAlways(w);
				cnt_w_in_all_colls += cnt_w_in_each_coll[j];
			}

			double cnt_w_in_coll = cnt_w_in_each_coll[colId];
			double cnt_sum_in_coll = cnt_sum_in_each_coll[colId];

			double pr_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;
			double pr_w_in_all_colls = cnt_w_in_all_colls / cnt_sum_in_all_colls;

			for (int j = 0; j < docWordCountBox.rowSize(); j++) {
				int docId = docWordCountBox.indexAtRowLoc(j);
				SparseVector wordCounts = docWordCountBox.rowAtLoc(j);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double pr_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				// double pr_w_in_doc = (cnt_w_in_doc + dirichlet_prior *
				// pr_w_in_coll) / (cnt_sum_in_doc + dirichlet_prior);

				pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;
				// pr_w_in_doc = (1 - mixture_for_all_colls) * pr_w_in_doc + mixture_for_all_colls * pr_w_in_all_colls;

				if (pr_w_in_doc > 0) {
					double div = pr_w_in_query * Math.log(pr_w_in_query / pr_w_in_doc);
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

	public SparseVector score(int colId, SparseVector qLM, SparseVector docRels) throws Exception {
		if (hyperParam.isUseDoubleScoring()) {
			for (int i = 0; i < num_colls; i++) {
				docScoreData[i] = score(i, qLM);
			}
		}

		SparseVector[] rms = getRelevanceModels();

		double[] mixture_for_each_coll_rm = new double[num_colls];

		double score_in_target_coll = 0;
		double score_sum_except_target_coll = 0;

		for (int i = 0; i < num_colls; i++) {
			double coll_prior = 0;
			SparseVector docScores = docScoreData[i];
			docScores.sortByValue();

			double num_docs_for_coll_prior = 0;
			for (int j = 0; j < docScores.size() && j < hyperParam.getNumFBDocs(); j++) {
				coll_prior += docScores.valueAtLoc(j);
				num_docs_for_coll_prior++;
			}

			coll_prior /= num_docs_for_coll_prior;

			docScores.sortByIndex();

			mixture_for_each_coll_rm[i] = coll_prior;

			if (i == colId) {
				score_in_target_coll = coll_prior;
			} else {
				score_sum_except_target_coll += coll_prior;
			}
		}

		if (hyperParam.isSmoothCollectionMixtures()) {
			double avg = ArrayMath.mean(mixture_for_each_coll_rm);
			// mixture_for_each_coll_rm[targetId] += (0.5 * avg);
			mixture_for_each_coll_rm[colId] += (avg);
		}

		// mixture_for_each_coll_rm[colId] = score_sum_except_target_coll;

		ArrayMath.normalize(mixture_for_each_coll_rm, mixture_for_each_coll_rm);

		SparseVector cbeem = VectorMath.addAfterScale(rms, mixture_for_each_coll_rm);
		cbeem.removeZeros();
		cbeem.keepTopN(hyperParam.getNumFBWords());
		cbeem.normalize();

		double[] mixture_for_each_qm = { 1 - hyperParam.getMixtureForFeedbackModel(), hyperParam.getMixtureForFeedbackModel() };
		ArrayMath.normalize(mixture_for_each_qm);

		SparseVector expQLM = VectorMath.addAfterScale(new Vector[] { qLM, cbeem }, mixture_for_each_qm);

		SparseVector ret = score(colId, expQLM);

		// BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQLM, wordIndexer));
		// SparseVector ret = DocumentSearcher.search(lbq, indexSearchers[colId], hyperParam.getTopK());
		// ret.normalize();

		if (makeLog) {
			logBuf.append(bq.toString() + "\n");
			logBuf.append(String.format("QM1:\t%s\n", VectorUtils.toCounter(qLM, wordIndexer).toString()));
			logBuf.append(String.format("QM2:\t%s\n", VectorUtils.toCounter(expQLM, wordIndexer).toString()));

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(4);

			for (int i = 0; i < rms.length; i++) {
				SparseVector rm = rms[i];
				double mixture = mixture_for_each_coll_rm[i];
				logBuf.append(

				String.format("RM%d (%s):\t%s\n", i + 1, nf.format(mixture), VectorUtils.toCounter(rm, wordIndexer).toString()));
			}

			logBuf.append(String.format("RMM:\t%s\n\n", VectorUtils.toCounter(cbeem, wordIndexer).toString()));

			if (docRels != null) {
				logBuf.append(RankComparator.compareRankings(docScoreData[colId], ret, docRels));
			}
			logBuf.append("\n");
		}

		return ret;
	}

	private SparseVector search(int colId, BaseQuery bq, SparseVector docRels) throws Exception {
		wordIndexer = new Indexer<String>();
		logBuf = new StringBuffer();

		docScoreData = new SparseVector[num_colls];
		this.bq = bq;

		List<String> queryWords = AnalyzerUtils.getWords(bq.getSearchText(), analyzer);
		StrCounter queryWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

		bq.setLuceneQuery(AnalyzerUtils.getQuery(queryWords));

		SparseVector qLM = VectorUtils.toSparseVector(queryWordCounts, wordIndexer, true);
		qLM.normalize();

		SparseVector qWordCounts = VectorUtils.toSparseVector(queryWordCounts, wordIndexer, false);

		// SparseVector expQueryModel = wikiQueryExpander.expand(wordIndexer, queryModel);
		BooleanQuery expSearchQuery = AnalyzerUtils.getQuery(VectorUtils.toCounter(qLM, wordIndexer));

		for (int i = 0; i < num_colls; i++) {
			Query searchQuery = bq.getLuceneQuery();
			int top_k = hyperParam.getTopK();
			if (colId != i) {
				top_k = hyperParam.getNumFBDocs();
			}
			docScoreData[i] = DocumentSearcher.search(searchQuery, indexSearchers[i], top_k);
		}

		// computePosteriors();

		setWordCountBoxes();

		SparseVector ret = score(colId, qLM, docRels);
		return ret;
	}

	private void computePosteriors() {
		for (int j = 0; j < docScoreData.length; j++) {
			SparseVector docScores = docScoreData[j];
			VectorMath.exponentiate(docScores, true);
		}
	}

	public void search(int colId, List<BaseQuery> baseQueries, List<SparseVector> queryDocRels, String resultFileName, String logFileName)
			throws Exception {
		if (logFileName != null) {
			logWriter = new TextFileWriter(logFileName);
			makeLog = true;
		}

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < baseQueries.size(); i++) {
			BaseQuery baseQuery = baseQueries.get(i);
			SparseVector docRels = null;

			if (queryDocRels != null) {
				docRels = queryDocRels.get(i);
			}

			SparseVector docScores = search(colId, baseQuery, docRels);

			ResultWriter.write(writer, baseQuery.getId(), docScores);

			if (logWriter != null) {
				logWriter.write(logBuf.toString().trim() + "\n\n");
			}
		}

		writer.close();
		if (logWriter != null) {
			logWriter.close();
		}
	}

	public void setMakeLog(boolean makeLog) {
		this.makeLog = makeLog;
	}

	private void setWordCountBoxes() throws Exception {
		wcbs = new WordCountBox[num_colls];

		for (int i = 0; i < num_colls; i++) {
			SparseVector docScores = docScoreData[i];
			IndexReader indexReader = indexSearchers[i].getIndexReader();
			wcbs[i] = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer);
		}
	}

}
