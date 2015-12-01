package ohs.news_ir;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		index();
		System.out.println("process ends.");
	}

	public static void index() throws Exception {
		TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_JSON_FILE);
		TextFileWriter writer = new TextFileWriter(NSPath.NEWS_COL_TEXT_FILE);
		List<String> labels = new ArrayList<String>();

		while (reader.hasNext()) {
			String line = reader.next();

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(line);

			if (reader.getNumLines() == 1) {
				for (Object key : jsonObject.keySet()) {
					labels.add(key.toString());
				}

				writer.write(StrUtils.join("\t", labels) + "\n");
			}

			List<String> values = new ArrayList<>();

			for (String key : labels) {
				String value = jsonObject.get(key).toString();
				value = value.replace("\n", "<NL>");
				value = value.replace("\\n", "<NL>");
				if (key.equals("content")) {
					System.out.println("=> " + value);
				}
				value = value.replace("\r ", " ");
				value = value.replace("\t", "<TAB>");
				values.add(value);
			}

			if (values.size() == labels.size()) {
				writer.write(StrUtils.join("\t", values) + "\n");
			} else {
				System.out.println(StrUtils.join("\t", values));
			}

		}
		reader.close();
		writer.close();

	}

}
