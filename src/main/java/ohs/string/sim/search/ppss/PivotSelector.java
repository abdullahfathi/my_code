package ohs.string.sim.search.ppss;

import java.io.Serializable;

import ohs.string.sim.search.ppss.Gram.Type;

public abstract class PivotSelector implements Serializable {

	protected int q;

	protected int pivot_size;

	protected int prefix_size;

	protected Gram[] grams;

	abstract public void select(Gram[] grams);

	protected void selectPrefixes() {
		for (int i = 0; i < grams.length; i++) {
			Gram gram = grams[i];
			if (i < prefix_size) {
				gram.setType(Type.PREFIX);
			} else {
				gram.setType(Type.SUFFIX);
			}
		}
	}

}
