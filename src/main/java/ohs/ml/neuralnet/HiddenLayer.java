package ohs.ml.neuralnet;

import java.util.ArrayList;

import ohs.matrix.DenseMatrix;
import ohs.matrix.DenseVector;

public class HiddenLayer extends Layer {

	private boolean useBias;

	public HiddenLayer(int num_nodes, boolean useBias) {
		this.useBias = useBias;

		int size = num_nodes;

		if (useBias) {
			size++;
		}

		nodes = new DenseVector(size);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Hidden Layer"));
		sb.append(String.format("useBias:\t%s\n", useBias));
		sb.append(String.format("Nodes:\t%d\n", nodes.size()));
		return sb.toString();
	}

	public int size() {
		return nodes.size();
	}

}
