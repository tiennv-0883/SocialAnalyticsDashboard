package com.example.socialanalytics.controller;

import java.util.Set;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.socialanalytics.service.EntityExportService;
import com.example.socialanalytics.service.EntityExportService.ExportFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Export", description = "Generic Excel export of entities (Reflection based)")
@RestController
@RequiredArgsConstructor
public class ExportController {

	private final EntityExportService exportService;

	@Operation(summary = "Entities that can be exported")
	@GetMapping("/export")
	public Set<String> exportableEntities() {
		return exportService.exportableEntities();
	}

	@Operation(summary = "Export all rows of an entity as .xlsx (max 100,000 rows)")
	@GetMapping("/export/{entity}")
	public ResponseEntity<byte[]> export(@PathVariable String entity) {
		ExportFile file;
		try {
			file = exportService.export(entity);
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
		}
		return ResponseEntity.ok()
			.contentType(ExcelController.XLSX)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
			.header("X-Export-Rows", String.valueOf(file.rows()))
			.header("X-Export-Truncated", String.valueOf(file.truncated()))
			.body(file.content());
	}

}
