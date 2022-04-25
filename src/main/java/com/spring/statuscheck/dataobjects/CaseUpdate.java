package com.spring.statuscheck.dataobjects;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

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
public class CaseUpdate implements Comparable<CaseUpdate> {
	String caseStatus;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date date;

	@Override
	public int compareTo(CaseUpdate o) {

		return this.getDate().compareTo(o.getDate());
	}
}
