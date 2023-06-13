package com.lya79.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class MockApplicationTests {

	@Test
	void contextLoads() {

		Pattern pattern = Pattern.compile("^/[a-zA-Z0-9]+[/]{0,1}$"); // TODO 要測試看看

		assert (!pattern.matcher("abc").matches());
		assert (pattern.matcher("/abc").matches());
		assert (!pattern.matcher("abc/").matches());
		assert (pattern.matcher("/abc/").matches());
		assert (!pattern.matcher("/abc//").matches());
		assert (!pattern.matcher("/abc/abc").matches());
		assert (!pattern.matcher("/abc/abc/").matches());
		
		assert (pattern.matcher("abc").matches());
	}

}
