package ohs.medical.ir;

import java.io.File;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ohs.io.IOUtils;
import ohs.matrix.SparseVector;
import ohs.types.CounterMap;
import ohs.types.common.IntHashMap;

import org.apache.lucene.search.Query;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TrecCdsQuery implements BaseQuery {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		List<BaseQuery> queries = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		for (int i = 0; i < queries.size(); i++) {
			System.out.println(queries.get(i));
			System.out.println();
		}

		System.out.println("process ends.");
	}

	private String id;

	private String description;

	private String summary;

	private String type;

	private Query luceneQuery;

	private SparseVector queryModel;

	private List<Integer> words;

	public TrecCdsQuery(String id, String description, String summary, String type) {
		super();
		this.id = id;
		this.description = description;
		this.summary = summary;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrecCdsQuery other = (TrecCdsQuery) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (summary == null) {
			if (other.summary != null)
				return false;
		} else if (!summary.equals(other.summary))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	@Override
	public Query getLuceneQuery() {
		return luceneQuery;
	}

	public String getSearchText() {
		String ret = description;
		ret = ret.replaceAll("[\\p{Punct}]+", " ");
		return ret;
	}

	public String getType() {
		return type;
	}

	@Override
	public List<Integer> getQueryWords() {
		return words;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((summary == null) ? 0 : summary.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void setLuceneQuery(Query luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("qid:\t%s\n", id));
		sb.append(String.format("type:\t%s\n", type));
		sb.append(String.format("description:\t%s\n", description));
		sb.append(String.format("summary:\t%s", summary));
		return sb.toString();
	}
}
