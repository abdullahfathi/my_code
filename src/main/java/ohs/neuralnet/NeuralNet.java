package ohs.neuralnet;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.LA;

public class NeuralNet {

	private double[][] W1;

	private double[][] W2;

	private double[] b1;

	private double[] b2;

	private NeuralNetParams param = new NeuralNetParams();

	public static void main(String[] args) {
		NeuralNet nn = new NeuralNet();

		double[][] xs = ArrayMath.random(0, 1, 100, 5);
		double[] ys = ArrayMath.random(0f, 1, 100);
		ArrayMath.round(ys, ys);

	}

	public NeuralNet() {
		W1 = new double[param.getNumHiddenNeurons()][param.getNumInputNeurons()];
		W1 = new double[param.getNumOutputNeurons()][param.getNumHiddenNeurons()];
	}

	public void train(double[][] xs, double[] ys) {
		double[] h = new double[param.getNumHiddenNeurons()];
		double[] z1 = new double[param.getNumHiddenNeurons()];
		double[] z2 = new double[param.getNumOutputNeurons()];

		double[][] yhs = new double[ys.length][param.getNumOutputNeurons()];
		double[] yh = new double[param.getNumOutputNeurons()];

		for (int i = 0; i < xs.length; i++) {
			LA.product(W1, xs[i], z1);
			ArrayMath.add(z1, b1, z1);
			ArrayMath.sigmoid(z1, h);

			LA.product(W2, h, z2);
			ArrayMath.add(z2, b1, z2);
			ArrayMath.softmax(z2, yh);
			ArrayUtils.copy(yh, yhs[i]);
		}
	}

}
