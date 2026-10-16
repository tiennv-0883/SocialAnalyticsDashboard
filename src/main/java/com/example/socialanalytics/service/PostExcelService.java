package com.example.socialanalytics.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.socialanalytics.dto.ImportResult;
import com.example.socialanalytics.dto.PostExcelRow;
import com.example.socialanalytics.dto.PostReportRow;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelMapper;
import com.example.socialanalytics.excel.ExcelMapper.RowResult;
import com.example.socialanalytics.excel.ExcelProcessingException;
import com.example.socialanalytics.exception.DuplicateResourceException;
import com.example.socialanalytics.messaging.ImportCompletedEvent;
import com.example.socialanalytics.messaging.ImportEventPublisher;
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

	public static final int MAX_IMPORT_ROWS = 5_000;

	private final PostService postService;

	private final PostRepository postRepository;

	private final SocialMetricRepository metricRepository;

	private final Validator validator;

	private final ImportEventPublisher importEventPublisher;

	/**
	 * Imports posts row by row: valid rows are saved, invalid or duplicate rows are
	 * reported back without stopping the whole import.
	 */
	public ImportResult importPosts(MultipartFile file) {
		validateFile(file);
		List<RowResult<PostExcelRow>> rows;
		try (InputStream input = file.getInputStream()) {
			rows = ExcelMapper.read(input, PostExcelRow.class, MAX_IMPORT_ROWS);
		}
		catch (IOException ex) {
			throw new ExcelProcessingException("Cannot read uploaded file", ex);
		}

		List<ImportResult.RowError> errors = new ArrayList<>();
		Map<SocialPlatform, Integer> importedByPlatform = new EnumMap<>(SocialPlatform.class);
		int imported = 0;
		for (RowResult<PostExcelRow> row : rows) {
			String error = row.isSuccess() ? save(row.rowNumber(), row.value()) : row.error();
			if (error == null) {
				imported++;
				importedByPlatform.merge(row.value().getPlatform(), 1, Integer::sum);
			}
			else {
				errors.add(new ImportResult.RowError(row.rowNumber(), error));
			}
		}
		log.info("Imported posts from '{}': {} ok, {} failed", file.getOriginalFilename(), imported, errors.size());
		importEventPublisher.publish(new ImportCompletedEvent(UUID.randomUUID().toString(), file.getOriginalFilename(),
				rows.size(), imported, errors.size(), importedByPlatform, Instant.now()));
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

	private String save(int rowNumber, PostExcelRow row) {
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
		catch (DataIntegrityViolationException ex) {
			log.warn("Import row {} rejected by database: {}", rowNumber, ex.getMostSpecificCause().getMessage());
			return "Could not save row: violates a database constraint (duplicate post or value too long)";
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
