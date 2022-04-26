package com.spring.statuscheck.statuscheck;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

		return casedataByDate;
	}

}
