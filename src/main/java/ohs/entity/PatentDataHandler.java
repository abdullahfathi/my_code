package ohs.entity;

import java.io.File;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.types.BidMap;
import ohs.types.Counter;

public class PatentDataHandler {

	private static boolean isNull(String s) {
		return s.equals("null") ? true : false;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		PatentDataHandler dh = new PatentDataHandler();
		dh.process();

		System.out.println("process ends.");
	}

	public void process() throws Exception {
		File[] dirs = new File(ENTPath.DATA_DIR + "raw-data/patent").listFiles();

		int num_patents = 0;

		Counter<String> c = new Counter<String>();

		for (int i = 0; i < dirs.length; i++) {
			File dir = dirs[i];
			if (!dir.getName().contains("patent")) {
				continue;
			}

			File[] files = dir.listFiles();

			for (int j = 0; j < files.length; j++) {
				File file = files[j];

				System.out.printf("read [%s]\n", file.getPath());

				TextFileReader reader = new TextFileReader(file.getPath(), IOUtils.EUC_KR);

				BidMap<Integer, String> attrMap = new BidMap<Integer, String>();

				while (reader.hasNext()) {
					String line = reader.next();

					if (reader.getNumLines() == 1) {
						String[] attrs = line.split("\t");
						for (int k = 0; k < attrs.length; k++) {
							attrMap.put(k, attrs[k]);
						}
					} else {
						num_patents++;

						BidMap<String, String> map = new BidMap<String, String>();

						String[] values = line.replace("\t", "#\t").split("\t");
						for (int l = 0; l < values.length; l++) {
							String v = values[l];
							v = v.replace("#", "");
							values[l] = v;
							String attr = attrMap.getValue(l);
							map.put(attr, v);
						}

						String pak = map.getValue("PAK");
						String ink = map.getValue("INK");

						String pae = map.getValue("PAE");
						String ine = map.getValue("INE");

						if (pak == null || ink == null || pak.length() == 0 || ink.length() == 0) {
							continue;
						}

						if (pae == null || ine == null || pae.length() == 0 || ine.length() == 0) {
							continue;
						}

						if (pak.equals(ink)) {
							c.incrementCount(pak, 1);
						} else if (pae.equals(ine)) {
							c.incrementCount(pak, 1);
						}
					}
				}
				reader.close();

			}
		}

		System.out.println(c.toString());
		System.out.println(num_patents);
		System.out.println(c.size());

	}
}
