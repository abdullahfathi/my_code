package ohs.ml.neuralnet;

import java.util.ArrayList;
import java.util.List;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.matrix.SparseVector;

public class MultiLayerPerceptron {

	public static void main(String[] args) {
		System.out.println("process beings.");

		int[] indexes = { 1, 2, 3, 4 };

		List<SparseVector> trainData = new ArrayList<SparseVector>();

		for (int i = 0; i < 10; i++) {
			SparseVector x = new SparseVector(indexes.length);

			double[] values = new double[indexes.length];
			ArrayMath.random(values, 1, 5);
			x.setIndexes(ArrayUtils.copy(indexes));
			x.setValues(values);

			int label = i % 2 == 0 ? 0 : 1;
			x.setLabel(label);

			trainData.add(x);
		}

		MultiLayerPerceptron mlp = new MultiLayerPerceptron(4, new int[] { 2 }, 2, true);
		mlp.train(trainData);

		System.out.println("process ends.");
	}

	private InputLayer inputLayer;

	private List<Layer> layers;

	private OutputLayer outputLayer;

	public MultiLayerPerceptron(int num_input_nodes, int[] num_nodes_in_hidden_layer, int num_output_nodes, boolean useBias) {
		layers = new ArrayList<Layer>();
		layers.add(new InputLayer(num_input_nodes, useBias));

		for (int i = 0; i < num_nodes_in_hidden_layer.length; i++) {
			layers.add(new HiddenLayer(num_nodes_in_hidden_layer[i], useBias));
		}
		layers.add(new OutputLayer(num_output_nodes));

		for (int i = 1; i < layers.size(); i++) {
			Layer prevLayer = layers.get(i - 1);
			Layer currLayer = layers.get(i);
			Connection con = new Connection(prevLayer.size(), currLayer.size());

			prevLayer.getConnections().add(con);
			currLayer.getConnections().add(con);
		}
	}

	public SparseVector output(SparseVector x) {
		inputLayer.setInput(x);

		for (int i = 0; i < layers.size(); i++) {
			
		}

		return null;
	}

	public void train(List<SparseVector> trainData) {
		for (int i = 0; i < trainData.size(); i++) {
			SparseVector x = trainData.get(i);
			InputLayer inputLayer = (InputLayer) layers.get(0);
			inputLayer.setInput(x);
		}
	}

}
