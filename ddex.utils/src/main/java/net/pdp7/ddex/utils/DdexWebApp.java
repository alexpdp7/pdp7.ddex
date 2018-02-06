package net.pdp7.ddex.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@SpringBootApplication
@Controller
public class DdexWebApp {

	public static void main(String[] args) {
		SpringApplication.run(DdexWebApp.class, args);
	}

	@PostMapping("/convert_ddex_to_excel")
	public ResponseEntity<Resource> convertDdexToExcel(@RequestParam("ddex") MultipartFile ddex) throws IOException {
		List<Map<String, Object>> table = new DdexToTableConverter().convert(ddex.getInputStream()).collect(Collectors.toList());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new TableToExcel().convert(table, "release").write(out);
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ddex.xls\"")
				.header(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
				.body(new ByteArrayResource(out.toByteArray()));
	}
}