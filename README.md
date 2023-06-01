# 使用方式

1. 先把 `rule.json`檔案放到想要讀取的檔案目錄底下

2. 開啟 `MatcherUtil.java`檔案, 修改 `rule.json`檔案路徑
```java
	@Value("file:C:\\Users\\USER\\Desktop\\springboot\\workspace\\rule.json")
	private Resource jsonFile;
```

3.啟動程式測試
`rule.json`裡面已經有先寫一條 API可以做測試.

# 設定配置
* API都設定在 `rule.json`即可, 下面提供一個範例.
* `testCase`、`request`、`method`、`url`這4種設定都是必要設定, 其他設定是選項可刪除.
* `status`一律使用數值型態.
* `method`、`header`、`body`直接給值不能使用正規表示法.
* `url`、`urlParameter的內部參數`、`urlParameter的內部參數`一律都使用正規表示法.

```json
{
  "default":{ // 通用設定(可以移除)
    "delay": 3000, // 全部 API回應都等待多少時間(單位:ms)
    "response":{ 
      "success":{ // API匹配成功回應的內容
        "status": 201,
        "header":{
          "content-type":"application/json; charset=utf-8"
        },
        "body": "\"message\": \"hello mock server\""
      },
      "error":{ // API匹配失敗回應的內容
        "status": 405,
        "header":{
          "content-type":"application/json; charset=utf-8"
        },
        "body": "\"message\": \"Not Found!\""
      }
    }
  },
  "testCase": [ // API個別設定
    {
      "request": {  // 請求條件
        "method": "POST", 
        "url": "^/fruit/[a-zA-Z]{2}[0-9]{4}$",
        "header":{ // header設定
          "content-type":"application/json; charset=utf-8",
          "color":"red" 
        },
        "urlParameter": { // URL帶的參數
          "id": "[a-zA-Z0-9]{6}", 
          "name": "[a-zA-Z0-9]{5,12}"
        },
        "applicationJson": { // application Json參數
          "color": "[a-zA-Z]{3,12}",
          "price": "[0-9]{1,10}"
        },
        "delay": 500 //  API回應等待多少時間(單位:ms)
      },
      "response": {
        "status": 200,
        "header":{
            "content-type": "application/json; charset=utf-8",
            "helloheader":"hello header"
        },
        "body": "\"name\": \"apple\",\"color\": \"red\""
      }
    }
  ]
}
```