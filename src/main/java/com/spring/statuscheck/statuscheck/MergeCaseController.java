package com.spring.statuscheck.statuscheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.statuscheck.dataobjects.CaseData;
import com.spring.statuscheck.dataobjects.CaseUpdate;
import com.spring.statuscheck.util.StatusCheckUtil;

@RestController
public class MergeCaseController {

	String invalidStatus = "Rejected";

	@PutMapping("/mergeCase/{caseType}")
	public void updateCases(@PathVariable String caseType) throws Exception {
		File output = new File("EAC-I765-update.txt");
		FileWriter writer = new FileWriter(output);
		Map<String, CaseData> masterFile = jsonfileToMap("EAC-I765");
		// Map<String, CaseData> UpdateFromFile = jsonfileToMap("EAC2290008030");
		// masterFile.entrySet().stream()
		// .collect(Collectors.toMap(Map.Entry::getKey, entry ->
		// UpdateFromFile.containsKey(entry)));
		// masterFile.keySet().removeAll(UpdateFromFile.keySet());
		// masterFile.putAll(UpdateFromFile);

		List<CaseData> finalList = updateFile(masterFile);

		StatusCheckUtil.writeToFile(writer, finalList);
	}

	private List<CaseData> updateFile(Map<String, CaseData> masterFile) {
		List<CaseData> finalList = new ArrayList<CaseData>();

		Iterator<Map.Entry<String, CaseData>> iterator = masterFile.entrySet().iterator();
		// List<String> removeFromMaster = Arrays.asList(invalidStatus);
		while (iterator.hasNext()) {

			Map.Entry<String, CaseData> entry = iterator.next();
			List<CaseUpdate> list = entry.getValue().getCaseUpdates();
			for (CaseUpdate c : list) {
				if (c.getCaseStatus().contains(invalidStatus)) {
					iterator.remove();
					break;
				}
			}
			finalList.add(entry.getValue());
		}
		Collections.sort(finalList);
		return finalList;
	}

	public Map<String, CaseData> jsonfileToMap(String fileName) {

		Map<String, CaseData> map = new HashMap<>();
		fileName += ".txt";
		ObjectMapper om = new ObjectMapper();
		om.setTimeZone(TimeZone.getDefault());
		if (new File(fileName).exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String lineRead = reader.readLine();

				while (lineRead != null) {

					CaseData caseDetails = om.readValue(lineRead, CaseData.class);

					map.put(caseDetails.getCaseNum(), caseDetails);
					lineRead = reader.readLine();
				}
				reader.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return map;
	}
}
