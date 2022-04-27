package com.spring.statuscheck.statuscheck;

import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.spring.statuscheck.dataobjects.CaseData;
import com.spring.statuscheck.dataobjects.CaseUpdate;
import com.spring.statuscheck.util.StatusCheckUtil;

@RestController
public class DailyUpdateController {

	@PutMapping("/dailyUpdate/{caseCode}/{caseType}")
	public ResponseEntity<Object> getStatus(@PathVariable String caseCode, @PathVariable String caseType)
			throws Exception {
		FileWriter writer = new FileWriter(new File(StatusCheckUtil.getFileName(caseCode) + "-update.txt"));
		Map<String, CaseData> map = StatusCheckUtil.jsonfileToMap(caseCode);
		if (map.size() > 0) {
			Set<String> keys = map.keySet();
			for (String key : keys) {
				String url = "https://egov.uscis.gov/casestatus/mycasestatus.do?appReceiptNum=" + key;
				String result = new RestTemplate().getForObject(url, String.class);
				CaseData response = checkCaseStatus(result, key, map.get(key), caseType);
				writer.write(response.toString() + "\n");
				writer.flush();
			}

		}
		if (writer != null)
			writer.close();

		return ResponseEntity.accepted().build();
	}

	public CaseData checkCaseStatus(String result, String caseNumber, CaseData existingCase, String caseType) {

		try {
			Document document = Jsoup.parse(result);
			Elements divWithStartingClass = document.select("div[class^=rows text-center]");

			return createCaseDetails(divWithStartingClass, caseNumber, existingCase, caseType);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public CaseData createCaseDetails(Elements divWithStartingClass, String caseNumber, CaseData existingCase,
			String caseType2) {

		CaseData caseDetail = new CaseData();
		CaseUpdate caseUpdate = new CaseUpdate();

		String caseResponse = divWithStartingClass.select("p").first().ownText();
		List<String> list = Arrays.asList(caseResponse.split(","));

		caseDetail.setCaseNum(caseNumber);

		caseUpdate.setDate(getFormattedDate(list));
		caseUpdate.setCaseStatus(divWithStartingClass.select("h1").last().text());

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

}
