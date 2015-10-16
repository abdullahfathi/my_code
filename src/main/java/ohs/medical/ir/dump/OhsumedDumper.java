package ohs.medical.ir.dump;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.medical.ir.MIRPath;
import ohs.utils.StopWatch;

public class OhsumedDumper {

	public static final String[] TAGS = { ".I", ".U", ".M", ".T", ".P", ".W", ".A", ".S" };

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		OhsumedDumper dh = new OhsumedDumper();
		// dh.makeTextDump();
		System.out.println("process ends.");
	}

	private String makeOutput(Map<String, String> map) {
		String seqId = map.get(".I");
		String medlineId = map.get(".U");
		String meshTerms = map.get(".M");
		String title = map.get(".T");
		String publicationType = map.get(".P");
		String abs = map.get(".W");
		String authors = map.get(".A");
		String source = map.get(".S");

		String output = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", seqId, medlineId, meshTerms, title, publicationType, abs, authors,
				source);

		return output;
	}

	public void makeTextDump() throws Exception {
		System.out.println("make text dump from OHSUMED.");

		File inputDir = new File(MIRPath.OHSUMED_COLLECTION_DIR);
		File outputFile = new File(MIRPath.OHSUMED_COLLECTION_FILE);
		System.out.println(outputFile.getCanonicalPath());

		TextFileWriter writer = new TextFileWriter(outputFile);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		File[] files = inputDir.listFiles();

		/*
		 * .I sequential identifier
		 * 
		 * .U MEDLINE identifier (UI)
		 * 
		 * .M Human-assigned MeSH terms (MH)
		 * 
		 * .T Title (TI)
		 * 
		 * .P Publication type (PT)
		 * 
		 * .W Abstract (AB)
		 * 
		 * .A Author (AU)
		 * 
		 * .S Source (SO)
		 */

		int numDocs = 0;

		for (int i = 0; i < files.length; i++) {
			Map<String, String> map = new HashMap<String, String>();
			TextFileReader reader = new TextFileReader(files[i].getPath());

			while (reader.hasNext()) {
				String line = reader.next();

				if (line.startsWith(".I")) {
					String[] parts = line.split(" ");
					String key = parts[0];
					String value = parts[1];

					if (map.size() == 0) {
						map.put(key, value);
					} else {
						writer.write(makeOutput(map) + "\n");
						map = new HashMap<String, String>();
						map.put(key, value);

						numDocs++;
					}
				} else {
					reader.hasNext();
					String value = reader.next();
					map.put(line, value);
				}
			}
			reader.close();
			writer.write(makeOutput(map) + "\n");

			numDocs++;
		}
		writer.close();

		System.out.printf("[%d] docs\n", numDocs);
	}
}
