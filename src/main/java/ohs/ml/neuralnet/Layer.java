package ohs.ml.neuralnet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ohs.matrix.DenseVector;

abstract class Layer implements Serializable {

	protected DenseVector nodes;

	protected List<Connection> conns = new ArrayList<Connection>();

	protected List<Layer> layers = new ArrayList<Layer>();

	public List<Connection> getConnections() {
		return conns;
	}

	public List<Layer> getLayers() {
		return layers;
	}

	public int size() {
		return nodes.size();
	}

}
