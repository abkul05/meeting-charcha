package com.honeywell.meetingsummarizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetingSummarizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeetingSummarizerApplication.class, args);
	}

}
