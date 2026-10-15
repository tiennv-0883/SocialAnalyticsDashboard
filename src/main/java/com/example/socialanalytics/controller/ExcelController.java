package com.example.socialanalytics.controller;

import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.socialanalytics.dto.ImportResult;
import com.example.socialanalytics.service.PostExcelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Excel", description = "Import posts from / export report to Excel (.xlsx)")
@RestController
@RequiredArgsConstructor
public class ExcelController {

	static final MediaType XLSX = MediaType
		.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final PostExcelService postExcelService;

	@Operation(summary = "Import posts from an .xlsx file",
			description = "Header row: platform, externalId, url, title, content. Valid rows are saved; "
					+ "invalid or duplicate rows are listed in 'errors'.")
	@PostMapping(value = "/import-posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ImportResult importPosts(@RequestPart("file") MultipartFile file) {
		return postExcelService.importPosts(file);
	}

	@Operation(summary = "Export posts with their latest metrics as .xlsx")
	@GetMapping("/export-report")
	public ResponseEntity<byte[]> exportReport() {
		String filename = "social-report-" + LocalDate.now() + ".xlsx";
		return ResponseEntity.ok()
			.contentType(XLSX)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
			.body(postExcelService.exportReport());
	}

}
