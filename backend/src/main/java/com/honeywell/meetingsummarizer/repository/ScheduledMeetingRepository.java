package com.honeywell.meetingsummarizer.repository;

import com.honeywell.meetingsummarizer.model.ScheduledMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledMeetingRepository extends JpaRepository<ScheduledMeeting, Long> {
    List<ScheduledMeeting> findByScheduledTimeBetweenAndReminderSentFalse(LocalDateTime start, LocalDateTime end);
}
