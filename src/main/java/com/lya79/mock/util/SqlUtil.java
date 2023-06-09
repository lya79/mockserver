package com.lya79.mock.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

@Component
public class SqlUtil implements IMockUtil {

	@Override
	public boolean handler(HttpServletRequest request, HttpServletResponse response) {

		// input: 連線到一個資料庫
		//
		// 找出全部 table
		// for loop 每一張 table{
		//    找出每欄位名稱和型態
		//    找出總共有多少筆紀錄
		//    找出的資訊轉換成物件暫存起來
		//	  備註: 儲存成物件後會暫存到類別變數, 下次在使用時會先檢查 table有沒有更新, 如果沒更新就使用暫存的就好, (參考 sql 1)
		// }
		// 
		// output: 會得到一個 HashSet的物件集合存放每個 table的物件
		
		// input: HttpServletRequest 
		// 找出 request的 method和 url
		// 判斷客戶端要執行的行為(參考 restful api行為) 
		// if 行為是 GET { // 代表要做資料查詢
		//    使用url判斷查詢哪一張表
		//    使用url和參數判斷要外帶入那些條件
		//    執行sql語法得到n筆紀錄
		//    將紀錄轉換成json
		//    回傳json內容
		// }else ...{
		//    ...
		// }
		// 備註: api設計參考 51-69行的範例 API
		
		return false;
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

