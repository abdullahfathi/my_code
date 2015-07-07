package ohs.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohs.entity.data.struct.BilingualText;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.ListMap;
import ohs.types.common.IntPair;

public class OrganizationDetector {

	public static enum UnivComponent {
		UNIVERSITY, COLLEGE, SCHOOL, DIVISION, DEPARTMENT
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		// String s = "44 sheets of a4 4";
		// String r = "\\b4\\b";
		//
		// Pattern p = Pattern.compile(r);
		//
		// Matcher m = p.matcher(s);
		// int start = 0;
		//
		// while (m.find()) {
		// StringBuffer sb = new StringBuffer();
		// sb.append("Matched:\n");
		// for (int i = 0; i <= m.groupCount(); i++) {
		// sb.append(String.format("%d\t%s\n", m.start(), m.group(i)));
		// }
		// System.out.println(sb.toString());
		// }

		OrganizationNormalizer nor = new OrganizationNormalizer(ENTPath.COMMON_DEPT_ABBR_DICT_FILE);

		OrganizationDetector det = new OrganizationDetector();

		String extOrgFileName = ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE;

		det.detect(new BilingualText("경북대 지리학과", "Department of Geography, Kyungpook National University"));
		det.detect(new BilingualText("부산대학교 조선해양공학과 대학원", ""));
		det.detect(new BilingualText("부산대학교 자연과학대학 화학과", ""));
		det.detect(new BilingualText("한양대학교 전자전기제어계측공학과",
				"Department of Electronic, Electrical, Control and Instrumentation Engineering, Hanyang University"));

		det.detect(new BilingualText("한국교원대학교 가정교육과", "Department of Home Economics Education, Korea National University of Education"));
		det.detect(new BilingualText("서울대학교 산림과학부", "Forest Science Department, Seoul National University"));

		// Counter<BilingualText> orgNames = DataReader.readBilingualTextCounter(extOrgFileName);
		//
		// for (BilingualText orgName : orgNames.getSortedKeys()) {
		// orgName = nor.normalize(orgName);
		// double cnt = orgNames.getCount(orgName);
		//
		// // if (cnt < 50 || cnt > 100) {
		// // continue;
		// // }
		//
		// ListMap<UnivComponent, IntPair>[] labelMaps = det.detect(orgName);
		//
		// }

	}

	private TextFileWriter writer = new TextFileWriter(ENTPath.DETECT_LOG_FILE);

	private String regex1 = "(^.+대(?:학교)?\\b)+(?: ?)?(\\b.+대학\\b)?(?: ?)?(\\b.+학부\\b)?(?: ?)?(\\b.+학?과\\b)?(?: ?)?(\\b.+교실\\b)?";

	private String regex2 = "^\\b[^\\s]+연구[소원]\\b";

	private String regex3 = "(\\bDepartment of [^,]+\\b|[^,]+ Department\\b)?(?:, ?)?(\\bDivision of [^,]+\\b)?(?:, ?)?(\\bSchool of [^,]+\\b)?(?:, ?)?(\\bCollege of [^,]+\\b)?(?:, ?)?(\\b[^,]+ University(?: of [^,]+\\b)?)";

	private Pattern p1 = Pattern.compile(regex1);

	private Pattern p2 = Pattern.compile(regex2);

	private Pattern p3 = Pattern.compile(regex3, Pattern.CASE_INSENSITIVE);

	public OrganizationDetector() {

	}

	public ListMap<UnivComponent, IntPair>[] detect(BilingualText orgName) {
		String engName = orgName.getEnglish();
		String korName = orgName.getKorean();

		ListMap<UnivComponent, IntPair>[] ret = new ListMap[2];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = new ListMap<UnivComponent, IntPair>();
		}

		if (korName.length() > 0) {
			ret[0] = detectKorean(korName);
		}

		if (engName.length() > 0) {
			ret[1] = detectEnglish(engName);
		}

		return ret;
	}

	public ListMap<UnivComponent, IntPair> detectEnglish(String s) {
		Matcher m = p3.matcher(s);

		StringBuffer sb = new StringBuffer();
		sb.append(s + "\n");

		ListMap<UnivComponent, IntPair> ret = new ListMap<UnivComponent, IntPair>();

		if (m.find()) {
			sb.append("Matched:\n");
			for (int i = 0; i <= m.groupCount(); i++) {
				sb.append(String.format("%d\t%s\n", i, m.group(i)));

				if (i == 0 || m.group(i) == null) {
					continue;
				}

				int start = m.start(i);
				int end = m.end(i);
				UnivComponent label = UnivComponent.values()[UnivComponent.values().length - i];

				ret.put(label, new IntPair(start, end));
			}

			if (s.length() > 0) {
				List<UnivComponent> labels = new ArrayList<UnivComponent>(ret.keySet());

				for (int i = 0; i < labels.size(); i++) {
					UnivComponent label = labels.get(i);

					sb.append(label);

					List<IntPair> locs = ret.get(label);

					for (int j = 0; j < locs.size(); j++) {
						IntPair loc = locs.get(j);
						sb.append("\t" + s.substring(loc.getFirst(), loc.getSecond()));
					}
					sb.append("\n");
				}

				writer.write(sb.toString() + "\n\n");
			}
		}

		return ret;
	}

	public ListMap<UnivComponent, IntPair> detectKorean(String s) {
		Matcher m = p1.matcher(s);

		StringBuffer sb = new StringBuffer();
		sb.append(s + "\n");

		ListMap<UnivComponent, IntPair> ret = new ListMap<UnivComponent, IntPair>();

		if (m.find()) {
			sb.append("Matched:\n");
			for (int i = 0; i <= m.groupCount(); i++) {
				sb.append(String.format("%d\t%s\n", i, m.group(i)));

				if (i == 0 || m.group(i) == null) {
					continue;
				}

				UnivComponent label = null;

				if (i == 1) {
					label = UnivComponent.values()[0];
				} else if (i == 2) {
					label = UnivComponent.values()[1];
				} else if (i == 3) {
					label = UnivComponent.values()[2];
				} else {
					label = UnivComponent.values()[4];
				}

				if (label == null) {
					continue;
				}

				int start = m.start(i);
				int end = m.end(i);
				ret.put(label, new IntPair(start, end));
			}

			if (s.length() > 0) {
				List<UnivComponent> labels = new ArrayList<UnivComponent>(ret.keySet());

				for (int i = 0; i < labels.size(); i++) {
					UnivComponent label = labels.get(i);

					sb.append(label);

					List<IntPair> locs = ret.get(label);

					for (int j = 0; j < locs.size(); j++) {
						IntPair loc = locs.get(j);
						sb.append("\t" + s.substring(loc.getFirst(), loc.getSecond()));
					}
					sb.append("\n");
				}

				writer.write(sb.toString() + "\n\n");
			}
		}

		return ret;
	}

}
