package ohs.ml.neuralnet;

import java.util.ArrayList;

import ohs.matrix.DenseVector;

public class OutputLayer extends Layer {

	public OutputLayer(int num_nodes) {
		nodes = new DenseVector(num_nodes);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Output Layer"));
		sb.append(String.format("Nodes:\t%d\n", nodes.size()));
		return sb.toString();
	}

}
