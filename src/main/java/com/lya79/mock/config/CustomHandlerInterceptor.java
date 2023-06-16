package com.lya79.mock.config;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.lya79.mock.service.CsvService;
import com.lya79.mock.service.SqlService;

@Component
public class CustomHandlerInterceptor implements HandlerInterceptor {
	private final static Logger logger = LoggerFactory.getLogger(CustomHandlerInterceptor.class);

	@Value("${app.mock.csv.enable:false}")
	private boolean enableCsvAPI;
	
	@Value("${app.mock.mysql.enable:false}")
	private boolean enableMySQLAPI;

	@Value("${file.upload.url:false}")
	private String uploadFilePath;

	@Autowired
	private CsvService csvService;

	@Autowired
	private SqlService mysqlService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// Controller前執行

		logger.info("查詢 CSV API: " + enableCsvAPI);
		if (enableCsvAPI) { // 啟用 csv api
			File dir = new File(uploadFilePath);
			if (dir.exists()) {
				File[] files = dir.listFiles();
				for (int i = 0; i < files.length; i++) {
					File file = files[i];
					if (!CsvService.isCsvFile(file)) {
						continue;
					}
					csvService.setPath(file.getPath());
					boolean match = csvService.handler(request, response);
					logger.info("CSV API匹配是否成功: " + match + ", 檔案名稱" + file.getPath());
					if (match) {
						return false; // 停止後續處理
					}
				}
			}
		}

		logger.info("查詢 SQL API: " + enableMySQLAPI);
		if (enableMySQLAPI) {// 啟用動態產生 restful的 mysql api
			boolean match = mysqlService.handler(request, response);
			logger.info("SQL API匹配是否成功: " + match);
			if (match) {
				return false; // 停止後續處理
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
}