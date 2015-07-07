package ohs.medical.ir.dump;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.medical.ir.MIRPath;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.StopWatch;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TrecDataProcessor {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		TrecDataProcessor dh = new TrecDataProcessor();
		// dh.makeTextDump();
		System.out.println("process ends.");
	}

	public static Set<String> readDuplications() {
		Set<String> ret = new TreeSet<String>();

		{
			TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_DUPLICATION_FILE_1);
			while (reader.hasNext()) {
				String line = reader.next();
				ret.add(line.trim());
			}
			reader.close();
		}

		{
			TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_DUPLICATION_FILE_2);
			while (reader.hasNext()) {
				String line = reader.next();
				String[] parts = line.split(" ");
				ret.add(parts[1]);
				ret.add(parts[2]);
			}
			reader.close();
		}

		return ret;
	}

	public void makeTextDump() throws Exception {
		Set<String> duplicationSet = readDuplications();

		List<File> docFiles = IOUtils.getFilesUnder(MIRPath.TREC_CDS_COLLECTION_DIR);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);

		DocumentBuilder parser = dbf.newDocumentBuilder();

		parser.setEntityResolver(new EntityResolver() {

			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				if (systemId.contains("")) {
					return new InputSource(new StringReader(""));
				}
				return null;
			}
		});

		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_COLLECTION_FILE);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int numValid = 0;
		int numMissPmcId = 0;
		int numMissTitle = 0;
		int numMissAbs = 0;
		int numMissBody = 0;
		int numDuplications = 0;

		TextFileWriter logWriter = new TextFileWriter(MIRPath.TREC_CDS_COLLECTION_LOG_FILE);

		for (int i = 0; i < docFiles.size(); i++) {
			File docFile = docFiles.get(i);

			String filePath = docFile.getPath();

			int idx = filePath.indexOf("docs");

			filePath = filePath.substring(idx + 5);
			filePath = filePath.replace("\\", "/");

			if (duplicationSet.contains(filePath)) {
				numDuplications++;
				System.out.printf("%d Duplications\t[%s]\n", numDuplications, filePath);
				continue;
			}

			String xmlText = IOUtils.readText(docFile.getPath());
			// xmlText = xmlText.replace("archivearticle.dtd",
			// "F:/data/trec/cds/JATS-archivearticle1.dtd");

			org.w3c.dom.Document xmlDoc = null;

			try {
				xmlDoc = parser.parse(new InputSource(new StringReader(xmlText)));
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			// org.w3c.dom.Document xmlDoc = parser.parse(docFile);

			String pmcId = "";
			String title = "";
			String abs = "";
			String body = "";

			NodeList nodeList = xmlDoc.getElementsByTagName("article-id");

			for (int j = 0; j < nodeList.getLength(); j++) {
				Element idElem = (Element) nodeList.item(j);
				if (idElem.getAttribute("pub-id-type").equals("pmc")) {
					pmcId = idElem.getTextContent().trim();
					break;
				}
			}

			if (pmcId.length() == 0) {
				numMissPmcId++;
				continue;
			}

			Element titleElem = (Element) xmlDoc.getElementsByTagName("article-title").item(0);
			Element absElem = (Element) xmlDoc.getElementsByTagName("abstract").item(0);
			Element bodyElem = (Element) xmlDoc.getElementsByTagName("body").item(0);

			if (titleElem != null) {
				title = titleElem.getTextContent().trim();
			}

			if (absElem != null) {
				abs = absElem.getTextContent().trim();
			}

			if (bodyElem != null) {
				StringBuffer sb = new StringBuffer();
				nodeList = bodyElem.getElementsByTagName("p");
				for (int j = 0; j < nodeList.getLength(); j++) {
					Element paraElem = (Element) nodeList.item(j);
					String text = paraElem.getTextContent().trim();
					text = text.replaceAll("[\\s]+", " ").trim();
					sb.append(text + "\n");
				}
				body = sb.toString().trim();
			}

			boolean missPmcId = false;
			boolean missTitle = false;
			boolean missAbs = false;
			boolean missBody = false;

			if (pmcId.length() == 0) {
				numMissPmcId++;
				missPmcId = true;
			}

			if (title.length() == 0) {
				numMissTitle++;
				missTitle = true;
			}

			if (abs.length() == 0) {
				numMissAbs++;
				missAbs = true;
			}

			if (body.length() == 0) {
				numMissBody++;
				missBody = true;
			}

			int numTotalMiss = numMissPmcId + numMissTitle + numMissAbs + numMissBody;

			StringBuffer sb = new StringBuffer();

			if (!missTitle) {
				sb.append(title + "\n");
			}

			if (!missAbs) {
				sb.append(abs + "\n");
			}

			if (!missBody) {
				sb.append(body);
			}

			String content = sb.toString().trim();

			if (content.length() > 0) {
				content = content.replaceAll("\n", "<NL>");
				String output = pmcId + "\t" + content;
				writer.write(output + "\n");
				numValid++;
			}

			if ((i + 1) % 1000 == 0) {
				System.out.printf("\r[%d/%d/%d, %s][%d + %d + %d + %d = %d]",

				numValid, i + 1, docFiles.size(), stopWatch.stop(), numMissPmcId, numMissTitle, numMissAbs, numMissBody, numTotalMiss);
			}

			if (bodyElem != null && body.length() == 0) {
				// logWriter.write(xmlText + "\n\n");
			}
		}
		writer.close();
		logWriter.close();

		System.out.printf("\r[%d/%d/%d, %s]\n", numValid, docFiles.size(), docFiles.size(), stopWatch.stop());
	}
}
