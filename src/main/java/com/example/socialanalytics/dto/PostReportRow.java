package com.example.socialanalytics.dto;

import java.time.Instant;

import com.example.socialanalytics.entity.SocialPlatform;
import com.example.socialanalytics.excel.ExcelColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostReportRow {

	@ExcelColumn(name = "postId", order = 1)
	private Long postId;

	@ExcelColumn(name = "platform", order = 2)
	private SocialPlatform platform;

	@ExcelColumn(name = "externalId", order = 3)
	private String externalId;

	@ExcelColumn(name = "url", order = 4)
	private String url;

	@ExcelColumn(name = "title", order = 5)
	private String title;

	@ExcelColumn(name = "likes", order = 6)
	private Long likes;

	@ExcelColumn(name = "shares", order = 7)
	private Long shares;

	@ExcelColumn(name = "comments", order = 8)
	private Long comments;

	@ExcelColumn(name = "followers", order = 9)
	private Long followers;

	@ExcelColumn(name = "lastCollectedAt (UTC)", order = 10)
	private Instant lastCollectedAt;

}
