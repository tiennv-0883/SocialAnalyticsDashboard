package com.example.socialanalytics.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.socialanalytics.entity.JobRun;
import com.example.socialanalytics.excel.ExcelProcessingException;
import com.example.socialanalytics.job.UpdateSocialMetricsJob;
import com.example.socialanalytics.repository.ImportLogRepository;
import com.example.socialanalytics.repository.JobRunRepository;
import com.example.socialanalytics.repository.PlatformStatsRepository;
import com.example.socialanalytics.repository.PostRepository;
import com.example.socialanalytics.repository.SocialMetricRepository;
import com.example.socialanalytics.repository.UserRepository;
import com.example.socialanalytics.service.PostExcelService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {

	private final UserRepository userRepository;

	private final PostRepository postRepository;

	private final SocialMetricRepository metricRepository;

	private final PostExcelService postExcelService;

	private final JobRunRepository jobRunRepository;

	private final UpdateSocialMetricsJob updateSocialMetricsJob;

	private final PlatformStatsRepository platformStatsRepository;

	private final ImportLogRepository importLogRepository;

	@GetMapping("/")
	public String home() {
		return "redirect:/dashboard";
	}

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
		model.addAttribute("name", principal.getAttribute("name"));
		model.addAttribute("username", principal.getAttribute("username"));
		model.addAttribute("email", principal.getAttribute("email"));
		model.addAttribute("provider", principal.getAttribute("provider"));
		Object userId = principal.getAttribute("userId");
		if (userId instanceof Long id) {
			userRepository.findById(id).ifPresent(user -> model.addAttribute("lastLoginAt", user.getLastLoginAt()));
		}
		model.addAttribute("postCount", postRepository.count());
		model.addAttribute("metricCount", metricRepository.count());
		jobRunRepository.findTopByJobNameAndStatusInOrderByFinishedAtDesc(UpdateSocialMetricsJob.JOB_NAME,
				List.of(JobRun.Status.SUCCESS, JobRun.Status.PARTIAL))
			.ifPresent(run -> model.addAttribute("lastUpdatedAt", run.getFinishedAt()));
		jobRunRepository.findTopByJobNameOrderByStartedAtDesc(UpdateSocialMetricsJob.JOB_NAME)
			.ifPresent(run -> model.addAttribute("lastRun", run));
		model.addAttribute("jobRunning", updateSocialMetricsJob.isRunning());
		model.addAttribute("platformStats", platformStatsRepository.findAllByOrderByPlatform());
		model.addAttribute("recentImports", importLogRepository.findTop5ByOrderByImportedAtDesc());
		return "dashboard";
	}

	@PostMapping("/dashboard/jobs/update-metrics")
	public String runUpdateMetricsJob(RedirectAttributes redirect) {
		boolean started = updateSocialMetricsJob.triggerNow();
		redirect.addFlashAttribute(started ? "jobMessage" : "jobError",
				started ? "Đã bắt đầu cập nhật metrics. Tải lại trang sau vài giây để xem kết quả."
						: "Job đang chạy, vui lòng đợi lần chạy hiện tại xong.");
		return "redirect:/dashboard";
	}

	@PostMapping("/dashboard/import")
	public String importPosts(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
		try {
			redirect.addFlashAttribute("importResult", postExcelService.importPosts(file));
		}
		catch (ExcelProcessingException ex) {
			redirect.addFlashAttribute("importError", ex.getMessage());
		}
		return "redirect:/dashboard";
	}

}
