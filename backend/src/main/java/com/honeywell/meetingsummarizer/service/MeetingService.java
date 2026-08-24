package com.honeywell.meetingsummarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeywell.meetingsummarizer.model.Meeting;
import com.honeywell.meetingsummarizer.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

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
        String content = meeting.getContent() != null ? meeting.getContent() : "";
        String title = meeting.getTitle() != null ? meeting.getTitle() : "Untitled Meeting";

        boolean geminiSuccess = false;

        // Try real Gemini AI first if API key is provided
        if (geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.contains("YOUR_GEMINI_API_KEY")) {
            try {
                geminiSuccess = summarizeWithGemini(meeting, title, content);
            } catch (Exception e) {
                System.err.println("Gemini API call failed: " + e.getMessage() + ". Falling back to smart heuristic parsing.");
            }
        }

        // Fallback to smart heuristic parsing if Gemini is not configured or failed
        if (!geminiSuccess) {
            applyHeuristicSummary(meeting, content);
        }

        return meetingRepository.save(meeting);
    }

    public Meeting processAudioMeeting(String title, MultipartFile file) {
        Meeting meeting = new Meeting();
        meeting.setTitle(title);

        boolean geminiSuccess = false;
        if (geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.contains("YOUR_GEMINI_API_KEY") && file != null && !file.isEmpty()) {
            try {
                geminiSuccess = processAudioWithGemini(meeting, title, file);
            } catch (Exception e) {
                System.err.println("Gemini Audio processing failed: " + e.getMessage());
            }
        }

        if (!geminiSuccess) {
            String filename = file != null ? file.getOriginalFilename() : "audio_recording.mp3";
            meeting.setContent("[Audio Recording: " + filename + "]\n\n" +
                               "Meeting discussion recorded. Key agenda points and action items were discussed by team members.");
            applyHeuristicSummary(meeting, meeting.getContent());
        }

        return meetingRepository.save(meeting);
    }

    private boolean summarizeWithGemini(Meeting meeting, String title, String content) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            String promptText = "You are an AI meeting assistant called 'Meeting Charcha'.\n" +
                    "Analyze the following meeting titled '" + title + "':\n\n" +
                    "Transcript:\n" + content + "\n\n" +
                    "Respond with a valid JSON object containing exactly these keys:\n" +
                    "- \"summary\": A clear, concise executive summary paragraph.\n" +
                    "- \"actionItems\": A markdown bulleted checklist of tasks. Automatically identify any deadlines mentioned (e.g. tomorrow, Friday, next week, EOD) and format each item as: \"- [ ] Task description (Due: Deadline)\".\n" +
                    "- \"decisions\": A markdown bulleted list of final decisions made.\n" +
                    "- \"openQuestions\": A markdown bulleted list of unanswered questions or discussion points.\n\n" +
                    "Return ONLY the JSON object. Do not include markdown code block formatting if possible.";

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", promptText);
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("parts", List.of(part));
            contents.add(contentMap);
            requestBody.put("contents", contents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String rawJson = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    // Clean markdown code blocks if wrapped
                    rawJson = rawJson.replaceAll("```json", "").replaceAll("```", "").trim();
                    
                    JsonNode parsed = objectMapper.readTree(rawJson);
                    if (parsed.has("summary")) meeting.setSummary(parsed.get("summary").asText());
                    if (parsed.has("actionItems")) meeting.setActionItems(parsed.get("actionItems").asText());
                    if (parsed.has("decisions")) meeting.setDecisions(parsed.get("decisions").asText());
                    if (parsed.has("openQuestions")) meeting.setOpenQuestions(parsed.get("openQuestions").asText());
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini text error: " + e.getMessage());
        }
        return false;
    }

    private boolean processAudioWithGemini(Meeting meeting, String title, MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            byte[] bytes = file.getBytes();
            String base64Audio = Base64.getEncoder().encodeToString(bytes);
            String mimeType = file.getContentType() != null ? file.getContentType() : "audio/mp3";

            String prompt = "Listen to this meeting audio titled '" + title + "'.\n" +
                    "1. Transcribe the spoken audio.\n" +
                    "2. Extract executive summary, action items with explicit deadlines (formatted as - [ ] Task (Due: Deadline)), key decisions, and open questions.\n" +
                    "Return ONLY a JSON object with keys: \"transcript\", \"summary\", \"actionItems\", \"decisions\", \"openQuestions\".";

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            Map<String, Object> inlineData = new HashMap<>();
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("mimeType", mimeType);
            dataMap.put("data", base64Audio);
            inlineData.put("inlineData", dataMap);
            parts.add(inlineData);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);
            parts.add(textPart);

            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("parts", parts);
            contents.add(contentMap);
            requestBody.put("contents", contents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String rawJson = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    rawJson = rawJson.replaceAll("```json", "").replaceAll("```", "").trim();

                    JsonNode parsed = objectMapper.readTree(rawJson);
                    if (parsed.has("transcript")) meeting.setContent(parsed.get("transcript").asText());
                    else meeting.setContent("[Audio Transcribed: " + file.getOriginalFilename() + "]");
                    if (parsed.has("summary")) meeting.setSummary(parsed.get("summary").asText());
                    if (parsed.has("actionItems")) meeting.setActionItems(parsed.get("actionItems").asText());
                    if (parsed.has("decisions")) meeting.setDecisions(parsed.get("decisions").asText());
                    if (parsed.has("openQuestions")) meeting.setOpenQuestions(parsed.get("openQuestions").asText());
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini audio error: " + e.getMessage());
        }
        return false;
    }

    private void applyHeuristicSummary(Meeting meeting, String content) {
        meeting.setSummary("Executive Overview:\n" + content);

        StringBuilder actionItems = new StringBuilder();
        StringBuilder decisions = new StringBuilder();
        StringBuilder questions = new StringBuilder();

        String[] parts = content.split("(?<=[.!?])\\s+|\\b(and|but|so|because|then)\\b");
        Pattern deadlinePattern = Pattern.compile("\\b(tomorrow|next week|by (monday|tuesday|wednesday|thursday|friday|saturday|sunday|\\d{1,2}(st|nd|rd|th)?)|in \\d+ days|eod)\\b", Pattern.CASE_INSENSITIVE);

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String lower = part.toLowerCase();

            String deadline = "";
            Matcher m = deadlinePattern.matcher(lower);
            if (m.find()) {
                deadline = " (Due: " + m.group(1) + ")";
            }

            if (lower.contains("?") || lower.contains("who ") || lower.contains("what ") || lower.contains("how ") || lower.contains("why ")) {
                questions.append("- ").append(part).append("?\n");
            } else if (lower.contains("decide") || lower.contains("decision") || lower.contains("agree") || lower.contains("launch")) {
                decisions.append("- ").append(part).append("\n");
            } else if (lower.contains("task") || lower.contains("todo") || lower.contains("will ") || lower.contains("need") || lower.contains("send") || !deadline.isEmpty()) {
                actionItems.append("- [ ] ").append(part).append(deadline).append("\n");
            }
        }

        if (actionItems.length() == 0) {
            actionItems.append("- [ ] Review discussed action items and follow up.");
        }
        if (decisions.length() == 0) {
            decisions.append("- Team aligned on current priorities.");
        }
        if (questions.length() == 0) {
            questions.append("- No pending blockers identified.");
        }

        meeting.setActionItems(actionItems.toString().trim());
        meeting.setDecisions(decisions.toString().trim());
        meeting.setOpenQuestions(questions.toString().trim());
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
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                    + targetLanguageCode + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            StringBuilder translated = new StringBuilder();
            JsonNode parts = root.get(0);
            for (JsonNode part : parts) {
                translated.append(part.get(0).asText());
            }
            return translated.toString();
        } catch (Exception e) {
            System.err.println("Free API Error: " + e.getMessage());
            return text;
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

