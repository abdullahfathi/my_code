package ohs.ml.neuralnet;

import java.util.ArrayList;

import ohs.matrix.DenseMatrix;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;

public class InputLayer extends Layer {

	private boolean useBias;

	private SparseVector input;

	public InputLayer(int num_nodes, boolean useBias) {
		this.useBias = useBias;

		int size = num_nodes;
		if (useBias) {
			size++;
		}
		nodes = new DenseVector(size);
	}

	public SparseVector getInput() {
		return input;
	}

	public void setInput(SparseVector input) {
		this.input = input;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Input Layer"));
		sb.append(String.format("useBias:\t%s\n", useBias));
		sb.append(String.format("Nodes:\t%d\n", nodes.size()));
		return sb.toString();
	}

}
