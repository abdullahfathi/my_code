package ohs.medical.ir;

import java.io.File;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.lucene.common.IndexFieldName;
import ohs.types.Counter;
import ohs.types.common.StrCounter;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.IO;

public class VocabularyConstructor {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] vocFileNames = MIRPath.VocFileNames;

		VocabularyConstructor vc = new VocabularyConstructor();

		for (int i = 0; i < indexDirNames.length - 1; i++) {
			// if (i != 2) {
			// continue;
			// }
			vc.construct(indexDirNames[i], vocFileNames[i]);
		}

		vc.merge(vocFileNames, MIRPath.VOCABULARY_FILE);

		System.out.println("process ends.");
	}

	public void construct(String indexDirName, String vocFileName) throws Exception {
		System.out.printf("construct a vocabulary from [%s]\n", indexDirName);
		IndexSearcher indexSearcher = DocumentSearcher.getIndexSearcher(indexDirName);

		IndexReader indexReader = indexSearcher.getIndexReader();

		Fields fields = MultiFields.getFields(indexReader);
		Terms terms = fields.terms(IndexFieldName.CONTENT);

		StrCounter c = new StrCounter();

		TermsEnum termsEnum = terms.iterator(null);
		BytesRef bytesRef = null;

		while ((bytesRef = termsEnum.next()) != null) {
			String word = bytesRef.utf8ToString();
			int docFreq = termsEnum.docFreq();
			double cnt = termsEnum.totalTermFreq();
			c.incrementCount(word, cnt);
		}

		TextFileWriter writer = new TextFileWriter(vocFileName);
		for (String word : c.getSortedKeys()) {
			double cnt = c.getCount(word);
			writer.write(word + "\t" + (int) cnt + "\n");
		}
		writer.close();
	}

	public void merge(String[] inputFileNames, String outputFileName) throws Exception {
		StrCounter counter = new StrCounter();
		for (int i = 0; i < inputFileNames.length; i++) {
			Counter<String> c = IOUtils.readCounter(inputFileNames[i]);
			counter.incrementAll(c);
		}
		IOUtils.write(outputFileName, counter);

	}
}
