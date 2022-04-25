package com.spring.statuscheck.statuscheck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.spring.statuscheck.dataobjects.CaseData;
import com.spring.statuscheck.dataobjects.CaseUpdate;

@RestController
public class CaseController {

	@GetMapping("/generate/{caseType}")
	public void generateCases(@PathVariable String caseType) throws Exception {
		FileWriter writer = initializeWriter(caseType);
		int MAX_RECORDS = 1800;
		int currentRecord = 0;

		// Starting Case
		int caseNumberNumeric = 6999;
		String caseCode = "EAC22900";
		StringBuilder caseNum = null;

		while (MAX_RECORDS > 0) {

			caseNum = new StringBuilder().append(caseCode);
			String caseNumToCheck = getNewCaseNumber(caseNumberNumeric);
			caseNumberNumeric++;
			String newCaseNumber = caseNum.append(caseNumToCheck).toString();
			String url = "https://egov.uscis.gov/casestatus/mycasestatus.do?appReceiptNum=" + newCaseNumber;
			String result = new RestTemplate().getForObject(url, String.class);
			if (result.contains(caseType)) {
				MAX_RECORDS--;
				currentRecord++;
				System.err.println("newCaseNumber: " + newCaseNumber + ",MAX_RECORDS:" + MAX_RECORDS + ",CurrentCount:"
						+ currentRecord);
				addCase(writer, result, newCaseNumber);				
			}
		}
		if (writer != null) {
			writer.flush();
			writer.close();
		}
	}

	private String getNewCaseNumber(int caseNumberNumeric) {
		String output = caseNumberNumeric + "";
		if (output.length() < 5)
			output = "0" + output;
		return output;
	}

	public void addCase(FileWriter writer, String result, String caseNumber) {

		try {
			Document document = Jsoup.parse(result);
			Elements divWithStartingClass = document.select("div[class^=rows text-center]");

			CaseData caseDetail = createCaseDetails(divWithStartingClass, caseNumber);

			addRecord(writer, caseDetail, caseDetail.getCaseType());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CaseData createCaseDetails(Elements divWithStartingClass, String caseNumber) {

		CaseData caseData = new CaseData();

		String caseResponse = divWithStartingClass.select("p").first().ownText();
		List<String> list = Arrays.asList(caseResponse.split(","));

		List<CaseUpdate> caseUpdates = new ArrayList<CaseUpdate>();
		CaseUpdate caseUpdate = new CaseUpdate();
		caseUpdate.setDate(getFormattedDate(list));
		caseUpdate.setCaseStatus(divWithStartingClass.select("h1").last().text());
		caseUpdates.add(caseUpdate);
		caseData.setCaseUpdates(caseUpdates);

		String[] caseTypeArray = list.get(2).split(" ");
		String caseType = caseTypeArray[caseTypeArray.length - 1];
		caseData.setCaseType(caseType);
		caseData.setCaseNum(caseNumber);
		caseData.setLastUpdateDate(new Date());

		return caseData;
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

	private void addRecord(FileWriter writer, CaseData caseDetails, String caseType) {
		try {
			writer.write(caseDetails.toString() + "\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private FileWriter initializeWriter(String caseType) {
		File output = new File("master-" + caseType + ".txt");
		FileWriter writer = null;
		try {
			writer = new FileWriter(output, true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return writer;
	}

}
