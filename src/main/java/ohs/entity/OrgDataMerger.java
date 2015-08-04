package ohs.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cc.mallet.util.IoUtils;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.IO;

import ohs.entity.data.struct.Organization;
import ohs.io.IOUtils;
import ohs.types.ListMap;
import ohs.utils.StrUtils;

public class OrgDataMerger {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		OrgDataMerger m = new OrgDataMerger();
		m.removeDuplications();

		System.out.println("process ends.");
	}

	public void removeDuplications() throws Exception {
		List<String> lines = IOUtils.readLines(ENTPath.BASE_ORG_NAME_FILE, IOUtils.EUC_KR);

		Iterator<String> iter = lines.iterator();
		iter.hasNext();
		iter.next();
		iter.remove();

		List<Organization> orgs = DataReader.readOrganizations(ENTPath.BASE_ORG_NAME_FILE);

		int size1 = lines.size();
		int size2 = orgs.size();

		Map<String, String> map1 = new TreeMap<String, String>();
		Map<String, Organization> map2 = new TreeMap<String, Organization>();

		for (int i = 0; i < orgs.size(); i++) {
			Organization current = orgs.get(i);
			String line = lines.get(i);
			String korName = current.getName().getKorean();

			Organization prev = map2.get(korName);

			if (prev == null) {
				map2.put(korName, current);
			} else {
				if (current.getHomepage().length() > 0) {
					System.out.println(prev);
					System.out.println(current);
					System.out.println();
					map2.put(korName, current);
					map1.put(korName, line);
				}
			}
		}

		IOUtils.write(ENTPath.DATA_DIR + "temp.txt", StrUtils.join("\n", lines));

		// List<String> korNames = new ArrayList<String>(map1.keySet());

		// for (int i = 0; i < korNames.size(); i++) {
		// String korName = korNames.get(i);
		// List<Organization> list = map1.get(korName);
		//
		// if (list.size() == 1) {
		// continue;
		// }
		//
		// for (int j = 0; j < list.size(); j++) {
		// System.out.println(list.get(j));
		// }
		//
		// System.out.println();
		//
		// }
	}
}
