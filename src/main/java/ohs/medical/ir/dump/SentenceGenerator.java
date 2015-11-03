package ohs.medical.ir.dump;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.lucene.common.MyTextField;
import ohs.medical.ir.MIRPath;
import ohs.medical.ir.NLPUtils;

/**
 * Construct an inverted index with source document collection.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class SentenceGenerator {

	public static final int ram_size = 5000;

	private static String getName(String text) {
		String regex = "\\([^\\(\\)]+\\)";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(text);

		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			m.appendReplacement(sb, "");
		}
		m.appendTail(sb);

		text = sb.toString().trim();
		return text;
	}

	public static Set<String> getStopSectionNames() {
		String[] stopSectionNames = { "references", "external links", "see also", "notes", "further reading" };
		Set<String> ret = new HashSet<String>();
		for (String s : stopSectionNames) {
			ret.add(s);
		}
		return ret;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		SentenceGenerator di = new SentenceGenerator();
		// di.generateForTrecCds();
		// di.generateForClefEHealth();
		di.generateForOhsumed();
		// di.generateForTrecGenomics();

		System.out.println("process ends.");
	}

	public SentenceGenerator() {

	}

	public void generateForClefEHealth() throws Exception {
		System.out.println("generate for CLEF eHealth.");

		TextFileWriter writer = new TextFileWriter(MIRPath.CLEF_EHEALTH_SENTS_FILE);

		TextFileReader reader = new TextFileReader(MIRPath.CLEF_EHEALTH_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(100000);

			String line = reader.next();
			String[] parts = line.split("\t");

			String uid = parts[0];
			String date = parts[1];
			String url = parts[2];
			String content = parts[3].replaceAll("<NL>", "\n");

			List<String> sents = new ArrayList<String>();
			sents.addAll(NLPUtils.tokenize(content));

			for (int i = 0; i < sents.size(); i++) {
				String output = String.format("%s\t%d\t%s", uid, i, sents.get(i));
				writer.write(output + "\n");
			}
		}
		reader.printLast();
		reader.close();

		writer.close();
	}

	public void generateForOhsumed() throws Exception {
		System.out.println("generate for OHSUMED.");

		TextFileWriter writer = new TextFileWriter(MIRPath.OHSUMED_SENTS_FILE);

		TextFileReader reader = new TextFileReader(MIRPath.OHSUMED_COL_FILE);
		reader.setPrintNexts(false);

		long num_sents = 0;

		while (reader.hasNext()) {
			reader.print(100000);

			String line = reader.next();
			String[] parts = line.split("\t");

			// if (parts.length != 2) {
			// continue;
			// }

			String seqId = parts[0];
			String medlineId = parts[1];
			String meshTerms = parts[2];
			String title = parts[3];
			String publicationType = parts[4];
			String abs = parts[5].replace("<NL>", "\n");
			String authors = parts[6];
			String source = parts[7];

			if (abs.equals("null")) {
				continue;
			}

			List<String> sents = NLPUtils.tokenize(abs);

			for (int i = 0; i < sents.size(); i++) {
				String output = String.format("%s\t%d\t%s", medlineId, i, sents.get(i));
				writer.write(output + "\n");
			}

			num_sents += sents.size();
		}
		reader.printLast();
		reader.close();

		writer.close();
	}

	public void generateForTrecCds() throws Exception {
		System.out.println("generate for TREC CDS.");

		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_SENTS_FILE);

		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(100000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 4) {
				continue;
			}

			for (int i = 0; i < parts.length; i++) {
				String s = parts[i];
				if (s.equals("null")) {
					parts[i] = "";
				}
			}

			String pmcId = parts[0];
			String title = parts[1];
			String abs = parts[2];
			String content = parts[3];

			List<String> sents = new ArrayList<String>();
			sents.addAll(NLPUtils.tokenize(abs.replace("<NL>", "\n")));
			sents.addAll(NLPUtils.tokenize(content.replace("<NL>", "\n")));

			for (int i = 0; i < sents.size(); i++) {
				String output = String.format("%s\t%d\t%s", pmcId, i, sents.get(i));
				writer.write(output + "\n");
			}
		}
		reader.printLast();
		writer.close();
	}

	public void generateForTrecGenomics() throws Exception {
		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_GENOMICS_SENTS_FILE);

		TextFileReader reader = new TextFileReader(MIRPath.TREC_GENOMICS_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(5000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String id = parts[0];
			String content = parts[1];

			int start = id.lastIndexOf("/");
			int end = id.lastIndexOf(".");
			id = id.substring(start + 1, end);

			List<String> sents = new ArrayList<String>();
			sents.addAll(NLPUtils.tokenize(content));

			for (int i = 0; i < sents.size(); i++) {
				String output = String.format("%s\t%d\t%s", id, i, sents.get(i));
				writer.write(output + "\n");
			}
		}
		reader.close();
		writer.close();
	}

}
