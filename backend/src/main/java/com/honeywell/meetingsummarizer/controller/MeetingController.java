package com.honeywell.meetingsummarizer.controller;

import com.honeywell.meetingsummarizer.model.Meeting;
import com.honeywell.meetingsummarizer.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*") // Allow requests from any frontend origin (e.g. Vercel, localhost)
public class MeetingController {

    private final MeetingService meetingService;

    @Autowired
    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public ResponseEntity<List<Meeting>> getAllMeetings() {
        return ResponseEntity.ok(meetingService.getAllMeetings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Meeting> getMeetingById(@PathVariable Long id) {
        return meetingService.getMeetingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Meeting> createMeeting(@RequestBody Meeting meeting) {
        if (meeting.getTitle() == null || meeting.getContent() == null) {
            return ResponseEntity.badRequest().build();
        }
        Meeting savedMeeting = meetingService.createAndSummarizeMeeting(meeting);
        return ResponseEntity.ok(savedMeeting);
    }

    @PostMapping("/audio")
    public ResponseEntity<Meeting> uploadAudio(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("title") String title) {
        
        if (file.isEmpty() || title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Simulate audio transcription
        Meeting meeting = new Meeting();
        meeting.setTitle(title);
        meeting.setContent("[Transcribed from audio file: " + file.getOriginalFilename() + "]\n\n" +
                           "This is a simulated transcription of the uploaded audio file. " +
                           "In a real environment, this audio would be sent to OpenAI Whisper or Google Gemini API.");

        Meeting savedMeeting = meetingService.createAndSummarizeMeeting(meeting);
        return ResponseEntity.ok(savedMeeting);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        if (meetingService.getMeetingById(id).isPresent()) {
            meetingService.deleteMeeting(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/translate")
    public ResponseEntity<Meeting> translateMeeting(@PathVariable Long id, @RequestParam("target") String targetLanguage) {
        if (meetingService.getMeetingById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Meeting translatedMeeting = meetingService.translateMeeting(id, targetLanguage);
        return ResponseEntity.ok(translatedMeeting);
    }
}
