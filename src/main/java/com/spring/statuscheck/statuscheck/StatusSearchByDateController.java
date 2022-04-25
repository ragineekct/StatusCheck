package com.spring.statuscheck.statuscheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import java.util.TimeZone;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.statuscheck.dataobjects.CaseData;
import com.spring.statuscheck.dataobjects.CaseUpdate;

@RestController
public class StatusSearchByDateController {
	
	@GetMapping("/statusByDate/{date}" )
	public List<CaseData> getCaseStatusByDate(@PathVariable String date) throws ParseException
	{
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd");
		Date date1=sdf.parse(date);
		List<CaseData> casedataByDate=new ArrayList<CaseData>();
		Map<String, CaseData> masterFile = jsonfileToMap("master-I-765");
		
		Iterator<Map.Entry<String, CaseData>> iterator = masterFile.entrySet().iterator();
		
		while(iterator.hasNext())
		{
			Map.Entry<String, CaseData> entry = iterator.next();
			List<CaseUpdate> list = entry.getValue().getCaseUpdates();
			
		//	System.err.println(list);
			for (CaseUpdate c : list) {
				Date date2=sdf.parse(c.getDate().toString());
				if (date1.compareTo(date2)==0)
					
					casedataByDate.add(entry.getValue());
			}
		}
		
		

		return casedataByDate;
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
