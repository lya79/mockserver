package com.lya79.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class MockApplicationTests {

	@Test
	void contextLoads() {
		ObjectMapper mapper = new ObjectMapper();
		
		String s1 = "{\n"
				+ "    \"employee\":\n"
				+ "    {\n"
				+ "        \"id\": \"1212\",\n"
				+ "        \"fullName\": \"John Miles\",\n"
				+ "        \"age\": 34\n"
				+ "    }\n"
				+ "}";
		
		String s2 = "{   \n"    
				+ "    \"employee\":   \n"
				+ "    {\n"
				+ "        \"id\"     : \"1212\",\n"
				+ "        \"age\": 34,\n"
				+ "        \"fullName\": \"John Miles\"\n"
				+ "    }\n"
				+ "}";
		
		try {
			assertEquals(mapper.readTree(s1), mapper.readTree(s2));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
