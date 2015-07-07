package ohs.medical.ir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohs.io.TextFileReader;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.common.IntCounter;
import ohs.types.common.StrBidMap;
import ohs.types.common.StrCounterMap;

public class DocumentIdMapper {

	public static List<SparseVector> mapDocIdsToIndexIds(List<BaseQuery> baseQueries, StrCounterMap relevanceData, StrBidMap docIdMap) {
		List<SparseVector> ret = new ArrayList<SparseVector>();
		for (BaseQuery bq : baseQueries) {
			String queryId = bq.getId();
			Counter<String> docScores = relevanceData.getCounter(queryId);
			IntCounter counter = new IntCounter();

			for (String docId : docScores.keySet()) {
				double score = docScores.getCount(docId);
				String indexId = docIdMap.getKey(docId);

				if (indexId == null) {
					System.out.printf("index-id is not found for doc-id [%s]\n", docId);
					continue;
				}
				counter.setCount(Integer.parseInt(indexId), score);
			}
			SparseVector sv = new SparseVector(counter, -1, -1);
			ret.add(sv);
		}

		return ret;
	}

	public static StrCounterMap mapIndexIdsToDocIds(StrCounterMap resultData, StrBidMap docIdMap) {
		StrCounterMap ret = new StrCounterMap();
		for (String queryId : resultData.keySet()) {
			Counter<String> indexScores = resultData.getCounter(queryId);
			for (String indexId : indexScores.keySet()) {
				double score = indexScores.getCount(indexId);
				String docId = docIdMap.getValue(indexId);
				ret.setCount(queryId, docId, score);
			}
		}
		return ret;
	}

	public static StrBidMap readDocumentIdMap(String fileName) {
		StrBidMap ret = new StrBidMap();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
			ret.put(parts[0], parts[1]);
		}
		reader.close();
		return ret;
	}
}
