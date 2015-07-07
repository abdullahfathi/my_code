package ohs.string.sim.search.ppss;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.entity.DataReader;
import ohs.entity.ENTPath;
import ohs.entity.data.struct.BilingualText;
import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.string.sim.func.AffineGap;
import ohs.string.sim.func.SmithWaterman;
import ohs.string.sim.search.ppss.Gram.Type;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.DeepMap;
import ohs.types.ListMap;
import ohs.types.Pair;

/**
 * 
 * Implementation of "A Pivotal Prefix Based Filtering Algorithm for String Similarity Search" at SIGMOD'14
 * 
 * 
 * @author Heung-Seon Oh
 */
public class PivotalPrefixStringSearcher {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		// test0();
		test1();
		// test2();
		System.out.println("process ends.");
	}

	public static void test0() throws Exception {
		PivotalPrefixStringSearcher ppss = new PivotalPrefixStringSearcher();
		ppss.read(ENTPath.PPSS_INDEX_FILE);
	}

	public static void test1() throws Exception {

		List<BilingualText> orgNames = DataReader.readBaseOrgNames(ENTPath.BASE_ORG_NAME_FILE);
		Counter<BilingualText> externalOrgCounts = DataReader.readBilingualTextCounter(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);

		List<StringRecord> strings = new ArrayList<StringRecord>();
		List<StringRecord> strings2 = new ArrayList<StringRecord>();

		for (int i = 0; i < orgNames.size(); i++) {
			strings.add(new StringRecord(i, orgNames.get(i).getKorean()));
			strings2.add(new StringRecord(i, orgNames.get(i).getKorean()));
		}

		GramSorter gramSorter = new GramSorter();

		{
			List<StringRecord> ss = new ArrayList<StringRecord>();

			for (BilingualText orgName : externalOrgCounts.keySet()) {
				String korName = orgName.getKorean();
				if (korName.length() == 0) {
					continue;
				}
				ss.add(new StringRecord(ss.size(), korName));
			}

			Counter<String> gramWeights1 = GramWeighter.compute(new GramGenerator(2), ss);
			Counter<String> gramWeights2 = GramWeighter.computeTFIDFs(new GramGenerator(2), ss);

			for (String g : gramWeights2.keySet()) {
				double w = gramWeights2.getCount(g);
				gramWeights2.setCount(g, 1f / w);
			}

			double min = gramWeights2.min();
			gramWeights2.scale(1f / min);

			System.out.println(gramWeights1);
			System.out.println(gramWeights2);
			gramSorter.setGramWeights(gramWeights1);
			// gramSorter.setIsAscendingOrder(false);
		}

		int q = 2;
		int tau = 2;

		PivotalPrefixStringSearcher ppss = new PivotalPrefixStringSearcher(q, tau, true);
		ppss.setGramSorter(gramSorter);
		ppss.index(strings);
		ppss.write(ENTPath.PPSS_INDEX_FILE);

		// {
		// TextFileWriter writer = new TextFileWriter(ENTPath.DATA_DIR + "ppss_res.txt");
		// for (int i = 0; i < strings2.size(); i++) {
		// String str = strings2.get(i);
		// System.out.println(str);
		//
		// // if (!str.contains("경남양돈산업클러스터사업단")) {
		// // continue;
		// // }
		// Counter<String> res = ext.search(str);
		// writer.write(String.format("Input:\t%s\n", str));
		// writer.write(String.format("Output:\t%s\n\n", res.toStringSortedByValues(false, false, res.size())));
		// }
		//
		// writer.close();
		// }

		{

			TextFileWriter writer = new TextFileWriter(ENTPath.DATA_DIR + "ppss_res.txt");
			List<BilingualText> orgNameList = externalOrgCounts.getSortedKeys();

			for (int i = 0; i < orgNameList.size(); i++) {
				BilingualText name = orgNameList.get(i);
				double cnt = externalOrgCounts.getCount(name);

				// if (cnt > 100 || cnt < 50) {
				// continue;
				// }

				// if (!name.getKorean().contains("서울대")) {
				// continue;
				// }

				System.out.println(name + "\n");
				Counter<StringRecord> res = ppss.search(name.getKorean());
				StringBuffer sb = new StringBuffer();
				sb.append("Input:\n");
				sb.append(name.toString() + "\n");
				sb.append(String.format("Output:\t%s\n\n", res.toStringSortedByValues(false, false, 100)));
				writer.write(sb.toString());
			}
			writer.close();
		}
	}

	public static void test2() {
		String[] strs = { "imyouteca", "ubuntucom", "utubbecou", "youtbecom", "yoytubeca" };
		String s = "yotubecom";

		List<StringRecord> strings = new ArrayList<StringRecord>();

		for (int i = 0; i < strs.length; i++) {
			strings.add(new StringRecord(i, strs[i]));
		}

		GramSorter gramSorter = new GramSorter();

		int q = 2;
		int tau = 2;

		PivotalPrefixStringSearcher ppss = new PivotalPrefixStringSearcher(q, tau, true);
		ppss.setGramSorter(gramSorter);
		ppss.index(strings);
		Counter<StringRecord> res = ppss.search(s);

		System.out.println(res.toString());
	}

	private int q;

	private int tau;

	private int prefix_size;

	private int pivot_size;

	private List<StringRecord> ss;

	private List<Gram[]> allGrams;

	private GramInvertedIndex L;

	private Map<String, Integer> gramOrders;

	private GramGenerator gramGenerator;

	private PivotSelector pivotSelector;

	private StringVerifier stringVerifier;

	private GramSorter gramSorter;

	private BidMap<StringRecord, Integer> idMap;

	public PivotalPrefixStringSearcher() {
		this(2, 2, false);
	}

	public PivotalPrefixStringSearcher(int q, int tau, boolean useOptimalPivotSelector) {
		this.q = q;
		this.tau = tau;

		pivot_size = tau + 1;

		prefix_size = q * tau + 1;

		this.gramSorter = new GramSorter();

		gramGenerator = new GramGenerator(q);

		stringVerifier = new StringVerifier(q, tau);

		pivotSelector = useOptimalPivotSelector

		? new OptimalPivotSelector(q, prefix_size, pivot_size) : new RandomPivotSelector(q, prefix_size, pivot_size);
	}

	private int[] determineSearchRange(int len, Type indexType, GramPostings postings) {
		int[] ret = new int[] { 0, postings.getEntries().size() };
		int start = 0;
		int end = postings.size();

		DeepMap<Type, Integer, Integer> typeLenLocMap = postings.getTypeLengthLocs();
		Map<Integer, Integer> M = typeLenLocMap.get(indexType);

		int start_key = len - tau;
		int end_key = len + tau + 1;

		Integer t_start = M.get(start_key);
		Integer t_end = M.get(end_key);

		if (t_start == null) {
			start_key = len - tau + 1;
			t_start = M.get(start_key);
		}

		if (t_start == null) {
			t_start = M.get(len);
		}

		if (t_end == null) {
			end_key = len + tau + 2;
			t_end = M.get(end_key);
		}

		if (t_end == null) {
			t_end = M.get(len + 1);
		}

		if (t_start != null) {
			start = t_start.intValue();
		}

		if (t_end != null) {
			end = t_end.intValue();
		}

		return ret;
	}

	public List<Gram[]> getAllGrams() {
		return allGrams;
	}

	public GramGenerator getGramGenerator() {
		return gramGenerator;
	}

	public GramSorter getGramSorter() {
		return gramSorter;
	}

	private Set<String> getIntersection(Set<String> a, Set<String> b) {
		Set<String> ret = new HashSet<String>();

		Set<String> large = null;
		Set<String> small = null;

		if (a.size() > b.size()) {
			large = a;
			small = b;
		} else {
			large = b;
			small = a;
		}
		for (String s : small) {
			if (large.contains(s)) {
				ret.add(s);
			}
		}
		return ret;
	}

	private int getLastLoc(List<Integer> list) {
		return list.size() > 0 ? list.get(list.size() - 1) : -1;
	}

	private Pair<String, Integer> getLastPrefix(Gram[] grams, ListMap<Type, Integer> groups) {
		List<Integer> prefixLocs = groups.get(Type.PREFIX);
		int last_loc = prefixLocs.get(prefixLocs.size() - 1);
		Gram gram = grams[last_loc];
		Integer order = gramOrders.get(gram.getString());

		if (order == null) {
			order = -1;
		}
		return new Pair<String, Integer>(gram.getString(), order);
	}

	public int getPivotSize() {
		return pivot_size;
	}

	public int getPrefixSize() {
		return prefix_size;
	}

	public int getQ() {
		return q;
	}

	public BidMap<StringRecord, Integer> getStringRecordIdMap() {
		return idMap;
	}

	public List<StringRecord> getStringRecords() {
		return ss;
	}

	private Set<String> getStringSet(Gram[] grams, List<Integer> locs) {
		Set<String> ret = new HashSet<String>();
		for (int i = 0; i < locs.size(); i++) {
			ret.add(grams[locs.get(i)].getString());
		}
		return ret;
	}

	public StringVerifier getStringVerifier() {
		return stringVerifier;
	}

	public int getTau() {
		return tau;
	}

	public void index(List<StringRecord> srs) {
		StringSorter.sort(srs);

		ss = new ArrayList<StringRecord>();
		allGrams = new ArrayList<Gram[]>();
		idMap = new BidMap<StringRecord, Integer>();

		for (int i = 0; i < srs.size(); i++) {
			StringRecord sr = srs.get(i);
			String s = sr.getString();
			Gram[] grams = gramGenerator.generate(s.toLowerCase());

			if (grams.length == 0) {
				continue;
			}

			allGrams.add(grams);
			ss.add(sr);
			idMap.put(sr, sr.getId());
		}

		if (gramSorter.getGramWeights() == null) {
			gramSorter.setGramWeights(GramWeighter.compute(allGrams));
		}

		gramSorter.sortGrams();
		gramOrders = gramSorter.getGramOrders();

		if (pivotSelector instanceof OptimalPivotSelector) {
			((OptimalPivotSelector) pivotSelector).setGramWeights(gramSorter.getGramWeights());
		}

		L = new GramInvertedIndex();

		for (int i = 0; i < allGrams.size(); i++) {
			String s = ss.get(i).getString();
			Gram[] grams = allGrams.get(i);

			gramSorter.order(grams);
			pivotSelector.select(grams);

			int len = s.length();

			for (int j = 0; j < grams.length; j++) {
				Gram gram = grams[j];
				String g = gram.getString();
				int start = gram.getStart();
				Type type = gram.getType();

				if (type == Type.SUFFIX) {
					continue;
				}

				GramPostings gp = L.get(g, true);
				gp.getEntries().add(new GramPostingEntry(i, start, type));

				DeepMap<Type, Integer, Integer> M = gp.getTypeLengthLocs();

				if (!M.containsKeys(Type.PREFIX, len)) {
					M.put(Type.PREFIX, len, gp.getEntries().size() - 1);
				}

				if (type == Type.PIVOT) {
					if (!M.containsKeys(Type.PIVOT, len)) {
						M.put(Type.PIVOT, len, gp.getEntries().size() - 1);
					}
				}
			}
		}
	}

	public void read(BufferedReader reader) throws Exception {
		{
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("## PPSS")) {
					break;
				}
			}
		}

		int num_read = GramUtils.getNumLinesToRead(reader);

		for (int i = 0; i < num_read; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\t");

			if (i == 0) {
				q = Integer.parseInt(parts[1]);
				prefix_size = q * tau + 1;
			} else if (i == 1) {
				tau = Integer.parseInt(parts[1]);
				pivot_size = tau + 1;
			} else if (i == 2) {
				pivotSelector = Boolean.parseBoolean(parts[1]) ?

				new OptimalPivotSelector(q, prefix_size, pivot_size) : new RandomPivotSelector(q, pivot_size, prefix_size);
			}
		}

		stringVerifier = new StringVerifier(q, tau);
		gramGenerator = new GramGenerator(q);

		ss = new ArrayList<StringRecord>();

		allGrams = new ArrayList<Gram[]>();

		num_read = GramUtils.getNumLinesToRead(reader);

		for (int i = 0; i < num_read; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			int id = Integer.parseInt(parts[1]);
			String s = parts[2];
			ss.add(new StringRecord(id, s));
			allGrams.add(gramGenerator.generate(s));
		}

		L = new GramInvertedIndex();
		L.read(reader);

		gramSorter = new GramSorter();
		Counter<String> gramWeights = new Counter<String>();

		num_read = GramUtils.getNumLinesToRead(reader);

		for (int i = 0; i < num_read; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			gramWeights.setCount(parts[0], Double.parseDouble(parts[1]));
		}

		gramSorter.setGramWeights(gramWeights);
		gramSorter.sortGrams();
	}

	public void read(String fileName) throws Exception {
		BufferedReader reader = IOUtils.openBufferedReader(fileName);
		read(reader);
		reader.close();
	}

	public Counter<StringRecord> search(String s) {
		Gram[] grams = gramGenerator.generate(s.toLowerCase());

		if (grams.length == 0) {
			return new Counter<StringRecord>();
		}

		gramSorter.order(grams);
		pivotSelector.select(grams);

		ListMap<Type, Integer> groups = GramUtils.groupByTypes(grams);
		Set<String> prefixesInS = getStringSet(grams, groups.get(Type.PREFIX));
		Set<String> pivotsInS = getStringSet(grams, groups.get(Type.PIVOT));
		Pair<String, Integer> lastPrefixInS = getLastPrefix(grams, groups);

		if (lastPrefixInS.getSecond() < 0) {
			return new Counter<StringRecord>();
		}

		Set<Integer> C = new HashSet<Integer>();

		Type[] searchTypes = new Type[] { Type.PIVOT, Type.PREFIX };
		Type[] indexTypes = new Type[] { Type.PREFIX, Type.PIVOT };

		int len = s.length();

		for (int i = 0; i < searchTypes.length; i++) {
			Type searchType = searchTypes[i];
			Type indexType = indexTypes[i];

			for (int loc : groups.get(searchType)) {
				Gram gram = grams[loc];
				String g = gram.getString();

				GramPostings gp = L.get(g, false);

				if (gp == null) {
					continue;
				}

				int[] range = determineSearchRange(len, indexType, gp);

				int start = range[0];
				int end = range[1];

				// System.out.println(gram.getString() + " -> " + postings.toString());

				for (int j = start; j < gp.size() && j < end; j++) {
					GramPostingEntry entry = gp.getEntries().get(j);
					if (searchType == Type.PREFIX) {
						if (indexType != Type.PIVOT) {
							continue;
						}
					}

					int rid = entry.getId();
					int p = entry.getStart();
					String r = ss.get(rid).getString();

					Gram[] rGrams = allGrams.get(rid);
					ListMap<Type, Integer> groupsInR = GramUtils.groupByTypes(rGrams);
					Set<String> prefixesInR = getStringSet(rGrams, groupsInR.get(Type.PREFIX));
					Set<String> pivotsInR = getStringSet(rGrams, groupsInR.get(Type.PIVOT));
					Pair<String, Integer> lastPrefixInR = getLastPrefix(rGrams, groupsInR);

					if (lastPrefixInR.getSecond() < 0) {
						continue;
					}

					/*
					 * Lemma 2. If ss r and s are similar, we have
					 * 
					 * If last(pre(r)) > last(pre(s)), piv(s) ∩ pre(r) != phi; If last(pre(r)) <= last(pre(s)), piv(r) ∩ pre(s) != phi;
					 */

					if (Math.abs(p - gram.getStart()) > tau

					|| (lastPrefixInS.getSecond() > lastPrefixInR.getSecond() && getIntersection(pivotsInR, prefixesInS).size() == 0)

					|| (lastPrefixInR.getSecond() > lastPrefixInS.getSecond() && getIntersection(pivotsInS, prefixesInR).size() == 0)

					) {
						continue;
					}

					C.add(rid);
				}
			}
		}

		{
			Set<String> ret = new HashSet<String>();
			for (int id : C) {
				String r = ss.get(id).getString();
				ret.add(r);
				// System.out.printf("%d, %s\n", id, r);
			}
		}

		Counter<StringRecord> A = new Counter<StringRecord>();

		SmithWaterman sw = new SmithWaterman();
		AffineGap ag = new AffineGap();

		for (int loc : C) {
			String r = ss.get(loc).getString();
			// if (stringVerifier.verify(s, grams, r)) {

			double ed = sw.getNormalizedScore(s, r);
			// double ed = sw.getBestScore(s, r);
			// double long_len = Math.max(s.length(), r.length());
			// double sim = 1 - (ed / long_len);

			A.incrementCount(ss.get(loc), ed);
			// }
		}

		return A;
	}

	public void setGramSorter(GramSorter gramSorter) {
		this.gramSorter = gramSorter;
	}

	public void setPivotSelector(PivotSelector pivotSelector) {
		this.pivotSelector = pivotSelector;
	}

	public void write(BufferedWriter writer) throws Exception {
		writer.write(String.format("## PPSS\n"));
		writer.write(String.format("## Basic Parameters\t%d\n", 3));
		writer.write(String.format("q\t%d\n", q));
		writer.write(String.format("tau\t%d\n", tau));
		writer.write(String.format("useOptimalPivotSelector\t%s\n", pivotSelector instanceof OptimalPivotSelector ? true : false));
		writer.write("\n");
		writer.write(String.format("## Strings\t%d\n", ss.size()));

		for (int i = 0; i < ss.size(); i++) {
			StringRecord sr = ss.get(i);
			writer.write(String.format("%d\t%d\t%s", i, sr.getId(), sr.getString()));
			if (i != ss.size() - 1) {
				writer.write("\n");
			}
			writer.flush();
		}
		writer.write("\n\n");

		L.write(writer);

		writer.write("\n\n");

		Counter<String> gramWeights = gramSorter.getGramWeights();

		writer.write(String.format("## Gram Weights\t%d\n", gramWeights.size()));

		List<String> grams = gramWeights.getSortedKeys();

		for (int i = 0; i < grams.size(); i++) {
			String g = grams.get(i);
			double weight = gramWeights.getCount(g);
			writer.write(String.format("%s\t%s", g, Double.toString(weight)));
			if (i != grams.size() - 1) {
				writer.write("\n");
			}
			writer.flush();
		}

	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = IOUtils.openBufferedWriter(fileName);
		write(writer);
		writer.close();
	}
}
