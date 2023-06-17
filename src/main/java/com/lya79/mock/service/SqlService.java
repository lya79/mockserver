package com.lya79.mock.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lya79.mock.dao.MysqlDao;
import com.lya79.mock.model.GlobalResponse;
import com.lya79.mock.model.Result;

//TODO 要提供一個方法讓用戶知道有哪一些sql api可以操作, 例如: controller提供 api可以列出總共有哪些 api和 那些表

@Component
public class SqlService implements IMockService {
	private final static Logger logger = LoggerFactory.getLogger(SqlService.class);

	@Autowired
	private MysqlDao mysqlDao;

	// 回傳給客戶端的內容格式
	private enum EFormatter {
		JSON
	}

	@Override
	public boolean handler(HttpServletRequest request, HttpServletResponse response) {
		String reqURLStr = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		if (reqURLStr == null || reqURLStr.trim().equals("")) { // XXX 如果 api有定義在 controller裡面就會拿到 null
			return false;
		}

		// 檢查 uri是否符合規則
		Pattern pattern = Pattern.compile("^/[a-zA-Z0-9]+[/]{0,1}$");
		Matcher matcher = pattern.matcher(reqURLStr);
		if (!matcher.matches()) {
			return false;
		}

		// 找出客戶端請求要訪問的 table
		String tableName = reqURLStr.replace("/", "");
		logger.info("請求要訪問的 table: " + tableName);

		// 檢查客戶端請求指定的 tableName是否存在
		try {
			if (!mysqlDao.isExistTableName(tableName)) {
				logger.info("無法匹配任何 table: " + tableName);
				return false;
			}
		} catch (Exception e) {
			logger.warn("檢查 table錯誤: " + e.toString());
			return false;
		}

		try {
			Result result = null;
			GlobalResponse resp = null;
			Exception ex = null;
			Map<String, Object> map = null;

			String reqMethod = request.getMethod().toUpperCase();
			switch (reqMethod) { // 判斷客戶端請求
			case "GET": // 查詢
				map = getUrlParameter(request);
				break;
			case "POST": // 新增
				map = getBodyParameter(request);
				if (!mysqlDao.create(tableName, map)) {
					ex = new Exception("db寫入失敗");
				}
				break;
			case "PUT": // 覆蓋資料
			case "PATCH": // 更新部分
				map = getBodyParameter(request);
				if (!mysqlDao.update(tableName, getUrlParameter(request), map, (reqMethod.equals("PUT")))) {// PUT情況要要求客戶端帶入完整欄位資料(POST情況)
					ex = new Exception("db更新失敗");
				}
				break;
			case "DELETE": // 刪除
				map = getUrlParameter(request);
				result = mysqlDao.query(tableName, map); // 刪除前先暫存
				if (!mysqlDao.delete(tableName, map)) {
					ex = new Exception("db刪除失敗");
				}
				break;
			default:
				return false;
			}

			if (ex == null) {
				if (result == null) {
					result = mysqlDao.query(tableName, map);
				}
				resp = new GlobalResponse(result.getColumnInfo(), result.getColumnData());
			} else {
				logger.info(ex.toString());
			}

			setResponse(response, resp, getFormatter(request), ex);

			return true;
		} catch (Exception e) {
			logger.warn("外部請求錯誤或是sql執行錯誤: " + e.toString());
			return false;
		}
	}

	private EFormatter getFormatter(HttpServletRequest request) { // XXX 目前先只有提供 json格式回傳
		return EFormatter.JSON;
	}

	private void setResponse(HttpServletResponse response, Object resource, EFormatter formatter, Exception ex)
			throws IOException {

		response.setStatus(ex == null ? 200 : 400);

		switch (formatter) {
		case JSON:
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			ObjectMapper objectMapper = new ObjectMapper();

			String jsonStr;
			if (ex == null) {
				ObjectNode jsonNodes = objectMapper.valueToTree(resource);
				jsonStr = objectMapper.writeValueAsString(jsonNodes);
				response.setContentLength(jsonStr.length());
			} else {
				jsonStr = "{ \"error:\":\"DB操作失敗\"}";
			}

			PrintWriter out = response.getWriter();
			out.print(jsonStr);
			out.flush();

			break;
		default:
			break;
		}

	}

	private Map<String, Object> getUrlParameter(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();

		Map<String, String[]> reqMap = request.getParameterMap();
		for (String key : reqMap.keySet()) {
			result.put(key, reqMap.get(key)[0]);
		}

		return result;
	}

