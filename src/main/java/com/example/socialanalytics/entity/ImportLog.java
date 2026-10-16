package com.example.socialanalytics.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_logs")
public class ImportLog extends BaseEntity {

	@Column(name = "import_id", nullable = false, unique = true, length = 36)
	private String importId;

	@Column(name = "file_name", length = 255)
	private String fileName;

	@Column(name = "total_rows", nullable = false)
	private int totalRows;

	@Column(nullable = false)
	private int imported;

	@Column(nullable = false)
	private int failed;

	@Column(name = "imported_at", nullable = false)
	private Instant importedAt;

}
