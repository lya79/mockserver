package com.lya79.mock.config;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.lya79.mock.util.CsvUtil;

@Component
public class CustomHandlerInterceptor implements HandlerInterceptor {
	@Value("${app.mock.csv.enable:false}")
	private boolean enableCsvAPI;

//	@Value("${app.mock.csv.path}")
//	private String csvFilePath;

	@Value("${file.upload.url:false}")
	private String uploadFilePath;

	@Autowired
	private CsvUtil csvUtil;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// Controller前執行

		if (enableCsvAPI) { // 啟用 csv api
			File dir = new File(uploadFilePath);
			if (dir.exists()) {
				File[] files = dir.listFiles();
				for (int i = 0; i < files.length; i++) {
					File file = files[i];
					if (!isCsvFile(file)) {
						continue;
					}
					String csvFilePath = file.getPath();
					boolean match = csvUtil.handler(request, response, csvFilePath);
					if (match) {
						return false; // 停止後續處理
					}
				}
			}
		}

		return true; // 通過攔截器
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
		// Controller處理完後執行
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
		// 整個請求及回應結束後執行
	}

	private boolean isCsvFile(File file) {
		String filename = file.getName();
		String extension = "";
		int idx = filename.lastIndexOf(".");
		if (idx >= 0) {
			extension = filename.substring(idx + 1);
		}
		return extension.equals("csv");
	}
}