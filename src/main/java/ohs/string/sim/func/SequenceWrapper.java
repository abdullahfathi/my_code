package ohs.string.sim.func;

import java.io.Serializable;

public interface SequenceWrapper<K> extends Serializable {

	public K get(int i);

	public int length();

}
