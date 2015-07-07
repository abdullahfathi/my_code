package ohs.medical.ir;

import java.io.StringReader;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;

public class NLPUtils {

	public static TokenizerFactory<? extends HasWord> tf = PTBTokenizer.factory(new CoreLabelTokenFactory(),
			"ptb3Escaping=false,normalizeParentheses=false,normalizeOtherBrackets=false");

	public static String tokenize(String text) {

		DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(new StringReader(text));
		documentPreprocessor.setTokenizerFactory(tf);

		StringBuffer sb = new StringBuffer();
		for (List<HasWord> item : documentPreprocessor) {

			for (int i = 0; i < item.size(); i++) {
				sb.append(item.get(i).word());
				if (i != item.size() - 1) {
					sb.append(" ");
				}
			}
			sb.append("\n");
		}
		return sb.toString().trim();
	}
}
