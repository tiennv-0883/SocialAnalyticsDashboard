package com.example.socialanalytics.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.socialanalytics.dto.ImportResult;
import com.example.socialanalytics.dto.PostExcelRow;
import com.example.socialanalytics.dto.PostReportRow;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.excel.ExcelMapper;
import com.example.socialanalytics.excel.ExcelMapper.RowResult;
import com.example.socialanalytics.excel.ExcelProcessingException;
import com.example.socialanalytics.exception.DuplicateResourceException;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostExcelService {

	private final PostService postService;

	private final PostRepository postRepository;

	private final SocialMetricRepository metricRepository;

	private final Validator validator;

	/**
	 * Imports posts row by row: valid rows are saved, invalid or duplicate rows are
	 * reported back without stopping the whole import.
	 */
	public ImportResult importPosts(MultipartFile file) {
		validateFile(file);
		List<RowResult<PostExcelRow>> rows;
		try (InputStream input = file.getInputStream()) {
			rows = ExcelMapper.read(input, PostExcelRow.class);
		}
		catch (IOException ex) {
			throw new ExcelProcessingException("Cannot read uploaded file", ex);
		}

		List<ImportResult.RowError> errors = new ArrayList<>();
		int imported = 0;
		for (RowResult<PostExcelRow> row : rows) {
			String error = row.isSuccess() ? save(row.value()) : row.error();
			if (error == null) {
				imported++;
			}
			else {
				errors.add(new ImportResult.RowError(row.rowNumber(), error));
			}
		}
		log.info("Imported posts from '{}': {} ok, {} failed", file.getOriginalFilename(), imported, errors.size());
		return new ImportResult(rows.size(), imported, errors.size(), errors);
	}

	@Transactional(readOnly = true)
	public byte[] exportReport() {
		Map<Long, SocialMetric> latestByPost = metricRepository.findLatestPerPost()
			.stream()
			.collect(Collectors.toMap(metric -> metric.getPost().getId(), Function.identity(),
					(a, b) -> a.getId() > b.getId() ? a : b));
		List<PostReportRow> rows = postRepository.findAll()
			.stream()
			.sorted(Comparator.comparing(Post::getId))
			.map(post -> toReportRow(post, latestByPost.get(post.getId())))
			.toList();
		return ExcelMapper.write(rows, PostReportRow.class, "Report");
	}

	private String save(PostExcelRow row) {
		Set<ConstraintViolation<PostExcelRow>> violations = validator.validate(row);
		if (!violations.isEmpty()) {
			return violations.stream()
				.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
				.sorted()
				.collect(Collectors.joining("; "));
		}
		try {
			postService.create(row.toRequest());
			return null;
		}
		catch (DuplicateResourceException ex) {
			return ex.getMessage();
		}
	}

	private static PostReportRow toReportRow(Post post, SocialMetric metric) {
		return new PostReportRow(post.getId(), post.getPlatform(), post.getExternalId(), post.getUrl(), post.getTitle(),
				metric == null ? null : metric.getLikes(), metric == null ? null : metric.getShares(),
				metric == null ? null : metric.getComments(), metric == null ? null : metric.getFollowers(),
				metric == null ? null : metric.getCollectedAt());
	}

	private static void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ExcelProcessingException("File is empty");
		}
		String name = file.getOriginalFilename();
		if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
			throw new ExcelProcessingException("Only .xlsx files are supported");
		}
	}

}
