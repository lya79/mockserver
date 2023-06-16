package com.lya79.mock.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IMockService {
	public boolean handler(HttpServletRequest request, HttpServletResponse response);
}
