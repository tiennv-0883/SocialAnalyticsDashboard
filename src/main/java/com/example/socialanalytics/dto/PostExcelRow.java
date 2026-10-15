package com.example.socialanalytics.dto;

import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelColumn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostExcelRow {

	@NotNull
	@ExcelColumn(name = "platform", order = 1)
	private SocialPlatform platform;

	@NotBlank
	@Size(max = 100)
	@ExcelColumn(name = "externalId", order = 2)
	private String externalId;

	@NotBlank
	@Size(max = 500)
	@ExcelColumn(name = "url", order = 3)
	private String url;

	@Size(max = 255)
	@ExcelColumn(name = "title", order = 4)
	private String title;

	@ExcelColumn(name = "content", order = 5)
	private String content;

	public PostRequest toRequest() {
		return new PostRequest(platform, externalId, url, title, content);
	}

}
