package com.example.socialanalytics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.socialanalytics.entity.ImportLog;

public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {

	boolean existsByImportId(String importId);

	List<ImportLog> findTop5ByOrderByImportedAtDesc();

}
