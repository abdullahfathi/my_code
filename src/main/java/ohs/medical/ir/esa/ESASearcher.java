package ohs.medical.ir.esa;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.TextFileWriter;
import ohs.lucene.common.IndexFieldName;
import ohs.math.ArrayMath;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.medical.ir.SearcherUtils;
import ohs.medical.ir.clef.ehealth_2015.CentralityEstimator;
import ohs.types.Counter;
import ohs.types.common.IntCounter;
import ohs.types.common.IntCounterMap;
import ohs.types.common.StrCounterMap;
import ohs.types.common.StrIndexer;
import ohs.utils.CounterUtils;
import ohs.utils.StopWatch;

public class ESASearcher {

	public static void doReranking(ESASearcher esaSearcher, StrCounterMap queryModels, StrCounterMap searchResult, File outputFile)
			throws Exception {
		System.out.printf("process for [%s]\n", outputFile.getName());

		List<String> queryIds = new ArrayList<String>(new TreeSet<String>(queryModels.keySet()));
		TextFileWriter writer = new TextFileWriter(outputFile.getPath());

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < queryIds.size(); i++) {
			if ((i + 1) % 10 == 0) {
				System.out.printf("\r[%d/%s, %s]", i + 1, queryIds.size(), stopWatch.stop());
			}
			String qId = queryIds.get(i);
			Counter<String> queryModel = queryModels.getCounter(qId);
			Counter<String> docScores = searchResult.getCounter(qId);
			SparseVector newDocScores = esaSearcher.score(queryModel, VectorUtils.toSparseVector(CounterUtils.toIntegerKeys(docScores)));
			newDocScores.sortByValue();

			for (int j = 0; j < newDocScores.size(); j++) {
				int docId = newDocScores.indexAtLoc(j);
				double score = newDocScores.valueAtLoc(j);
				writer.write(qId + "\t" + docId + "\t" + score + "\n");
			}
		}
		writer.close();

