package com.example.socialanalytics.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.socialanalytics.dto.PostExcelRow;
import com.example.socialanalytics.dto.PostReportRow;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelMapper.RowResult;
import com.example.socialanalytics.excel.ExcelTestFiles.Formula;

class ExcelMapperTest {

	@Test
	void writeThenReadRoundTrips() {
		List<PostExcelRow> rows = List.of(new PostExcelRow(SocialPlatform.FACEBOOK, "fb-1", "https://fb.com/1", "A", null),
				new PostExcelRow(SocialPlatform.TWITTER, "tw-1", "https://x.com/1", "B", "text"));

		byte[] bytes = ExcelMapper.write(rows, PostExcelRow.class, "Posts");
		List<RowResult<PostExcelRow>> read = ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class);

		assertThat(read).allMatch(RowResult::isSuccess);
		assertThat(read).extracting(RowResult::value).containsExactlyElementsOf(rows);
		assertThat(read).extracting(RowResult::rowNumber).containsExactly(2, 3);
	}

	@Test
	void readMatchesHeadersByNameInAnyOrderAndSkipsBlankRows() throws IOException {
		byte[] bytes = ExcelTestFiles.workbook(new Object[][] { { "URL", "Title", "Platform", "ExternalId" },
				{ "https://fb.com/1", "Hello", "facebook", 12345 }, {}, { "https://x.com/2", null, "TWITTER", "tw-2" } });

		List<RowResult<PostExcelRow>> read = ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class);

		assertThat(read).hasSize(2);
		PostExcelRow first = read.get(0).value();
		assertThat(first.getPlatform()).isEqualTo(SocialPlatform.FACEBOOK);
		assertThat(first.getExternalId()).isEqualTo("12345");
		assertThat(first.getTitle()).isEqualTo("Hello");
		assertThat(read.get(1).value().getTitle()).isNull();
		assertThat(read.get(1).rowNumber()).isEqualTo(4);
	}

	@Test
	void readReportsInvalidEnumPerRow() throws IOException {
		byte[] bytes = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "url" }, { "INSTAGRAM", "ig-1", "u" },
				{ "TWITTER", "tw-1", "u" } });

		List<RowResult<PostExcelRow>> read = ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class);

		assertThat(read.get(0).isSuccess()).isFalse();
		assertThat(read.get(0).error()).contains("platform").contains("INSTAGRAM");
		assertThat(read.get(1).isSuccess()).isTrue();
	}

	@Test
	void readUsesFormulaResultsInsteadOfFormulaText() throws IOException {
		byte[] bytes = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "url", "title" },
				{ "FACEBOOK", new Formula("100+23"), new Formula("\"https://fb.com/\"&B2"), new Formula("UPPER(\"hello\")") } });

		PostExcelRow row = ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class).get(0).value();

		assertThat(row.getExternalId()).isEqualTo("123");
		assertThat(row.getUrl()).isEqualTo("https://fb.com/123");
		assertThat(row.getTitle()).isEqualTo("HELLO");
	}

	@Test
	void readReportsFormulaErrorPerRow() throws IOException {
		byte[] bytes = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "url" },
				{ "FACEBOOK", new Formula("1/0"), "u" }, { "TWITTER", "tw-1", "u" } });

		List<RowResult<PostExcelRow>> read = ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class);

		assertThat(read.get(0).error()).contains("externalId").contains("=1/0").contains("returns an error");
		assertThat(read.get(1).isSuccess()).isTrue();
	}

	@Test
	void readRejectsFileMissingRequiredColumnOnce() throws IOException {
		byte[] bytes = ExcelTestFiles.workbook(new Object[][] { { "platform", "externalId", "title" },
				{ "FACEBOOK", "fb-1", "a" }, { "FACEBOOK", "fb-2", "b" } });

		assertThatThrownBy(() -> ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class))
			.isInstanceOf(ExcelProcessingException.class)
			.hasMessageStartingWith("Missing required columns: [url]");
	}

	@Test
	void readRejectsFileOverRowLimitBeforeMapping() throws IOException {
		Object[][] rows = new Object[5][];
		rows[0] = new Object[] { "platform", "externalId", "url" };
		for (int i = 1; i < rows.length; i++) {
			rows[i] = new Object[] { "FACEBOOK", "fb-" + i, "u" };
		}
		byte[] bytes = ExcelTestFiles.workbook(rows);

		assertThat(ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class, 4)).hasSize(4);
		assertThatThrownBy(() -> ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class, 3))
			.isInstanceOf(ExcelProcessingException.class)
			.hasMessage("Too many rows: at most 3 data rows are allowed");
	}

	@Test
	void readFailsWhenNoKnownHeader() throws IOException {
		byte[] bytes = ExcelTestFiles.workbook(new Object[][] { { "foo", "bar" }, { "1", "2" } });

		assertThatThrownBy(() -> ExcelMapper.read(new ByteArrayInputStream(bytes), PostExcelRow.class))
			.isInstanceOf(ExcelProcessingException.class)
			.hasMessageContaining("No known columns");
	}

	@Test
	void readFailsOnNonExcelContent() {
		assertThatThrownBy(() -> ExcelMapper.read(new ByteArrayInputStream("not excel".getBytes()), PostExcelRow.class))
			.isInstanceOf(ExcelProcessingException.class);
	}

	@Test
	void writeOrdersColumnsAndKeepsInstantAsUtc() {
		Instant collectedAt = Instant.parse("2026-10-15T08:30:00Z");
		PostReportRow row = new PostReportRow(1L, SocialPlatform.FACEBOOK, "fb-1", "u", "t", 10L, 2L, 3L, 100L,
				collectedAt);

		byte[] bytes = ExcelMapper.write(List.of(row), PostReportRow.class, "Report");
		List<RowResult<PostReportRow>> read = ExcelMapper.read(new ByteArrayInputStream(bytes), PostReportRow.class);

		assertThat(read.get(0).value()).isEqualTo(row);
	}

	@Test
	void rejectsClassWithoutColumns() {
		assertThatThrownBy(() -> ExcelMapper.write(List.of("x"), String.class, "S"))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
