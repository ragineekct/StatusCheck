package com.spring.statuscheck.dataobjects;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CaseData extends NewCaseDetails implements Comparable<NewCaseDetails> {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date lastUpdateDate;

	@Override
	public int compareTo(NewCaseDetails next) {
		return this.getCaseNum().compareTo(next.getCaseNum());
	}
}
