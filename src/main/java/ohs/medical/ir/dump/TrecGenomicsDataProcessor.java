package ohs.medical.ir.dump;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ohs.io.TextFileWriter;
import ohs.medical.ir.MIRPath;

public class TrecGenomicsDataProcessor {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		TrecGenomicsDataProcessor d = new TrecGenomicsDataProcessor();
		d.makeTextDump();
		System.out.println("process ends.");
	}

	public void makeTextDump() throws Exception {
		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_GENOMICS_COLLECTION_FILE);
		int num_files = 0;

		File[] dataFiles = new File(MIRPath.TREC_GENOMICS_COLLECTION_DIR).listFiles();

		for (int i = 0; i < dataFiles.length; i++) {
			File dataFile = dataFiles[i];

			if (dataFile.isFile() && dataFile.getName().endsWith(".zip")) {

			} else {
				continue;
			}

			ZipInputStream is = new ZipInputStream(new FileInputStream(dataFile));
			ZipEntry entry = null;
			// read every single entry in TAR file
			while ((entry = is.getNextEntry()) != null) {
				// the following two lines remove the .tar.gz extension for the folder name
				// System.out.println(entry.getName());

				if (!entry.isDirectory()) {
					num_files++;

					// if (num_files > 1000) {
					// break;
					// }

					if (num_files % 10000 == 0) {
						System.out.println(num_files);
					}

					String fileName = entry.getName();
					StringBuffer sb = new StringBuffer();

					int c;

					while ((c = is.read()) != -1) {
						sb.append((char) c);
					}

					if (sb.length() > 0) {
						// String content = sb.toString().trim();
						// content = content.replace("\r\n", "<NL>");
						Document doc = Jsoup.parse(sb.toString());
						String content = doc.text().trim().replace("\r\n", "<NL>");

						String outoput = fileName + "\t" + content;
						writer.write(outoput + "\n");
					}
				}
			}
			is.close();
		}
		writer.close();
		System.out.println(num_files);
	}
}
