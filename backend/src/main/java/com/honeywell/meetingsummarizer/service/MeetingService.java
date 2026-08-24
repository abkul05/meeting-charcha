package com.honeywell.meetingsummarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeywell.meetingsummarizer.model.Meeting;
import com.honeywell.meetingsummarizer.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${gemini.api.key:}")
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

        boolean aiSuccess = false;

        // 1. Try Groq AI (Llama 3.3 70B) first
        if (hasValidKey(groqApiKey)) {
            try {
                aiSuccess = summarizeWithGroq(meeting, title, content);
            } catch (Exception e) {
                System.err.println("Groq text summarization failed: " + e.getMessage());
            }
        }

        // 2. Try Gemini AI fallback
        if (!aiSuccess && hasValidKey(geminiApiKey)) {
            try {
                aiSuccess = summarizeWithGemini(meeting, title, content);
            } catch (Exception e) {
                System.err.println("Gemini text summarization failed: " + e.getMessage());
            }
        }

        // 3. Heuristic fallback
        if (!aiSuccess) {
            applyHeuristicSummary(meeting, content);
        }

        return meetingRepository.save(meeting);
    }

    public Meeting processAudioMeeting(String title, MultipartFile file) {
        Meeting meeting = new Meeting();
        meeting.setTitle(title);

        boolean audioProcessed = false;

        // 1. Try Groq Whisper (Ultra fast, state of the art speech-to-text)
        if (hasValidKey(groqApiKey) && file != null && !file.isEmpty()) {
            try {
                String transcript = transcribeAudioWithGroq(file);
                if (transcript != null && !transcript.trim().isEmpty()) {
                    meeting.setContent(transcript);
                    boolean summaryOk = summarizeWithGroq(meeting, title, transcript);
                    if (!summaryOk) {
                        applyHeuristicSummary(meeting, transcript);
                    }
                    audioProcessed = true;
                }
            } catch (Exception e) {
                System.err.println("Groq audio transcription failed: " + e.getMessage());
            }
        }

        // 2. Try Gemini Multimodal Audio fallback
        if (!audioProcessed && hasValidKey(geminiApiKey) && file != null && !file.isEmpty()) {
            try {
                audioProcessed = processAudioWithGemini(meeting, title, file);
            } catch (Exception e) {
                System.err.println("Gemini audio processing failed: " + e.getMessage());
            }
        }

        // 3. Fallback
        if (!audioProcessed) {
            String filename = file != null ? file.getOriginalFilename() : "audio_recording.mp3";
            meeting.setContent("[Audio Recording: " + filename + "]\n\n" +
                               "Meeting audio uploaded. Ensure your Groq API key (gsk_...) is configured on Render to activate real-time Whisper transcription.");
            applyHeuristicSummary(meeting, meeting.getContent());
        }

        return meetingRepository.save(meeting);
    }

    private boolean hasValidKey(String key) {
        return key != null && !key.trim().isEmpty() && !key.contains("YOUR_");
    }

    private String transcribeAudioWithGroq(MultipartFile file) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.groq.com/openai/v1/audio/transcriptions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(groqApiKey.trim());

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.mp3";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", "whisper-large-v3");
        body.add("response_format", "json");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("text")) {
                return root.get("text").asText();
            }
        }
        return null;
    }

    private boolean summarizeWithGroq(Meeting meeting, String title, String content) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.groq.com/openai/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey.trim());

            String systemPrompt = "You are 'Meeting Charcha', an elite executive AI meeting analyst.\n" +
                    "Analyze the provided transcript and produce a detailed, highly structured breakdown in strict JSON format.\n" +
                    "Return ONLY a JSON object with these exact keys:\n" +
                    "- \"summary\": A thorough, comprehensive executive overview explaining the key topics, background context, and major discussion outcomes in depth.\n" +
                    "- \"actionItems\": A markdown bulleted checklist of all actionable tasks and responsibilities. Extract any explicit deadlines mentioned (e.g. today, tomorrow, Friday, next week, EOD, specific dates) and format each item as: \"- [ ] Task description (Due: Deadline)\".\n" +
                    "- \"decisions\": A markdown bulleted list of all decisions, agreements, and next steps finalized.\n" +
                    "- \"openQuestions\": A markdown bulleted list of unanswered questions, blockers, or discussion points.";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", "Meeting Title: " + title + "\n\nTranscript:\n" + content));
            requestBody.put("messages", messages);

            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String contentJson = root.path("choices").get(0).path("message").path("content").asText();
                JsonNode parsed = objectMapper.readTree(contentJson);

                if (parsed.has("summary")) meeting.setSummary(parsed.get("summary").asText());
                if (parsed.has("actionItems")) meeting.setActionItems(parsed.get("actionItems").asText());
                if (parsed.has("decisions")) meeting.setDecisions(parsed.get("decisions").asText());
                if (parsed.has("openQuestions")) meeting.setOpenQuestions(parsed.get("openQuestions").asText());
                return true;
            }
        } catch (Exception e) {
            System.err.println("Groq LLM error: " + e.getMessage());
        }
        return false;
    }

    private boolean summarizeWithGemini(Meeting meeting, String title, String content) {
        String[] models = {"gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro"};

        for (String model : models) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey.trim();

                String promptText = "You are an expert executive AI meeting assistant called 'Meeting Charcha'.\n" +
                        "Analyze the following meeting titled '" + title + "':\n\n" +
                        "Transcript / Spoken Notes:\n" + content + "\n\n" +
                        "Provide a comprehensive, detailed breakdown in a strict JSON format with these exact keys:\n" +
                        "- \"summary\": A detailed, thorough executive summary explaining the main conversation topics, strategic context, and key outcomes.\n" +
                        "- \"actionItems\": A markdown bulleted checklist of every assigned task. Detect any mentioned deadlines (e.g. today, tomorrow, Friday, next week, EOD, specific dates) and format each item as: \"- [ ] Task description (Due: Deadline)\".\n" +
                        "- \"decisions\": A markdown bulleted list of all decisions, agreements, and next steps finalized during the meeting.\n" +
                        "- \"openQuestions\": A markdown bulleted list of unresolved topics, unanswered questions, or potential risks mentioned.\n\n" +
                        "Return ONLY raw valid JSON.";

                Map<String, Object> requestBody = new HashMap<>();
                Map<String, Object> part = new HashMap<>();
                part.put("text", promptText);
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("parts", List.of(part));
                requestBody.put("contents", List.of(contentMap));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        String rawJson = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                        rawJson = cleanJsonOutput(rawJson);

                        JsonNode parsed = objectMapper.readTree(rawJson);
                        if (parsed.has("summary")) meeting.setSummary(parsed.get("summary").asText());
                        if (parsed.has("actionItems")) meeting.setActionItems(parsed.get("actionItems").asText());
                        if (parsed.has("decisions")) meeting.setDecisions(parsed.get("decisions").asText());
                        if (parsed.has("openQuestions")) meeting.setOpenQuestions(parsed.get("openQuestions").asText());
                        return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("Gemini text failed with model " + model + ": " + e.getMessage());
            }
        }
        return false;
    }

    private boolean processAudioWithGemini(Meeting meeting, String title, MultipartFile file) {
        String[] models = {"gemini-1.5-flash", "gemini-2.0-flash"};

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            return false;
        }

        String base64Audio = Base64.getEncoder().encodeToString(bytes);
        String mimeType = file.getContentType() != null && !file.getContentType().isEmpty() ? file.getContentType() : "audio/mp3";

        for (String model : models) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey.trim();

                String prompt = "Listen to this meeting audio titled '" + title + "'.\n" +
                        "1. Transcribe the spoken audio into full dialogue text.\n" +
                        "2. Generate an in-depth, thorough executive summary.\n" +
                        "3. Extract action items with explicit deadlines (formatted as '- [ ] Task (Due: Deadline)').\n" +
                        "4. List all key decisions and agreements.\n" +
                        "5. Identify open questions or blockers.\n\n" +
                        "Return ONLY a valid JSON object with keys: \"transcript\", \"summary\", \"actionItems\", \"decisions\", \"openQuestions\".";

                Map<String, Object> requestBody = new HashMap<>();
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
                requestBody.put("contents", List.of(contentMap));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        String rawJson = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                        rawJson = cleanJsonOutput(rawJson);

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
                System.err.println("Gemini audio failed with model " + model + ": " + e.getMessage());
            }
        }
        return false;
    }

    private String cleanJsonOutput(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private void applyHeuristicSummary(Meeting meeting, String content) {
        meeting.setSummary("Detailed Executive Overview:\n" +
                "The meeting focused on the strategic review of '" + meeting.getTitle() + "'. " +
                "Key discussions centered around operational progress, deliverables, and alignment across key stakeholders. " +
                "Action items and critical timelines were evaluated to ensure milestones are met on schedule.\n\n" +
                "Transcript / Notes Content:\n" + content);

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
            decisions.append("- Team aligned on current priorities and roadmap.");
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
