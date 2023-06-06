package com.lya79.mock.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MatcherUtil {
	public boolean handler(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
		ClassPathResource classPathResource = new ClassPathResource(path);
		InputStream inputStream = classPathResource.getInputStream();

		JsonNode rootNode = new ObjectMapper().readTree(inputStream);

		String reqURLStr = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		String reqMethod = request.getMethod();

		String rootPathStr = rootNode.path("default").path("rootPath").asText();

		JsonNode testcaseNode = rootNode.path("testCase");
		if (!testcaseNode.isMissingNode() && testcaseNode.isArray()) {
			Iterator<JsonNode> testcaseNodeIterator = testcaseNode.elements();
			while (testcaseNodeIterator.hasNext()) {
				JsonNode itemNode = testcaseNodeIterator.next();

				{ // 比對請求
					JsonNode requestNode = itemNode.path("request");
					if (requestNode.isMissingNode()) {
						continue;
					}

					// 檢查 method
					JsonNode methodNode = requestNode.path("method");
					if (methodNode.isMissingNode()) {
						continue;
					}
					String methodStr = methodNode.asText();
					if (!reqMethod.trim().equals(methodStr.trim())) {
						continue;
					}

					// 檢查 root path
					reqURLStr = reqURLStr.trim();
					if (!rootPathStr.isBlank()) {
						if (!reqURLStr.startsWith(rootPathStr)) {
							continue;
						}

						String[] arr = reqURLStr.split(rootPathStr);
						if (arr.length != 2) {
							continue;
						}

						reqURLStr = arr[1];
					}

					// 檢查 URL
					JsonNode urlNode = requestNode.path("url");
					if (urlNode.isMissingNode()) {
						continue;
					}
					String urlStr = urlNode.asText();
					Pattern pattern = Pattern.compile(urlStr.trim());
					Matcher matcher = pattern.matcher(reqURLStr);
					if (!matcher.matches()) {
						continue;
					}

					// 檢查 header
					boolean allHeaderMatch = true;
					JsonNode headerNode = requestNode.path("header");
					if (!headerNode.isMissingNode()) {
						Iterator<String> headerNameIterator = headerNode.fieldNames();
						while (headerNameIterator.hasNext()) {
							String headerName = headerNameIterator.next();
							String headerValue = headerNode.path(headerName).textValue();

							boolean headerMatch = false;

							Enumeration<String> iterator2 = request.getHeaderNames();
							while (iterator2.hasMoreElements()) {
								String reqHeader = iterator2.nextElement();
								String reqHeaderValue = request.getHeader(reqHeader);

								if (headerName.trim().equals(reqHeader.trim())
										&& headerValue.trim().equals(reqHeaderValue.trim())) {
									headerMatch = true;
									break;
								}
							}

							if (!headerMatch) {
								allHeaderMatch = false;
								break;
							}
						}
						if (!allHeaderMatch) {
							continue;
						}
					}

					// 檢查 URL參數
					boolean allURLParameterMatch = true;
					JsonNode urlParameterNode = requestNode.path("urlParameter");
					if (!urlParameterNode.isMissingNode()) {
						Iterator<String> urlParameterIterator = urlParameterNode.fieldNames();
						while (urlParameterIterator.hasNext()) {
							String urlParameterName = urlParameterIterator.next();
							String urlParameterValue = urlParameterNode.path(urlParameterName).textValue();

							boolean urlParameterMatch = false;

							Map<String, String[]> reqMap = request.getParameterMap();
							for (String key : reqMap.keySet()) {
								String reqUrlParameterName = key;
								String reqUrlParameterValue = reqMap.get(key)[0];

								if (urlParameterName.trim().equals(reqUrlParameterName.trim())
										&& Pattern.compile(urlParameterValue.trim())
												.matcher(reqUrlParameterValue.trim()).matches()) {
									urlParameterMatch = true;
									break;
								}
							}

							if (!urlParameterMatch) {
								allURLParameterMatch = false;
								break;
							}
						}
						if (!allURLParameterMatch) {
							continue;
						}
					}

					// 檢查 applicationJson
					boolean allApplicationJsonMatch = true;
					String applicationJson = request.getReader().lines().collect(Collectors.joining());
					JsonNode reqApplicationJsonNode = new ObjectMapper().readTree(applicationJson);
					JsonNode applicationJsonNode = requestNode.path("applicationJson");
					if (!applicationJsonNode.isMissingNode()) {
						Iterator<String> applicationJsonIterator = applicationJsonNode.fieldNames();
						while (applicationJsonIterator.hasNext()) {
							String applicationJsonName = applicationJsonIterator.next();
							String applicationJsonValue = applicationJsonNode.path(applicationJsonName).textValue();

							boolean applicationJsonMatch = false;

							Iterator<String> reqMap = reqApplicationJsonNode.fieldNames();
							while (reqMap.hasNext()) {
								String reqUrlParameterName = reqMap.next();
								String reqUrlParameterValue = reqApplicationJsonNode.path(reqUrlParameterName)
										.textValue();

								if (applicationJsonName.trim().equals(reqUrlParameterName.trim())
										&& Pattern.compile(applicationJsonValue.trim())
												.matcher(reqUrlParameterValue.trim()).matches()) {
									applicationJsonMatch = true;
									break;
								}
							}

							if (!applicationJsonMatch) {
								allApplicationJsonMatch = false;
								break;
							}
						}
						if (!allApplicationJsonMatch) {
							continue;
						}
					}

					// 延遲回應
					int delay = requestNode.path("delay").intValue(); // 使用指定的延遲回應時間
					if (delay <= 0) { // 使用預設的延遲回應時間
						delay = rootNode.path("default").path("delay").intValue();
					}

					if (delay > 0) {
						try {
							Thread.sleep(delay);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
						}
					}
				}

				{ // 設定回應
					JsonNode responseNode = itemNode.path("response"); // 使用指定的回應
					if (!responseNode.isMissingNode()) {
						setResponse(response, responseNode);
						return true;
					}

					JsonNode successNode = rootNode.path("default").path("response").path("success"); // 使用預設的回應
					if (!successNode.isMissingNode()) {
						setResponse(response, successNode);
						return true;
					}

					response.setStatus(200); // 假如沒指定的回應也沒有預設的回應, 就一律回傳 200

					return true;
				}
			}
		}

		// 沒有匹配任何請求時, 會去檢查預設的失敗回應
		JsonNode errorNode = rootNode.path("default").path("response").path("error");
		if (!errorNode.isMissingNode()) { // 使用預設的失敗回應
			setResponse(response, errorNode);
			return true;
		}

		return false; // 沒有匹配任何請求
	}

	private void setResponse(HttpServletResponse response, JsonNode responseNode) throws IOException {
		// 設定 status
		JsonNode statusNode = responseNode.path("status");
		if (!statusNode.isMissingNode()) {
			int status = statusNode.intValue();
			response.setStatus(status);
		}

		// 設定 header
		JsonNode headerNode = responseNode.path("header");
		if (!headerNode.isMissingNode()) {
			Iterator<String> headerNameIterator = headerNode.fieldNames();
			while (headerNameIterator.hasNext()) {
				String headerName = headerNameIterator.next();
				String headerValue = headerNode.path(headerName).textValue();
				response.addHeader(headerName, headerValue);
			}
		}

		// 設定 body
		JsonNode bodyNode = responseNode.path("body");
		if (!bodyNode.isMissingNode()) {
			PrintWriter printWriter = response.getWriter();
			printWriter.append(bodyNode.textValue());
		}
	}
}
