package com.spring.statuscheck.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.statuscheck.dataobjects.CaseData;

public class StatusCheckUtil {

	public static final String EAC_CODE = "EAC";

	public static Map<String, CaseData> jsonfileToMap(String caseCode) {

		Map<String, CaseData> map = new HashMap<>();
		String fileName = getFileName(caseCode) + ".txt";
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

	public static String getFileName(String caseCode) {
		if (EAC_CODE.equalsIgnoreCase(caseCode))
			return "EAC-I765";

		return null;
	}

	public static void writeToFile(FileWriter writer, List<CaseData> finalList) throws IOException {
		for (CaseData c : finalList) {
			try {

				writer.write(c.toString() + "\n");
				writer.flush();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		writer.close();
	}

}
