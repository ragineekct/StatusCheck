package com.spring.statuscheck.statuscheck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.spring.statuscheck.dataobjects.CaseData;
import com.spring.statuscheck.dataobjects.CaseUpdate;
import com.spring.statuscheck.util.StatusCheckUtil;

@RestController
public class DailyUpdateController {

	@GetMapping("/dailyUpdate/{caseCode}/{caseType}")
	public ResponseEntity<List<CaseData>> getStatus(@PathVariable String caseCode, @PathVariable String caseType)
			throws Exception {
		List<CaseData> response = new ArrayList<>();
		Map<String, CaseData> map = StatusCheckUtil.jsonfileToMap(caseCode);
		if (map.size() > 0) {
			Set<String> keys = map.keySet();
			for (String key : keys) {
				String url = "https://egov.uscis.gov/casestatus/mycasestatus.do?appReceiptNum=" + key;
				String result = new RestTemplate().getForObject(url, String.class);
				CaseData res = checkCaseStatus(result, key, map.get(key), caseType);
				if (res != null)
					response.add(res);
			}

		}
		updateFile(response, map);
		filterResponse(response);

		return ResponseEntity.ok(response);
	}

	private void filterResponse(List<CaseData> response) {
		for (CaseData c : response) {
			int size = c.getCaseUpdates().size();
			if (size > 2) {
				for (int i = size - 2; i > 0; i--) {
					c.getCaseUpdates().remove(i);
				}
			}
		}

	}

	private void updateFile(List<CaseData> response, Map<String, CaseData> map) {

		if (response != null && response.size() > 0) {
			for (CaseData caseDetail : response) {
				if (map.containsKey(caseDetail.getCaseNum())) {
					map.remove(caseDetail.getCaseNum());
				}
			}

			List<CaseData> updatedFile = new ArrayList<CaseData>();
			updatedFile.addAll(response);

			for (Entry<String, CaseData> entry : map.entrySet())
				updatedFile.add(entry.getValue());
			Collections.sort(updatedFile);
			File output = new File("EAC-I765-update.txt");
			output.delete();
			for (CaseData caseDetail : updatedFile) {
				generateFile(caseDetail, output);
			}
		}
	}

	public CaseData checkCaseStatus(String result, String caseNumber, CaseData existingCase, String caseType) {

		try {
			Document document = Jsoup.parse(result);
			Elements divWithStartingClass = document.select("div[class^=rows text-center]");

			CaseData caseDetail = createCaseDetails(divWithStartingClass, caseNumber, existingCase, caseType);

			if (caseDetail.equals(existingCase))
				return null;
			else if (caseDetail.getCaseType() != null && existingCase == null)
				// generateFile(caseDetail, false);

				return caseDetail;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void generateFile(CaseData caseDetails, File output) {

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

	public CaseData createCaseDetails(Elements divWithStartingClass, String caseNumber, CaseData existingCase,
			String caseType2) {

		CaseData caseDetail = new CaseData();
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

}
