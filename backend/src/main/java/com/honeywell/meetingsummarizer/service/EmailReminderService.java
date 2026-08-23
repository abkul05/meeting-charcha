package com.honeywell.meetingsummarizer.service;

import com.honeywell.meetingsummarizer.model.ScheduledMeeting;
import com.honeywell.meetingsummarizer.repository.ScheduledMeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailReminderService {

    private final ScheduledMeetingRepository scheduledMeetingRepository;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Autowired
    public EmailReminderService(ScheduledMeetingRepository scheduledMeetingRepository) {
        this.scheduledMeetingRepository = scheduledMeetingRepository;
    }

    // Runs every minute
    @Scheduled(fixedRate = 60000)
    public void checkAndSendReminders() {
        LocalDateTime now = LocalDateTime.now();
        // Look for meetings happening in the next 10 to 11 minutes
        LocalDateTime startWindow = now.plusMinutes(9).plusSeconds(30);
        LocalDateTime endWindow = now.plusMinutes(11);

        List<ScheduledMeeting> upcomingMeetings = scheduledMeetingRepository
                .findByScheduledTimeBetweenAndReminderSentFalse(startWindow, endWindow);

        for (ScheduledMeeting meeting : upcomingMeetings) {
            sendEmailReminder(meeting);
            meeting.setReminderSent(true);
            scheduledMeetingRepository.save(meeting);
        }
    }

    private void sendEmailReminder(ScheduledMeeting meeting) {
        if (resendApiKey == null || resendApiKey.contains("YOUR_RESEND_API_KEY")) {
            System.out.println("[MOCK EMAIL] Reminder! Your meeting '" + meeting.getTitle() + "' is starting in 10 minutes!");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.resend.com/emails";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", "onboarding@resend.dev"); // Resend testing domain
            requestBody.put("to", meeting.getUserEmail());
            requestBody.put("subject", "Reminder: " + meeting.getTitle() + " is starting soon!");
            
            String htmlContent = "<h2>Meeting Reminder</h2>" +
                                 "<p>Your meeting <strong>" + meeting.getTitle() + "</strong> is starting in 10 minutes!</p>";
            requestBody.put("html", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            System.out.println("Resend API Email Sent: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("Failed to send email via Resend API: " + e.getMessage());
        }
    }
}
