package com.lya79.mock.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GlobalResponse {
	public List<Map<String, Object>> columnInfo; // 欄位型態, key:欄位名稱, value:型態
	public List<Map<String, Object>> columnData; // 資料, key:欄位名稱, value:欄位值
}
