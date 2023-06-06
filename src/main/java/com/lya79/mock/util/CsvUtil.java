package com.lya79.mock.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class CsvUtil {
	public boolean handler(HttpServletRequest request, HttpServletResponse response, String path) {
		String[] csvHeader = new String[] { "type", "method", "url", "status", "header", "parameter", "content",
				"delay" };

		try {
			Reader reader = Files.newBufferedReader(Paths.get(path));
			CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());

			for (CSVRecord csvRecord : csvParser) {
				String reqStr = csvRecord.get(csvHeader[0]);
				String methodStr = csvRecord.get(csvHeader[1]);
				String urlStr = csvRecord.get(csvHeader[2]);
				String statusStr = csvRecord.get(csvHeader[3]);
				String headerStr = csvRecord.get(csvHeader[4]);
				String parameterStr = csvRecord.get(csvHeader[5]);
				String contentStr = csvRecord.get(csvHeader[6]);
				String delayStr = csvRecord.get(csvHeader[7]);

//				System.out.println(reqStr + ", " + methodStr + ", " + urlStr + ", " + statusStr + ", " + headerStr
//				+ ", " + parameterStr + ", " + contentStr + ", " + delayStr);

				if (!reqStr.equals("request")) {
					continue;
				}

				// 檢查 method
//				if (!isMatchMethod(request, methodStr)) {
//					continue;
//				}
//				
				// 檢查 url
//				if (!isMatchURL(request, urlStr)) {
//					continue;
//				}

				{ // 檢查 header
					Map<String, String> headerMap = new HashMap<>();
					String[] arr = headerStr.split("\n");
					for (int i = 0; i < arr.length; i++) {
//						String[] headerArr = arr[i];
						String[] arr2 = arr[i].split("=", 2);
						if (arr2.length != 2) { // TODO 無效的設定
							continue;
						}

						String key = arr2[0];
						String value = arr2[1];
						headerMap.put(key, value);
					}

					if (headerMap.size() > 0 && !isMatchHeader(request, headerMap)) {
						continue;
					}
				}

			}

			csvParser.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false; // 沒有匹配任何請求
	}

	private boolean isMatchMethod(HttpServletRequest request, String text) {
		String reqMethod = request.getMethod();
		return reqMethod.trim().equals(text.trim());
	}

	private boolean isMatchURL(HttpServletRequest request, String text) {
		String reqURLStr = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		Pattern pattern = Pattern.compile(text.trim());
		Matcher matcher = pattern.matcher(reqURLStr);
		return matcher.matches();
	}

	private boolean isMatchHeader(HttpServletRequest request, Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String headerName = entry.getKey();
			String headerValue = entry.getValue();

			boolean match = false;

			Enumeration<String> iterator = request.getHeaderNames();
			while (iterator.hasMoreElements()) {
				String reqHeader = iterator.nextElement();
				String reqHeaderValue = request.getHeader(reqHeader);

				if (headerName.trim().equals(reqHeader.trim()) && headerValue.trim().equals(reqHeaderValue.trim())) {
					match = true;
					break;
				}
			}

			if (!match) {
				return false;
			}
		}

		return true;
	}

	private boolean isMatchUrlParameter(HttpServletRequest request, Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String urlParameterName = entry.getKey();
			String urlParameterValue = entry.getValue();

			boolean match = false;

			Map<String, String[]> reqMap = request.getParameterMap();
			for (String key : reqMap.keySet()) {
				String reqUrlParameterName = key;
				String reqUrlParameterValue = reqMap.get(key)[0];

				if (urlParameterName.trim().equals(reqUrlParameterName.trim())
						&& Pattern.compile(urlParameterValue.trim()).matcher(reqUrlParameterValue.trim()).matches()) {
					match = true;
					break;
				}
			}

			if (!match) {
				return false;
			}
		}

		return true;
	}

	private boolean isMatchUrlParameter(HttpServletRequest request, String content) { // TODO
// TODO 會有多層結構
// !開頭的key使用正規表示法
//		{
//			  "!name": [a-zA-Z0-9]{5,12},
//			  "color": "red",
//			  "detail": {
//			    "size": "small",
//			    "!price": [
//			      [a-zA-Z0-9]{6},
//			      [a-zA-Z0-9]{6}
//			    ]
//			  }
//			}
		return true;
	}

	private void sleep(String text) {
		int sleep = 0;

		try {
			sleep = Integer.parseInt(text);
		} catch (NumberFormatException nfe) {
			System.out.println(nfe);
		}

		if (sleep <= 0) {
			return;
		}

		try {
			Thread.sleep(sleep);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
}
