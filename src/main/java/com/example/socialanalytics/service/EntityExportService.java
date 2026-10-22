package com.example.socialanalytics.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.socialanalytics.entity.ImportLog;
import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.entity.PlatformStats;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.excel.ReflectiveExcelWriter;
import com.example.socialanalytics.repository.ImportLogRepository;
import com.example.socialanalytics.repository.JobRunRepository;
import com.example.socialanalytics.repository.PlatformStatsRepository;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EntityExportService {

	public static final int PAGE_SIZE = 1_000;


	public record ExportFile(String fileName, byte[] content, int rows, boolean truncated) {
	}

	private record Target<T>(Class<T> type, JpaRepository<T, Long> repository) {
	}

	private final Map<String, Target<?>> targets = new LinkedHashMap<>();

	private final EntityManager entityManager;

	private final int maxRows;

	public EntityExportService(PostRepository posts, SocialMetricRepository metrics, JobRunRepository jobRuns,
			ImportLogRepository importLogs, PlatformStatsRepository platformStats, EntityManager entityManager,
			@Value("${app.export.max-rows:100000}") int maxRows) {
		targets.put("posts", new Target<>(Post.class, posts));
		targets.put("metrics", new Target<>(SocialMetric.class, metrics));
		targets.put("job-runs", new Target<>(JobRun.class, jobRuns));
		targets.put("import-logs", new Target<>(ImportLog.class, importLogs));
		targets.put("platform-stats", new Target<>(PlatformStats.class, platformStats));
		this.entityManager = entityManager;
		this.maxRows = maxRows;
	}

	public Set<String> exportableEntities() {
		return targets.keySet();
	}

	@Transactional(readOnly = true)
	public ExportFile export(String entity) {
		Target<?> target = targets.get(entity);
		if (target == null) {
			throw new IllegalArgumentException("Cannot export '" + entity + "'. Available: " + targets.keySet());
		}
		return export(entity, target);
	}

	private <T> ExportFile export(String entity, Target<T> target) {
		long start = System.currentTimeMillis();
		try (ReflectiveExcelWriter<T> writer = new ReflectiveExcelWriter<>(target.type(), entity)) {
			boolean truncated = false;
			int pageNumber = 0;
			Page<T> page;
			do {
				page = target.repository().findAll(PageRequest.of(pageNumber++, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));
				List<T> rows = page.getContent();
				int room = maxRows - writer.rowCount();
				writer.append(rows.size() > room ? rows.subList(0, room) : rows);
				entityManager.clear();
				truncated = writer.rowCount() >= maxRows && (page.hasNext() || rows.size() > room);
			}
			while (page.hasNext() && !truncated);
			byte[] content = writer.toBytes();
			log.info("Exported {} {} rows in {}ms{}", writer.rowCount(), entity, System.currentTimeMillis() - start,
					truncated ? " (truncated)" : "");
			return new ExportFile(entity + "-" + LocalDate.now() + ".xlsx", content, writer.rowCount(), truncated);
		}
	}

}
