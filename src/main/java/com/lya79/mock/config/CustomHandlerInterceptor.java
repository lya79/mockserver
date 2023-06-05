package com.lya79.mock.config;

import java.io.File;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lya79.mock.util.MatcherUtil;

@Component
public class CustomHandlerInterceptor implements HandlerInterceptor {
	@Value("${app.mock.resources.enable:false}")
	private boolean enableResourcesAPI;

	@Autowired
	private MatcherUtil requestMatcher;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// Controller前執行

		if (enableResourcesAPI) {
			String path = "static" + File.separator + "rule.json";
			ClassPathResource classPathResource = new ClassPathResource(path);
			InputStream inputStream = classPathResource.getInputStream();

			JsonNode rootNode = new ObjectMapper().readTree(inputStream);

			requestMatcher.handler(request, response, rootNode);
			return false; // 停止後續處理
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