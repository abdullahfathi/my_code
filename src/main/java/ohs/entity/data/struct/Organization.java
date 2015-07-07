package ohs.entity.data.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Organization implements Serializable {

	private String sid;

	private BilingualText name;

	private Set<String> korVariants;

	private Set<String> engVariants;

	private List<Organization> history;

	private int id;

	public Organization(int id, String sid, BilingualText name) {
		this(id, sid, name, null, null, null);
	}

	public Organization(int id, String sid, BilingualText name, Set<String> korVariants, Set<String> engVariants, List<Organization> history) {
		super();
		this.id = id;
		this.sid = sid;
		this.name = name;
		this.korVariants = korVariants;
		this.engVariants = engVariants;
		this.history = history;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Organization other = (Organization) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sid == null) {
			if (other.sid != null)
				return false;
		} else if (!sid.equals(other.sid))
			return false;
		return true;
	}

	public Set<String> getEnglishVariants() {
		return engVariants;
	}

	public List<Organization> getHistory() {
		return history;
	}

	public int getId() {
		return id;
	}

	public Set<String> getKoreanVariants() {
		return korVariants;
	}

	public BilingualText getName() {
		return name;
	}

	public String getStringId() {
		return sid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sid == null) ? 0 : sid.hashCode());
		return result;
	}

	public void setEnglishVariants(Set<String> englishVariants) {
		this.engVariants = englishVariants;
	}

	public void setHistory(List<Organization> history) {
		this.history = history;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setKoreanVariants(Set<String> koreanVariants) {
		this.korVariants = koreanVariants;
	}

	public void setName(BilingualText name) {
		this.name = name;
	}

	public void setStringId(String sid) {
		this.sid = sid;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("ID = %s\n", sid));
		sb.append(String.format("Korean ORG = %s\n", name.getKorean()));
		sb.append(String.format("English ORG = %s\n", name.getEnglish()));
		return sb.toString();
	}

}
