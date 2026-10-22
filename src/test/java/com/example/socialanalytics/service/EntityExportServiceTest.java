package com.example.socialanalytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.service.EntityExportService.ExportFile;

@DataJpaTest
@ActiveProfiles("test")
@Import(EntityExportService.class)
@TestPropertySource(properties = "app.export.max-rows=2")
class EntityExportServiceTest {

	@Autowired
	private EntityExportService exportService;

	@Autowired
	private TestEntityManager em;

	@Test
	void keepsNewestRowsWhenLimitIsReached() throws Exception {
		List<Long> ids = new ArrayList<>();
		for (int i = 1; i <= 3; i++) {
			Post post = new Post();
			post.setPlatform(SocialPlatform.FACEBOOK);
			post.setExternalId("export-" + i);
			post.setUrl("https://fb.com/" + i);
			ids.add(em.persistAndGetId(post, Long.class));
		}
		em.flush();

		ExportFile file = exportService.export("posts");

		assertThat(file.rows()).isEqualTo(2);
		assertThat(file.truncated()).isTrue();
		Sheet sheet = new XSSFWorkbook(new ByteArrayInputStream(file.content())).getSheetAt(0);
		DataFormatter formatter = new DataFormatter();
		assertThat(List.of(formatter.formatCellValue(sheet.getRow(1).getCell(0)),
				formatter.formatCellValue(sheet.getRow(2).getCell(0))))
			.containsExactly(ids.get(2).toString(), ids.get(1).toString());
	}

	@Test
	void underLimitIsNotTruncated() {
		Post post = new Post();
		post.setPlatform(SocialPlatform.TWITTER);
		post.setExternalId("export-only");
		post.setUrl("https://x.com/1");
		em.persist(post);
		em.flush();

		ExportFile file = exportService.export("posts");

		assertThat(file.rows()).isEqualTo(1);
		assertThat(file.truncated()).isFalse();
	}

}
