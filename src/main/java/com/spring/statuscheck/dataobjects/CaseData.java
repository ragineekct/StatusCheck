package com.spring.statuscheck.dataobjects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CaseData implements Comparable<CaseData> {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date lastUpdateDate;

	@Override
	public int compareTo(CaseData next) {
		return this.getCaseNum().compareTo(next.getCaseNum());
	}

	String caseNum;
	String caseType;
	List<CaseUpdate> caseUpdates;

	public List<CaseUpdate> getCaseUpdates() {
		if (caseUpdates == null)
			return new ArrayList<>();
		return caseUpdates;

	}

	public String objectToJson() {
		String output = null;
		ObjectMapper om = new ObjectMapper();
		try {
			output = om.writeValueAsString(this);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return output;

	}

	@Override
	public String toString() {
		return this.objectToJson();
	}
}
