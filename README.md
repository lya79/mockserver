# 介紹

藉由 csv檔案內的設定可以動態產生 api, 達到預先設定請求格式和回傳內容, 不需要修改程式碼, 並且提供 API可以更新 csv檔案.

# 使用方法
直接啟動程式即可.
> 備註: 參考 `public/sample.csv`內預先設定的範本 API來發送請求.

# 更新 csv檔案
* 方法1: 直接將 csv檔案放置到 public目錄.
* 方法2: 透過網頁打開 localhost:8080可以開啟上傳 csv檔案的畫面.

# csv檔案設定
* csv檔案都存放在 public目錄. 
* 使用 csv格式儲存設定 api.
* 可以有多份 csv檔案

# 備註

 `GET /list`這個 API可以查詢目前有哪些 csv檔案
> 例如: http://localhost:8080/list

 `GET /download`這個 API可以下載指定 csv檔案
> 例如:  http://localhost:8080/download?filename=sample.csv
