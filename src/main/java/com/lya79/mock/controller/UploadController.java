package com.lya79.mock.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
//@Slf4j
public class UploadController {

//	@Value("${file.upload.path}")
//	private String path;

	@Value("${file.upload.url}")
	private String uploadFilePath;

	@GetMapping("/")
	public String uploadPage() {
		return "upload";
	}

	@PostMapping("/upload")
	@ResponseBody
	public String upload(@RequestPart MultipartFile[] files) throws IOException {
//		StringBuffer message = new StringBuffer();
//
//		for (MultipartFile file : files) {
//			String fileName = file.getOriginalFilename();
//			String filePath = path + fileName;
//			System.out.println("dest path:" + filePath);
//
//			InputStream is = file.getInputStream();
//			File dest = new File(filePath);
//			Files.copy(is, dest.toPath());
//			is.close();
//
//			message.append("Upload file success : " + dest.getAbsolutePath()).append("<br>");
//		}
//		
//		return message.toString();

		for (int i = 0; i < files.length; i++) {
			String fileName = files[i].getOriginalFilename();
			File dest = new File(uploadFilePath + '/' + fileName);
			if (!dest.getParentFile().exists()) {
				dest.getParentFile().mkdirs();
			}
			try {
				files[i].transferTo(dest); // 覆蓋檔案
			} catch (Exception e) {
				return "error upload";
			}
		}

		return "ok upload";
	}

	@ResponseBody
	@RequestMapping(value = { "/list" }, method = { RequestMethod.GET }, produces = "application/json;charset=UTF-8")
	public String list(HttpServletResponse response) { // 註冊新帳號
		// url ex: http://localhost:8080/list
		// output:
//		    [ {
//			  "filename" : "hello2.png",
//			  "lastModified" : "Mon Jun 05 14:25:39 CST 2023"
//			}, {
//			  "filename" : "Untitled-1.txt",
//			  "lastModified" : "Mon Jun 05 14:53:58 CST 2023"
//			} ]

		HashMap<String, String> map = new HashMap<>();

		File dir = new File(uploadFilePath);
		if (!dir.exists()) {
			return "";
		}

		class fileTmp {
			public String filename;
			public String lastModified;
		}

		fileTmp[] fileTmps = new fileTmp[dir.listFiles().length];

		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			fileTmps[i] = new fileTmp();

			File file = files[i];
			fileTmps[i].filename = file.getName();
			fileTmps[i].lastModified = new Date(file.lastModified()).toString();
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.valueToTree(fileTmps);

		return node.toPrettyString();
	}

	@RequestMapping("/download")
	public String download(HttpServletResponse response, @RequestParam("filename") String fileName) {
		// url ex: http://localhost:8080/download?filename=vvvvv.txt

		File file = new File(uploadFilePath + '/' + fileName);
		if (!file.exists()) {
			return "下载文件不存在";
		}

		response.reset();
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setContentLength((int) file.length());
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
			byte[] buff = new byte[1024];
			OutputStream os = response.getOutputStream();
			int i = 0;
			while ((i = bis.read(buff)) != -1) {
				os.write(buff, 0, i);
				os.flush();
			}
		} catch (IOException e) {
			return "error download";
		}
		return "ok download";
	}
}