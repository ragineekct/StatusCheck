package com.spring.statuscheck.statuscheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.statuscheck.dataobjects.CaseDetails;
import com.spring.statuscheck.dataobjects.StatusResponse;

@RestController
public class StatusCheckController {

	@GetMapping("/statuscheck/{caseNum}")
	public List<StatusResponse> getStatus(@PathVariable String caseNum) throws Exception {
		List<StatusResponse> response = null;
		String caseNumber = caseNum; // TODO get it as a variable

		Map<String, CaseDetails> map = jsonfileToMap();

		Long startNum = Long.parseLong(caseNumber.substring(3, 13));
		String caseCode = caseNumber.substring(0, 3);
		int count = 100;
		while (count > 0) {
			startNum += count;
			String var = caseCode + startNum.toString();

			String url = "https://egov.uscis.gov/casestatus/mycasestatus.do?appReceiptNum=" + var;
			RestTemplate restTemplate = new RestTemplate();
			String result = restTemplate.getForObject(url, String.class);
			count--;
			response = readStatus(result, var, map);
		}

		return response;
	}

	public List<StatusResponse> readStatus(String xml, String caseNumber, Map<String, CaseDetails> map) {
		List<StatusResponse> response = new ArrayList<>();
		CaseDetails caseDetail = new CaseDetails();
		Elements divWithStartingClass = null;
		try {
			String html = xml;
			Document document = Jsoup.parse(html);

			divWithStartingClass = document.select("div[class^=rows text-center]");

			caseDetail.setCaseStatus(divWithStartingClass.select("h1").last().text());

			caseDetail.setCaseNum(caseNumber);

			String caseResponse = divWithStartingClass.select("p").first().ownText();

			setCaseElemets(caseResponse, caseDetail);

			CaseDetails oldCaseDetails = map.get(caseNumber);

			if (!caseDetail.equals(oldCaseDetails)) {
				StatusResponse res = new StatusResponse(oldCaseDetails, caseDetail);
				response.add(res);
				// updateCase
			} else if (caseDetail.getCaseType() != null)
				generateFile(caseDetail);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private void generateFile(CaseDetails caseDetails) throws IOException {
		File output = new File("output.txt");
		FileWriter writer = new FileWriter(output, true);

		writer.write(caseDetails.toString() + "\n");
		writer.flush();
		writer.close();
	}

	public void setCaseElemets(String caseResponse, CaseDetails caseDetail) {
		List<String> l = Arrays.asList(caseResponse.split(","));

		String d = new StringBuilder().append(l.get(0).substring(3)).append(",").append(l.get(1)).toString();

//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
//		LocalDate date = LocalDate.parse(d, formatter);
		Date caseDate = null;
		try {
			caseDate = new SimpleDateFormat("MMMM d, yyyy").parse(d);
		} catch (ParseException e) {

		}

		caseDetail.setDate(caseDate);

		String[] caseTypeArray = l.get(2).split(" ");

		String caseType = caseTypeArray[caseTypeArray.length - 1];

		if (!caseType.equalsIgnoreCase("I-765"))
			caseDetail.setCaseType(null);
		else
			caseDetail.setCaseType(caseType);
	}

	public Map<String, CaseDetails> jsonfileToMap() {

		Map<String, CaseDetails> map = new HashMap<>();
		ObjectMapper om = new ObjectMapper();

		try {
			BufferedReader reader = new BufferedReader(new FileReader("output.txt"));
			String lineRead = reader.readLine();
			while (lineRead != null) {
				CaseDetails caseDetails = om.readValue(lineRead, CaseDetails.class);
				map.put(caseDetails.getCaseNum(), caseDetails);
				lineRead = reader.readLine();
			}
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return map;
	}

}
