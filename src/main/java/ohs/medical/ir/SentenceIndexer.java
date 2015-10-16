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

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		new SentenceIndexer(MIRPath.TREC_CDS_SENTS_FILE, MIRPath.TREC_CDS_INDEX_SENT_DIR).index();
		new SentenceIndexer(MIRPath.CLEF_EHEALTH_SENTS_FILE, MIRPath.CLEF_EHEALTH_INDEX_SENT_DIR).index();
		new SentenceIndexer(MIRPath.OHSUMED_SENTS_FILE, MIRPath.OHSUMED_INDEX_SENT_DIR).index();
		new SentenceIndexer(MIRPath.TREC_GENOMICS_SENTS_FILE, MIRPath.TREC_GENOMICS_INDEX_SENT_DIR).index();

		System.out.println("process ends.");
	}

	private String sentFileName;

	private String indexDirName;

	public SentenceIndexer(String sentFileName, String indexDirName) {
		this.sentFileName = sentFileName;
		this.indexDirName = indexDirName;
	}

	public void index() throws Exception {
		System.out.printf("index sentences in [%s] to [%s]\n", sentFileName, indexDirName);

		IndexWriter iw = getIndexWriter(indexDirName);

		TextFileReader tfr = new TextFileReader(sentFileName);
		tfr.setPrintNexts(false);

		while (tfr.hasNext()) {
			tfr.print(100000);

			String line = tfr.next();
			String[] parts = line.split("\t");

			String docId = parts[0];
			String no = parts[1];
			String sent = parts[2];

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.DOCUMENT_ID, docId, Field.Store.YES));
			doc.add(new StringField(IndexFieldName.SENTENCE_ID, no, Field.Store.YES));
			doc.add(new MyTextField(IndexFieldName.CONTENT, sent, Store.YES));

			iw.addDocument(doc);
		}
		tfr.printLast();
		tfr.close();

		iw.close();
	}

}
