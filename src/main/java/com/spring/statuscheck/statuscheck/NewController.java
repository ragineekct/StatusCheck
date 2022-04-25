package com.spring.statuscheck.statuscheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.opendevl.JFlat;
import com.spring.statuscheck.dataobjects.CaseUpdate;
import com.spring.statuscheck.dataobjects.NewCaseDetails;

@RestController
public class NewController {

	private String originalCaseNumber = null;

	@GetMapping("/status/{caseNumber}/{caseType}")
	public ResponseEntity<List<NewCaseDetails>> getStatus(@PathVariable String caseNumber,
			@PathVariable String caseType) throws Exception {
		originalCaseNumber = caseNumber;
		List<NewCaseDetails> response = new ArrayList<>();
		Map<String, NewCaseDetails> map = jsonfileToMap();
		if (map.size() > 0) {
			Set<String> keys = map.keySet();
			for (String key : keys) {
				String url = "https://egov.uscis.gov/casestatus/mycasestatus.do?appReceiptNum=" + key;
				String result = new RestTemplate().getForObject(url, String.class);
				NewCaseDetails res = checkCaseStatus(result, key, map.get(key), caseType);
				if (res != null)
					response.add(res);
			}

		} else {
			int start = getCaseNumberNumericStartPosition(caseNumber);
			if (start == -1)
				return ResponseEntity.badRequest().build();
			int MAX_RECORDS = 200;
			Long caseNumberNumeric = Long.parseLong(caseNumber.substring(start));
			caseNumberNumeric -= 300;
			String caseCode = caseNumber.substring(0, start);

			while (MAX_RECORDS > 0) {
				String newCaseNumber = caseCode + (caseNumberNumeric++).toString();
				System.err.println("New Case:" + newCaseNumber + ", " + "MAX:" + MAX_RECORDS);
				String url = "https://egov.uscis.gov/casestatus/mycasestatus.do?appReceiptNum=" + newCaseNumber;
				RestTemplate restTemplate = new RestTemplate();
				String result = restTemplate.getForObject(url, String.class);

				if (result.contains(caseType)) {
					MAX_RECORDS--;
					NewCaseDetails res = checkCaseStatus(result, newCaseNumber, map.get(newCaseNumber), caseType);
					if (res != null)
						response.add(res);
				}

			}
		}
		updateFile(response, map);
		filterResponse(response);
		generateCSV(response);
		return ResponseEntity.ok(response);
	}

