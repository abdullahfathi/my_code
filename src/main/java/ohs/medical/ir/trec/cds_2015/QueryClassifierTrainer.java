package ohs.medical.ir.trec.cds_2015;

import java.util.ArrayList;
import java.util.List;

import ohs.io.TextFileReader;
import ohs.lucene.common.IndexFieldName;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.medical.ir.MIRPath;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.utils.StrUtils;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class QueryClassifierTrainer {

	private static List<SparseVector> getDocumentWordCounts(

	IndexReader indexReader, Indexer<String> wordIndexer, List<Integer> indexIds, List<Integer> typeIds) throws Exception {

		CounterMap<Integer, Integer> docWordCounts = new CounterMap<Integer, Integer>();

		List<SparseVector> svs = new ArrayList<SparseVector>();

		for (int j = 0; j < indexIds.size(); j++) {
			int indexId = indexIds.get(j);
			int typeId = typeIds.get(j);

			Terms termVector = indexReader.getTermVector(indexId, IndexFieldName.CONTENT);

			if (termVector == null) {
				continue;
			}

			TermsEnum reuse = null;
			TermsEnum iterator = termVector.iterator(reuse);
			BytesRef ref = null;
			DocsAndPositionsEnum docsAndPositions = null;
			Counter<Integer> counter = new Counter<Integer>();

			while ((ref = iterator.next()) != null) {
				docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
				if (docsAndPositions.nextDoc() != 0) {
					throw new AssertionError();
				}
				String word = ref.utf8ToString();
				int w = wordIndexer.indexOf(word);

				if (w < 0) {
					continue;
				}

				int freq = docsAndPositions.freq();
				counter.incrementCount(w, freq);
			}

			SparseVector sv = VectorUtils.toSparseVector(counter);
			sv.setLabel(typeId);
			sv.setDim(wordIndexer.size());
			svs.add(sv);
		}

		return svs;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		QueryClassifierTrainer trainer = new QueryClassifierTrainer();
		trainer.train();

		System.out.println("process ends.");
	}

	private Indexer<String> labelIndexer;

	private Indexer<String> featureIndexer;

	private List<SparseVector> trainData;

	private Model model;

	private void generateTrainData() throws Exception {
		System.out.println("generate training data.");

		ListMap<Integer, SparseVector> map = new ListMap<Integer, SparseVector>();

		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_QUERY_DOC_FILE);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] parts = line.split("\t");
				int qid = Integer.parseInt(parts[1]);

				Counter<String> c = new Counter<String>();
				String[] toks = parts[2].split(" ");
				for (int j = 0; j < toks.length; j++) {
					String[] two = StrUtils.split2Two(":", toks[j]);
					c.incrementCount(two[0], Double.parseDouble(two[1]));
				}
				System.out.println(line);
			}
		}
		reader.close();

	}

	private Parameter getSVMParamter() {
		Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC_DUAL, 1, Double.POSITIVE_INFINITY, 0.1);

		if (param.getEps() == Double.POSITIVE_INFINITY) {
			switch (param.getSolverType()) {
			case L2R_LR:
			case L2R_L2LOSS_SVC:
				param.setEps(0.01);
				break;
			case L2R_L2LOSS_SVR:
				param.setEps(0.001);
				break;
			case L2R_L2LOSS_SVC_DUAL:
			case L2R_L1LOSS_SVC_DUAL:
			case MCSVM_CS:
			case L2R_LR_DUAL:
				param.setEps(0.1);
				break;
			case L1R_L2LOSS_SVC:
			case L1R_LR:
				param.setEps(0.01);
				break;
			case L2R_L1LOSS_SVR_DUAL:
			case L2R_L2LOSS_SVR_DUAL:
				param.setEps(0.1);
				break;
			default:
				throw new IllegalStateException("unknown solver type: " + param.getSolverType());
			}
		}

		return param;
	}

	public void train() throws Exception {
		System.out.println("train.");

		generateTrainData();
		trainSVMs();
	}

	private void trainSVMs() {
		System.out.println("train SVMs.");
		Problem prob = new Problem();
		prob.l = trainData.size();
		prob.n = featureIndexer.size() + 1;
		prob.y = new double[prob.l];
		prob.x = new Feature[prob.l][];
		prob.bias = -1;

		if (prob.bias >= 0) {
			prob.n++;
		}

		for (int i = 0; i < trainData.size(); i++) {
			SparseVector x = trainData.get(i);

			Feature[] input = new Feature[prob.bias > 0 ? x.size() + 1 : x.size()];

			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j) + 1;
				double value = x.valueAtLoc(j);

				assert index >= 0;

				input[j] = new FeatureNode(index + 1, value);
			}

			if (prob.bias >= 0) {
				input[input.length - 1] = new FeatureNode(prob.n, prob.bias);
			}

			prob.x[i] = input;
			prob.y[i] = x.label();
		}

		model = Linear.train(prob, getSVMParamter());
	}

}
