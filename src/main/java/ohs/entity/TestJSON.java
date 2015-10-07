package ohs.entity;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import ohs.io.IOUtils;

public class TestJSON {

	public static void main(String[] args) throws Exception {
		write();
	}

	public static void write() throws Exception {
		String jsonText = IOUtils.readText(ENTPath.DATA_DIR + "doc_output.json", IOUtils.EUC_KR);
		JSONParser jsonParser = new JSONParser();
		JSONObject obj = (JSONObject) jsonParser.parse(jsonText);
		JSONObject obj2 = (JSONObject) obj.get("META_MAIN");
		
		// // JSON데이터를 넣어 JSON Object 로 만들어 준다.
		// JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonInfo);
		//
		// JsonObject obj2 = obj.get("META_MAIN").getAsJsonArray().get(0).getAsJsonObject();
		// JsonObject obj3 = obj2.get("META_RSRCHR_INFO").getAsJsonArray().get(0).getAsJsonObject();
		//
		// JsonObject obj4 = obj.getAsJsonObject("META_MAIN").getAsJsonObject("META_RSRCHR_INFO");
//		System.out.println(obj.get);
		// System.out.println(obj2);
		// System.out.println(obj3);

		// for (Entry<String, JsonElement> e : obj.entrySet()) {
		// System.out.println(e.getKey());
		// System.out.println(e.getValue());
		// }

	}
}
