package com.spring.statuscheck.dataobjects;

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
public class StatusResponse {
	CaseDetails oldCaseDetails;
	CaseDetails newCaseDetails;

}
