package ohs.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohs.entity.data.struct.BilingualText;
import ohs.io.TextFileReader;
import ohs.types.Counter;

public class DataReader {

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

	public static List<BilingualText> readBaseOrgNames(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		List<BilingualText> ret = new ArrayList<BilingualText>();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.replace("\t", "\t_").split("\t");

			for (int i = 0; i < parts.length; i++) {
				if (parts[i].startsWith("_")) {
					parts[i] = parts[i].substring(1);
				}
			}

			for (int i = 0; i < parts.length; i++) {
				if (parts[i].equals("empty")) {
					parts[i] = "";
				}
			}

			String korName = parts[0].trim();
			String engName = parts[1].trim();

			BilingualText orgName = new BilingualText(korName, engName);
			ret.add(orgName);
		}
		reader.close();
		return ret;
	}

}
