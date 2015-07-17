package ohs.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.Organization;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.types.Counter;
import ohs.types.ListMap;
import ohs.types.common.StrCounter;

public class DataReader {

	public static void main(String[] args) {
		System.out.println("process begins.");

		List<Organization> orgs = readOrganizations(ENTPath.BASE_ORG_NAME_FILE);
		readOrganizationHistories(ENTPath.BASE_ORG_HISTORY_FILE);

		System.out.println("process ends.");
	}

	public static List<BilingualText> readBaseOrgNames(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		List<BilingualText> ret = new ArrayList<BilingualText>();
		List<Organization> orgs = readOrganizations(fileName);
		for (Organization org : orgs) {
			ret.add(org.getName());
		}
		return ret;
	}

	public static Counter<BilingualText> readBilingualTextCounter(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		Counter<BilingualText> ret = new Counter<BilingualText>();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			String korOrg = parts[0].equals("null") ? "" : parts[0];
			String engOrg = parts[1].equals("null") ? "" : parts[1];
			double cnt = Double.parseDouble(parts[2]);

			BilingualText orgName = new BilingualText(korOrg, engOrg);

			ret.incrementCount(orgName, cnt);
		}
		reader.close();
		return ret;
	}

	public static List<Organization> readOrganizations(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		List<Organization> ret = new ArrayList<Organization>();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}

			String line = reader.next();
			String[] parts = line.replace("\t", "\t_").split("\t");

			for (int i = 0; i < parts.length; i++) {
				if (parts[i].startsWith("_")) {
					parts[i] = parts[i].substring(1);
				}

				if (parts[i].equals("empty")) {
					parts[i] = "";
				}
			}

			int id = Integer.parseInt(parts[0]);
			String country = parts[1];
			String type = parts[2];

			String korName = parts[3].trim();
			String engName = parts[4].trim();
			BilingualText orgName = new BilingualText(korName, engName);

			String korAbbr = parts[5];
			String engAbbr = parts[6];

			String homepage = parts[9];

			Organization org = new Organization(id, null, orgName);
			org.getKoreanVariants().add(korAbbr);
			org.getEnglishVariants().add(engAbbr);
			org.setHomepage(homepage);
			ret.add(org);
		}
		reader.close();
		return ret;
	}

	public static List<Organization> readOrganizationHistories(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		List<Organization> ret = new ArrayList<Organization>();

		// List<Organization> lines = new ArrayList<Organization>();

		List<String[]> lines = new ArrayList<String[]>();

		TextFileReader reader = new TextFileReader(fileName, IOUtils.EUC_KR);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.replace("\t", "\t_").split("\t");
			// String[] parts = line.split("\t");

			for (int j = 0; j < parts.length; j++) {
				if (parts[j].startsWith("_")) {
					parts[j] = parts[j].substring(1);
				}

				if (parts[j].equals("empty")) {
					parts[j] = "";
				}
			}

			// try {
			if (parts[3].length() == 0) {
				// try {
				if (lines.size() > 3) {
					parse(lines);
				}
				// } catch (Exception e) {
				// e.printStackTrace();
				// }

				lines = new ArrayList<String[]>();

			} else {
				lines.add(parts);
			}
			// } catch (Exception e) {
			// System.out.println(StrUtils.join("\t", parts));
			// }

		}
		reader.close();
		return ret;
	}

	public static Organization parse(List<String[]> lines) {

		Map<String, Organization> map = new HashMap<String, Organization>();

		for (int i = 0; i < lines.size(); i++) {
			String[] parts = lines.get(i);

			// try {
			int id = -1;
			String country = parts[1];
			String type = parts[2];

			String korName = parts[3].trim();
			String engName = parts[4].trim();
			BilingualText orgName = new BilingualText(korName, engName);

			String korAbbr = parts[5];
			String engAbbr = parts[6];

			String korHistory = parts[7];
			String engHistory = parts[8];
			String year = parts[9];
			String homepage = parts[10];
			String desc = parts[11];

			Organization org = map.get(korName);

			if (org == null) {
				org = new Organization(id, null, orgName);
				org.getKoreanVariants().add(korAbbr);
				org.getEnglishVariants().add(engAbbr);
				org.setHomepage(homepage);
				map.put(korName, org);
			} else {
				System.out.println();
			}

			if (korHistory.length() == 0) {
				org.setYear(Integer.parseInt(year));
			} else {
				String[] events = korHistory.split(";");
				String[] eventYears = eventYears = year.split(";");

				if (events.length != eventYears.length) {
					System.out.println("errors");
					System.exit(0);
				}

				for (int j = 0; j < events.length; j++) {
					String event = events[j];
					String eventYear = eventYears[j];

					String[] memberNames = event.split("[+-]");

					for (int k = 0; k < memberNames.length; k++) {
						String memberName = memberNames[k];

						if (memberName.length() == 0) {
							continue;
						}

						Organization member = map.get(memberName);

						if (member == null) {
							member = new Organization(-1, null, new BilingualText(memberName, ""));
						} else {
							System.out.println();
						}

						org.getHistory().add(member);
					}

					org.setYear(Integer.parseInt(eventYear));
				}
			}
		}

		List<Organization> orgs = new ArrayList<Organization>();

		{

			List<Organization> temp = new ArrayList<Organization>(map.values());

			Counter<Integer> c = new Counter<Integer>();

			for (int i = 0; i < temp.size(); i++) {
				c.setCount(i, temp.get(i).getYear());
			}

			List<Integer> locs = c.getSortedKeys(true);

			for (int i = 0; i < locs.size(); i++) {
				int loc = locs.get(i);
				orgs.add(temp.get(loc));
			}

		}

		for (int i = 0; i < orgs.size(); i++) {
			System.out.println(orgs.get(i));
			System.out.println();
		}

		System.out.println("------------------------------");

		return null;
	}

}
