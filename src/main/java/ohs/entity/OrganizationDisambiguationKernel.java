package ohs.entity;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohs.classifier.centroid.CentroidClassifier;
import ohs.entity.OrganizationDetector.UnivComponent;
import ohs.entity.data.struct.BilingualText;
import ohs.io.IOUtils;
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
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.common.IntPair;

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

		String orgFileName = ENTPath.BASE_ORG_NAME_FILE;
		String extOrgFileName = ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE;
		String abbrFileName = ENTPath.COMMON_DEPT_ABBR_DICT_FILE;

		OrganizationDisambiguationKernel odk = new OrganizationDisambiguationKernel();
		odk.createOrganizationNormalizer(abbrFileName);
		odk.createSearchers(orgFileName, extOrgFileName);
		// odk.createClassifiers();
		// odk.write(ENTPath.ODK_FILE);

		Counter<BilingualText> orgNames = DataReader.readBilingualTextCounter(extOrgFileName);

		for (BilingualText orgName : orgNames.getSortedKeys()) {
			double cnt = orgNames.getCount(orgName);

			if (cnt < 50 || cnt > 100) {
				continue;
			}

			Counter<StringRecord> ret = odk.disambiguate(orgName);
		}

		System.out.println("process ends.");
	}

	private PivotalPrefixStringSearcher[] searchers = new PivotalPrefixStringSearcher[2];

	private CentroidClassifier[] classifiers = new CentroidClassifier[2];

	private OrganizationNormalizer normalizer;

	private OrganizationDetector detector = new OrganizationDetector();

	private BidMap<BilingualText, Integer> baseOrgMap;

	private TextFileWriter logWriter = new TextFileWriter(ENTPath.ODK_LOG_FILE);

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

	public void createSearchers(String orgFileName, String extOrgFileName) {
		List<BilingualText> orgNames = DataReader.readBaseOrgNames(orgFileName);

		baseOrgMap = new BidMap<BilingualText, Integer>();

		for (int i = 0; i < orgNames.size(); i++) {
			BilingualText orgName = orgNames.get(i);
			orgName = normalizer.normalize(orgName);
			orgNames.set(i, orgName);
			baseOrgMap.put(orgName, i);
		}

		Counter<BilingualText> c = null;

		if (extOrgFileName != null) {
			c = DataReader.readBilingualTextCounter(extOrgFileName);
		}

		for (int i = 0; i < searchers.length; i++) {
			List<StringRecord> srs = new ArrayList<StringRecord>();

			int q = 2;
			int tau = 3;

			for (int j = 0; j < orgNames.size(); j++) {
				BilingualText orgName = orgNames.get(j);
				int id = baseOrgMap.getValue(orgName);

				String name = null;
				if (i == 0) {
					name = orgName.getKorean();
				} else {
					name = orgName.getEnglish();
				}

				if (name.length() == 0 || name.length() < q) {
					continue;
				}

				srs.add(new StringRecord(id, name));
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
		Counter<StringRecord>[] searchSoreData = new Counter[2];
		Counter<StringRecord>[] classifierScoreData = new Counter[2];

		for (int i = 0; i < searchSoreData.length; i++) {
			searchSoreData[i] = new Counter<StringRecord>();
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

			PivotalPrefixStringSearcher searcher = searchers[i];
			CentroidClassifier classifier = classifiers[i];

			Counter<StringRecord> orgScores1 = searcher.search(subName);
			Counter<StringRecord> orgScores2 = new Counter<StringRecord>(orgScores1);

			if (classifier != null) {
				Indexer<String> featIndexer = classifier.getFeatureIndexer();

				Set<Integer> labelSet = new HashSet<Integer>();

				for (StringRecord sr : orgScores1.keySet()) {
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
					orgScores2.setCount(sr, score);
				}
			}

			searchSoreData[i] = orgScores1;
			classifierScoreData[i] = orgScores2;
		}

		logWriter.write(orgName.toString() + "\n");

		for (int i = 0; i < searchSoreData.length; i++) {
			logWriter.write(searchSoreData[i].toString());
			if (i != searchSoreData.length - 1) {
				logWriter.write("\n");
			}
		}

		logWriter.write("\n\n");

		// logWriter.write(orgScores2.toString() + "\n\n");

		Counter<StringRecord> ret = new Counter<StringRecord>();

		return ret;
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
