package ohs.medical.ir;

import java.io.File;
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
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.parser.Content;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.lucene.common.IndexFieldName;
import ohs.lucene.common.MedicalEnglishAnalyzer;
import ohs.lucene.common.MyTextField;
import ohs.utils.StrUtils;

/**
 * Construct an inverted index with source document collection.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class SentenceIndexer {

	public static final int ram_size = 5000;

	public static IndexWriter getIndexWriter(String outputDirName) throws Exception {
		IOUtils.deleteFilesUnder(outputDirName);

		IndexWriterConfig iwc = new IndexWriterConfig(MedicalEnglishAnalyzer.getAnalyzer());
		// IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(ram_size);

		IndexWriter ret = new IndexWriter(FSDirectory.open(Paths.get(outputDirName)), iwc);
		return ret;
	}

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
		SentenceIndexer di = new SentenceIndexer();
		// di.indexTrecCds();
		// di.indexClefEHealth();
		di.indexOhsumed();
		// di.indexTrecGenomics();

		System.out.println("process ends.");
	}

	public SentenceIndexer() {

	}

	public void indexClefEHealth() throws Exception {
		System.out.println("index CLEF eHealth.");
		IndexWriter indexWriter = getIndexWriter(MIRPath.CLEF_EHEALTH_INDEX_DIR);

		TextFileReader reader = new TextFileReader(MIRPath.CLEF_EHEALTH_COLLECTION_FILE);
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
				Document doc = new Document();
				doc.add(new StringField(IndexFieldName.DOCUMENT_ID, uid, Field.Store.YES));
				doc.add(new StringField(IndexFieldName.SENTENCE_ID, i + "", Field.Store.YES));
				doc.add(new MyTextField(IndexFieldName.CONTENT, sents.get(i), Store.YES));
				indexWriter.addDocument(doc);
			}
		}
		reader.printLast();
		reader.close();

		indexWriter.close();
	}

	public void indexOhsumed() throws Exception {
		System.out.println("index OHSUMED.");

		IndexWriter writer = getIndexWriter(MIRPath.OHSUMED_INDEX_SENT_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.OHSUMED_COLLECTION_FILE);
		reader.setPrintNexts(false);

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

			List<String> sents = NLPUtils.tokenize(abs);

			for (int i = 0; i < sents.size(); i++) {
				Document doc = new Document();
				doc.add(new StringField(IndexFieldName.DOCUMENT_ID, medlineId, Field.Store.YES));
				doc.add(new StringField(IndexFieldName.SENTENCE_ID, i + "", Field.Store.YES));
				doc.add(new MyTextField(IndexFieldName.CONTENT, sents.get(i), Field.Store.YES));
				writer.addDocument(doc);
			}
		}
		reader.printLast();
		writer.close();
	}

	public void indexTrecCds() throws Exception {
		System.out.println("index TREC CDS.");

		IndexWriter writer = getIndexWriter(MIRPath.TREC_CDS_INDEX_SENT_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_COLLECTION_2_FILE);
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
				if (s.equals("empty")) {
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
				Document doc = new Document();
				doc.add(new StringField(IndexFieldName.DOCUMENT_ID, pmcId, Field.Store.YES));
				doc.add(new StringField(IndexFieldName.SENTENCE_ID, i + "", Field.Store.YES));
				doc.add(new MyTextField(IndexFieldName.CONTENT, sents.get(i), Store.YES));
				writer.addDocument(doc);
			}
		}
		reader.printLast();
		writer.close();
	}

	public void indexTrecGenomics() throws Exception {
		IndexWriter indexWriter = getIndexWriter(MIRPath.TREC_GENOMICS_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.TREC_GENOMICS_COLLECTION_FILE);
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
				Document doc = new Document();
				doc.add(new StringField(IndexFieldName.DOCUMENT_ID, id, Field.Store.YES));
				doc.add(new StringField(IndexFieldName.SENTENCE_ID, i + "", Field.Store.YES));
				doc.add(new MyTextField(IndexFieldName.CONTENT, sents.get(i), Store.YES));
				indexWriter.addDocument(doc);
			}
		}
		reader.close();
		indexWriter.close();
	}

}