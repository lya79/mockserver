package com.lya79.mock.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lya79.mock.controller.MemberDao;
import com.lya79.mock.controller.QueryResult;

import lombok.AllArgsConstructor;
import lombok.Data;

@Component
public class SqlUtil implements IMockUtil {

	@Autowired
	private MemberDao memberDao;

	// 回傳給客戶端的內容格式
	enum EFormatter {
		JSON, XML
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
		System.out.println("請求要訪問的 table: " + tableName);

		// 檢查客戶端請求指定的 tableName是否存在
		try {
			if (!memberDao.isExistTableName(tableName)) {
				System.out.println("無法匹配任何 table: " + tableName);
				return false;
			}
		} catch (Exception e) {
			System.err.println("外部請求錯誤或是sql執行錯誤: " + e.toString());
			return false;
		}

		// TODO 要提供一個方法讓用戶知道有哪一些sql api可以操作, 例如: controller提供 api可以列出總共有哪些 api和 那些表

		try {
			String reqMethod = request.getMethod().toUpperCase();
			switch (reqMethod) { // 判斷客戶端請求
			case "GET": // 查詢
				Map<String, String> parameterMap = getUrlParameter(request); // 取出 url參數, 選項
				QueryResult queryResult = memberDao.query(tableName, parameterMap);

				@Data
				@AllArgsConstructor
				class Response {
					public List<Map<String, Object>> columnType; // 欄位型態, key:欄位名稱, value:型態
					public List<Map<String, Object>> columnData; // 資料, key:欄位名稱, value:欄位值
				}

				Response resource = new Response(queryResult.getColumnInfo(), queryResult.getColumnData());
				setResponseByQuery(response, resource, getFormatter(request));
				return true; // content-type=application/json; charset=utf-8
			case "POST": // 新增
				// POST /{tableName} 欄位資料放在body內用json格式
				break;
			case "PUT": // 覆蓋資料
				// PUT /{tableName}/ 欄位資料放在body內用json格式
				break;
			case "PATCH": // 更新部分
				// PATCH /{tableName}/ 欄位資料放在body內用json格式
				break;
			case "DELETE": // 刪除
				// DELETE /{tableName}
				break;
			default:
				return false;
			}
		} catch (Exception e) {
			System.err.println("外部請求錯誤或是sql執行錯誤: " + e.toString()); // TODO json回傳給客戶端
			return false;
		}

		return false;
	}

	private EFormatter getFormatter(HttpServletRequest request) {
//		application/xhtml+xml ：XHTML格式
//		application/xml     ： XML数据格式
//		application/atom+xml  ：Atom XML聚合格式
//		application/json    ： JSON数据格式
//		application/pdf       ：pdf格式
		
//		request.getContentType();
		System.out.println("getContentType: "+request.getContentType());

		return EFormatter.JSON;
	}

	private void setResponseByQuery(HttpServletResponse response, Object resource, EFormatter formatter)
			throws IOException {
		response.setStatus(200);

		switch (formatter) {
		case JSON:
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode jsonNodes = objectMapper.valueToTree(resource);
			String jsonStr = objectMapper.writeValueAsString(jsonNodes);
			response.setContentLength(jsonStr.length());

			PrintWriter out = response.getWriter();
			out.print(jsonStr);
			out.flush();
			break;
		case XML: // TODO 輸出 xml

			break;
		default:
			break;
		}

	}

	private Map<String, String> getUrlParameter(HttpServletRequest request) {
		Map<String, String> result = new HashMap<String, String>();

		Map<String, String[]> reqMap = request.getParameterMap();
		for (String key : reqMap.keySet()) {
			result.put(key, reqMap.get(key)[0]);
		}

		return result;
	}
}

//{
//	  "columnType": [
//	    {
//	      "name": "name",
//	      "type": "vchar"
//	    },
//	    {
//	      "name": "deptid",
//	      "type": "int"
//	    },
//	    {
//	      "name": "salary",
//	      "type": "int"
//	    }
//	  ],
//	  "data": [
//	    {
//	      "name": "name",
//	      "value": "hello2"
//	    },
//	    {
//	      "name": "deptid",
//	      "value": 20
//	    },
//	    {
//	      "name": "salary",
//	      "value": 2000
//	    }
//	  ]
//	}

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
