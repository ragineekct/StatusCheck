package com.spring.statuscheck.statuscheck;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.spring.statuscheck.dataobjects.CaseData;
import com.spring.statuscheck.dataobjects.CaseUpdate;
import com.spring.statuscheck.util.StatusCheckUtil;

@RestController
public class StatusSearchByDateController {

	String[] approvedList = { "Case Was Approved", "New Card Is Being Produced" };

	@GetMapping("/statusByDate/{caseCode}/{date}")
	public List<CaseData> getCaseStatusByDate(@PathVariable String date, @PathVariable String caseCode)
			throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date1 = sdf.parse(date);
		List<CaseData> casedataByDate = new ArrayList<CaseData>();
		Map<String, CaseData> masterFile = StatusCheckUtil.jsonfileToMap(caseCode);

		Iterator<Map.Entry<String, CaseData>> iterator = masterFile.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, CaseData> entry = iterator.next();
			List<CaseUpdate> list = entry.getValue().getCaseUpdates();

			for (CaseUpdate c : list) {
				if (c.getDate() != null) {
					if (date1.compareTo(c.getDate()) == 0)
						casedataByDate.add(entry.getValue());
				}
			}
		}
		Collections.sort(casedataByDate);
		return casedataByDate;
	}

	@GetMapping("/statusByDate/{caseCode}/{date}/{status}")
	public List<CaseData> getCaseStatusByDateAndStatus(@PathVariable String date, @PathVariable String caseCode,
			@PathVariable String status) throws ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date1 = sdf.parse(date);
		List<CaseData> casedataByDate = new ArrayList<CaseData>();
		Map<String, CaseData> masterFile = StatusCheckUtil.jsonfileToMap(caseCode);

		Iterator<Map.Entry<String, CaseData>> iterator = masterFile.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, CaseData> entry = iterator.next();
			List<CaseUpdate> list = entry.getValue().getCaseUpdates();

			for (CaseUpdate c : list) {
				if (c.getDate() != null) {
					if (date1.compareTo(c.getDate()) == 0 && checkStatus(status, c)) {
						casedataByDate.add(entry.getValue());
						break;
					}
				}
			}
		}
		filterData(date1, casedataByDate);
		Collections.sort((casedataByDate));
		return casedataByDate;
	}

	private void filterData(Date date1, List<CaseData> casedataByDate) {
		List<CaseUpdate> removeList = new ArrayList<CaseUpdate>();
		for (CaseData d : casedataByDate) {
			for (CaseUpdate up : d.getCaseUpdates()) {
				if (!up.getCaseStatus().contains("Case Was Received") && up.getDate() != null
						&& up.getDate().compareTo(date1) != 0)
					removeList.add(up);
			}
			d.getCaseUpdates().removeAll(removeList);
		}
	}

	private boolean checkStatus(String status, CaseUpdate c) {
		List<String> list = initializeMap(status);
		for (String s : list) {
			if (c.getCaseStatus().toLowerCase().contains(s.toLowerCase()))
				return true;
		}
		return false;

	}

	private List<String> initializeMap(String status) {

		Map<String, List<String>> map = new HashMap<>();
		map.put("approved", Arrays.asList(approvedList));
		return map.get(status.toLowerCase());
	}

}
