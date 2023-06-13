package com.lya79.mock.controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemberDao { // TODO 多線呈時候是否會贈成資料不同步問題?

	@Autowired
	private JdbcTemplate jdbcTemplate;

//	public void addMember(MemberAccount memberAccount) {
//		System.out.println("EXCUTE INSERT staffinfo");
//		jdbcTemplate.update(
//				"INSERT INTO staffinfo(PASSWORD, EMAIL, ADDRESS, CELLPHONE, CREATE_DATE) " + "VALUES (?,?,?,?,NOW())",
//				memberAccount.getPassword(), memberAccount.getEmail(), memberAccount.getAddress(),
//				memberAccount.getCellphone());
//	}

	// 查詢整張表回傳全部紀錄 GET /{tableName} // 回傳全部紀錄, 排序預設 ORDER BY 主鍵 ASC
	// 查詢指定條件回傳 n筆紀錄 GET /{tableName}?{limit=2&offset=0} //分頁功能, 參數是選項可不帶
	// 查詢指定紀錄 GET /{tableName}?{name=aaa&size=4} // 可以帶入欄位作為參數進行查詢
	public QueryResult query(String tableName, Map<String, String> parameterMap) throws Exception {
		Map<String, String> columnTypeMap = new HashMap<String, String>();// 表內的每個欄位名稱和類型
		List<Map<String, Object>> columnInfo = new LinkedList<Map<String, Object>>();
		;
		String orderByColumn = null; // 用來 order by的欄位名稱
		{
			StringBuilder sb = new StringBuilder();
			sb.append("select COLUMN_NAME ,DATA_TYPE");
			sb.append(" from INFORMATION_SCHEMA.Columns");
			sb.append(" where Table_Name = '").append(tableName).append("'");

			String sql = sb.toString();
//			System.out.println( "sql: " + sql);

			List<Map<String, Object>> tmpColumnInfo = jdbcTemplate.queryForList(sql);
			for (Map<String, Object> row : tmpColumnInfo) {
				String columnName = (String) row.get("COLUMN_NAME");
				String dataType = (String) row.get("DATA_TYPE");
				columnTypeMap.put(columnName, dataType);
//				System.out.println("column: " + columnName + " > " + dataType);
				if (orderByColumn == null) { // 將第一個欄位當成 order by條件
					orderByColumn = columnName;
				}

				Map<String, Object> map = new HashMap<String, Object>();
				columnInfo.add(map);
				map.put("name", columnName);
				map.put("type", dataType);
			}
		}

		int limit = parseInt(parameterMap.get("limit")); // 輸出 n筆
		int offset = parseInt(parameterMap.get("offset")); // 把查詢到的資料再略過前 n筆

		StringBuilder sb = new StringBuilder();
		sb.append("select *");
		sb.append(" from ").append(tableName);

		int count = parameterMap.size();
		for (Entry<String, String> entry : parameterMap.entrySet()) {
			String key = entry.getKey();
			if (key.equals("limit") || key.equals("offset")) { // 扣除 limit和 offset
				count -= 1;
			}
		}

//		System.out.println("count: " + count);
		if (count > 0) {
			sb.append(" where ");

			for (Entry<String, String> entry : parameterMap.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (key.equals("limit") || key.equals("offset")) {
					continue;
				}

				sb.append(key).append("=");

				/**
				 * TODO 要依照不同類型去做特別處理, 目前先暫時只有判斷字串和非字串
				 * 
				 * 數值型態 TINYINT SMALLINT MEDIUMINT INT INTEGER BIGINT FLOAT DOUBLE DECIMAL
				 * 日期和時間型態 DATE TIME YEAR DATETIME TIMESTAMP 字串型態 CHAR VARCHAR TINYBLOB TINYTEXT
				 * BLOB TEXT MEDIUMBLOB MEDIUMTEXT LONGBLOB LONGTEXT
				 */

				String columnType = columnTypeMap.get(key);
				if (columnType.equalsIgnoreCase("CHAR") || columnType.equalsIgnoreCase("VARCHAR")
						|| columnType.equalsIgnoreCase("TINYBLOB") || columnType.equalsIgnoreCase("TINYTEXT")
						|| columnType.equalsIgnoreCase("BLOB") || columnType.equalsIgnoreCase("TEXT")
						|| columnType.equalsIgnoreCase("MEDIUMBLOB") || columnType.equalsIgnoreCase("MEDIUMTEXT")
						|| columnType.equalsIgnoreCase("LONGBLOB") || columnType.equalsIgnoreCase("LONGTEXT")) {
					// 字串型態
					sb.append("'").append(val).append("'");
				} else {
					// 數值型態
					sb.append(val);
				}

				count -= 1;
				if (count > 0) {
					sb.append(" and ");
				}
			}
		}

		sb.append(" order by ").append(orderByColumn).append(" asc");

		if (limit > 0 || offset > 0) {
			sb.append(" limit ").append(limit);
			sb.append(" offset ").append(offset);
		}

		String sql = sb.toString();
		System.out.println("exec sql: " + sql);

		List<Map<String, Object>> columnData = jdbcTemplate.queryForList(sql);
//		System.out.println("size: " + columnData.size());

		return new QueryResult(columnInfo, columnData);
	}

	// 指定的 table是否存在
	public boolean isExistTableName(String tableName) throws Exception {
		List<String> tableNames;

		String sql = "show tables";

		tableNames = jdbcTemplate.queryForList(sql, String.class);
		if (tableNames == null || tableNames.isEmpty()) {
			return false;
		}

//		System.out.println("size: " + tableNames.size() + ", tables: " + tableNames.toString());

		for (String name : tableNames) {
			if (name.equals(tableName)) {
				return true;
			}
		}

		return false;
	}

	private int parseInt(String str) throws Exception {
		if (str == null || str.trim().equals("")) {
			return 0;
		}

		try {
			int value = Integer.parseInt(str);
			return value;
		} catch (NumberFormatException nfe) {
			throw new Exception("檢查url參數, url參數錯誤: " + str);
		}
	}
}