	private Map<String, Object> getBodyParameter(HttpServletRequest request) throws IOException {
		Map<String, Object> result = new HashMap<>();

		String applicationJson = request.getReader().lines().collect(Collectors.joining());
		JsonNode reqApplicationJsonNode = new ObjectMapper().readTree(applicationJson);

		Iterator<Map.Entry<String, JsonNode>> jsonNodes = reqApplicationJsonNode.fields();
		while (jsonNodes.hasNext()) {
			Map.Entry<String, JsonNode> node = jsonNodes.next();
			String key = node.getKey().toString();
			Object val;
			if (node.getValue().isTextual()) {
				val = node.getValue().textValue();
			} else if (node.getValue().isDouble()) {
				val = node.getValue().asDouble();
			} else {
				val = node.getValue().asInt();
			}
			result.put(key, val);
		}

		return result;
	}
}

// sql 1
//查看資料表最後更新時間
//select TABLE_NAME,UPDATE_TIME 
//from information_schema.TABLES 
//where TABLE_SCHEMA=' 数据库名 ' and information_schema.TABLES.TABLE_NAME = ' 表名 ';

//範例 API
//一律使用json回傳,post請求的內容也使用json
//
//GET
//查詢整張表回傳全部紀錄		GET	/{tableName} // 回傳全部紀錄, 排序預設 ORDER BY 主鍵 ASC
//查詢指定條件回傳 n筆紀錄 	GET	/{tableName}?{limit=2&offset=0} //分頁功能, 參數是選項可不帶
//查詢指定紀錄				GET	/{tableName}/{主鍵}/ //主鍵如果是是複合為組成就用 -分隔, 例如 123-apple
//
//POST
//新增一筆紀錄				POST /{tableName} 欄位資料放在body內用json格式
//
//PUT
//覆蓋整筆紀錄全部值			PUT /{tableName}/{主鍵}/ 欄位資料放在body內用json格式
//
//PATCH
//更新某筆紀錄的部分值		PATCH /{tableName}/{主鍵}/ 欄位資料放在body內用json格式
//
//DELETE
//刪除某筆紀錄				DELETE /{tableName}/{主鍵}

// restful api行為
//GET（SELECT）：从服务器取出资源（一项或多项）。
//POST（CREATE）：在服务器新建一个资源。
//PUT（UPDATE）：在服务器更新资源（客户端提供改变后的完整资源）。
//PATCH（UPDATE）：在服务器更新资源（客户端提供改变的属性）。
//DELETE（DELETE）：从服务器删除资源。

// restful api sample
//GET /zoos：列出所有动物园
//POST /zoos：新建一个动物园
//GET /zoos/ID：获取某个指定动物园的信息
//PUT /zoos/ID：更新某个指定动物园的信息（提供该动物园的全部信息）
//PATCH /zoos/ID：更新某个指定动物园的信息（提供该动物园的部分信息）
//DELETE /zoos/ID：删除某个动物园
//GET /zoos/ID/animals：列出某个指定动物园的所有动物
//DELETE /zoos/ID/animals/ID：删除某个指定动物园的指定动物

//200 OK - [GET]：服务器成功返回用户请求的数据，该操作是幂等的（Idempotent）。
//201 CREATED - [POST/PUT/PATCH]：用户新建或修改数据成功。
//202 Accepted - [*]：表示一个请求已经进入后台排队（异步任务）
//204 NO CONTENT - [DELETE]：用户删除数据成功。
//400 INVALID REQUEST - [POST/PUT/PATCH]：用户发出的请求有错误，服务器没有进行新建或修改数据的操作，该操作是幂等的。
//401 Unauthorized - [*]：表示用户没有权限（令牌、用户名、密码错误）。
//403 Forbidden - [*] 表示用户得到授权（与401错误相对），但是访问是被禁止的。
//404 NOT FOUND - [*]：用户发出的请求针对的是不存在的记录，服务器没有进行操作，该操作是幂等的。
//406 Not Acceptable - [GET]：用户请求的格式不可得（比如用户请求JSON格式，但是只有XML格式）。
//410 Gone -[GET]：用户请求的资源被永久删除，且不会再得到的。
//422 Unprocesable entity - [POST/PUT/PATCH] 当创建一个对象时，发生一个验证错误。
//500 INTERNAL SERVER ERROR - [*]：服务器发生错误，用户将无法判断发出的请求是否成功。
