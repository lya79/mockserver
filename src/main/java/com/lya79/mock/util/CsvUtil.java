package com.lya79.mock.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CsvUtil {
	public boolean handler(HttpServletRequest request, HttpServletResponse response, String path) {
		String[] csvHeader = new String[] { "type", "method", "url", "status", "header", "parameter", "body", "delay" };

		CSVParser csvParser;

		try {
			Reader reader = Files.newBufferedReader(Paths.get(path));
			csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
		} catch (IOException e) {
			System.err.println("csv讀取失敗: " + e.toString());
			return false;
		}

		boolean match = false; // 請求是否匹配

		try {
			for (CSVRecord csvRecord : csvParser) {
				String reqStr = csvRecord.get(csvHeader[0]);
				String methodStr = csvRecord.get(csvHeader[1]);
				String urlStr = csvRecord.get(csvHeader[2]);
				String statusStr = csvRecord.get(csvHeader[3]);
				String headerStr = csvRecord.get(csvHeader[4]);
				String parameterStr = csvRecord.get(csvHeader[5]);
				String bodyStr = csvRecord.get(csvHeader[6]);
				String delayStr = csvRecord.get(csvHeader[7]);

				// 處理請求
				if (reqStr.equalsIgnoreCase("request")) {
					match = false;

					if (!isMatchMethod(request, methodStr) // 檢查 method
							|| !isMatchURL(request, urlStr) // 檢查 url
							|| !isMatchHeader(request, headerStr) // 檢查 header
							|| !isMatchUrlParameter(request, parameterStr) // 檢查 url參數
							|| !isMatchBody(request, bodyStr) // 檢查 content
					) {
						continue;
					}

					// 延遲回應
					delay(delayStr);

					match = true; // 請求匹配成功
					continue;
				}

				// 處理回應
				if (reqStr.equalsIgnoreCase("response") && match) {
					match = false;

					setStatus(response, statusStr);
					setHeader(response, headerStr);
					setBody(response, bodyStr);

					return true; // 請求匹配成功
				}
			}
		} catch (Exception e) {
			System.err.println("csv設定檔案錯誤: " + e.toString());
			return false;
		} finally {
			try {
				csvParser.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false; // 沒有匹配任何請求
	}

	private void setStatus(HttpServletResponse response, String text) throws NumberFormatException {
		int status = Integer.parseInt(text);
		response.setStatus(status);
	}

	private void setHeader(HttpServletResponse response, String text) throws Exception {
		String[] arr = text.split("\n");
		for (int i = 0; i < arr.length; i++) {
			String[] arr2 = arr[i].split("=", 2);
			if (arr2.length != 2) {
				throw new Exception("設定header, parameter參數錯誤: " + text);
			}

			String key = arr2[0];
			String value = arr2[1];
			response.addHeader(key, value);
		}
	}

	private void setBody(HttpServletResponse response, String text) throws IOException {
		PrintWriter printWriter = response.getWriter();
		printWriter.append(text);
	}

	private boolean isMatchMethod(HttpServletRequest request, String text) throws Exception {
		if (text == null || text.trim().equals("")) {
			throw new Exception("檢查method, 參數錯誤: " + text);
		}

		String reqMethod = request.getMethod();
		return reqMethod.trim().equalsIgnoreCase(text.trim());
	}

	private boolean isMatchURL(HttpServletRequest request, String text) throws Exception {
		if (text == null || text.trim().equals("")) {
			throw new Exception("檢查url, 參數錯誤: " + text);
		}

		String reqURLStr = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		Pattern pattern = Pattern.compile(text.trim());
		Matcher matcher = pattern.matcher(reqURLStr);

		return matcher.matches();
	}

	private boolean isMatchHeader(HttpServletRequest request, String text) throws Exception {
		if (text == null || text.trim().equals("")) {
			return true;
		}

		Map<String, String> map = new HashMap<>();
		String[] arr = text.split("\n");
		for (int i = 0; i < arr.length; i++) {
			String[] arr2 = arr[i].split("=", 2);
			if (arr2.length != 2) {
				throw new Exception("檢查header, parameter參數錯誤: " + text);
			}

			String key = arr2[0];
			String value = arr2[1];
			map.put(key, value);
		}

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

	private boolean isMatchUrlParameter(HttpServletRequest request, String text) throws Exception {
		if (text == null || text.trim().equals("")) {
			return true;
		}

		Map<String, String> map = new HashMap<>();
		String[] arr = text.split("\n");
		for (int i = 0; i < arr.length; i++) {
			String[] arr2 = arr[i].split("=", 2);
			if (arr2.length != 2) {
				throw new Exception("檢查url, parameter參數錯誤: " + text);
			}

			String key = arr2[0];
			String value = arr2[1];
			map.put(key, value);
		}

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

	private boolean isMatchBody(HttpServletRequest request, String text) throws IOException { // XXX 正規化判斷參數
		if (text == null || text.trim().equals("")) {
			return true;
		}

		String applicationJson = request.getReader().lines().collect(Collectors.joining());
		JsonNode reqApplicationJsonNode = new ObjectMapper().readTree(applicationJson);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode bodyNode = mapper.readTree(text);

		return bodyNode.equals(reqApplicationJsonNode);
	}

	private void delay(String text) throws NumberFormatException {
		int sleep = Integer.parseInt(text);
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
