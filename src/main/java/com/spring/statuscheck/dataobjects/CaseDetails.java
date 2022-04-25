package com.spring.statuscheck.dataobjects;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CaseDetails {
	String caseNum;
	String caseType;
	String caseStatus;
	Date date;

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
