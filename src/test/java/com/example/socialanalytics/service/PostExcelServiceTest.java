package com.example.socialanalytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import com.example.socialanalytics.dto.ImportResult;
import com.example.socialanalytics.dto.PostRequest;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelTestFiles;
import com.example.socialanalytics.messaging.ImportCompletedEvent;
import com.example.socialanalytics.messaging.ImportEventPublisher;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class PostExcelServiceTest {

	private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

	private final PostService postService = mock(PostService.class);

	private final ImportEventPublisher publisher = mock(ImportEventPublisher.class);

	private final PostExcelService service = new PostExcelService(postService, mock(PostRepository.class),
			mock(SocialMetricRepository.class), validatorFactory.getValidator(), publisher);

	@AfterEach
	void close() {
		validatorFactory.close();
	}

	@Test
	void databaseErrorFailsOnlyThatRowAndImportContinues() throws Exception {
		when(postService.create(argThat(request -> request != null && request.externalId().equals("fb-2"))))
			.thenThrow(new DataIntegrityViolationException("Duplicate entry 'FACEBOOK-fb-2'"));

		ImportResult result = service.importPosts(file(new Object[][] { { "platform", "externalId", "url" },
				{ "FACEBOOK", "fb-1", "u1" }, { "FACEBOOK", "fb-2", "u2" }, { "FACEBOOK", "fb-3", "u3" } }));

		assertThat(result.imported()).isEqualTo(2);
		assertThat(result.failed()).isEqualTo(1);
		assertThat(result.errors()).singleElement().satisfies(error -> {
			assertThat(error.row()).isEqualTo(3);
			assertThat(error.message()).startsWith("Could not save row");
		});
		verify(postService, times(3)).create(any(PostRequest.class));
		verify(publisher).publish(argThat((ImportCompletedEvent event) -> event.imported() == 2 && event.failed() == 1
				&& event.importedByPlatform().get(SocialPlatform.FACEBOOK) == 2 && event.importId() != null));
	}

	@Test
	void contentLongerThanColumnIsRejectedBeforeSaving() throws Exception {
		String tooLong = "x".repeat(Post.CONTENT_MAX_LENGTH + 1);

		ImportResult result = service.importPosts(file(new Object[][] { { "platform", "externalId", "url", "content" },
				{ "FACEBOOK", "fb-long", "u", tooLong } }));

		assertThat(result.failed()).isEqualTo(1);
		assertThat(result.errors().get(0).message()).startsWith("content size must be between 0 and 16000");
		verify(postService, never()).create(any());
	}

	private static MockMultipartFile file(Object[][] rows) throws Exception {
		return new MockMultipartFile("file", "posts.xlsx", null, ExcelTestFiles.workbook(rows));
	}

}
