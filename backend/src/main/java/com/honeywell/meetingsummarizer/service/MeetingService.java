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

        // 3. Smart Heuristic fallback if AI keys are missing
        if (!aiSuccess) {
            applyHeuristicSummary(meeting, content);
        }

        return meetingRepository.save(meeting);
    }

    public Meeting processAudioMeeting(String title, MultipartFile file) {
        Meeting meeting = new Meeting();
        meeting.setTitle(title);

        boolean audioProcessed = false;

        // 1. Try Groq Whisper (Ultra-fast speech-to-text)
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
                               "Audio uploaded successfully. Add your GROQ_API_KEY in Render to enable instant Whisper transcription.");
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
        String[] models = {"llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"};

        for (String model : models) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://api.groq.com/openai/v1/chat/completions";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey.trim());

                String systemPrompt = "You are 'Meeting Charcha', an elite executive AI meeting analyst.\n" +
                        "Analyze the provided transcript and produce a concise executive summary, actionable tasks, decisions, and questions.\n" +
                        "CRITICAL INSTRUCTIONS:\n" +
                        "1. \"summary\": Write a concise, 2-3 sentence executive gist explaining what the discussion was about, key context, and the overall outcome. DO NOT copy or repeat the transcript verbatim.\n" +
                        "2. \"actionItems\": A markdown bulleted checklist of any tasks or commitments mentioned, with deadlines if stated formatted as \"- [ ] Task (Due: Deadline)\". If no tasks, write \"- No action items identified.\"\n" +
                        "3. \"decisions\": Markdown bullet points summarizing the core takeaways, announcements, or agreements.\n" +
                        "4. \"openQuestions\": Markdown bullet points of any questions asked or follow-ups needed.\n\n" +
                        "Return ONLY valid JSON matching this schema: {\"summary\": \"...\", \"actionItems\": \"...\", \"decisions\": \"...\", \"openQuestions\": \"...\"}";

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("temperature", 0.2);

                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));
                messages.add(Map.of("role", "user", "content", "Meeting Title: " + title + "\n\nSpoken Content:\n" + content));
                requestBody.put("messages", messages);

                Map<String, String> responseFormat = new HashMap<>();
                responseFormat.put("type", "json_object");
                requestBody.put("response_format", responseFormat);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String contentJson = root.path("choices").get(0).path("message").path("content").asText();
                    String cleaned = cleanJsonOutput(contentJson);
                    JsonNode parsed = objectMapper.readTree(cleaned);

                    String summary = extractJsonField(parsed, "summary", null);
                    String actionItems = extractJsonField(parsed, "actionItems", "- No specific action items mentioned.");
                    String decisions = extractJsonField(parsed, "decisions", "- Key points noted from conversation.");
                    String openQuestions = extractJsonField(parsed, "openQuestions", "- None identified.");

                    if (summary != null && !summary.trim().isEmpty()) {
                        meeting.setSummary(summary);
                        meeting.setActionItems(actionItems);
                        meeting.setDecisions(decisions);
                        meeting.setOpenQuestions(openQuestions);
                        return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq LLM failed with model " + model + ": " + e.getMessage());
            }
        }
        return false;
    }

    private boolean summarizeWithGemini(Meeting meeting, String title, String content) {
        String[] models = {"gemini-1.5-flash", "gemini-2.0-flash"};

        for (String model : models) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey.trim();

                String promptText = "You are 'Meeting Charcha', an expert AI analyst.\n" +
                        "Analyze this spoken content:\n\n" + content + "\n\n" +
                        "Provide a strict JSON object explaining the actual content with exact keys:\n" +
                        "- \"summary\": A clear summary explaining exactly what was said.\n" +
                        "- \"actionItems\": Markdown checklist with deadlines if mentioned (format: \"- [ ] Task (Due: Deadline)\").\n" +
                        "- \"decisions\": Markdown bullet points of key takeaways and decisions.\n" +
                        "- \"openQuestions\": Markdown bullet points of open questions or follow-ups.\n" +
                        "Return ONLY raw JSON.";

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
                        String cleaned = cleanJsonOutput(rawJson);
                        JsonNode parsed = objectMapper.readTree(cleaned);

                        meeting.setSummary(extractJsonField(parsed, "summary", content));
                        meeting.setActionItems(extractJsonField(parsed, "actionItems", "- [ ] Review discussion."));
                        meeting.setDecisions(extractJsonField(parsed, "decisions", "- Key points noted."));
                        meeting.setOpenQuestions(extractJsonField(parsed, "openQuestions", "- None identified."));
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

                String prompt = "Listen to this audio file and return a JSON object with keys: \"transcript\", \"summary\", \"actionItems\", \"decisions\", \"openQuestions\".";

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
                        String cleaned = cleanJsonOutput(rawJson);
                        JsonNode parsed = objectMapper.readTree(cleaned);

                        if (parsed.has("transcript")) meeting.setContent(parsed.get("transcript").asText());
                        else meeting.setContent("[Audio Transcribed: " + file.getOriginalFilename() + "]");

                        meeting.setSummary(extractJsonField(parsed, "summary", meeting.getContent()));
                        meeting.setActionItems(extractJsonField(parsed, "actionItems", "- [ ] Review discussion."));
                        meeting.setDecisions(extractJsonField(parsed, "decisions", "- Key takeaways captured."));
                        meeting.setOpenQuestions(extractJsonField(parsed, "openQuestions", "- None identified."));
                        return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("Gemini audio failed with model " + model + ": " + e.getMessage());
            }
        }
        return false;
    }

    private String extractJsonField(JsonNode node, String fieldName, String defaultValue) {
        if (node == null || !node.isObject()) return defaultValue;

        String targetNorm = fieldName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String keyNorm = entry.getKey().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

            if (keyNorm.equals(targetNorm) || keyNorm.contains(targetNorm) || targetNorm.contains(keyNorm)) {
                JsonNode f = entry.getValue();
                if (f.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode item : f) {
                        String text = item.asText().trim();
                        if (!text.startsWith("-")) {
                            sb.append("- ");
                        }
                        sb.append(text).append("\n");
                    }
                    return sb.toString().trim();
                } else if (f.isTextual() && !f.asText().trim().isEmpty()) {
                    return f.asText().trim();
                }
            }
        }
        return defaultValue;
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
        meeting.setSummary("Summary of Discussion:\n" + content);

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
            } else if (lower.contains("decide") || lower.contains("decision") || lower.contains("agree") || lower.contains("launch") || lower.contains("announce")) {
                decisions.append("- ").append(part).append("\n");
            } else if (lower.contains("task") || lower.contains("todo") || lower.contains("will ") || lower.contains("need") || lower.contains("send") || !deadline.isEmpty()) {
                actionItems.append("- [ ] ").append(part).append(deadline).append("\n");
            }
        }

        if (actionItems.length() == 0) {
            actionItems.append("- [ ] Review audio transcript and discussed points.");
        }
        if (decisions.length() == 0) {
            decisions.append("- Key discussion points captured from audio.");
        }
        if (questions.length() == 0) {
            questions.append("- No open blockers identified.");
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
