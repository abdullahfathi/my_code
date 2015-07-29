package ohs.medical.ir.dump;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.medical.ir.MIRPath;
import ohs.utils.StrUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;

public class ClefEHealthDataProcessor {

	public static Set<String> getStopFileExtensions() {
		Set<String> ret = new HashSet<String>();
		ret.add("doc");
		ret.add("docx");
		ret.add("pdf");
		ret.add("swf");
		ret.add("ppt");
		ret.add("pptx");
		ret.add("png");
		ret.add("flv");
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ClefEHealthDataProcessor dh = new ClefEHealthDataProcessor();
		dh.makeTextDump();

		System.out.println("process ends.");
	}

	public void makeTextDump() {
		System.out.println("make text dump from CLEF eHealth.");

		File logFile = new File(MIRPath.CLEF_EHEALTH_DIR + "doc_ids.txt");

		Set<String> docIdSet = new HashSet<String>();

		if (logFile.exists()) {
			TextFileReader reader = new TextFileReader(logFile.getPath());
			while (reader.hasNext()) {
				docIdSet.add(reader.next());
			}
			reader.close();
		}

		TextFileWriter writer = new TextFileWriter(MIRPath.CLEF_EHEALTH_COLLECTION_FILE, IOUtils.UTF_8, true);

		int numDocsInCollection = 0;

		Set<String> stopExpSet = getStopFileExtensions();

		File[] files = new File(MIRPath.CLEF_EHEALTH_COLLECTION_DIR).listFiles();

		for (int i = 0, numFiles = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isDirectory()) {
				continue;
			}

			List<File> inputFiles = IOUtils.getFilesUnder(file);

			for (int j = 0; j < inputFiles.size(); j++) {
				File inputFile = inputFiles.get(j);
				numFiles++;

				TextFileReader reader = new TextFileReader(inputFile.getPath());
				List<String> lines = new ArrayList<String>();
				int numDocsInFile = 0;

				while (reader.hasNext()) {
					String line = reader.next().trim();

					if (line.equals("")) {
						continue;
					}

					lines.add(line);

					if (line.startsWith("#EOR")) {
						String uid = lines.get(0);
						String date = lines.get(1);
						String url = lines.get(2);
						String html = StrUtils.join("\n", lines, 4, lines.size() - 1);

						if (!uid.startsWith("#UID") || !date.startsWith("#DATE")

						|| !url.startsWith("#URL") || !lines.get(3).startsWith("#CONTENT")) {
							lines = new ArrayList<String>();
							continue;
						}

						uid = uid.substring(5);
						date = date.substring(6);
						url = url.substring(5);

						if (docIdSet.contains(uid)) {
							lines = new ArrayList<String>();
							continue;
						}

						Pattern p = Pattern.compile("\\.([a-z]+)$");
						Matcher m = p.matcher(url);

						if (m.find()) {
							String exp = m.group(1).toLowerCase();

							if (stopExpSet.contains(exp)) {
								lines = new ArrayList<String>();
								continue;
							}
						}

						Document doc = Jsoup.parse(html);
						String content = doc.text();
						// content = tokenize(content);
						String output = String.format("%s\t%s\t%s\t%s", uid, date, url, content.replaceAll("\n", "<NL>"));
						writer.write(output + "\n");

						lines = new ArrayList<String>();
						numDocsInFile++;
					}
				}
				reader.close();
				numDocsInCollection += numDocsInFile;

				System.out.printf("%d: [%d] documents in [%s]\n", numFiles, numDocsInFile, inputFile.getName());
			}
		}

		writer.close();

		System.out.printf("Total documents: %d\n", numDocsInCollection);
	}

}