	private void generateCSV(List<NewCaseDetails> response) {
		try {
			JFlat flatMe = new JFlat(response.toString());

			// get the 2D representation of JSON document
			flatMe.json2Sheet().headerSeparator("_").getJsonAsSheet();

			// write the 2D representation in csv format
			flatMe.write2csv("DailyChange.csv");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File csvOutputFile = new File("DailyChanges.csv");
		try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
			response.stream().map(this::convertToCSV).forEach(pw::println);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String convertToCSV(NewCaseDetails data) {
		return Stream.of(data.toString()).map(this::escapeSpecialCharacters).collect(Collectors.joining(","));
	}

	public String escapeSpecialCharacters(String data) {
		String escapedData = data.replaceAll("\\R", " ");
		if (data.contains(",") || data.contains("\"") || data.contains("'")) {
			data = data.replace("\"", "\"\"");
			escapedData = "\"" + data + "\"";
		}
		return escapedData;
	}

	private void filterResponse(List<NewCaseDetails> response) {
		for (NewCaseDetails c : response) {
			if (c.getCaseUpdates().size() > 2) {
				for (int i = 1; i < c.getCaseUpdates().size() - 1; i++) {
					c.getCaseUpdates().remove(i);
				}
			}
		}

	}

	private void updateFile(List<NewCaseDetails> response, Map<String, NewCaseDetails> map) {

		if (response != null && response.size() > 0) {
			for (NewCaseDetails caseDetail : response) {
				if (map.containsKey(caseDetail.getCaseNum())) {
					map.remove(caseDetail.getCaseNum());
				}
			}

			List<NewCaseDetails> updatedFile = new ArrayList<NewCaseDetails>();
			updatedFile.addAll(response);

			for (Entry<String, NewCaseDetails> entry : map.entrySet())
				updatedFile.add(entry.getValue());
			Collections.sort(updatedFile);
			File output = new File(originalCaseNumber + "-update.txt");
			output.delete();
			for (NewCaseDetails caseDetail : updatedFile) {
				generateFile(caseDetail, true);
			}
		}
	}

	private int getCaseNumberNumericStartPosition(String caseNumber) {

		for (int i = 0; i < caseNumber.length(); i++) {
			if (StringUtil.isNumeric(caseNumber.charAt(i) + ""))
				return i;
		}

		return -1;
	}

	public NewCaseDetails checkCaseStatus(String result, String caseNumber, NewCaseDetails existingCase,
			String caseType) {

		try {
			Document document = Jsoup.parse(result);
			Elements divWithStartingClass = document.select("div[class^=rows text-center]");

			NewCaseDetails caseDetail = createCaseDetails(divWithStartingClass, caseNumber, existingCase, caseType);

			if (caseDetail.equals(existingCase))
				return null;
			else if (caseDetail.getCaseType() != null && existingCase == null)
				generateFile(caseDetail, false);

			return caseDetail;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void generateFile(NewCaseDetails caseDetails, boolean update) {
		File output = new File(originalCaseNumber + ".txt");
		if (update) {
			output = new File(originalCaseNumber + "-update.txt");
		}
		try {
			FileWriter writer = new FileWriter(output, true);
			writer.write(caseDetails.toString() + "\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public NewCaseDetails createCaseDetails(Elements divWithStartingClass, String caseNumber,
			NewCaseDetails existingCase, String caseType2) {

		NewCaseDetails caseDetail = new NewCaseDetails();
		CaseUpdate caseUpdate = new CaseUpdate();

		String caseResponse = divWithStartingClass.select("p").first().ownText();
		List<String> list = Arrays.asList(caseResponse.split(","));

		caseDetail.setCaseNum(caseNumber);

		caseUpdate.setDate(getFormattedDate(list));
		caseUpdate.setCaseStatus(divWithStartingClass.select("h1").last().text());

		if (existingCase != null) {
			caseDetail.setCaseType(caseType2);
			List<CaseUpdate> caseUpdates = existingCase.getCaseUpdates();
			List<CaseUpdate> caseUpdatesNew = new ArrayList<CaseUpdate>();

			if (caseUpdates.size() > 0) {
				for (CaseUpdate anyCase : caseUpdates) {
					if (!anyCase.equals(caseUpdate))
						caseUpdatesNew.add(anyCase);
				}
			}
			caseUpdatesNew.add(caseUpdate);
			caseDetail.setCaseUpdates(caseUpdatesNew);

			return caseDetail;
		} else {
			String[] caseTypeArray = list.get(2).split(" ");
			String caseType = caseTypeArray[caseTypeArray.length - 1];
			List<CaseUpdate> caseUpdates = new ArrayList<CaseUpdate>();
			caseDetail.setCaseType(caseType.equalsIgnoreCase(caseType2) ? caseType : null);
			caseUpdates.add(caseUpdate);
			caseDetail.setCaseUpdates(caseUpdates);
		}
		return caseDetail;
	}

	private Date getFormattedDate(List<String> l) {
		if (l.get(0) == null || l.get(1) == null || l.get(0).length() < 3)
			return null;
		String dateFromResponse = new StringBuilder().append(l.get(0).substring(3)).append(",").append(l.get(1))
				.toString();
		Date caseDate = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
			sdf.setTimeZone(TimeZone.getDefault());
			caseDate = sdf.parse(dateFromResponse);
		} catch (ParseException e) {

		}

		return caseDate;
	}

	public Map<String, NewCaseDetails> jsonfileToMap() {

		Map<String, NewCaseDetails> map = new HashMap<>();
		String fileName = originalCaseNumber + ".txt";
		ObjectMapper om = new ObjectMapper();
		om.setTimeZone(TimeZone.getDefault());
		if (new File(fileName).exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String lineRead = reader.readLine();

				while (lineRead != null) {

					NewCaseDetails caseDetails = om.readValue(lineRead, NewCaseDetails.class);

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
