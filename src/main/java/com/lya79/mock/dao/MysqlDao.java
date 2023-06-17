package com.lya79.mock.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.lya79.mock.model.Result;

//TODO 多線呈時候是否會贈成資料不同步問題?
//TODO 需要支援多個儲存庫?多個資料庫?
//TODO 自動更新欄位時間

@Repository
public class MysqlDao {
	private final static Logger logger = LoggerFactory.getLogger(MysqlDao.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	enum EDataType {
		STRING, INT, DOUBLE
	}

	public boolean delete(String tableName, Map<String, Object> parameterMap) throws Exception {
		List<Map<String, Object>> columnInfo = getColumnInfo(tableName);
		Map<String, EDataType> dataTypeMap = new HashMap<>();

		// 比對客戶端帶來的參數名稱和型態是否符合 table內的欄位
		for (Map<String, Object> map : columnInfo) {
			String columnName = (String) map.get("COLUMN_NAME");
			EDataType dataType = getDataType((String) map.get("DATA_TYPE"));
			dataTypeMap.put(columnName, dataType);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(tableName);

		int count = parameterMap.size();
		sb.append(" where");
		for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();

			sb.append(" ").append(key).append("=");
			if (dataTypeMap.get(key) == EDataType.STRING) {
				sb.append("'").append(val).append("'");
			} else {
				sb.append(val);
			}

			count -= 1;
			if (count > 0) {
				sb.append(" and ");
			}
		}

		String sql = sb.toString();
		logger.info("執行SQL: " + sql);

		try {
			if (jdbcTemplate.update(sql) <= 0) {
				logger.info("寫入table失敗, sql:" + sql);
				return false;
			}
		} catch (DuplicateKeyException e) {
			logger.info("寫入table失敗, 主鍵重複, sql:" + sql);
			return false;
		} catch (Exception e) {
			logger.info("寫入table失敗, sql:" + sql + ", e:" + e.toString());
			return false;
		}

		return true;
	}

	public Result query(String tableName, Map<String, Object> parameterMap) throws Exception {
		List<Map<String, Object>> columnInfo = getColumnInfo(tableName);

		int limit = parseInt((String) parameterMap.get("limit")); // 輸出 n筆
		int offset = parseInt((String) parameterMap.get("offset")); // 把查詢到的資料再略過前 n筆

		StringBuilder sb = new StringBuilder();
		sb.append("select *");
		sb.append(" from ").append(tableName);

		int count = parameterMap.size();
		for (Entry<String, Object> entry : parameterMap.entrySet()) {
			String key = entry.getKey();
			if (key.equals("limit") || key.equals("offset")) { // 扣除 limit和 offset
				count -= 1;
			}
		}

		Map<String, String> columnTypeMap = getColumnTypeByColumnInfo(columnInfo);

		if (count > 0) {
			sb.append(" where ");

			for (Entry<String, Object> entry : parameterMap.entrySet()) {
				String key = entry.getKey();
				Object val = entry.getValue();

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
				if (getDataType(columnType) == EDataType.INT || getDataType(columnType) == EDataType.DOUBLE) {// 數值型態
					sb.append(val);
				} else { // 字串型態
					sb.append("'").append(val).append("'");
				}

				count -= 1;
				if (count > 0) {
					sb.append(" and ");
				}
			}
		}

		sb.append(" order by ").append(getOrderByByColumnInfo(columnInfo)).append(" asc");

		if (limit > 0 || offset > 0) {
			sb.append(" limit ").append(limit);
			sb.append(" offset ").append(offset);
		}

		String sql = sb.toString();
		logger.info("執行SQL: " + sql);

		List<Map<String, Object>> columnData = jdbcTemplate.queryForList(sql);

		return new Result(columnInfo, columnData);
	}

	public boolean update(String tableName, Map<String, Object> urlParameterMap, Map<String, Object> bodyParameterMap,
			boolean putMethod) throws Exception {
		List<Map<String, Object>> columnInfo = getColumnInfo(tableName);
		Map<String, EDataType> dataTypeMap = new HashMap<>();

		// 比對客戶端帶來的參數名稱和型態是否符合 table內的欄位
		for (Map<String, Object> map : columnInfo) {
			String columnName = (String) map.get("COLUMN_NAME");
			EDataType dataType = getDataType((String) map.get("DATA_TYPE"));
			dataTypeMap.put(columnName, dataType);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(tableName);

		int count = bodyParameterMap.size();
		sb.append(" set");
		for (Map.Entry<String, Object> entry : bodyParameterMap.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();

			sb.append(" ").append(key).append("=");
			if (dataTypeMap.get(key) == EDataType.STRING) {
				sb.append("'").append(val).append("'");
			} else {
				sb.append(val);
			}

			count -= 1;
			if (count > 0) {
				sb.append(", ");
			}
		}

		count = urlParameterMap.size();
		sb.append(" where");
		for (Map.Entry<String, Object> entry : urlParameterMap.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();

			sb.append(" ").append(key).append("=");
			if (dataTypeMap.get(key) == EDataType.STRING) {
				sb.append("'").append(val).append("'");
			} else {
				sb.append(val);
			}

			count -= 1;
			if (count > 0) {
				sb.append(" and ");
			}
		}

		String sql = sb.toString();
		logger.info("執行SQL: " + sql);

		try {
			if (jdbcTemplate.update(sql) <= 0) {
				logger.info("寫入table失敗, sql:" + sql);
				return false;
			}
		} catch (DuplicateKeyException e) {
			logger.info("寫入table失敗, 主鍵重複, sql:" + sql);
			return false;
		} catch (Exception e) {
			logger.info("寫入table失敗, sql:" + sql + ", e:" + e.toString());
			return false;
		}

		return true;
	}

	public boolean create(String tableName, Map<String, Object> parameterMap) throws Exception {
		List<Map<String, Object>> columnInfo = getColumnInfo(tableName);

		ArrayList<String> columnNameList = new ArrayList<>();
		ArrayList<Object> columnValList = new ArrayList<>();
		ArrayList<Boolean> autoIncrementList = new ArrayList<>();

		// 比對客戶端帶來的參數名稱和型態是否符合 table內的欄位
		for (Map<String, Object> map : columnInfo) {
			String columnName = (String) map.get("COLUMN_NAME");
			EDataType dataType = getDataType((String) map.get("DATA_TYPE"));
			String extra = (String) map.get("EXTRA");

			Object val = parameterMap.get(columnName);
			if (val == null) {
				if (extra.equalsIgnoreCase("auto_increment")) {
					continue;
				}
				logger.info("請求參數錯誤, 缺少欄位:" + columnName);
				return false;
			}

			columnNameList.add(columnName);
			columnValList.add(val);
			autoIncrementList.add(extra.equalsIgnoreCase("auto_increment"));

			logger.info(columnName + ", " + extra);

			switch (dataType) {
			case INT:
			case DOUBLE:
				if (!(val instanceof Integer) && !(val instanceof Double)) {
					logger.info("請求參數錯誤, 參數型態錯誤, 參數: " + columnName + ", val:" + val);
					return false;
				}
				break;
			case STRING:
			default:
				break;
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(tableName);
		sb.append(" (");
		{
			int count = columnNameList.size();
			for (int i = 0; i < columnNameList.size(); i++) {
				String columnName = columnNameList.get(i);

				count -= 1;

				if (autoIncrementList.get(i)) {
					continue;
				}

				sb.append(" ").append(columnName);

				if (count > 0) {
					sb.append(", ");
				}
			}
		}
		sb.append(" )");
		sb.append(" values");
		sb.append(" (");
		{
			int count = columnValList.size();
			for (int i = 0; i < columnValList.size(); i++) {
				Object columnVal = columnValList.get(i);

				count -= 1;

				if (autoIncrementList.get(i)) {
					continue;
				}

				if (columnVal instanceof Integer) {
					sb.append(" ").append((Integer) columnVal);
				} else {
					sb.append(" '").append((String) columnVal).append("'");
				}

				if (count > 0) {
					sb.append(", ");
				}
			}
		}
		sb.append(" )");

		String sql = sb.toString();
		logger.info("執行SQL: " + sql);

		try {
			if (jdbcTemplate.update(sql) <= 0) {
				logger.info("寫入table失敗, sql:" + sql);
				return false;
			}
		} catch (DuplicateKeyException e) {
			logger.info("寫入table失敗, 主鍵重複, sql:" + sql);
			return false;
		} catch (Exception e) {
			logger.info("寫入table失敗, sql:" + sql + ", e:" + e.toString());
			return false;
		}

		return true;
	}

	private List<Map<String, Object>> getColumnInfo(String tableName) throws DataAccessException {
		StringBuilder sb = new StringBuilder();
		sb.append("select");
		sb.append(" COLUMN_NAME, DATA_TYPE, COLUMN_KEY, EXTRA");
		sb.append(" from INFORMATION_SCHEMA.COLUMNS");
		sb.append(" where TABLE_NAME='").append(tableName).append("'");

		String sql = sb.toString();
		logger.info("執行 sql: " + sql);

		List<Map<String, Object>> columnInfo = jdbcTemplate.queryForList(sql);

		return columnInfo;
	}

	private EDataType getDataType(String dataType) {
		if (dataType.equalsIgnoreCase("TINYINT") || dataType.equalsIgnoreCase("SMALLINT")
				|| dataType.equalsIgnoreCase("MEDIUMINT") || dataType.equalsIgnoreCase("INT")
				|| dataType.equalsIgnoreCase("INTEGER") || dataType.equalsIgnoreCase("BIGINT")
				|| dataType.equalsIgnoreCase("FLOAT") || dataType.equalsIgnoreCase("DOUBLE")
				|| dataType.equalsIgnoreCase("DECIMAL")) {
			return EDataType.INT;
		}

		if (dataType.equalsIgnoreCase("FLOAT") || dataType.equalsIgnoreCase("DOUBLE")
				|| dataType.equalsIgnoreCase("DECIMAL")) {
			return EDataType.DOUBLE;
		}

		if (dataType.equalsIgnoreCase("CHAR") || dataType.equalsIgnoreCase("VARCHAR")
				|| dataType.equalsIgnoreCase("TINYBLOB") || dataType.equalsIgnoreCase("TINYTEXT")
				|| dataType.equalsIgnoreCase("BLOB") || dataType.equalsIgnoreCase("TEXT")
				|| dataType.equalsIgnoreCase("MEDIUMBLOB") || dataType.equalsIgnoreCase("MEDIUMTEXT")
				|| dataType.equalsIgnoreCase("LONGBLOB") || dataType.equalsIgnoreCase("LONGTEXT")) {
			return EDataType.STRING;
		}

		return EDataType.STRING; // XXX 先暫時設定成字串
	}

	private Map<String, String> getColumnTypeByColumnInfo(List<Map<String, Object>> columnInfo) {
		Map<String, String> columnTypeMap = new HashMap<>();

		for (Map<String, Object> map : columnInfo) {
			columnTypeMap.put((String) map.get("COLUMN_NAME"), (String) map.get("DATA_TYPE"));

		}

		return columnTypeMap;
	}

	private String getOrderByByColumnInfo(List<Map<String, Object>> columnInfo) {
		for (Map<String, Object> map : columnInfo) {
			return (String) map.get("COLUMN_NAME");

		}
		return null; // 不會發生
	}

	// 指定的 table是否存在
	public boolean isExistTableName(String tableName) throws Exception {
		List<String> tableNames;

		String sql = "show tables";
		logger.info("執行SQL: " + sql);

		tableNames = jdbcTemplate.queryForList(sql, String.class);
		if (tableNames == null || tableNames.isEmpty()) {
			return false;
		}

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
			throw new Exception("無法將字串轉換成數值: " + str);
		}
	}
}