package ohs.ling.struct;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SpanComparator implements Comparator<Span> {
	public int compare(Span o1, Span o2) {
		return o1.getStart() > o2.getStart() ? 1 : -1;
	}

	public static void sort(List<Span> spans) {
		Collections.sort(spans, new SpanComparator());
	}
}
