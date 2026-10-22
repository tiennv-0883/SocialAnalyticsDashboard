package com.example.socialanalytics.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialMetric;
import com.example.socialanalytics.entity.SocialPlatform;

class ReflectiveExcelWriterTest {

	@Test
	void columnsComeFromFieldsOfWholeHierarchyWithGetters() {
		assertThat(ReflectiveExcelWriter.columns(Post.class)).extracting(ReflectiveExcelWriter.Column::header)
			.containsExactly("id", "createdAt", "updatedAt", "platform", "externalId", "url", "title", "content");
	}

	@Test
	void associationIsExportedAsItsId() throws IOException {
		Post post = new Post();
		post.setId(42L);
		SocialMetric metric = new SocialMetric();
		metric.setId(7L);
		metric.setPost(post);
		metric.setLikes(15);
		metric.setCollectedAt(Instant.parse("2026-10-20T08:00:00Z"));

		Sheet sheet = export(SocialMetric.class, List.of(metric));

		Row header = sheet.getRow(0);
		Row row = sheet.getRow(1);
		assertThat(cell(header, "postId")).isNotNegative();
		assertThat(text(row, cell(header, "postId"))).isEqualTo("42");
		assertThat(text(row, cell(header, "likes"))).isEqualTo("15");
	}

	@Test
	void writesEnumsNullsAndManyRows() throws IOException {
		JobRun failed = new JobRun();
		failed.setId(1L);
		failed.setJobName("job");
		failed.setTrigger(JobRun.Trigger.MANUAL);
		failed.setStatus(JobRun.Status.FAILED);
		failed.setStartedAt(Instant.now());

		Sheet sheet = export(JobRun.class, List.of(failed, failed, failed));

		assertThat(sheet.getLastRowNum()).isEqualTo(3);
		Row header = sheet.getRow(0);
		assertThat(text(sheet.getRow(1), cell(header, "status"))).isEqualTo("FAILED");
		assertThat(text(sheet.getRow(1), cell(header, "finishedAt"))).isEmpty();
		assertThat(cell(header, "durationMs")).as("derived getter without field is not a column").isNegative();
	}

	@Test
	void enumOnlyClassHasPlatformColumn() {
		assertThat(ReflectiveExcelWriter.columns(Post.class)).anySatisfy(column -> {
			assertThat(column.header()).isEqualTo("platform");
			assertThat(column.getter().getReturnType()).isEqualTo(SocialPlatform.class);
		});
	}

	private static <T> Sheet export(Class<T> type, List<T> rows) throws IOException {
		byte[] bytes;
		try (ReflectiveExcelWriter<T> writer = new ReflectiveExcelWriter<>(type, "data")) {
			writer.append(rows);
			bytes = writer.toBytes();
		}
		Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
		return workbook.getSheetAt(0);
	}

	private static int cell(Row header, String name) {
		for (int c = 0; c < header.getLastCellNum(); c++) {
			if (name.equals(header.getCell(c).getStringCellValue())) {
				return c;
			}
		}
		return -1;
	}

	private static String text(Row row, int column) {
		return new DataFormatter().formatCellValue(row.getCell(column));
	}

}
