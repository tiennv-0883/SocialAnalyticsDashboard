package com.example.socialanalytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.socialanalytics.dto.PostRequest;
import com.example.socialanalytics.dto.PostResponse;
import com.example.socialanalytics.entity.Post;
import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.exception.DuplicateResourceException;
import com.example.socialanalytics.exception.ResourceNotFoundException;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	@Mock
	private PostRepository postRepository;

	@Mock
	private SocialMetricRepository metricRepository;

	@InjectMocks
	private PostService postService;

	private final PostRequest request = new PostRequest(SocialPlatform.FACEBOOK, "fb-1", "https://fb.com/p/1",
			"Title", "Content");

	@Test
	void findAllWithoutPlatformQueriesAllPosts() {
		Pageable pageable = PageRequest.of(0, 10);
		when(postRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(post(1L, "fb-1"))));

		assertThat(postService.findAll(null, pageable).getContent()).extracting(PostResponse::externalId)
			.containsExactly("fb-1");
		verify(postRepository, never()).findByPlatform(any(), any());
	}

	@Test
	void findAllWithPlatformFiltersByPlatform() {
		Pageable pageable = PageRequest.of(0, 10);
		when(postRepository.findByPlatform(SocialPlatform.TWITTER, pageable)).thenReturn(new PageImpl<>(List.of()));

		assertThat(postService.findAll(SocialPlatform.TWITTER, pageable)).isEmpty();
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(postRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> postService.findById(99L)).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("99");
	}

	@Test
	void createSavesNewPost() {
		when(postRepository.existsByPlatformAndExternalId(SocialPlatform.FACEBOOK, "fb-1")).thenReturn(false);
		when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
			Post saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		PostResponse response = postService.create(request);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.url()).isEqualTo("https://fb.com/p/1");
		assertThat(response.title()).isEqualTo("Title");
	}

	@Test
	void createRejectsDuplicate() {
		when(postRepository.existsByPlatformAndExternalId(SocialPlatform.FACEBOOK, "fb-1")).thenReturn(true);

		assertThatThrownBy(() -> postService.create(request)).isInstanceOf(DuplicateResourceException.class);
		verify(postRepository, never()).save(any());
	}

	@Test
	void updateRejectsKeyThatBelongsToAnotherPost() {
		when(postRepository.findById(1L)).thenReturn(Optional.of(post(1L, "fb-old")));
		when(postRepository.existsByPlatformAndExternalId(SocialPlatform.FACEBOOK, "fb-1")).thenReturn(true);

		assertThatThrownBy(() -> postService.update(1L, request)).isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void updateKeepingSameKeySkipsDuplicateCheck() {
		when(postRepository.findById(1L)).thenReturn(Optional.of(post(1L, "fb-1")));

		PostResponse response = postService.update(1L, request);

		assertThat(response.title()).isEqualTo("Title");
		verify(postRepository, never()).existsByPlatformAndExternalId(any(), any());
	}

	@Test
	void deleteRemovesMetricsThenPost() {
		Post post = post(1L, "fb-1");
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		postService.delete(1L);

		verify(metricRepository).deleteByPostId(1L);
		verify(postRepository).delete(post);
	}

	static Post post(Long id, String externalId) {
		Post post = new Post();
		post.setId(id);
		post.setPlatform(SocialPlatform.FACEBOOK);
		post.setExternalId(externalId);
		post.setUrl("https://fb.com/p/" + id);
		return post;
	}

}
