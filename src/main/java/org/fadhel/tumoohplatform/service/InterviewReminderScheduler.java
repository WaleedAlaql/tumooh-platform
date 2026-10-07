package org.fadhel.tumoohplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewReminderScheduler {

    private final InterviewService interviewService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Riyadh")
    public void sendDueInterviewReminders() {
        interviewService.sendScheduledInterviewReminders();
    }
}
