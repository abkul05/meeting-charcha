package com.honeywell.meetingsummarizer.service;

import com.honeywell.meetingsummarizer.model.Meeting;
import com.honeywell.meetingsummarizer.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;

    @Autowired
    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Meeting> getMeetingById(Long id) {
        return meetingRepository.findById(id);
    }

    public Meeting createAndSummarizeMeeting(Meeting meeting) {
        // Here we would integrate with an AI service like Google Gemini or OpenAI.
        // For demonstration, we'll generate mock structured data based on content.
        // Simulating processing delay
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String content = meeting.getContent() != null ? meeting.getContent() : "";
        
        // ALWAYS provide a summary so the box is never empty
        meeting.setSummary("Meeting Transcript / Overview:\n" + content);

        StringBuilder actionItems = new StringBuilder();
        StringBuilder decisions = new StringBuilder();
        StringBuilder questions = new StringBuilder();

        // Speech-to-text often lacks punctuation. Split by punctuation OR common conjunctions.
        String[] parts = content.split("(?<=[.!?])\\s+|\\b(and|but|so|because|then)\\b");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            String lower = part.toLowerCase();
            
            // Check for deadlines
            String deadline = "";
            java.util.regex.Pattern deadlinePattern = java.util.regex.Pattern.compile("\\b(tomorrow|next week|by (monday|tuesday|wednesday|thursday|friday|saturday|sunday|\\d{1,2}(st|nd|rd|th)?)|in \\d+ days|eod)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = deadlinePattern.matcher(lower);
            if (m.find()) {
                deadline = " (Due: " + m.group(1) + ")";
            }

            if (lower.contains("?") || lower.contains("who ") || lower.contains("what ") || lower.contains("how ") || lower.contains("why ")) {
                questions.append("- ").append(part).append("?\n");
            } else if (lower.contains("decide") || lower.contains("decision") || lower.contains("agree") || lower.contains("launch")) {
                decisions.append("- ").append(part).append("\n");
            } else if (lower.contains("task") || lower.contains("todo") || lower.contains("will ") || lower.contains("need") || lower.contains("send") || !deadline.isEmpty()) {
                // If it has a deadline, it's definitely a task!
                actionItems.append("- [ ] ").append(part).append(deadline).append("\n");
            }
        }

        // Add smart fallbacks so the cards never look completely broken
        if (actionItems.length() == 0) {
            actionItems.append("- [ ] Review meeting notes.\n- [ ] Schedule follow-up if necessary.");
        }
        if (decisions.length() == 0) {
            decisions.append("- General discussion (no specific decisions recorded).");
        }
        if (questions.length() == 0) {
            questions.append("- No open questions recorded.");
        }

        meeting.setActionItems(actionItems.toString().trim());
        meeting.setDecisions(decisions.toString().trim());
        meeting.setOpenQuestions(questions.toString().trim());
        
        return meetingRepository.save(meeting);
    }
    
    public void deleteMeeting(Long id) {
        meetingRepository.deleteById(id);
    }

    private String getLanguageCode(String lang) {
        switch (lang.toLowerCase()) {
            case "spanish": return "es";
            case "french": return "fr";
            case "german": return "de";
            case "hindi": return "hi";
            case "japanese": return "ja";
            default: return "es";
        }
    }

    private String doFreeTranslation(String text, String targetLanguageCode) {
        if (text == null || text.trim().isEmpty()) return text;
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" 
                         + targetLanguageCode + "&dt=t&q=" + java.net.URLEncoder.encode(text, "UTF-8");
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response.getBody());
            
            StringBuilder translated = new StringBuilder();
            com.fasterxml.jackson.databind.JsonNode parts = root.get(0);
            for (com.fasterxml.jackson.databind.JsonNode part : parts) {
                translated.append(part.get(0).asText());
            }
            return translated.toString();
        } catch (Exception e) {
            System.err.println("Free API Error: " + e.getMessage());
            return "[Translation Failed] " + text;
        }
    }

    public Meeting translateMeeting(Long id, String targetLanguage) {
        Meeting meeting = meetingRepository.findById(id).orElseThrow();
        String langCode = getLanguageCode(targetLanguage);
        
        meeting.setSummary(doFreeTranslation(meeting.getSummary(), langCode));
        meeting.setActionItems(doFreeTranslation(meeting.getActionItems(), langCode));
        meeting.setDecisions(doFreeTranslation(meeting.getDecisions(), langCode));
        meeting.setOpenQuestions(doFreeTranslation(meeting.getOpenQuestions(), langCode));
        
        return meetingRepository.save(meeting);
    }
}
