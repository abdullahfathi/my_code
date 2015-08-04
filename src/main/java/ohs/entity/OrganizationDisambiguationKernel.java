package ohs.entity;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.classifier.centroid.CentroidClassifier;
import ohs.entity.OrganizationDetector.UnivComponent;
import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.Organization;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.sim.search.ppss.Gram;
import ohs.string.sim.search.ppss.GramGenerator;
import ohs.string.sim.search.ppss.GramSorter;
import ohs.string.sim.search.ppss.GramWeighter;
import ohs.string.sim.search.ppss.PivotalPrefixStringSearcher;
import ohs.string.sim.search.ppss.StringRecord;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.DeepMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.common.IntPair;
import ohs.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * 
 * 
 * 
 */
public class OrganizationDisambiguationKernel implements Serializable {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		test();
		System.out.println("process ends.");
	}

	public static void test() {
		String orgFileName = ENTPath.BASE_ORG_NAME_FILE;
		String extOrgFileName = ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE;
		String abbrFileName = ENTPath.COMMON_DEPT_ABBR_DICT_FILE;

		OrganizationDisambiguationKernel odk = new OrganizationDisambiguationKernel();
		odk.readOrganizations(orgFileName);
		odk.createOrganizationNormalizer(abbrFileName);
		odk.createSearchers(null);
		// odk.createClassifiers();
		// odk.write(ENTPath.ODK_FILE);

//		{
//			TextFileReader reader = new TextFileReader(ENTPath.PATENT_ORG_FILE_2);
//			TextFileWriter writer = new TextFileWriter(ENTPath.ODK_OUTPUT_PATENT_FILE);
//
//			while (reader.hasNext()) {
//				String line = reader.next();
//				String[] parts = line.split("\t");
//
//				String korName = null;
//				Counter<String> engNameCounts = new Counter<String>();
//
//				for (int i = 0; i < parts.length; i++) {
//					String[] two = StrUtils.split2Two(":", parts[i]);
//					String name = two[0];
//					double cnt = Double.parseDouble(two[1]);
//					if (i == 0) {
//						korName = name;
//					} else {
//						engNameCounts.incrementCount(name, cnt);
//					}
//				}
//
//				BilingualText orgName = new BilingualText(korName, engNameCounts.argMax());
//
//				Counter<StringRecord> ret = odk.disambiguate(orgName);
//
//				List<StringRecord> keys = ret.getSortedKeys();
//				int num_candidates = 10;
//				StringBuffer sb = new StringBuffer(line);
//				for (int i = 0; i < keys.size() && i < num_candidates; i++) {
//					StringRecord sr = keys.get(i);
//					double score = ret.getCount(sr);
//					sb.append(String.format("\n%d\t%s\t%f", i + 1, sr, score));
//				}
//
//				writer.write(sb.toString() + "\n\n");
//			}
//			writer.close();
//		}

		{
			TextFileWriter writer = new TextFileWriter(ENTPath.ODK_OUTPUT_PAPER_FILE);
			Counter<BilingualText> orgNames = DataReader.readBilingualTextCounter(extOrgFileName);

			for (BilingualText orgName : orgNames.getSortedKeys()) {
				double cnt = orgNames.getCount(orgName);

				if (cnt < 50 || cnt > 100) {
					continue;
				}

				Counter<StringRecord> ret = odk.disambiguate(orgName);

				List<StringRecord> keys = ret.getSortedKeys();
				int num_candidates = 10;
				StringBuffer sb = new StringBuffer(orgName.toString());
				for (int i = 0; i < keys.size() && i < num_candidates; i++) {
					StringRecord sr = keys.get(i);
					double score = ret.getCount(sr);
					sb.append(String.format("\n%d\t%s\t%f", i + 1, sr, score));
				}

				writer.write(sb.toString() + "\n\n");
			}
			writer.close();
		}
	}

	private PivotalPrefixStringSearcher[] searchers = new PivotalPrefixStringSearcher[2];

	private CentroidClassifier[] classifiers = new CentroidClassifier[2];

	private OrganizationNormalizer normalizer;

	private OrganizationDetector detector = new OrganizationDetector();

	private TextFileWriter logWriter = new TextFileWriter(ENTPath.ODK_LOG_FILE);

	private List<Organization> orgs;

	private Map<Integer, Organization> orgMap;

	private Map<Integer, Integer>[] recordToOrgMaps = new Map[2];

	public OrganizationDisambiguationKernel() {

	}

	public void createClassifiers() {
		for (int i = 0; i < searchers.length; i++) {
			PivotalPrefixStringSearcher searcher = searchers[i];

			Indexer<String> featIndexer = new Indexer<String>();
			List<SparseVector> trainData = new ArrayList<SparseVector>();

			{
				List<StringRecord> srs = searcher.getStringRecords();

				for (int j = 0; j < srs.size(); j++) {
					StringRecord sr = srs.get(j);
					Gram[] grams = searcher.getGramGenerator().generate(sr.getString());

					Counter<Integer> c = new Counter<Integer>();
					for (int k = 0; k < grams.length; k++) {
						Gram gram = grams[k];
						int f = featIndexer.getIndex(gram.getString());
						c.incrementCount(f, 1);
					}
					SparseVector v = VectorUtils.toSparseVector(c);
					v.setLabel(sr.getId());
					trainData.add(v);
				}
			}

			CentroidClassifier classifier = CentroidClassifier.train(trainData);
			classifier.setFeatureIndexer(featIndexer);

			// int maxIter = 20;
			// double weight = 0.2;
			// double learningRate = 0.5;
			// double minMargin = 0.9;
			//
			// CentroidsUpdater updater = new CentroidsUpdater(classifiers, trainData, null, true, false, maxIter, weight, learningRate,
			// minMargin);
			//
			// updater.update();

			classifiers[i] = classifier;
		}
	}

	public void createOrganizationNormalizer(String fileName) {
		normalizer = new OrganizationNormalizer(fileName);
	}

	public void createSearchers(String extOrgFileName) {
		Counter<BilingualText> c = null;

		if (extOrgFileName != null) {
			c = DataReader.readBilingualTextCounter(extOrgFileName);
		}

		for (int i = 0, rid = 0; i < searchers.length; i++) {
			List<StringRecord> srs = new ArrayList<StringRecord>();
			Map<Integer, Integer> recordToOrgMap = new HashMap<Integer, Integer>();
			recordToOrgMaps[i] = recordToOrgMap;

			int q = 2;
			int tau = 2;

			for (int j = 0; j < orgs.size(); j++) {
				Organization org = orgs.get(j);

				String name = null;
				Set<String> variants = null;

				if (i == 0) {
					name = org.getName().getKorean();
					name = normalizer.normalizeKorean(name);
					variants = org.getKoreanVariants();
				} else {
					name = org.getName().getEnglish();
					name = normalizer.normalizeEnglish(name);
					name = name.toLowerCase();

					variants = new HashSet<String>();

					for (String v : org.getEnglishVariants()) {
						variants.add(v.toLowerCase());
					}

				}

				if (name.length() == 0 || name.length() < q) {
					continue;
				}

				srs.add(new StringRecord(rid, name));
				recordToOrgMap.put(rid, org.getId());
				rid++;

				for (String variant : variants) {
					if (variant.length() == 0 || variant.length() < q) {
						continue;
					}

					srs.add(new StringRecord(rid, variant));
					recordToOrgMap.put(rid, org.getId());
					rid++;
				}
			}

			GramSorter gramSorter = new GramSorter();

			if (c != null) {
				Counter<BilingualText> extOrgCounts = new Counter<BilingualText>();

				for (BilingualText orgName : c.keySet()) {
					double cnt = c.getCount(orgName);
					orgName = normalizer.normalize(orgName);
					extOrgCounts.setCount(orgName, cnt);
				}

				List<StringRecord> ss = new ArrayList<StringRecord>();

				for (BilingualText orgName : extOrgCounts.keySet()) {
					String name = (i == 0) ? orgName.getKorean() : orgName.getEnglish();
					if (name.length() == 0 || name.length() < q) {
						continue;
					}
					ss.add(new StringRecord(ss.size(), name));
				}

				Counter<String> gramWeights = GramWeighter.compute(new GramGenerator(q), ss);
				gramSorter.setGramWeights(gramWeights);
			}

			PivotalPrefixStringSearcher searcher = new PivotalPrefixStringSearcher(q, tau, true);
			searcher.setGramSorter(gramSorter);
			searcher.index(srs);

			searchers[i] = searcher;

		}
	}

	public Counter<StringRecord> disambiguate(BilingualText orgName) {
		orgName = normalizer.normalize(orgName);
		ListMap<UnivComponent, IntPair>[] labelMaps = detector.detect(orgName);

		String[] names = new String[] { orgName.getKorean(), orgName.getEnglish() };
		Counter<StringRecord>[] searchScoreData = new Counter[2];
		Counter<StringRecord>[] classifierScoreData = new Counter[2];

		for (int i = 0; i < searchScoreData.length; i++) {
			searchScoreData[i] = new Counter<StringRecord>();
			classifierScoreData[i] = new Counter<StringRecord>();
		}

		for (int i = 0; i < names.length; i++) {
			String name = names[i];

			if (name.length() == 0) {
				continue;
			}

			ListMap<UnivComponent, IntPair> labelLocs = labelMaps[i];
			List<IntPair> locs = labelLocs.get(OrganizationDetector.UnivComponent.UNIVERSITY);

			String subName = name;

			if (locs.size() == 1) {
				IntPair loc = locs.get(0);
				subName = subName.substring(loc.getFirst(), loc.getSecond());
			}

			if (i == 1) {
				subName = subName.toLowerCase();
			}

			PivotalPrefixStringSearcher searcher = searchers[i];
			CentroidClassifier classifier = classifiers[i];

			Counter<StringRecord> searchScore = searcher.search(subName);

			{
				Map<Integer, Integer> recordToOrgMap = recordToOrgMaps[i];
				CounterMap<Integer, Integer> cm1 = new CounterMap<Integer, Integer>();
				// CounterMap<String, String> cm2 = new CounterMap<String, String>();

				Counter<StringRecord> tempScores = new Counter<StringRecord>();

				for (StringRecord sr : searchScore.keySet()) {
					double score = searchScore.getCount(sr);
					int rid = sr.getId();
					int orgid = recordToOrgMap.get(rid);
					// Organization org = orgMap.get(orgid);
					cm1.incrementCount(orgid, rid, score);
					// cm2.incrementCount(org.getName().getKorean(), sr.getString(), score);
				}

				for (int orgid : cm1.keySet()) {
					Counter<Integer> c = cm1.getCounter(orgid);
					Organization org = orgMap.get(orgid);
					tempScores.setCount(new StringRecord(orgid, org.getName().getKorean()), c.max());
				}

				searchScore = tempScores;
				// System.out.println(cm);

			}

			Counter<StringRecord> classifierScores = new Counter<StringRecord>(searchScore);

			if (classifier != null) {
				Indexer<String> featIndexer = classifier.getFeatureIndexer();

				Set<Integer> labelSet = new HashSet<Integer>();

				for (StringRecord sr : searchScore.keySet()) {
					labelSet.add(sr.getId());
				}

				Counter<Integer> c = new Counter<Integer>();
				Gram[] grams = searcher.getGramGenerator().generate(name);

				for (int j = 0; j < grams.length; j++) {
					int f = featIndexer.indexOf(grams[j].getString());
					if (f < 0) {
						continue;
					}
					c.incrementCount(f, 1);
				}

				// System.out.println(VectorUtils.toCounter(c, featIndexer));

				SparseVector q = VectorUtils.toSparseVector(c);
				VectorMath.unitVector(q);

				SparseVector output = classifier.score(q, labelSet);
				BidMap<StringRecord, Integer> idMap = searcher.getStringRecordIdMap();

				for (int j = 0; j < output.size(); j++) {
					int orgId = output.indexAtLoc(j);
					StringRecord sr = idMap.getKey(orgId);
					double score = output.valueAtLoc(j);
					classifierScores.setCount(sr, score);
				}
			}

			searchScoreData[i] = searchScore;
			classifierScoreData[i] = classifierScores;
		}

		logWriter.write(orgName.toString() + "\n");

		for (int i = 0; i < searchScoreData.length; i++) {
			logWriter.write(searchScoreData[i].toString());
			if (i != searchScoreData.length - 1) {
				logWriter.write("\n");
			}
		}

		logWriter.write("\n\n");

		Counter<StringRecord> ret = new Counter<StringRecord>();
		Counter<StringRecord> c = new Counter<StringRecord>();

		for (int i = 0; i < searchScoreData.length; i++) {
			Counter<StringRecord> searchScores = searchScoreData[i];
			for (StringRecord sr : searchScores.keySet()) {
				double score = searchScores.getCount(sr);
				ret.incrementCount(sr, score);
				c.incrementCount(sr, 1);
			}
		}

		for (StringRecord sr : ret.keySet()) {
			double score = ret.getCount(sr);
			double cnt = c.getCount(sr);
			score /= cnt;
			ret.setCount(sr, score);
		}

		// logWriter.write(orgScores2.toString() + "\n\n");

		return ret;
	}

	public void readOrganizations(String orgFileName) {
		orgs = DataReader.readOrganizations(orgFileName);
		orgMap = new HashMap<Integer, Organization>();

		for (int i = 0; i < orgs.size(); i++) {
			Organization org = orgs.get(i);
			org.setId(i);
			orgMap.put(org.getId(), org);
		}
	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = IOUtils.openBufferedWriter(fileName);

		for (int i = 0; i < searchers.length; i++) {
			searchers[i].write(writer);
			if (i != searchers.length - 1) {
				writer.write("\n\n");
			}
		}
		writer.close();
	}

}
