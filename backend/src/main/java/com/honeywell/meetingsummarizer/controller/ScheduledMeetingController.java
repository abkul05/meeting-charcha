package com.honeywell.meetingsummarizer.controller;

import com.honeywell.meetingsummarizer.model.ScheduledMeeting;
import com.honeywell.meetingsummarizer.repository.ScheduledMeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
public class ScheduledMeetingController {

    private final ScheduledMeetingRepository scheduledMeetingRepository;

    @Autowired
    public ScheduledMeetingController(ScheduledMeetingRepository scheduledMeetingRepository) {
        this.scheduledMeetingRepository = scheduledMeetingRepository;
    }

    @GetMapping
    public ResponseEntity<List<ScheduledMeeting>> getAllScheduledMeetings() {
        return ResponseEntity.ok(scheduledMeetingRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<ScheduledMeeting> scheduleMeeting(@RequestBody ScheduledMeeting meeting) {
        if (meeting.getTitle() == null || meeting.getScheduledTime() == null || meeting.getUserEmail() == null) {
            return ResponseEntity.badRequest().build();
        }
        meeting.setReminderSent(false);
        ScheduledMeeting savedMeeting = scheduledMeetingRepository.save(meeting);
        return ResponseEntity.ok(savedMeeting);
    }
}
