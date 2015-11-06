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

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// String[] sentFileNames = MIRPath.SentFileNames;
		// String[] sentIndexDirNames = MIRPath.SentIndexDirNames;

		// for (int i = 0; i < sentIndexDirNames.length; i++) {
		// new SentenceIndexer(sentFileNames[i], sentIndexDirNames[i]).index();
		// }

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

		IndexWriter iw = DocumentIndexer.getIndexWriter(indexDirName);

		TextFileReader reader = new TextFileReader(sentFileName);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(100000);

			String line = reader.next();
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
		reader.printLast();
		reader.close();

		iw.close();
	}

}
