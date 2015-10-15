package ohs.ml.neuralnet;

import ohs.math.ArrayMath;
import ohs.matrix.DenseMatrix;

public class Connection {

	private DenseMatrix weights;

	public Connection(int num_inputs, int num_outputs) {
		weights = new DenseMatrix(num_outputs, num_inputs);

		for (int i = 0; i < weights.rowSize(); i++) {
			ArrayMath.random(weights.row(i).values(), 0, 1);
		}

	}

	public DenseMatrix getWeights() {
		return weights;
	}
}