		System.out.printf("\r[%d/%s, %s]\n", queryIds.size(), queryIds.size(), stopWatch.stop());
	}

	public static void main(String[] args) throws Exception {

	}

	private IndexSearcher indexSearcher;

	private SparseVector collWordCounts;

	private SparseVector docScores;

	private Counter<String> queryModel;

	private StrIndexer wordIndexer;

	private SparseMatrix wordConceptWeights;

	private double cnt_sum_in_coll;

	private double num_docs_in_coll;

	private SparseMatrix docWordWeightData;

	private SparseVector wordDocFreqs;

	private SparseVector qm;

	private boolean useRandomWalk = false;

	private ESA esa;

	public ESASearcher(ESA esa, File indexDir) throws Exception {
		this.esa = esa;

		indexSearcher = SearcherUtils.getIndexSearcher(indexDir.getPath());
		cnt_sum_in_coll = indexSearcher.getIndexReader().getSumTotalTermFreq(IndexFieldName.CONTENT);
		num_docs_in_coll = indexSearcher.getIndexReader().maxDoc();
	}

	private void computeDocumentWordWeights() throws Exception {
		IndexReader indexReader = indexSearcher.getIndexReader();
		IntCounterMap c1 = new IntCounterMap();
		IntCounter c2 = new IntCounter();
		IntCounter c3 = new IntCounter();

		for (int j = 0; j < docScores.size(); j++) {
			int docId = docScores.indexAtLoc(j);
			double score = docScores.valueAtLoc(j);
			Document doc = indexReader.document(docId);

			Terms termVector = indexReader.getTermVector(docId, IndexFieldName.CONTENT);

			if (termVector == null) {
				continue;
			}

			TermsEnum reuse = null;
			TermsEnum iterator = termVector.iterator();
			BytesRef ref = null;
			DocsAndPositionsEnum docsAndPositions = null;
			IntCounter wordCounts = new IntCounter();
			List<Integer> words = new ArrayList<Integer>();

			while ((ref = iterator.next()) != null) {
				docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
				if (docsAndPositions.nextDoc() != 0) {
					throw new AssertionError();
				}

				String word = ref.utf8ToString();
				int w = wordIndexer.getIndex(word);
				int freq = docsAndPositions.freq();
				wordCounts.incrementCount(w, freq);

				for (int k = 0; k < freq; k++) {
					final int position = docsAndPositions.nextPosition();
					words.add(w);
				}
			}
			c1.setCounter(docId, wordCounts);
		}

		for (int w = 0; w < wordIndexer.size(); w++) {
			String word = wordIndexer.getObject(w);
			Term termInstance = new Term(IndexFieldName.CONTENT, word);
			double cnt = indexReader.totalTermFreq(termInstance);
			double df = indexReader.docFreq(termInstance);
			c2.setCount(w, cnt);
			c3.setCount(w, df);
		}

		docWordWeightData = VectorUtils.toSpasreMatrix(c1);
		collWordCounts = VectorUtils.toSparseVector(c2);
		wordDocFreqs = VectorUtils.toSparseVector(c3);

		for (int i = 0; i < docWordWeightData.rowSize(); i++) {
			SparseVector wordCounts = docWordWeightData.vectorAtRowLoc(i);
			for (int j = 0; j < wordCounts.size(); j++) {
				int w = wordCounts.indexAtLoc(j);
				double cnt = wordCounts.valueAtLoc(j);
				double tf = 1 + Math.log(cnt);
				double df = wordDocFreqs.valueAlways(w);
				double idf = Math.log((num_docs_in_coll + 1) / df);
				double tfidf = tf * idf;
				wordCounts.setAtLoc(j, tfidf);
			}
			wordCounts.normalizeByL2Norm();
		}

	}

	private SparseVector getQueryWordWeights() {
		SparseVector ret = qm.copy();

		for (int i = 0; i < ret.size(); i++) {
			int w = ret.indexAtLoc(i);
			double tf = ret.valueAtLoc(i);
			double df = wordDocFreqs.valueAlways(w);
			double idf = Math.log((num_docs_in_coll + 1) / df);
			double tfidf = tf * idf;
			ret.setAtLoc(i, tfidf);
		}
		ret.normalizeByL2Norm();
		return ret;
	}

	public SparseVector score(Counter<String> queryModel, SparseVector docScores) throws Exception {
		this.queryModel = queryModel;
		this.docScores = docScores;

		computeDocumentWordWeights();

		SparseVector queryWordWeights = getQueryWordWeights();
		SparseVector queryConceptWeights = null;

		{
			IntCounter c = new IntCounter();

			for (int i = 0; i < queryWordWeights.size(); i++) {
				int q = queryWordWeights.indexAtLoc(i);
				double q_weight = queryWordWeights.valueAtLoc(i);
				SparseVector conceptWeights = wordConceptWeights.rowAlways(q);

				for (int j = 0; j < conceptWeights.size(); j++) {
					int cId = conceptWeights.indexAtLoc(j);
					double c_weight = conceptWeights.valueAtLoc(j);
					c.incrementCount(cId, q_weight * c_weight);
				}
			}
			queryConceptWeights = VectorUtils.toSparseVector(c);
			queryConceptWeights.scale(1f / queryWordWeights.size());
		}

		Map<Integer, SparseVector> map = new HashMap<Integer, SparseVector>();

		for (int i = 0; i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			SparseVector docWordWeights = docWordWeightData.rowAlways(docId);
			IntCounter cc = new IntCounter();

			for (int j = 0; j < docWordWeights.size(); j++) {
				int w = docWordWeights.indexAtLoc(j);
				double w_weight = docWordWeights.valueAlways(w);

				SparseVector conceptWeights = wordConceptWeights.rowAlways(w);

				for (int k = 0; k < conceptWeights.size(); k++) {
					int cId = conceptWeights.indexAtLoc(k);
					double c_weight = conceptWeights.valueAtLoc(k);
					cc.incrementCount(cId, w_weight * c_weight);
				}
			}

			SparseVector docConceptWeights = VectorUtils.toSparseVector(cc);
			docConceptWeights.scale(1f / docWordWeights.size());
			map.put(docId, docConceptWeights);
		}

		SparseMatrix docConceptWeightData = new SparseMatrix(map);
		SparseVector ret = docScores.copy();

		if (useRandomWalk) {
			CentralityEstimator estimator = new CentralityEstimator();
			SparseVector docCents = estimator.estimate(queryConceptWeights, docConceptWeightData);
			ArrayMath.multiply(ret.values(), docCents.values(), ret.values());
		} else {
			SparseVector docCosines = new SparseVector(docScores.size());
			for (int i = 0; i < docConceptWeightData.rowSize(); i++) {
				int docId = docConceptWeightData.indexAtRowLoc(i);
				SparseVector docConceptWeights = docConceptWeightData.vectorAtRowLoc(i);
				double cosine = VectorMath.cosine(queryConceptWeights, docConceptWeights, false);
				docCosines.incrementAtLoc(i, docId, cosine);
			}

			ArrayMath.multiply(ret.values(), docCosines.values(), ret.values());
		}
		ret.normalizeAfterSummation();
		return ret;
	}

	public void setUseRandomWalk(boolean useRandomWalk) {
		this.useRandomWalk = useRandomWalk;
	}
}
