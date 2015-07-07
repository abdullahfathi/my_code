package ohs.entity;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

import com.mysql.jdbc.ResultSetMetaData;

public class DataLoader {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataLoader dl = new DataLoader();
		// dbc.upload();
		// dl.downloadReports();
		dl.downloadPatents();

		System.out.println("process ends.");

	}

	private Connection conn;

	private TextFileWriter logWriter;

	public DataLoader() {

	}

	private void close() throws Exception {
		conn.close();
	}

	private String createInsertSQL(Map<PaperAttr, String> attrValueMap) {
		StringBuffer sb = new StringBuffer();
		sb.append("INSERT INTO PAPER_RAW_META");

		sb.append(" (");

		List<PaperAttr> attrs = new ArrayList<PaperAttr>(attrValueMap.keySet());

		for (int i = 0; i < attrs.size(); i++) {
			PaperAttr attr = attrs.get(i);
			sb.append(attr);

			if (i != attrs.size() - 1) {
				sb.append(", ");
			}
		}

		sb.append(") VALUES (");

		for (int i = 0; i < attrs.size(); i++) {
			PaperAttr attr = attrs.get(i);
			sb.append("?");
			if (i != attrs.size() - 1) {
				sb.append(", ");
			}
		}
		sb.append(") ");
		return sb.toString();
	}

	private void open() throws Exception {
		String text = IOUtils.readText("../../data/entity_iden/db_account.txt");
		String[] parts = text.split("\t");
		String url = String.format("jdbc:mysql://%s??useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true", parts[0]);
		String id = parts[1];
		String password = parts[2];

		conn = DriverManager.getConnection(url, id, password);
	}

	public void downloadPatents() throws Exception {
		System.out.println("download patents.");
		open();

		String sql = "SELECT * FROM PATENT_RAW_META";

		PreparedStatement pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();

		ResultSetMetaData rsmd = (ResultSetMetaData) rs.getMetaData();

		List<String> labels = new ArrayList<String>();

		for (int i = 0; i < rsmd.getColumnCount(); i++) {
			System.out.printf("%d\t%s\n", i, rsmd.getColumnName(i + 1));
			labels.add(rsmd.getColumnName(i + 1));
		}

		TextFileWriter writer = new TextFileWriter(ENTPath.PATENT_TEXT_FILE);
		writer.write(StrUtils.join("\t", labels) + "\n");

		int cnt = 0;
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		while (rs.next()) {
			if (++cnt % 1000 == 0) {
				System.out.printf("\r[%d, %s]", cnt, stopWatch.stop());
			}

			List<String> items = new ArrayList<String>();
			for (int i = 0; i < labels.size(); i++) {
				String label = labels.get(i);
				String value = rs.getString(i + 1);

				if (value == null) {
					value = "";
				}

				if (value.length() == 0) {
					value = "empty";
				}

				items.add(value);
			}

			if (items.size() == labels.size()) {
				String output = StrUtils.join("\t", items);
				writer.write(output + "\n");
			}
		}
		writer.close();
		System.out.printf("\r[%d, %s]\n", cnt, stopWatch.stop());

		pstmt.close();

		close();
	}

	public void downloadReports() throws Exception {
		open();

		String sql = "select * from EDEN_2015.REPORT_RAW_META";

		PreparedStatement pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();

		ResultSetMetaData rsmd = (ResultSetMetaData) rs.getMetaData();

		List<String> labels = new ArrayList<String>();

		for (int i = 0; i < rsmd.getColumnCount(); i++) {
			System.out.printf("%d\t%s\n", i, rsmd.getColumnName(i + 1));
			labels.add(rsmd.getColumnName(i + 1));

			if (rsmd.getColumnName(i + 1).equals("PY")) {
				break;
			}
		}

		TextFileWriter writer = new TextFileWriter(ENTPath.REPORT_TEXT_FILE);
		writer.write(StrUtils.join("\t", labels) + "\n");

		int cnt = 0;
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		while (rs.next()) {
			if (++cnt % 1000 == 0) {
				System.out.printf("\r[%d, %s]", cnt, stopWatch.stop());
			}

			List<String> items = new ArrayList<String>();
			for (int i = 0; i < labels.size(); i++) {
				String label = labels.get(i);
				String value = rs.getString(i + 1);

				if (value == null) {
					value = "";
				}

				if (value.length() == 0) {
					value = "empty";
				}

				items.add(value);
			}

			if (items.size() == labels.size()) {
				String output = StrUtils.join("\t", items);
				writer.write(output + "\n");
			}
		}
		writer.close();
		System.out.printf("\r[%d, %s]\n", cnt, stopWatch.stop());

		pstmt.close();

		close();
	}

	public void uploadPapers() throws Exception {
		open();

		Statement stmt = conn.createStatement();
		String sql = "truncate PAPER_RAW_META";
		stmt.executeUpdate(sql);

		List<File> files = IOUtils.getFilesUnder(new File(ENTPath.DOMESTIC_PAPER_META_DIR));
		logWriter = new TextFileWriter(ENTPath.DOMESTIC_PAPER_DATA_LOADER_LOG_FILE);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < files.size(); i++) {
			// if (i < 4) {
			// continue;
			// }
			File file = files.get(i);

			PaperDataFileIterator iter = new PaperDataFileIterator(file.getPath());
			int num_inserts = 0;

			while (iter.hasNext()) {
				Map<PaperAttr, String> attrValueMap = iter.next();

				sql = createInsertSQL(attrValueMap);

				try {
					PreparedStatement pstmt = conn.prepareStatement(sql);

					List<PaperAttr> attrs = new ArrayList<PaperAttr>(attrValueMap.keySet());

					for (int j = 0; j < attrs.size(); j++) {
						PaperAttr attr = attrs.get(j);
						String value = attrValueMap.get(attr);

						if (attr.equals("NP") || attr.equals("RC")) {
							pstmt.setInt(j + 1, Integer.parseInt(value));
						} else {
							pstmt.setString(j + 1, value);
						}
					}
					pstmt.executeUpdate();
					num_inserts++;
				} catch (Exception e) {
					e.printStackTrace();
					// System.out.println(sql);
					// System.out.println(s);
					logWriter.write(attrValueMap + "\n\n");
				}

				// if (iter.getNumPapers() % 100000 == 0) {
				// System.out.printf("\r[%s, %d/%d, %s]", file.getName(), iter.getNumPapers(), num_insert, stopWatch.stop());
				// }
			}

			System.out.printf("[%s, %d/%d, %s]\n", file.getName(), iter.getNumPapers(), num_inserts, stopWatch.stop());
		}

		logWriter.close();

		stmt.close();
		close();
	}
}
