package com.spring.statuscheck.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatusCheckTest {       

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		String str = "On October 22, 2021, we received your Form I-765, Application for Employment Authorization, Receipt Number EAC2290008030, and sent you the receipt notice that describes how we will process your case. Please follow the instructions in the notice. If you have any questions, contact";

		List<String> l = Arrays.asList(str.split(","));

		String d = new StringBuilder().append(l.get(0).substring(3)).append(",").append(l.get(1)).toString();
		// System.out.println(d);

		try {
			Date caseDate = new SimpleDateFormat("MMMM d, yyyy").parse(d);
		//	caseDate.toLocaleString();
		} catch (ParseException e) {

		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
		LocalDate date = LocalDate.parse(d, formatter);
		// System.out.println(date);

		String[] caseTypeArray = l.get(2).split(" ");
		String caseType = caseTypeArray[caseTypeArray.length - 1];  
		BufferedReader reader = new BufferedReader(new FileReader("output.txt"));
		String line= reader.readLine();
		while(line!=null) {
			System.out.println(line);
			line=reader.readLine();
		}
		String caseNumber="EAC2290008030";System.out.println(caseNumber);
		Long startNum = Long.parseLong(caseNumber.substring(3, 13));
		String caseCode = caseNumber.substring(0, 3);
		int count = 10;
		while (count > 0) {
			String var=caseCode+ (startNum++).toString();
			System.out.println(var);
			count--;
			}
		
		
		// System.out.println(caseType);

	}

}
