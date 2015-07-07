package ohs.medical.ir.trec.cds_2015;

import java.util.ArrayList;
import java.util.List;

import ohs.io.TextFileWriter;
import ohs.lucene.common.AnalyzerUtils;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.medical.ir.BaseQuery;
import ohs.medical.ir.DocumentIdMapper;
import ohs.medical.ir.DocumentIndexer;
import ohs.medical.ir.DocumentSearcher;
import ohs.medical.ir.KLDivergenceScorer;
import ohs.medical.ir.MIRPath;
import ohs.medical.ir.QueryReader;
import ohs.medical.ir.RelevanceModelBuilder;
import ohs.medical.ir.RelevanceReader;
import ohs.medical.ir.WordCountBox;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounterMap;
import ohs.utils.StrUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class WikiQueryExpander {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		IndexSearcher trecIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);
		IndexSearcher wikiIndexSearcher = DocumentSearcher.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki.txt";
		String logFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki_log.txt";

		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
		StrCounterMap relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);

		WikiQueryExpander expander = new WikiQueryExpander(wikiIndexSearcher, analyzer, true, 2000, 0.5, 20, 50);
		// WikiQueryExpander ex = new WikiQueryExpander(wikiIndexSearcher, analyzer, true, 2000, 0.5, 20, 100);

		TextFileWriter writer = new TextFileWriter(resultFileName);
		TextFileWriter logWriter = new TextFileWriter(logFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			String q = expander.expand(bq);
			BooleanQuery lbq = AnalyzerUtils.getQuery(q, analyzer);
			SparseVector docScores = DocumentSearcher.search(lbq, trecIndexSearcher, 1000);
			logWriter.write(expander.getLogBuffer().toString() + "\n\n");
			ResultWriter.write(writer, bq.getId(), docScores);
		}

		writer.close();
		logWriter.close();

		TrecSearcher.evalute();

		System.out.println("process ends.");
	}

	private IndexSearcher wikiIndexSearcher;

	private Analyzer analyzer;

	private TextFileWriter logWriter;

	private Indexer<String> wordIndexer;

	private double dirichlet_prior = 2000;

	private double mixture_for_rm = 0.5;

	private int num_exp_concepts = 10;

	private WordCountBox wcb;

	private TextFileWriter writer;

	private StringBuffer logBuf;

	private boolean makeLog;

	private SparseVector queryModel;

	private int num_top_concepts = 100;

	public WikiQueryExpander(IndexSearcher wikiIndexSearcher, Analyzer analyzer) {
		this(wikiIndexSearcher, analyzer, false, 2000, 0.5, 10, 100);
	}

	public WikiQueryExpander(IndexSearcher wikiIndexSearcher, Analyzer analyzer, boolean makeLog, double dirichlet_prior,
			double mixture_for_rm, int num_exp_concepts, int num_top_concepts) {
		super();
		this.wikiIndexSearcher = wikiIndexSearcher;
		this.analyzer = analyzer;
		this.makeLog = makeLog;
		this.dirichlet_prior = dirichlet_prior;
		this.mixture_for_rm = mixture_for_rm;
		this.num_exp_concepts = num_exp_concepts;
		this.num_top_concepts = num_top_concepts;
	}

	public String expand(BaseQuery bq) throws Exception {
		return expand(new Indexer<String>(), bq);
	}

	public String expand(Indexer<String> wordIndexer, BaseQuery bq) throws Exception {
		logBuf = new StringBuffer();

		StringBuffer qb1 = new StringBuffer(bq.getSearchText().trim());

		Counter<String> qwcs1 = AnalyzerUtils.getWordCounts(qb1.toString(), analyzer);
		BooleanQuery q1 = AnalyzerUtils.getQuery(qwcs1);

		SparseVector conceptScores1 = DocumentSearcher.search(q1, wikiIndexSearcher, num_top_concepts);

		SparseVector queryModel = VectorUtils.toSparseVector(qwcs1, wordIndexer, true);
		queryModel.normalizeAfterSummation();

		WordCountBox wcb = WordCountBox.getWordCountBox(wikiIndexSearcher.getIndexReader(), conceptScores1, wordIndexer);

		// conceptScores = KLDivergenceScorer.scoreDocuments(wcb, queryModel);

		SparseVector relevanceModel = RelevanceModelBuilder.getRelevanceModel(wcb, conceptScores1);

		SparseVector expQueryModel = VectorMath.addAfterScale(queryModel, relevanceModel, 1 - mixture_for_rm, mixture_for_rm);

		KLDivergenceScorer kldScorer = new KLDivergenceScorer();
		SparseVector conceptScores2 = kldScorer.scoreDocuments(wcb, expQueryModel);

		BidMap<Integer, String> conceptMap = new BidMap<Integer, String>();
		ListMap<String, String> categoryMap = new ListMap<String, String>();

		for (int i = 0; i < conceptScores2.size(); i++) {
			int cid = conceptScores2.indexAtLoc(i);
			double score2 = conceptScores2.valueAtLoc(i);
			double score1 = conceptScores1.valueAlways(cid);

			Document article = wikiIndexSearcher.doc(cid);
			String concept = article.get(IndexFieldName.TITLE);
			String categories = article.get(IndexFieldName.CATEGORY);
			conceptMap.put(cid, concept);

			for (String cat : categories.split("\n")) {
				categoryMap.put(concept, cat);
			}
		}

		List<String> concepts = new ArrayList<String>();

		StringBuffer qb2 = new StringBuffer();
		qb2.append(qb1.toString() + "\n\n");

		conceptScores2.sortByValue();

		for (int i = 0; i < conceptScores2.size() && concepts.size() < num_exp_concepts; i++) {
			int cid = conceptScores2.indexAtLoc(i);
			String concept = conceptMap.getValue(cid);
			if (concept.startsWith("List of") || concept.startsWith("Wikipedia:")) {
				continue;
			}
			concepts.add(concept);
			//
			// qb2.append(concept);
			// qb2.append("\n");
		}

		relevanceModel.sortByValue();

		List<String> words = new ArrayList<String>();

		for (int i = 0; i < relevanceModel.size() && words.size() < num_exp_concepts; i++) {
			int w = relevanceModel.indexAtLoc(i);
			String word = wordIndexer.getObject(w);
			if (word.startsWith("#")) {
				continue;
			}
			words.add(word);
		}

		qb2.append(StrUtils.join("\n", words));

		Counter<String> qwcs2 = AnalyzerUtils.getWordCounts(qb2.toString(), analyzer);

		if (makeLog) {
			logBuf.append(String.format("ID:\t%s\n", bq.getId()));
			logBuf.append(String.format("Q1:\t%s\n", bq.getSearchText().trim()));
			logBuf.append(String.format("Q2:\t%s\n", qb2.toString().replace("\n", "<NL>")));
			logBuf.append(String.format("QC1:\t%s\n", qwcs1.toString()));
			logBuf.append(String.format("QC2:\t%s\n", qwcs2.toString()));
			logBuf.append(String.format("QM1:\t%s\n", VectorUtils.toCounter(queryModel, wordIndexer).toString(200)));
			logBuf.append(String.format("QM2:\t%s\n", VectorUtils.toCounter(expQueryModel, wordIndexer).toString(200)));
			logBuf.append("Concepts:\n");

			SparseVector ranking1 = conceptScores1.ranking();
			SparseVector ranking2 = conceptScores2.ranking();
			ranking2.sortByValue(false);

			for (int j = 0; j < ranking2.size() && j < 20; j++) {
				int cid = ranking2.indexAtLoc(j);
				String concept = conceptMap.getValue(cid);

				int rank1 = (int) ranking1.valueAlways(cid);
				int rank2 = (int) ranking2.valueAtLoc(j);

				logBuf.append(String.format("%d:\t%s\t%d\t%d", j + 1, concept, rank1, rank2));
				if (j != ranking2.size() - 1) {
					logBuf.append("\n");
				}
			}
			logBuf.append("Words:\n");
		}
		return qb2.toString();
	}

	public StringBuffer getLogBuffer() {
		return logBuf;
	}

	public SparseVector getQueryModel() {
		return queryModel;
	}
}
