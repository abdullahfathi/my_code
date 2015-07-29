package ohs.medical.ir.query;

import java.util.List;

import ohs.matrix.SparseVector;
import ohs.types.common.IntHashMap;

import org.apache.lucene.search.Query;

public interface BaseQuery {

	public String getId();

	public Query getLuceneQuery();

	public List<Integer> getQueryWords();

	// public SparseVector getQueryModel();

	public String getSearchText();

	public void setLuceneQuery(Query luceneQuery);

	// public void setQueryModel(SparseVector queryModel);

	// public void setQueryWords(List<Integer> words);

}
