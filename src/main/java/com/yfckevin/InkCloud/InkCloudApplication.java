package com.yfckevin.InkCloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@SpringBootApplication
public class InkCloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(InkCloudApplication.class, args);
	}

	@Bean(name = "picSuffix")
	public SimpleDateFormat picSuffix() {
		SimpleDateFormat picSuffix = new SimpleDateFormat("yyyyMMddHHmmss");
		picSuffix.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
		return picSuffix;
	}
	@Bean(name = "sdf")
	public SimpleDateFormat sdf () {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
		return sdf;
	}

	@Bean
	public ObjectMapper objectMapper(){
		return new ObjectMapper();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
