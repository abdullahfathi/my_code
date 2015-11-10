package ohs.neuralnet;

import edu.stanford.nlp.parser.shiftreduce.ShiftReduceTrainOptions.TrainingMethod;
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

		nn.train(xs, ys);

	}

	public NeuralNet() {
		W1 = new double[param.getNumHiddenNeurons()][param.getNumInputNeurons()];
		W2 = new double[param.getNumOutputNeurons()][param.getNumHiddenNeurons()];
		b1 = new double[param.getNumHiddenNeurons()];
		b2 = new double[param.getNumOutputNeurons()];

		ArrayMath.random(0, 1, W1);
		ArrayMath.random(0, 1, W2);
		ArrayMath.random(0, 1, b1);
		ArrayMath.random(0, 1, b2);
	}

	public void train(double[][] xs, double[] ys) {
		double[] h = new double[param.getNumHiddenNeurons()];
		double[] z1 = new double[param.getNumHiddenNeurons()];
		double[] z2 = new double[param.getNumOutputNeurons()];

		double[][] yhs = new double[ys.length][param.getNumOutputNeurons()];
		double[] yh = new double[param.getNumOutputNeurons()];

		for (int i = 0; i < xs.length; i++) {
			ArrayMath.product(W1, xs[i], z1);
			ArrayMath.add(z1, b1, z1);
			ArrayMath.sigmoid(z1, h);

			ArrayMath.product(W2, h, z2);
			ArrayMath.add(z2, b2, z2);
			ArrayMath.softmax(z2, yh);
			ArrayUtils.copy(yh, yhs[i]);
		}
	}

}
