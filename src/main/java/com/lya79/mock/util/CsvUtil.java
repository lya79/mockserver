package com.lya79.mock.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Setter;

@Component
public class CsvUtil implements IMockUtil {
	@Setter
	private String path;

	private Date lastModified;
	private List<CSVRecord> records = new ArrayList<CSVRecord>();

	@Override
	public boolean handler(HttpServletRequest request, HttpServletResponse response) {
		String[] csvHeader = new String[] { "type", "delay", "method", "url", "status", "header", "parameter", "body",
				"cookie", "cookie-expires" };

		{
			File file = new File(this.path);
			Date tmpLastModified = new Date(file.lastModified());

			// 節省效能避免每次都重新讀取檔案
			// 檔案最後更新時間和目前暫存的不一樣就要重新讀取
			boolean update = lastModified == null || !tmpLastModified.equals(lastModified);
			update = update || this.records.isEmpty();

			if (update) {
				if (!this.records.isEmpty()) {
					this.records.clear();
				}

				CSVParser csvParser = null;
				try {
					Reader reader = Files.newBufferedReader(Paths.get(this.path), StandardCharsets.UTF_8);
					csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
					this.records.addAll(csvParser.getRecords());
					csvParser.close();
					System.out.println("重新讀取 csv, path: " + this.path);
				} catch (IOException e) {
					if (!this.records.isEmpty()) {
						this.records.clear();
					}
					System.err.println("csv讀取失敗, path:" + csvParser + ", error:" + e.toString());
					return false;
				} finally {
					if (csvParser != null && !csvParser.isClosed()) {
						try {
							csvParser.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

			this.lastModified = tmpLastModified;
		}

		boolean match = false; // 請求是否匹配

		try {
			for (CSVRecord csvRecord : this.records) {
				String reqStr = csvRecord.get(csvHeader[0]);
				String delayStr = csvRecord.get(csvHeader[1]);
				String methodStr = csvRecord.get(csvHeader[2]);
				String urlStr = csvRecord.get(csvHeader[3]);
				String statusStr = csvRecord.get(csvHeader[4]);
				String headerStr = csvRecord.get(csvHeader[5]);
				String parameterStr = csvRecord.get(csvHeader[6]);
				String bodyStr = csvRecord.get(csvHeader[7]);
				String cookieStr = csvRecord.get(csvHeader[8]);
				String cookieExpiresStr = csvRecord.get(csvHeader[9]);

				// 處理請求
				if (reqStr.equalsIgnoreCase("request")) {
					match = false;

					if (!isMatchMethod(request, methodStr) // 檢查 method
							|| !isMatchURL(request, urlStr) // 檢查 url
							|| !isMatchHeader(request, headerStr) // 檢查 header
							|| !isMatchUrlParameter(request, parameterStr) // 檢查 url參數
							|| !isMatchBody(request, bodyStr) // 檢查 content
							|| !isMatchCookie(request, cookieStr) // 檢查 cookie
					) {
						continue;
					}

					// 延遲回應
					delay(delayStr);

					match = true; // 請求匹配成功
					
					System.out.println("mapping row:" + (csvRecord.getRecordNumber() + 1) + ", method:" + methodStr
							+ ", url:" + urlStr);
					continue;
				}

				// 處理回應
				if (reqStr.equalsIgnoreCase("response") && match) {
					match = false;

					setStatus(response, statusStr);
					setHeader(response, headerStr);
					setBody(response, bodyStr);
					setCookie(response, cookieStr, cookieExpiresStr);

					return true; // 請求匹配成功
				}
			}
		} catch (Exception e) {
			System.err.println("csv設定檔案錯誤: " + e.toString());
			return false;
		}

		return false; // 沒有匹配任何請求
	}

	private void setStatus(HttpServletResponse response, String text) throws NumberFormatException {
		int status = Integer.parseInt(text);
		response.setStatus(status);
	}

	private void setHeader(HttpServletResponse response, String text) throws Exception {
		if (text == null || text.trim().equals("")) {
			return;
		}

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
		if (text == null || text.trim().equals("")) {
			return;
		}

		PrintWriter printWriter = response.getWriter();
		printWriter.append(text);
	}

	private void setCookie(HttpServletResponse response, String text, String cookieExpiresStr) throws Exception {
		if (text == null || text.trim().equals("")) {
			return;
		}

		Integer expires = -1; // 沒設定有效時間一律設定成 -1

		if (cookieExpiresStr != null && !cookieExpiresStr.trim().equals("")) {
			expires = Integer.parseInt(cookieExpiresStr);
		}

		String[] arr = text.split("\n");
		for (int i = 0; i < arr.length; i++) {
			String[] arr2 = arr[i].split("=", 2);
			if (arr2.length != 2) {
				throw new Exception("檢查cookie, 參數錯誤: " + text);
			}

			String key = arr2[0];
			String value = arr2[1];
			Cookie cookie = new Cookie(key, value);

			if (expires != null) {
				cookie.setMaxAge(expires);
			}

			// XXX 暫時用不到
//			cookie.setHttpOnly(false);
//			cookie.setDomain(text);
//			cookie.setPath(text);

			response.addCookie(cookie);
		}
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

	private boolean isMatchCookie(HttpServletRequest request, String text) throws Exception {
		if (text == null || text.trim().equals("")) {
			return true;
		}

		Map<String, String> map = new HashMap<>();
		String[] arr = text.split("\n");
		for (int i = 0; i < arr.length; i++) {
			String[] arr2 = arr[i].split("=", 2);
			if (arr2.length != 2) {
				throw new Exception("檢查cookie, 參數錯誤: " + text);
			}

			String key = arr2[0];
			String value = arr2[1];
			map.put(key, value);
		}

		for (Map.Entry<String, String> entry : map.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();

			boolean match = false;

			javax.servlet.http.Cookie[] reqCookieArr = request.getCookies();
			if (reqCookieArr == null) {
				return false;
			}

			for (javax.servlet.http.Cookie reqCookie : reqCookieArr) {
				String reqName = reqCookie.getName();
				String reqValue = reqCookie.getValue();

				if (name.trim().equals(reqName.trim()) && value.trim().equals(reqValue.trim())) {
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

	public static boolean isCsvFile(File file) {
		String filename = file.getName();
		String extension = "";
		int idx = filename.lastIndexOf(".");
		if (idx >= 0) {
			extension = filename.substring(idx + 1);
		}
		return extension.equals("csv");
	}
}
