package com.lya79.mock.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IMockUtil {
	public boolean handler(HttpServletRequest request, HttpServletResponse response);
}
