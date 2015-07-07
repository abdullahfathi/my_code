package ohs.medical.ir.clef.ehealth_2015;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.poi.util.IntList;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.common.IntArrayList;
import ohs.types.common.IntCounter;
import ohs.types.common.IntCounterMap;
import ohs.types.common.StrIndexer;

/**
 * This class implements centralities of categories.
 * 
 * The standard centralites are computed by PageRank algorithms where a graph over categories are constructed.
 * 
 * 
 * 
 * 
 * 1. Kurland, O. and Lee, L. 2005. PageRank without hyperlinks: structural re-ranking using links induced by language models. Proceedings
 * of the 28th annual international ACM SIGIR conference on Research and development in information retrieval, 306–313.
 * 
 * 
 * 2. Strube, M. and Ponzetto, S.P. 2006. WikiRelate! computing semantic relatedness using wikipedia. proceedings of the 21st national
 * conference on Artificial intelligence - Volume 2, AAAI Press, 1419–1424.
 * 
 * 
 * @author Heung-Seon Oh
 * 
 * 
 */
public class CentralityEstimator {

	private StringBuffer logBuff;

	public String getLog() {
		return logBuff.toString();
	}

	private StrIndexer docIndexer;

	public SparseVector estimate(SparseVector queryConceptWeights, SparseMatrix docConceptWeightData) {
		List<SparseVector> weightVectors = new ArrayList<SparseVector>();
		weightVectors.add(queryConceptWeights);

		IntArrayList docIds = new IntArrayList();

		for (int i = 0; i < docConceptWeightData.rowSize(); i++) {
			int docId = docConceptWeightData.indexAtRowLoc(i);
			SparseVector v = docConceptWeightData.vectorAtRowLoc(i);
			weightVectors.add(v);
			docIds.add(docId);
		}

		int dim = weightVectors.size();

		double[][] sim_matrix = ArrayUtils.newMatrix(dim, 0);

		for (int i = 0; i < dim; i++) {
			sim_matrix[i][i] = 1;

			for (int j = i + 1; j < dim; j++) {
				double sim = 1;
				SparseVector v1 = weightVectors.get(i);
				SparseVector v2 = weightVectors.get(j);
				double cosine = VectorMath.cosine(v1, v2, false);
				sim_matrix[i][j] = cosine;
				sim_matrix[j][i] = cosine;
			}
		}

		double[] cents = ArrayMath.doRandomWalk(sim_matrix, 100, 0.00000001, 0.85);

		IntCounter ret = new IntCounter();

		for (int i = 1; i < cents.length; i++) {
			double cent = cents[i];
			int docId = docIds.get(i - 1);
			ret.incrementCount(docId, cent);
		}
		return VectorUtils.toSparseVector(ret);

	}
}
