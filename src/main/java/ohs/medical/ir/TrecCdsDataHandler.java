package ohs.medical.ir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.utils.StrUtils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TrecCdsDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		TrecCdsDataHandler dh = new TrecCdsDataHandler();
		dh.makeRawTextDump();
		dh.makeTextDump();
		System.out.println("process ends.");
	}

	public void makeRawTextDump() throws Exception {
		String[] fileNames = { "pmc-text-00.tar.gz", "pmc-text-01.tar.gz", "pmc-text-02.tar.gz", "pmc-text-03.tar.gz" };
		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_COLLECTION_FILE);
		int num_files = 0;

		for (int i = 0; i < fileNames.length; i++) {
			String tarFileName = MIRPath.TREC_CDS_COLLECTION_DIR + fileNames[i];
			File tarFile = new File(tarFileName);
			TarArchiveInputStream is = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tarFile)));
			TarArchiveEntry entry = null;
			// read every single entry in TAR file
			while ((entry = is.getNextTarEntry()) != null) {
				// the following two lines remove the .tar.gz extension for the folder name
				// System.out.println(entry.getName());

				if (entry.isFile()) {
					num_files++;

					if (num_files > 1000) {
						break;
					}

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
						String outoput = fileName + "\t" + sb.toString().trim().replace("\t", " ").replace("\n", "<NL>");
						writer.write(outoput + "\n");
					}
				}
			}
			is.close();
		}
		writer.close();
		System.out.println(num_files);

	}

	public static Set<String> readValidDocIDs() {
		Set<String> ret = new TreeSet<String>();
		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_VALID_DOC_ID_FILE);
		while (reader.hasNext()) {
			String line = reader.next();
			ret.add(line.trim());
		}
		reader.close();
		return ret;
	}

	public void makeTextDump() throws Exception {
		System.out.println("make text dump from TREC CDS.");

		Set<String> validDocIds = readValidDocIDs();

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

		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_COLLECTION_FILE);
		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_COLLECTION_2_FILE);

		reader.setPrintNexts(false);

		int numValid = 0;
		int num_miss_pmcid = 0;
		int nuim_miss_title = 0;
		int num_miss_abstracts = 0;
		int num_miss_bodies = 0;
		int numDocs = 0;

		while (reader.hasNext()) {
			reader.print(10000);
			String line = reader.next();
			String[] parts = line.split("\t");
			String fileName = parts[0];
			String xmlText = parts[1];

			fileName = fileName.split("/")[2];

			String docId = IOUtils.removeExtension(fileName);

			if (!validDocIds.contains(docId)) {
				continue;
			}

			// xmlText = xmlText.replace("archivearticle.dtd",
			// "F:/data/trec/cds/JATS-archivearticle1.dtd");

			Document xmlDoc = null;

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

			for (int k = 0; k < nodeList.getLength(); k++) {
				Element idElem = (Element) nodeList.item(k);
				if (idElem.getAttribute("pub-id-type").equals("pmc")) {
					pmcId = idElem.getTextContent().trim();
					break;
				}
			}

			if (pmcId.length() == 0 || !validDocIds.contains(pmcId)) {
				num_miss_pmcid++;
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
				for (int k = 0; k < nodeList.getLength(); k++) {
					Element paraElem = (Element) nodeList.item(k);
					String text = paraElem.getTextContent().trim();
					text = text.replaceAll("[\\s]+", " ").trim();
					sb.append(text + "\n");
				}
				body = sb.toString().trim().replace("\n", "<NL>");
			}

			boolean missPmcId = false;
			boolean missTitle = false;
			boolean missAbs = false;
			boolean missBody = false;

			if (pmcId.length() == 0) {
				num_miss_pmcid++;
				missPmcId = true;
				pmcId = "empty";
			}

			if (title.length() == 0) {
				nuim_miss_title++;
				missTitle = true;
				title = "empty";
			}

			if (abs.length() == 0) {
				num_miss_abstracts++;
				missAbs = true;

				abs = "empty";
			}

			if (body.length() == 0) {
				num_miss_bodies++;
				missBody = true;

				body = "empty";
			}

			int num_miss = num_miss_pmcid + nuim_miss_title + num_miss_abstracts + num_miss_bodies;

			if (pmcId.length() > 0) {
				String output = StrUtils.join("\t", new String[] { pmcId, title, abs, body });
				writer.write(output + "\n");
				numValid++;
			}
		}
		reader.printLast();
		writer.close();

	}
}
