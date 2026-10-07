package org.fadhel.tumoohplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.tumoohplatform.Api.ApiException;
import org.fadhel.tumoohplatform.dto.in.InterviewRequest;
import org.fadhel.tumoohplatform.model.Interview;
import org.fadhel.tumoohplatform.model.JobApplication;
import org.fadhel.tumoohplatform.model.User;
import org.fadhel.tumoohplatform.repository.InterviewRepository;
import org.fadhel.tumoohplatform.repository.JobApplicationRepository;
import org.fadhel.tumoohplatform.repository.UserRepository;
import org.fadhel.tumoohplatform.dto.out.InterviewResponse;
import org.fadhel.tumoohplatform.dto.out.InterviewUpcomingResponse;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final EmailService emailService;

    public List<InterviewResponse> getAllInterviews() {
        return interviewRepository.findAll().stream().map(this::mapToInterviewResponse).toList();
    }

    public List<InterviewResponse> getInterviewsByUserId(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new ApiException("User not found"));
        return interviewRepository.findByUserId(userId).stream().map(this::mapToInterviewResponse).toList();
    }

    private InterviewResponse mapToInterviewResponse(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getJobApplication().getId(),
                interview.getUser().getId(),
                interview.getInterviewDate(),
                interview.getReminderSent(),
                interview.getStatus());
    }

    public void createInterview(InterviewRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException("User not found"));

        JobApplication jobApplication = jobApplicationRepository.findById(request.getJobApplicationId())
                .orElseThrow(() -> new ApiException("Job application not found"));

        Interview interview = new Interview();
        interview.setInterviewDate(request.getInterviewDate());
        interview.setStatus(request.getStatus());
        interview.setUser(user);
        interview.setJobApplication(jobApplication);
        interview.setReminderSent(false);

        interviewRepository.save(interview);
    }

    public void updateInterview(Long id, InterviewRequest request) {
        Interview interview = getInterviewById(id);

        if (request.getInterviewDate() != null
                && !request.getInterviewDate().equals(interview.getInterviewDate())) {
            interview.setReminderSent(false);
        }
        interview.setInterviewDate(request.getInterviewDate());
        interview.setStatus(request.getStatus());

        interviewRepository.save(interview);
    }

    public void deleteInterview(Long id) {
        interviewRepository.delete(getInterviewById(id));
    }

    public Interview getInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ApiException("Interview not found"));
    }

    public void updateInterviewStatus(Long id, String status) {
        Interview interview = getInterviewById(id);
        interview.setStatus(status);
        interviewRepository.save(interview);
    }

    public List<InterviewUpcomingResponse> getUpcomingInterviewsByUserId(Long userId, LocalDateTime from, String status) {
        userRepository.findById(userId).orElseThrow(() -> new ApiException("User not found"));
        LocalDateTime fromDate = from != null ? from : LocalDateTime.now();
        return interviewRepository
                .findByUserIdAndInterviewDateGreaterThanEqualOrderByInterviewDateAsc(userId, fromDate)
                .stream()
                .filter(i -> status == null || status.isBlank() || status.equalsIgnoreCase(i.getStatus()))
                .map(i -> new InterviewUpcomingResponse(
                        i.getId(),
                        i.getInterviewDate(),
                        i.getStatus(),
                        i.getJobApplication().getJob().getPosition(),
                        i.getJobApplication().getId()))
                .toList();
    }

    public void sendInterviewReminderEmail(Long id) {
        Interview interview = getInterviewById(id);
        User user = interview.getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ApiException("User email is not available.");
        }
        String jobTitle = interview.getJobApplication().getJob().getPosition();
        String when = interview.getInterviewDate() != null
                ? interview.getInterviewDate().toString()
                : "scheduled time";
        emailService.sendTemplatedEmail(
                user.getEmail(),
                "Tumooh interview reminder",
                "email/interview-reminder",
                Map.of(
                        "emailTitle", "Interview reminder",
                        "emailSubtitle", "Upcoming interview details",
                        "jobTitle", jobTitle,
                        "interviewDate", when,
                        "status", interview.getStatus() != null ? interview.getStatus() : "—"));
        interview.setReminderSent(true);
        interviewRepository.save(interview);
    }

    public void sendScheduledInterviewReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Interview> scheduled = interviewRepository.findByInterviewDateAfterAndStatusIgnoreCase(now, "SCHEDULED");
        for (Interview interview : scheduled) {
            if (Boolean.TRUE.equals(interview.getReminderSent())) {
                continue;
            }
            LocalDateTime interviewDate = interview.getInterviewDate();
            if (interviewDate == null) {
                continue;
            }
            LocalDateTime sendAfter = interviewDate.minusDays(1);
            if (now.isBefore(sendAfter)) {
                continue;
            }
            try {
                sendInterviewReminderEmail(interview.getId());
            } catch (Exception e) {
                log.warn("Scheduled interview reminder failed for id {}: {}", interview.getId(), e.getMessage());
            }
        }
    }
}